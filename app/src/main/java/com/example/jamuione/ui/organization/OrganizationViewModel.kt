package com.example.jamuione.ui.organization

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.*
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.OrganizationRepository
import com.example.jamuione.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrganizationViewModel @Inject constructor(
    private val organizationRepository: OrganizationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _createOrgResult = MutableStateFlow<Resource<String>>(Resource.Idle())
    val createOrgResult: StateFlow<Resource<String>> = _createOrgResult

    private val _createAnnouncementResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val createAnnouncementResult: StateFlow<Resource<Boolean>> = _createAnnouncementResult

    private val _managedOrgs = MutableStateFlow<Resource<List<Organization>>>(Resource.Idle())
    val managedOrgs: StateFlow<Resource<List<Organization>>> = _managedOrgs

    private val _discoveryOrgs = MutableStateFlow<Resource<List<Organization>>>(Resource.Idle())
    val discoveryOrgs: StateFlow<Resource<List<Organization>>> = _discoveryOrgs

    private val _selectedOrg = MutableStateFlow<Resource<Organization>>(Resource.Idle())
    val selectedOrg: StateFlow<Resource<Organization>> = _selectedOrg

    private val _isFollowingMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isFollowingMap: StateFlow<Map<String, Boolean>> = _isFollowingMap

    fun createOrganization(
        name: String,
        type: OrganizationType,
        description: String,
        category: String,
        state: String,
        district: String,
        locality: String,
        logoUri: Uri?
    ) {
        val userId = authRepository.getCurrentUser()?.uid ?: return
        val org = Organization(
            name = name,
            type = type,
            description = description,
            category = category,
            state = state,
            district = district,
            locality = locality,
            createdBy = userId
        )
        viewModelScope.launch {
            organizationRepository.createOrganization(org, logoUri).collectLatest {
                _createOrgResult.value = it
            }
        }
    }

    fun loadManagedOrganizations() {
        val userId = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            organizationRepository.getUserManagedOrganizations(userId).collectLatest {
                _managedOrgs.value = it
            }
        }
    }

    fun loadOrganizationsForDiscovery(state: String? = null, district: String? = null) {
        viewModelScope.launch {
            organizationRepository.getOrganizationsByLocation(state, district).collectLatest { resource ->
                _discoveryOrgs.value = resource
                if (resource is Resource.Success) {
                    val uid = authRepository.getCurrentUser()?.uid
                    if (uid != null) {
                        resource.data?.forEach { org ->
                            observeFollowingState(org.organizationId, uid)
                        }
                    }
                }
            }
        }
    }

    private fun observeFollowingState(orgId: String, userId: String) {
        if (_isFollowingMap.value.containsKey(orgId)) return
        viewModelScope.launch {
            organizationRepository.observeIsFollowing(orgId, userId).collectLatest { isFollowing ->
                _isFollowingMap.value = _isFollowingMap.value + (orgId to isFollowing)
            }
        }
    }

    fun toggleFollow(orgId: String) {
        val userId = authRepository.getCurrentUser()?.uid ?: return
        val isFollowing = _isFollowingMap.value[orgId] ?: false
        viewModelScope.launch {
            if (isFollowing) {
                organizationRepository.unfollowOrganization(orgId, userId).collectLatest { }
            } else {
                organizationRepository.followOrganization(orgId, userId).collectLatest { }
            }
        }
    }

    fun resetCreateResult() {
        _createOrgResult.value = Resource.Idle()
    }

    fun createAnnouncement(orgId: String, title: String, content: String, imageUri: Uri?) {
        val announcement = OrganizationAnnouncement(
            organizationId = orgId,
            title = title,
            content = content
        )
        viewModelScope.launch {
            organizationRepository.createAnnouncement(announcement, imageUri).collectLatest {
                _createAnnouncementResult.value = it
            }
        }
    }

    fun resetCreateAnnouncementResult() {
        _createAnnouncementResult.value = Resource.Idle()
    }

    fun loadOrganizationDetails(orgId: String) {
        viewModelScope.launch {
            organizationRepository.getOrganization(orgId).collectLatest {
                _selectedOrg.value = it
            }
        }
    }

    fun checkAdminPermission(orgId: String, onResult: (Boolean) -> Unit) {
        val userId = authRepository.getCurrentUser()?.uid ?: run { onResult(false); return }
        viewModelScope.launch {
            organizationRepository.getAdmins(orgId).first { it !is Resource.Loading }.let { resource ->
                if (resource is Resource.Success) {
                    onResult(resource.data?.any { it.userId == userId } ?: false)
                } else {
                    onResult(false)
                }
            }
        }
    }
}
