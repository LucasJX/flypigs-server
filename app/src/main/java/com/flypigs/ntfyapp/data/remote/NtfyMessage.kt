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

    @SerializedName("priority")
    val priority: Int? = null,

    @SerializedName("tags")
    val tags: List<String>? = null,

    @SerializedName("click")
    val click: String? = null,

    @SerializedName("icon")
    val icon: String? = null
)
