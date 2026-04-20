package com.example.frontend.data.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Message(
    @SerializedName("_id")
    val id: String,

    @SerializedName("conversationId")
    val conversationId: String,

    @SerializedName("sender")
    val sender: User,

    @SerializedName("text")
    val text: String,

    @SerializedName("replyTo")
    val replyTo: Message? = null,

    @SerializedName("reactions")
    val reactions: List<Reaction> = emptyList(),

    @SerializedName("isDeleted")
    val isDeleted: Boolean = false,

    @SerializedName("createdAt")
    val createdAt: Date? = null,

    @SerializedName("updatedAt")
    val updatedAt: Date? = null
)
