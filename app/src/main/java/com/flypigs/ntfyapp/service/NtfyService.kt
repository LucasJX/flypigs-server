package com.flypigs.ntfyapp.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.flypigs.ntfyapp.R
import com.flypigs.ntfyapp.data.remote.NtfyMessage
import com.flypigs.ntfyapp.data.remote.NtfyWebSocket
import com.flypigs.ntfyapp.data.repository.MessageRepository
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

        const val PREF_SERVER_URL = "server_url"
        const val PREF_TOPIC = "topic"
        const val PREF_USERNAME = "username"
        const val PREF_PASSWORD = "password"
        const val DEFAULT_SERVER = "https://ntfy.sh"
        const val DEFAULT_TOPIC = "test"

        fun start(context: Context, serverUrl: String, topic: String, username: String? = null, password: String? = null) {
            val intent = Intent(context, NtfyService::class.java).apply {
                action = ACTION_START
                putExtra(PREF_SERVER_URL, serverUrl)
                putExtra(PREF_TOPIC, topic)
                putExtra(PREF_USERNAME, username)
                putExtra(PREF_PASSWORD, password)
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

    @Inject
    lateinit var repository: MessageRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: NtfyWebSocket? = null
    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("ntfy_prefs", MODE_PRIVATE)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val serverUrl = intent.getStringExtra(PREF_SERVER_URL) ?: DEFAULT_SERVER
                val topic = intent.getStringExtra(PREF_TOPIC) ?: DEFAULT_TOPIC
                val username = intent.getStringExtra(PREF_USERNAME)
                val password = intent.getStringExtra(PREF_PASSWORD)

                prefs.edit().apply {
                    putString(PREF_SERVER_URL, serverUrl)
                    putString(PREF_TOPIC, topic)
                    putString(PREF_USERNAME, username)
                    putString(PREF_PASSWORD, password)
                    apply()
                }

                startForeground(NOTIFICATION_ID, createServiceNotification())
                connectWebSocket(serverUrl, topic, username, password)
            }
            ACTION_STOP -> {
                disconnectWebSocket()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                val serverUrl = prefs.getString(PREF_SERVER_URL, DEFAULT_SERVER) ?: DEFAULT_SERVER
                val topic = prefs.getString(PREF_TOPIC, DEFAULT_TOPIC) ?: DEFAULT_TOPIC
                val username = prefs.getString(PREF_USERNAME, null)
                val password = prefs.getString(PREF_PASSWORD, null)

                startForeground(NOTIFICATION_ID, createServiceNotification())
                connectWebSocket(serverUrl, topic, username, password)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        disconnectWebSocket()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved — scheduling restart in 1s")
        val restartIntent = Intent(this, NtfyService::class.java).apply {
            action = ACTION_START
            putExtra(PREF_SERVER_URL, prefs.getString(PREF_SERVER_URL, DEFAULT_SERVER))
            putExtra(PREF_TOPIC, prefs.getString(PREF_TOPIC, DEFAULT_TOPIC))
            putExtra(PREF_USERNAME, prefs.getString(PREF_USERNAME, null))
            putExtra(PREF_PASSWORD, prefs.getString(PREF_PASSWORD, null))
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

    private fun connectWebSocket(serverUrl: String, topic: String, username: String?, password: String?) {
        disconnectWebSocket()

        webSocket = NtfyWebSocket(
            serverUrl = serverUrl,
            topic = topic,
            username = username,
            password = password,
            onMessage = { message ->
                serviceScope.launch {
                    handleIncomingMessage(message)
                }
            },
            onConnectionChanged = { connected ->
                updateServiceNotification(connected)
            }
        )
        webSocket?.connect()
    }

    private fun disconnectWebSocket() {
        webSocket?.disconnect()
        webSocket = null
    }

    private suspend fun handleIncomingMessage(ntfyMessage: NtfyMessage) {
        try {
            val entity = repository.insertMessage(ntfyMessage)
            showMessageNotification(ntfyMessage, entity.category)
            Log.d(TAG, "Message saved: ${ntfyMessage.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save message", e)
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

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
        }
        manager.createNotificationChannel(messageChannel)
    }

    private fun createServiceNotification(connected: Boolean = false): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (connected) "已连接" else "连接中..."

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
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, MESSAGE_CHANNEL_ID)
            .setContentTitle(message.title ?: message.topic)
            .setContentText(message.message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(message.id.hashCode(), notification)
    }
}
