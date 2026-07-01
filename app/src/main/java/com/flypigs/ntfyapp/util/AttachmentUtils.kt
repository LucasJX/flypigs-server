package com.flypigs.ntfyapp.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * 附件下载 / 保存工具 (v6 新增)
 *
 * 设计要点：
 * - 图片保存走 MediaStore.Images (Android 10+) / 文件系统 (Android 9-)
 * - 非图片附件下载到 Download 目录
 * - 所有操作在 IO 线程执行，通过 Result 返回成功/失败
 * - 失败时给用户 Toast 提示
 */
object AttachmentUtils {

    private val httpClient by lazy { OkHttpClient() }

    /**
     * 保存远程图片到相册
     * - Android 10+: 走 MediaStore.Images.Media.EXTERNAL_CONTENT_URI（Scoped Storage）
     * - Android 9-: 写入 Environment.DIRECTORY_PICTURES 公共目录
     *
     * @return Result.success(URI) 或 Result.failure(throwable)
     */
    suspend fun saveImageToGallery(
        context: Context,
        url: String,
        displayName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val safeName = sanitizeFileName(displayName).ifBlank { "flypigs_${System.currentTimeMillis()}.jpg" }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: MediaStore 路径
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, safeName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Flypigs")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("无法创建相册条目")
                resolver.openOutputStream(uri)?.use { out ->
                    httpClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                        if (!resp.isSuccessful) error("下载失败: HTTP ${resp.code}")
                        resp.body?.byteStream()?.use { input ->
                            input.copyTo(out)
                        } ?: error("响应体为空")
                    }
                } ?: error("无法打开输出流")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                // Android 9 及以下：直接写文件系统
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val flypigsDir = File(picturesDir, "Flypigs").apply { mkdirs() }
                val target = File(flypigsDir, safeName)
                httpClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    if (!resp.isSuccessful) error("下载失败: HTTP ${resp.code}")
                    FileOutputStream(target).use { out ->
                        resp.body?.byteStream()?.use { input ->
                            input.copyTo(out)
                        } ?: error("响应体为空")
                    }
                }
                Uri.fromFile(target)
            }
        }
    }

    /**
     * 下载附件到 Download 目录（不限于图片）
     * - Android 10+: MediaStore.Downloads
     * - Android 9-: Environment.DIRECTORY_DOWNLOADS
     */
    suspend fun downloadToDownloads(
        context: Context,
        url: String,
        fileName: String,
        mimeType: String?
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val safeName = sanitizeFileName(fileName).ifBlank { "flypigs_${System.currentTimeMillis()}" }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType ?: "application/octet-stream")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Flypigs")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("无法创建下载条目")
                resolver.openOutputStream(uri)?.use { out ->
                    httpClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                        if (!resp.isSuccessful) error("下载失败: HTTP ${resp.code}")
                        resp.body?.byteStream()?.use { input ->
                            input.copyTo(out)
                        } ?: error("响应体为空")
                    }
                } ?: error("无法打开输出流")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val flypigsDir = File(downloadDir, "Flypigs").apply { mkdirs() }
                val target = File(flypigsDir, safeName)
                httpClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    if (!resp.isSuccessful) error("下载失败: HTTP ${resp.code}")
                    FileOutputStream(target).use { out ->
                        resp.body?.byteStream()?.use { input ->
                            input.copyTo(out)
                        } ?: error("响应体为空")
                    }
                }
                Uri.fromFile(target)
            }
        }
    }

    /**
     * 保存成功后给用户 Toast 提示（在主线程调用）
     */
    fun showSavedToast(context: Context, target: String) {
        Toast.makeText(context, "已保存到 $target", Toast.LENGTH_SHORT).show()
    }

    fun showErrorToast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), "_").take(120)
}