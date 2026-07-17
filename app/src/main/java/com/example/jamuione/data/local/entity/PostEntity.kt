package com.example.jamuione.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorProfileImage: String? = null,
    val content: String,
    val imageUrl: String? = null,
    val state: String,
    val district: String,
    val locality: String,
    val timestamp: Long,
    val helpfulCount: Int = 0,
    val commentsCount: Int = 0
)
