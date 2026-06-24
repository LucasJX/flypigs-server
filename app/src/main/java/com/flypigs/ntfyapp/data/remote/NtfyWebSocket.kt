package com.flypigs.ntfyapp.data.remote

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class NtfyWebSocket(
    private val serverUrl: String,
    private val topic: String,
    private val username: String? = null,
    private val password: String? = null,
    private val onMessage: (NtfyMessage) -> Unit,
    private val onConnectionChanged: ((Boolean) -> Unit)? = null
) {
    companion object {
        private const val TAG = "NtfyWebSocket"
        private val RECONNECT_DELAYS = longArrayOf(5000, 10000, 15000, 30000, 60000)
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())
    private var webSocket: WebSocket? = null
    private var reconnectAttempt = 0
    private var isConnecting = false
    var isConnected = false
        private set

    fun connect() {
        if (isConnecting || isConnected) return
        isConnecting = true

        val wsUrl = serverUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/')

        val urlBuilder = StringBuilder("$wsUrl/$topic/ws")
        // ntfy 1.33+ supports ?since= for message history
        // We'll just connect to the live stream for now

        val requestBuilder = Request.Builder()
            .url(urlBuilder.toString())

        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", Credentials.basic(username, password))
        }

        Log.d(TAG, "Connecting to $wsUrl/$topic/ws")

        webSocket = client.newWebSocket(requestBuilder.build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to $topic")
                isConnecting = false
                isConnected = true
                reconnectAttempt = 0
                handler.post { onConnectionChanged?.invoke(true) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = gson.fromJson(text, NtfyMessage::class.java)
                    if (message.event == "message") {
                        Log.d(TAG, "Received message: ${message.title ?: message.message.take(50)}")
                        onMessage(message)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse message: $text", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Connection failed", t)
                isConnecting = false
                isConnected = false
                handler.post { onConnectionChanged?.invoke(false) }
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Connection closed: $code $reason")
                isConnecting = false
                isConnected = false
                handler.post { onConnectionChanged?.invoke(false) }
            }
        })
    }

    fun disconnect() {
        handler.removeCallbacksAndMessages(null)
        webSocket?.close(1000, "App closed")
        webSocket = null
        isConnecting = false
        isConnected = false
    }

    private fun scheduleReconnect() {
        val delay = RECONNECT_DELAYS[reconnectAttempt.coerceAtMost(RECONNECT_DELAYS.size - 1)]
        Log.d(TAG, "Reconnecting in ${delay / 1000}s (attempt ${reconnectAttempt + 1})")

        handler.postDelayed({
            reconnectAttempt++
            connect()
        }, delay)
    }
}
