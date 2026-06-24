package com.flypigs.ntfyapp.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NtfyApi @Inject constructor() {

    companion object {
        private const val TAG = "NtfyApi"
        private val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
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
}
