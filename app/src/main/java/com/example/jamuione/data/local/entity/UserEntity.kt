package com.example.jamuione.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val email: String,
    val state: String,
    val district: String,
    val locality: String,
    val nativeState: String = "",
    val nativeDistrict: String = "",
    val profession: String = "",
    val company: String? = null,
    val bio: String? = null,
    val isBloodDonor: Boolean = false,
    val profileImage: String? = null,
    val profileCompleted: Boolean = false,
    val isVerified: Boolean = false,
    val showInCommunity: Boolean = true,
    val joinedAt: Long = 0L,
    val isNativeCommunityMember: Boolean = false,
    val communitySection: String? = null // locality, district, state
)
