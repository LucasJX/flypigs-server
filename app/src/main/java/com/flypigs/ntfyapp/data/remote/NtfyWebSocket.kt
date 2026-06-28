package com.flypigs.ntfyapp.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.TimeUnit

class NtfyWebSocket(
    private val serverUrl: String,
    private val topic: String,
    private val username: String? = null,
    private val password: String? = null,
    private val token: String? = null,
    private val client: OkHttpClient,                              // 外部注入共享 client
    private val onMessage: (NtfyMessage) -> Unit,
    private val onConnectionChanged: ((Boolean) -> Unit)? = null,
    private val reconnectScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) // 替代 Handler
) {
    companion object {
        private const val TAG = "NtfyWebSocket"
        private val RECONNECT_DELAYS = longArrayOf(5000, 10000, 15000, 30000, 60000)
        private const val MAX_RECONNECT_ATTEMPTS = 20

        /** 共享 OkHttpClient — 避免每个连接新建一个 client */
        val sharedClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build()
        }
    }

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var reconnectAttempt = 0
    private var isConnecting = false
    private var reconnectJob: Job? = null          // 跟踪当前重连任务
    var isConnected = false
        private set

    fun connect() {
        if (isConnecting || isConnected) return
        isConnecting = true

        val wsUrl = serverUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/')

        val requestBuilder = Request.Builder()
            .url("$wsUrl/$topic/ws")

        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", Credentials.basic(username, password))
        } else if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        Log.d(TAG, "Connecting to $wsUrl/$topic/ws")

        webSocket = client.newWebSocket(requestBuilder.build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to $topic")
                isConnecting = false
                isConnected = true
                reconnectAttempt = 0
                reconnectScope.launch { onConnectionChanged?.invoke(true) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = parseNtfyMessage(text)
                    if (message != null && message.event == "message") {
                        Log.d(TAG, "Received message: ${message.title ?: message.message?.take(50) ?: "(no content)"}")
                        onMessage(message)
                    } else if (message == null) {
                        Log.w(TAG, "Skipping unparseable message: ${text.take(200)}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error processing message: ${text.take(200)}", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Connection failed", t)
                isConnecting = false
                isConnected = false
                reconnectScope.launch { onConnectionChanged?.invoke(false) }
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Connection closed: $code $reason")
                isConnecting = false
                isConnected = false
                reconnectScope.launch { onConnectionChanged?.invoke(false) }
            }
        })
    }

    /**
     * 容错 JSON 解析 — 字段缺失或类型不匹配时返回默认值而非抛异常
     */
    private fun parseNtfyMessage(text: String): NtfyMessage? {
        return try {
            gson.fromJson(text, NtfyMessage::class.java)
        } catch (e: JsonSyntaxException) {
            Log.w(TAG, "JSON syntax error, attempting partial parse")
            try {
                val json = JsonParser.parseString(text).asJsonObject
                NtfyMessage(
                    id = json.get("id")?.asString ?: UUID.randomUUID().toString(),
                    event = json.get("event")?.asString ?: "message",
                    topic = json.get("topic")?.asString ?: this.topic,
                    title = json.get("title")?.asString,
                    message = json.get("message")?.asString ?: "",
                    priorityRaw = json.get("priority"),
                    tags = json.get("tags")?.asJsonArray?.mapNotNull { it?.asString },
                    time = json.get("time")?.asLong ?: System.currentTimeMillis()
                )
            } catch (e2: Exception) {
                Log.e(TAG, "Partial parse also failed", e2)
                null
            }
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()          // 只取消当前重连 Job，不取消整个 scope
        reconnectJob = null
        webSocket?.close(1000, "App closed")
        webSocket = null
        isConnecting = false
        isConnected = false
    }

    private fun scheduleReconnect() {
        if (reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached ($MAX_RECONNECT_ATTEMPTS), stopping")
            reconnectScope.launch { onConnectionChanged?.invoke(false) }
            return
        }
        val delayMs = RECONNECT_DELAYS[reconnectAttempt.coerceAtMost(RECONNECT_DELAYS.size - 1)]
        Log.d(TAG, "Reconnecting in ${delayMs / 1000}s (attempt ${reconnectAttempt + 1}/$MAX_RECONNECT_ATTEMPTS)")

        reconnectJob = reconnectScope.launch {
            delay(delayMs)
            reconnectAttempt++
            connect()
        }
    }

    /**
     * 解析 ntfy priority，支持整数和字符串格式
     * ntfy 格式: 1-5 或 "min"/"low"/"default"/"high"/"urgent"
     */
    private fun parsePriority(priorityElement: com.google.gson.JsonElement?): Int {
        if (priorityElement == null) return 3

        // 尝试整数
        try {
            val intValue = priorityElement.asInt
            if (intValue in 1..5) return intValue
        } catch (_: Exception) {}

        // 尝试字符串
        return when (priorityElement.asString?.lowercase()) {
            "min" -> 1
            "low" -> 2
            "default" -> 3
            "high" -> 4
            "urgent" -> 5
            else -> 3
        }
    }
}
