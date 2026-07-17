package com.example.jamuione.domain.repository

import android.net.Uri
import com.example.jamuione.domain.model.*
import com.example.jamuione.util.Resource
import kotlinx.coroutines.flow.Flow

interface OrganizationRepository {
    fun createOrganization(organization: Organization, logoUri: Uri?): Flow<Resource<String>>
    fun getOrganization(orgId: String): Flow<Resource<Organization>>
    fun getUserManagedOrganizations(userId: String): Flow<Resource<List<Organization>>>
    fun getOrganizationsByLocation(state: String? = null, district: String? = null, type: OrganizationType? = null): Flow<Resource<List<Organization>>>
    fun followOrganization(orgId: String, userId: String): Flow<Resource<Boolean>>
    fun unfollowOrganization(orgId: String, userId: String): Flow<Resource<Boolean>>
    fun observeIsFollowing(orgId: String, userId: String): Flow<Boolean>
    
    // Content Management
    fun createAnnouncement(announcement: OrganizationAnnouncement, imageUri: Uri?): Flow<Resource<Boolean>>
    fun getAnnouncements(orgId: String): Flow<Resource<List<OrganizationAnnouncement>>>
    
    // Admin Management
    fun addAdmin(orgId: String, userId: String, role: String): Flow<Resource<Boolean>>
    fun getAdmins(orgId: String): Flow<Resource<List<OrganizationAdmin>>>
}
