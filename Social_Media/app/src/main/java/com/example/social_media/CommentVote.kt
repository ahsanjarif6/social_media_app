package com.example.social_media

import kotlinx.serialization.Serializable

@Serializable
data class CommentVote(
    val user_id: String,
    val comment_id: String,
    val vote: Int
)
