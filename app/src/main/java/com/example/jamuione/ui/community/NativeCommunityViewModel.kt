package com.example.jamuione.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.User
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.UserRepository
import com.example.jamuione.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NativeCommunityViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _members = MutableStateFlow<Resource<List<User>>>(Resource.Idle())
    val members: StateFlow<Resource<List<User>>> = _members

    private val _cachedMembers = MutableStateFlow<List<User>>(emptyList())
    val cachedMembers: StateFlow<List<User>> = _cachedMembers

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        fetchCurrentUser()
        observeCachedMembers()
    }

    private fun fetchCurrentUser() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            userRepository.getUserProfile(uid).collectLatest { resource ->
                if (resource is Resource.Success) {
                    _currentUser.value = resource.data
                    resource.data?.let { user ->
                        if (user.nativeDistrict.isNotEmpty() && user.district.isNotEmpty()) {
                            loadMembers(user.nativeDistrict, user.district)
                        }
                    }
                }
            }
        }
    }

    private fun loadMembers(nativeDistrict: String, currentDistrict: String) {
        viewModelScope.launch {
            userRepository.getNativeCommunityMembers(nativeDistrict, currentDistrict).collectLatest {
                _members.value = it
            }
        }
    }

    private fun observeCachedMembers() {
        viewModelScope.launch {
            userRepository.getCachedNativeCommunity().collectLatest {
                _cachedMembers.value = it
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val filteredMembers = combine(_members, _cachedMembers, _searchQuery) { membersResource, cached, query ->
        val list = if (membersResource is Resource.Success) membersResource.data ?: emptyList() else cached
        if (query.isBlank()) {
            list
        } else {
            list.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.profession.contains(query, ignoreCase = true) || 
                (it.company?.contains(query, ignoreCase = true) ?: false)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
