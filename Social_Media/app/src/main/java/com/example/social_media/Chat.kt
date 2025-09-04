package com.example.social_media

import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: String,
    val user1_id: String,
    val user2_id: String,
    val created_at: String
)
