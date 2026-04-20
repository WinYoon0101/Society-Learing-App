package com.example.frontend.data.model

import com.google.gson.annotations.SerializedName

data class Reaction(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("emoji")
    val emoji: String // ❤️, 😆, 😮, 😢, 😡, 👍
)
