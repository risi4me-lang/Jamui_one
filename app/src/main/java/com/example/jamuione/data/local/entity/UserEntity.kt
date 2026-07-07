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
    val profileImage: String? = null,
    val profileCompleted: Boolean = false,
    val isVerified: Boolean = false
)
