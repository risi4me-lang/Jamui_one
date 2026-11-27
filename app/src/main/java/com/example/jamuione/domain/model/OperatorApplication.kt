package com.example.jamuione.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OperatorApplication(
    val userId: String = "",
    val businessName: String = "",
    val vehicleType: String = "",
    val vehicleNumber: String = "",
    val phone: String = "",
    val routes: String = "",
    val notes: String = "",
    val status: String = "pending", // 'pending', 'approved', 'rejected'
    val createdAt: Long = 0L
)
