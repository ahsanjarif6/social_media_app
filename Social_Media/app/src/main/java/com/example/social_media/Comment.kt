package com.example.social_media

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String,
    val post_id: String,
    val user_id: String,
    val content: String,
    val created_at: String
)

