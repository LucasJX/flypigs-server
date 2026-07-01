package com.flypigs.ntfyapp.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.flypigs.ntfyapp.R
import com.flypigs.ntfyapp.data.local.SecureStorage
import com.flypigs.ntfyapp.data.local.entity.ServerEntity
import com.flypigs.ntfyapp.data.local.entity.TopicEntity
import com.flypigs.ntfyapp.data.remote.NtfyApi
import com.flypigs.ntfyapp.data.remote.NtfyMessage
import com.flypigs.ntfyapp.data.remote.NtfyWebSocket
import com.flypigs.ntfyapp.data.repository.MessageRepository
import com.flypigs.ntfyapp.data.repository.ServerRepository
import com.flypigs.ntfyapp.data.repository.TopicRepository
import com.flypigs.ntfyapp.ui.MainActivity
import com.flypigs.ntfyapp.util.UrlRewriter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NtfyService : Service() {

    companion object {
        private const val TAG = "NtfyService"
        private const val CHANNEL_ID = "ntfy_service"
        private const val MESSAGE_CHANNEL_ID = "ntfy_messages"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.flypigs.ntfyapp.START"
        const val ACTION_STOP = "com.flypigs.ntfyapp.STOP"
        const val ACTION_RECONNECT_ALL = "com.flypigs.ntfyapp.RECONNECT_ALL"

        /**
         * 启动服务 — 不再通过 Intent 传递敏感信息
         * Service 启动后通过 Hilt 注入的 Repository 从 Room + SecureStorage 读取凭据
         */
        fun start(context: Context) {
            val intent = Intent(context, NtfyService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, NtfyService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @Inject lateinit var messageRepository: MessageRepository
    @Inject lateinit var serverRepository: ServerRepository
    @Inject lateinit var topicRepository: TopicRepository
    @Inject lateinit var ntfyApi: NtfyApi

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connections = mutableMapOf<String, NtfyWebSocket>()  // key: "serverId:topicName"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createServiceNotification())
                connectAllEnabledTopics()
            }
            ACTION_STOP -> {
                disconnectAll()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_RECONNECT_ALL -> {
                startForeground(NOTIFICATION_ID, createServiceNotification())
                disconnectAll()
                connectAllEnabledTopics()
            }
            else -> {
                // 系统重建 Service 时（START_STICKY），重新连接
                startForeground(NOTIFICATION_ID, createServiceNotification())
                connectAllEnabledTopics()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        disconnectAll()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved — scheduling restart in 1s")
        val restartIntent = Intent(this, NtfyService::class.java).apply {
            action = ACTION_START
        }
        val pendingIntent = PendingIntent.getService(
            this, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    /**
     * 从 Room 读取所有 enabled Topic，为每个建立 WebSocket 连接
     * 密码从 SecureStorage 读取，不再从 Intent/SharedPreferences 传入
     */
    private fun connectAllEnabledTopics() {
        serviceScope.launch {
            try {
                val servers = serverRepository.getAllServersSuspend()
                val enabledTopics = topicRepository.getAllEnabledTopicsSuspend()

                if (enabledTopics.isEmpty()) {
                    Log.w(TAG, "No enabled topics to connect")
                    return@launch
                }

                for (topic in enabledTopics) {
                    val server = servers.find { it.id == topic.serverId }
                    if (server == null) {
                        Log.w(TAG, "Server not found for topic ${topic.name}, skipping")
                        continue
                    }

                    val password = SecureStorage.getPassword(server.id)
                    val key = "${server.id}:${topic.name}"

                    // 检查是否已有活跃连接（断开的连接需要清理）
                    val existing = connections[key]
                    if (existing == null || !existing.isConnected) {
                        // 清理断开的连接
                        existing?.disconnect()

                        val ws = NtfyWebSocket(
                            serverUrl = server.url,
                            topic = topic.name,
                            username = server.username,
                            password = password,
                            token = server.token,
                            client = NtfyWebSocket.sharedClient,
                            onMessage = { message ->
                                serviceScope.launch {
                                    handleIncomingMessage(message, server.id, topic.name)
                                }
                            },
                            onConnectionChanged = { connected ->
                                serviceScope.launch {
                                    serverRepository.updateConnectionStatus(server.id, connected)
                                }
                                updateServiceNotification(connected)
                            }
                        )
                        connections[key] = ws
                        ws.connect()
                        Log.d(TAG, "Connecting to ${server.url}/${topic.name}")
                    } else {
                        Log.d(TAG, "Already connected to ${server.url}/${topic.name}, re-syncing history")
                    }

                    // 始终拉取历史消息（已连接时也拉）— 补偿 WebSocket 断连期间漏掉的消息
                    // 用 since=lastLocalTs-60s 避免边界遗漏；insertMessage 唯一约束会跳过重复
                    serviceScope.launch {
                        syncHistoryForTopic(server, topic)
                    }
                }

                Log.d(TAG, "Total connections: ${connections.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect topics", e)
            }
        }
    }

    /**
     * 拉取历史消息 — 补偿 WebSocket 断连期间漏掉的消息
     * 从本地数据库最后一条消息的时间戳开始拉取
     */
    private suspend fun syncHistoryForTopic(server: ServerEntity, topic: TopicEntity) {
        try {
            val password = SecureStorage.getPassword(server.id)

            // 获取本地最后一条消息的时间戳（秒）
            val lastTimestamp = messageRepository.getLatestMessageTimestampForTopic(topic.name)
            // 从最后一条消息前 1 分钟开始拉取，避免边界遗漏
            val since = if (lastTimestamp > 0) lastTimestamp - 60 else 0

            Log.d(TAG, "Syncing history for ${topic.name} since=$since (lastLocal=$lastTimestamp)")

            val history = ntfyApi.fetchHistory(
                serverUrl = server.url,
                topic = topic.name,
                since = since,
                username = server.username,
                password = password
            )

            if (history.isEmpty()) {
                Log.d(TAG, "No historical messages for ${topic.name}")
                return
            }

            var inserted = 0
            for (msg in history) {
                try {
                    // 改写 attachment URL — 用当前 server 的 URL 替换 base
                    val rewritten = if (msg.attachment != null) {
                        val newUrl = UrlRewriter.rewriteAttachmentUrl(msg.attachment.url, server.url)
                            ?: msg.attachment.url
                        if (newUrl != msg.attachment.url) {
                            msg.copy(attachment = msg.attachment.copy(url = newUrl))
                        } else msg
                    } else msg
                    messageRepository.insertMessage(rewritten)
                    inserted++
                } catch (e: Exception) {
                    // 重复消息会触发唯一约束冲突，静默跳过
                    Log.d(TAG, "Skipping duplicate message: ${msg.id}")
                }
            }

            Log.d(TAG, "History sync complete for ${topic.name}: $inserted/${history.size} inserted")
        } catch (e: Exception) {
            Log.e(TAG, "History sync failed for ${topic.name}", e)
        }
    }

    private fun disconnectAll() {
        connections.values.forEach { it.disconnect() }
        connections.clear()
    }

    private suspend fun handleIncomingMessage(ntfyMessage: NtfyMessage, serverId: String, topicName: String) {
        try {
            // 改写 attachment URL — 用当前 server 的 URL 替换 base
            // 让 APK 不依赖 ntfy 服务端的 base-url 配置（适配任意部署）
            val server = serverRepository.getServerById(serverId)
            val rewritten = if (server != null && ntfyMessage.attachment != null) {
                val newUrl = UrlRewriter.rewriteAttachmentUrl(ntfyMessage.attachment.url, server.url)
                    ?: ntfyMessage.attachment.url
                if (newUrl != ntfyMessage.attachment.url) {
                    ntfyMessage.copy(
                        attachment = ntfyMessage.attachment.copy(url = newUrl)
                    )
                } else ntfyMessage
            } else ntfyMessage

            val entity = messageRepository.insertMessage(rewritten)
            showMessageNotification(rewritten, entity.category)
            Log.d(TAG, "Message saved: ${rewritten.id} from $topicName${if (rewritten.attachment != null) " [attachment: ${rewritten.attachment.url}]" else ""}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save message", e)
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // 不删除旧渠道 — Android 禁止删除正在被前台服务使用的渠道
        // createNotificationChannel 是幂等的，已存在则跳过

        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "ntfy 服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "ntfy 后台服务运行状态"
            setShowBadge(false)
        }
        manager.createNotificationChannel(serviceChannel)

        val messageChannel = NotificationChannel(
            MESSAGE_CHANNEL_ID,
            "ntfy 消息",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "ntfy 推送消息通知"
            enableVibration(true)
            enableLights(true)
            lightColor = android.graphics.Color.RED
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            setShowBadge(true)
        }
        manager.createNotificationChannel(messageChannel)
    }

    private fun createServiceNotification(connected: Boolean = false): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (connected) "已连接 (${connections.size} topic)" else "连接中..."

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ntfy 通知服务")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateServiceNotification(connected: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createServiceNotification(connected))
    }

    private fun showMessageNotification(message: NtfyMessage, category: String) {
        val pendingIntent = PendingIntent.getActivity(
            this, message.id.hashCode(),
            Intent(this, MainActivity::class.java).apply {
                putExtra("message_id", message.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 根据优先级设置不同的提醒方式
        val priority = message.priority ?: 3
        val isHighPriority = priority >= 4

        val notification = NotificationCompat.Builder(this, MESSAGE_CHANNEL_ID)
            .setContentTitle(message.title ?: message.topic)
            .setContentText(message.message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .apply {
                if (isHighPriority) {
                    // 高优先级：声音 + 振动 + heads-up
                    setDefaults(NotificationCompat.DEFAULT_ALL)
                    setVibrate(longArrayOf(0, 250, 100, 250))
                } else {
                    // 普通：仅声音
                    setDefaults(NotificationCompat.DEFAULT_SOUND)
                }
            }
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(message.id.hashCode(), notification)
    }
}
