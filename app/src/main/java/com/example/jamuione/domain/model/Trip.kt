package com.example.jamuione.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Trip(
    val id: String = "",
    val userId: String = "",
    val type: String = "operator", // 'operator' or 'request'
    val operatorName: String = "",
    val phone: String = "",
    val vehicle: String = "Sumo", // 'Sumo', 'Bus', 'Car', 'Auto', 'Other'
    val fromKey: String = "",
    val toKey: String = "",
    val fromName: String = "",
    val toName: String = "",
    val departureAt: Long = 0L, // Long millis
    val seatsTotal: Int = 0,
    val seatsLeft: Int = 0,
    val farePerSeat: Int = 0, // Int rupees, 0 means ask
    val pickupPoint: String = "",
    val notes: String = "",
    val status: String = "open", // 'open', 'full', 'departed', 'cancelled'
    val state: String = "",
    val district: String = "",
    val seatsNeeded: Int = 0,
    val timeWindow: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
