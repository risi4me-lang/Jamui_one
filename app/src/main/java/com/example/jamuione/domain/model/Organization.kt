package com.example.jamuione.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class OrganizationType {
    BUSINESS, INSTITUTION, COMMUNITY
}

@Serializable
data class Organization(
    val organizationId: String = "",
    val type: OrganizationType = OrganizationType.COMMUNITY,
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val state: String = "",
    val district: String = "",
    val locality: String = "",
    val logoUrl: String? = null,
    val coverImageUrl: String? = null,
    val isVerified: Boolean = false,
    val memberCount: Int = 0,
    val followerCount: Int = 0,
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val status: String = "ACTIVE"
)

@Serializable
data class OrganizationAdmin(
    val userId: String = "",
    val role: String = "ADMIN", // ADMIN, MODERATOR
    val addedAt: Long = 0L
)

@Serializable
data class OrganizationAnnouncement(
    val id: String = "",
    val organizationId: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0L,
    val views: Int = 0
)
