package com.example.social_media

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String,
    val user_id: String,
    val caption: String,
    val media_url: String,
    val media_type: String,
    val created_at: String
)
