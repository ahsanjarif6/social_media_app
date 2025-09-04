package com.example.social_media

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String
)
