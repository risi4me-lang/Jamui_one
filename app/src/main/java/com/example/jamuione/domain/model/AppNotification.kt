package com.example.jamuione.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "", // LIKE, COMMENT, SYSTEM, EVENT, POLL
    val targetId: String = "", // postId, noticeId, etc.
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val senderName: String? = null,
    val senderProfileImage: String? = null
)
