package com.example.jamuione.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.jamuione.data.local.dao.PostDao
import com.example.jamuione.data.local.dao.UserDao
import com.example.jamuione.data.local.entity.PostEntity
import com.example.jamuione.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, PostEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
}
