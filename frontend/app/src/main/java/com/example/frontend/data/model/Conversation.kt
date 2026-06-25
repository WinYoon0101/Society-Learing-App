package com.example.frontend.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

data class Conversation(
    @SerializedName("_id")
    val id: String,

    @SerializedName("members")
    val members: List<User>,

    @SerializedName("isGroup")
    val isGroup: Boolean = false,

    // Tên nhóm (group); rỗng → FE ghép tên thành viên
    @SerializedName("name")
    val name: String? = null,

    // userId của admin/người tạo group (ObjectId string, chưa populate)
    @SerializedName("admin")
    val admin: String? = null,

    @SerializedName("lastMessage")
    val lastMessage: Message? = null,

    // Map<userId, nickname> — biệt danh mỗi thành viên trong conversation (BE trả về)
    @SerializedName("nicknames")
    val nicknames: Map<String, String>? = null,

    // userId đã ẩn đoạn chat (xóa-phía-mình)
    @SerializedName("deletedBy")
    val deletedBy: List<String>? = null,

    // userId đã tắt thông báo tin nhắn / cuộc gọi
    @SerializedName("mutedMessages")
    val mutedMessages: List<String>? = null,

    @SerializedName("mutedCalls")
    val mutedCalls: List<String>? = null,

    @SerializedName("createdAt")
    val createdAt: Date? = null,

    @SerializedName("updatedAt")
    val updatedAt: Date? = null
) : Serializable
