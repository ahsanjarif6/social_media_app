package com.example.social_media

import kotlinx.serialization.Serializable

@Serializable
data class Follows(
    val follower_id: String,
    val following_id: String
)
