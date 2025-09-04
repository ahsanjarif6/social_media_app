package com.example.social_media

import kotlinx.serialization.Serializable

@Serializable
data class Vote(
    val post_id: String,
    val user_id: String,
    val vote: Int
)
