package com.example.social_media

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val chat_id: String,
    val sender_id: String,
    val receiver_id: String,
    val content: String,
    val created_at: String,
    val read: Boolean
)