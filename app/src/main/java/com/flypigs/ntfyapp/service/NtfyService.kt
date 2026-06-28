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
import com.flypigs.ntfyapp.data.local.entity.TopicEntity
import com.flypigs.ntfyapp.data.remote.NtfyMessage
import com.flypigs.ntfyapp.data.remote.NtfyWebSocket
import com.flypigs.ntfyapp.data.repository.MessageRepository
import com.flypigs.ntfyapp.data.repository.ServerRepository
import com.flypigs.ntfyapp.data.repository.TopicRepository
import com.flypigs.ntfyapp.ui.MainActivity
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

                    if (connections.containsKey(key)) {
                        continue  // 已连接，跳过
                    }

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
                }

                Log.d(TAG, "Total connections: ${connections.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect topics", e)
            }
        }
    }

    private fun disconnectAll() {
        connections.values.forEach { it.disconnect() }
        connections.clear()
    }

    private suspend fun handleIncomingMessage(ntfyMessage: NtfyMessage, serverId: String, topicName: String) {
        try {
            val entity = messageRepository.insertMessage(ntfyMessage)
            showMessageNotification(ntfyMessage, entity.category)
            Log.d(TAG, "Message saved: ${ntfyMessage.id} from $topicName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save message", e)
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // 删除旧渠道（强制重建，确保设置生效）
        manager.deleteNotificationChannel(CHANNEL_ID)
        manager.deleteNotificationChannel(MESSAGE_CHANNEL_ID)

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
