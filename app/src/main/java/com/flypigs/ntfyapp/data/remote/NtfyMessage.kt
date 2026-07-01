package com.flypigs.ntfyapp.data.remote

import com.google.gson.annotations.SerializedName

data class NtfyMessage(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("time")
    val time: Long = 0,

    @SerializedName("event")
    val event: String = "",

    @SerializedName("topic")
    val topic: String = "",

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("message")
    val message: String = "",

    // 修改: 支持字符串和数字格式的 priority
    @SerializedName("priority")
    val priorityRaw: Any? = null,

    @SerializedName("tags")
    val tags: List<String>? = null,

    @SerializedName("click")
    val click: String? = null,

    @SerializedName("icon")
    val icon: String? = null,

    // ── 附件 (v6 新增) ─────────────────────────────
    // ntfy 服务端在收到 multipart 上传后会回传 attachment 对象
    @SerializedName("attachment")
    val attachment: Attachment? = null
) {

    /**
     * ntfy 附件对象（multipart 上传后的 file URL）
     * @see <a href="https://docs.ntfy.sh/publish/#attachments">ntfy docs</a>
     */
    data class Attachment(
        @SerializedName("name") val name: String,
        @SerializedName("type") val type: String? = null,    // MIME, e.g. "image/jpeg"
        @SerializedName("size") val size: Long = 0,
        @SerializedName("url") val url: String,             // 服务端 file URL
        @SerializedName("expires") val expires: Long? = null // Unix 秒
    )
    // 计算属性：将 priorityRaw 转换为 Int
    val priority: Int
        get() = parsePriority(priorityRaw)

    companion object {
        private fun parsePriority(raw: Any?): Int {
            return when (raw) {
                is Int -> raw
                is Long -> raw.toInt()
                is Double -> raw.toInt()
                is String -> when (raw.lowercase()) {
                    "min" -> 1
                    "low" -> 2
                    "default" -> 3
                    "high" -> 4
                    "urgent" -> 5
                    else -> raw.toIntOrNull() ?: 3
                }
                else -> 3
            }
        }
    }
}
