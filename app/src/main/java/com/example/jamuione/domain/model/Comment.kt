package com.example.jamuione.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String = "",
    val postId: String = "",
    val parentCommentId: String? = null,
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String? = null,
    val content: String = "",
    val timestamp: Long = 0L
)
