package com.example.jamuione.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Like(
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String? = null,
    val isVerified: Boolean = false,
    val timestamp: Long = 0L
)
