package com.example.jamuione.domain.repository

import com.example.jamuione.domain.model.User
import com.example.jamuione.util.Resource
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(uid: String): Flow<Resource<User?>>
    fun createUserProfile(user: User): Flow<Resource<Boolean>>
    fun saveUserProfile(user: User): Flow<Resource<Boolean>>
    fun updateUserFcmToken(uid: String, token: String): Flow<Resource<Boolean>>
    fun getCachedUser(uid: String): Flow<User?>
    fun getNativeCommunityMembers(nativeDistrict: String, currentDistrict: String): Flow<Resource<List<User>>>
    fun getCachedNativeCommunity(): Flow<List<User>>
}
