package com.example.frontend.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

data class Conversation(
    @SerializedName("_id")
    val id: String,

    @SerializedName("members")
    val members: List<User>,

    @SerializedName("lastMessage")
    val lastMessage: Message? = null,

    // Map<userId, nickname> — biệt danh mỗi thành viên trong conversation (BE trả về)
    @SerializedName("nicknames")
    val nicknames: Map<String, String>? = null,

    @SerializedName("createdAt")
    val createdAt: Date? = null,

    @SerializedName("updatedAt")
    val updatedAt: Date? = null
) : Serializable
