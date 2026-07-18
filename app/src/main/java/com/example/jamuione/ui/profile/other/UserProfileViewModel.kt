package com.example.jamuione.ui.profile.other

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.User
import com.example.jamuione.domain.repository.UserRepository
import com.example.jamuione.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userProfile = MutableStateFlow<Resource<User?>>(Resource.Idle())
    val userProfile: StateFlow<Resource<User?>> = _userProfile

    fun loadUserProfile(uid: String) {
        viewModelScope.launch {
            userRepository.getUserProfile(uid).collectLatest {
                _userProfile.value = it
            }
        }
    }
}
