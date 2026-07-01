package com.flypigs.ntfyapp.util

import java.net.URI

/**
 * 重新构造 attachment URL — 用用户配置的 server URL 替换原始 URL 的 base
 *
 * 目的：让 APK 不依赖 ntfy 服务端的 base-url 配置。
 * 原因：
 *   1. 不同部署的 ntfy base-url 可能不同（内网 IP / 反代域名 / 自定义）
 *   2. 同一个 server 多个用户可能用不同访问方式（直连 vs 反代）
 *   3. 用户改 server URL 时不需要重新拉历史（DB 里 attachment URL 始终能用）
 *   4. 防止硬编码 base-url 导致换 server 的人用不了
 *
 * 示例：
 *   - originalUrl = "http://192.168.100.100:2586/file/abc.jpg"
 *     serverBaseUrl = "https://ntfy.flypigs.net:2020"
 *     → "https://ntfy.flypigs.net:2020/file/abc.jpg"
 *
 *   - originalUrl = "https://ntfy.sh/file/abc.jpg?token=xxx"
 *     serverBaseUrl = "https://ntfy.flypigs.net:2020"
 *     → "https://ntfy.flypigs.net:2020/file/abc.jpg?token=xxx"
 *
 * @param originalUrl   ntfy 服务端返回的 attachment url（base 不可控）
 * @param serverBaseUrl 用户配置的 server URL（用于替换 base）
 * @return 重写后的 URL；解析失败 / 输入为空时返回原 URL
 */
object UrlRewriter {
    fun rewriteAttachmentUrl(originalUrl: String?, serverBaseUrl: String): String? {
        if (originalUrl.isNullOrBlank()) return originalUrl
        val base = serverBaseUrl.trimEnd('/')
        if (base.isBlank()) return originalUrl
        return try {
            val original = URI(originalUrl)
            val path = original.rawPath ?: return originalUrl
            val query = original.rawQuery
            if (query != null) "$base$path?$query" else "$base$path"
        } catch (e: Exception) {
            // 原 URL 解析失败（极少见），保留原值
            originalUrl
        }
    }
}
