package com.example.jamuione.domain.model

import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val state: String = "", // Maps to currentState
    val district: String = "", // Maps to currentDistrict
    val locality: String = "", // Maps to currentLocality
    val nativeState: String = "",
    val nativeDistrict: String = "",
    val profession: String = "",
    val company: String? = null,
    val profileImage: String? = null,
    val role: String = "user",
    val profileCompleted: Boolean = false,
    val showInCommunity: Boolean = true,
    val fcmToken: String? = null,
    @get:PropertyName("isVerified")
    @set:PropertyName("isVerified")
    var isVerified: Boolean = false,
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    val joinedAt: Long = 0L,
    val lastSeen: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
