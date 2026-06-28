package com.flypigs.ntfyapp.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.use
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NtfyApi @Inject constructor() {

    companion object {
        private const val TAG = "NtfyApi"
        private val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)  // 历史消息拉取可能较慢
            .build()
        private val gson = Gson()
    }

    /**
     * 测试服务器连接（Basic Auth 认证）
     */
    suspend fun testConnection(
        serverUrl: String,
        username: String? = null,
        password: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = serverUrl.trimEnd('/') + "/v1/health"
            val requestBuilder = Request.Builder()
                .url(url)
                .get()

            // 添加 Basic Auth 认证
            if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                val credentials = Credentials.basic(username, password)
                requestBuilder.addHeader("Authorization", credentials)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            Log.d(TAG, "Health check: ${response.code} for $serverUrl")
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed for $serverUrl", e)
            false
        }
    }

    /**
     * 拉取历史消息 — WebSocket 断连期间漏掉的消息
     * 使用 ntfy HTTP JSON 流接口: GET /topic/json?since=TIMESTAMP&poll=1
     *
     * @param serverUrl 服务器地址 (https://ntfy.example.com)
     * @param topic     订阅主题
     * @param since     起始时间戳 (Unix 秒)，0 表示拉取所有
     * @param username  Basic Auth 用户名
     * @param password  Basic Auth 密码
     * @return 解析后的消息列表
     */
    suspend fun fetchHistory(
        serverUrl: String,
        topic: String,
        since: Long = 0,
        username: String? = null,
        password: String? = null
    ): List<NtfyMessage> = withContext(Dispatchers.IO) {
        try {
            val url = "${serverUrl.trimEnd('/')}/$topic/json?poll=1&since=$since"
            val requestBuilder = Request.Builder()
                .url(url)
                .get()

            if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", Credentials.basic(username, password))
            }

            Log.d(TAG, "Fetching history: $url")
            val response = client.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "History fetch failed: ${response.code}")
                return@withContext emptyList()
            }

            val messages = mutableListOf<NtfyMessage>()
            response.body?.source()?.buffer()?.use { source ->
                while (true) {
                    val line = source.readUtf8Line() ?: break
                    if (line.isBlank()) continue
                    try {
                        val msg: NtfyMessage = gson.fromJson(line, NtfyMessage::class.java)
                        if (msg.event == "message") {
                            messages.add(msg)
                        }
                    } catch (e: JsonSyntaxException) {
                        Log.w(TAG, "Skipping malformed line: ${line.take(100)}")
                    }
                }
            }

            Log.d(TAG, "Fetched ${messages.size} historical messages for $topic")
            messages
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch history for $topic", e)
            emptyList()
        }
    }
}
