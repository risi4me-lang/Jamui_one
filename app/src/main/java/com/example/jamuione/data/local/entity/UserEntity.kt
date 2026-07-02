package com.example.jamuione.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val mobileNumber: String,
    val district: String,
    val ward: String,
    val profilePictureUrl: String? = null
)
