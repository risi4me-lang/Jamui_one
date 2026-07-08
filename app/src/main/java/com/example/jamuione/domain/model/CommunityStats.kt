package com.example.jamuione.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CommunityStats(
    val totalMembers: Long = 0,
    val verifiedMembers: Long = 0,
    val professionals: Long = 0,
    val bloodDonors: Long = 0
)
