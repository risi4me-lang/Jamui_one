package com.example.jamuione.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.CommunityStats
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

    private val _localityMembers = MutableStateFlow<Resource<List<User>>>(Resource.Idle())
    val localityMembers: StateFlow<Resource<List<User>>> = _localityMembers

    private val _districtMembers = MutableStateFlow<Resource<List<User>>>(Resource.Idle())
    val districtMembers: StateFlow<Resource<List<User>>> = _districtMembers

    private val _everywhereMembers = MutableStateFlow<Resource<List<User>>>(Resource.Idle())
    val everywhereMembers: StateFlow<Resource<List<User>>> = _everywhereMembers

    private val _residents = MutableStateFlow<Resource<List<User>>>(Resource.Idle())
    val residents: StateFlow<Resource<List<User>>> = _residents

    private val _neighbors = MutableStateFlow<Resource<List<User>>>(Resource.Idle())
    val neighbors: StateFlow<Resource<List<User>>> = _neighbors

    private val _stats = MutableStateFlow<Resource<CommunityStats>>(Resource.Idle())
    val stats: StateFlow<Resource<CommunityStats>> = _stats

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        fetchCurrentUser()
    }

    private fun fetchCurrentUser() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            userRepository.getUserProfile(uid).collectLatest { resource ->
                if (resource is Resource.Success) {
                    _currentUser.value = resource.data
                }
            }
        }
    }

    fun loadHometownData() {
        val user = _currentUser.value ?: return
        if (user.nativeDistrict.isEmpty()) return

        viewModelScope.launch {
            userRepository.getCommunityStats(user.nativeDistrict).collectLatest {
                _stats.value = it
            }
        }
        
        viewModelScope.launch {
            userRepository.getCommunityMembers(
                nativeDistrict = user.nativeDistrict,
                currentLocality = user.locality,
                section = "locality"
            ).collectLatest { _localityMembers.value = it }
        }

        viewModelScope.launch {
            userRepository.getCommunityMembers(
                nativeDistrict = user.nativeDistrict,
                currentDistrict = user.district,
                section = "district"
            ).collectLatest { _districtMembers.value = it }
        }

        viewModelScope.launch {
            userRepository.getCommunityMembers(
                nativeDistrict = user.nativeDistrict,
                section = "hometown"
            ).collectLatest { _everywhereMembers.value = it }
        }
    }

    fun loadLocalityData() {
        val user = _currentUser.value ?: return
        if (user.locality.isEmpty()) return
        viewModelScope.launch {
            userRepository.getLocalityResidents(user.locality).collectLatest {
                _residents.value = it
            }
        }
    }

    fun loadNeighborData() {
        val user = _currentUser.value ?: return
        if (user.district.isEmpty()) return
        viewModelScope.launch {
            userRepository.getDistrictResidents(user.district).collectLatest {
                _neighbors.value = it
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun filterList(resource: Resource<List<User>>, query: String): List<User> {
        val list = (resource as? Resource.Success)?.data ?: emptyList()
        if (query.isBlank()) return list
        return list.filter { 
            it.name.contains(query, ignoreCase = true) || 
            it.profession.contains(query, ignoreCase = true) || 
            (it.company?.contains(query, ignoreCase = true) ?: false)
        }
    }
}
