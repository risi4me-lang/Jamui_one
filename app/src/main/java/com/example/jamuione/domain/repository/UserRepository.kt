package com.example.jamuione.domain.repository

import com.example.jamuione.domain.model.CommunityStats
import com.example.jamuione.domain.model.User
import com.example.jamuione.util.Resource
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(uid: String): Flow<Resource<User?>>
    fun createUserProfile(user: User): Flow<Resource<Boolean>>
    fun saveUserProfile(user: User): Flow<Resource<Boolean>>
    fun updateUserFcmToken(uid: String, token: String): Flow<Resource<Boolean>>
    fun getCachedUser(uid: String): Flow<User?>
    
    // Community members with section support
    fun getCommunityMembers(
        nativeDistrict: String,
        currentLocality: String? = null,
        currentDistrict: String? = null,
        currentState: String? = null,
        section: String
    ): Flow<Resource<List<User>>>

    fun getCachedCommunityMembers(section: String): Flow<List<User>>
    
    // Statistics
    fun getCommunityStats(nativeDistrict: String): Flow<Resource<CommunityStats>>

    fun getLocalityResidents(locality: String): Flow<Resource<List<User>>>

    fun getDistrictMemberCount(district: String): Flow<Resource<Long>>

    fun deleteAccount(uid: String): Flow<Resource<Boolean>>
}
