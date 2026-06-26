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

    // Cờ chưa đọc (BE tính từ lastMessage vs lastRead) → chấm xanh kiểu Messenger
    @SerializedName("unread")
    val unread: Boolean = false,

    // Đã tắt thông báo tin nhắn cho mình (BE tính từ mutedMessages) → hiện icon chuông gạch
    @SerializedName("muted")
    val muted: Boolean = false,

    // Đã tắt thông báo cuộc gọi cho mình (BE tính từ mutedCalls) → hiện icon chuông gạch (G7.5)
    @SerializedName("mutedCall")
    val mutedCall: Boolean = false,

    @SerializedName("createdAt")
    val createdAt: Date? = null,

    @SerializedName("updatedAt")
    val updatedAt: Date? = null
) : Serializable
