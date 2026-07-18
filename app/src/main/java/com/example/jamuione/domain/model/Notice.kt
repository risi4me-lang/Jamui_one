package com.example.jamuione.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Notice(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String? = null,
    val isVerified: Boolean = false,
    val title: String = "",
    val searchableTitle: String = "",
    val description: String = "",
    val category: String = "", // Announcement, Event, Jobs, Rent/Flatmate, Buy & Sell, Lost & Found, Blood Donation, Help Needed
    val state: String = "",
    val district: String = "",
    val locality: String = "",
    val createdAt: Long = 0L,
    val expiryDate: Long = 0L,
    val contactNumber: String = "",
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0L,
    val pollQuestion: String? = null,
    val pollOptions: List<String>? = null,
    val pollVotes: Map<String, Long>? = null, // OptionIndex -> VoteCount
    val userVotes: Map<String, Long>? = null, // UserId -> OptionIndex
    val pollClosesAt: Long? = null,
    val eventDate: Long? = null,
    val eventLocation: String? = null,
    val rsvpCount: Long = 0L,
    val organizationId: String? = null,
    val organizationName: String? = null
)
