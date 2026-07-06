package com.example.jamuione.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val id: String = "",
    val userId: String = "",
    val contentId: String = "",
    val contentType: String = "", // "post" or "notice"
    val reason: String = "",
    val timestamp: Long = 0L
)
