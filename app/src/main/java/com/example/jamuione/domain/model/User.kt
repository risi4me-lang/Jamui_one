package com.example.jamuione.domain.model

import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val state: String = "",
    val district: String = "",
    val locality: String = "",
    val profileImage: String? = null,
    val role: String = "user",
    val profileCompleted: Boolean = false,
    val fcmToken: String? = null,
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
