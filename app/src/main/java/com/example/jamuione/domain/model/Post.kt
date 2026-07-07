package com.example.jamuione.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String? = null,
    val isVerified: Boolean = false,
    val content: String = "",
    val imageUrl: String? = null,
    val state: String = "",
    val district: String = "",
    val locality: String = "",
    val timestamp: Long = 0L,
    val likesCount: Int = 0,
    val commentsCount: Int = 0
)
