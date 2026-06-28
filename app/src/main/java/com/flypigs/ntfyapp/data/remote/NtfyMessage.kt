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
    val icon: String? = null
) {
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
