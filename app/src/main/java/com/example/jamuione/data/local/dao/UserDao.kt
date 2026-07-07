package com.example.jamuione.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jamuione.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :uid")
    fun getUser(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isNativeCommunityMember = 1 ORDER BY joinedAt DESC")
    fun getNativeCommunityMembers(): Flow<List<UserEntity>>

    @Query("DELETE FROM users WHERE isNativeCommunityMember = 1")
    suspend fun clearNativeCommunity()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearUser()
}
