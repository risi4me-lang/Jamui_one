package com.example.jamuione.ui.auth

import android.content.Context
import android.util.Log
import com.example.jamuione.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.User
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.UserRepository
import com.example.jamuione.util.Resource
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Authenticated : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _userProfile = MutableStateFlow<Resource<User?>>(Resource.Idle())
    val userProfile: StateFlow<Resource<User?>> = _userProfile

    private val _profileSaved = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val profileSaved: StateFlow<Resource<Boolean>> = _profileSaved

    fun signIn(context: Context) {
        viewModelScope.launch {
            Log.d("AUTH_TRACE", "Signup button clicked (Google)")
            authRepository.signInWithGoogle(context).collectLatest { resource ->
                handleAuthResource(resource, "Google")
            }
        }
    }

    fun signInEmail(email: String, password: String) {
        viewModelScope.launch {
            Log.d("AUTH_TRACE", "Login button clicked (Email)")
            authRepository.signInWithEmail(email, password).collectLatest { resource ->
                handleAuthResource(resource, "Email Login")
            }
        }
    }

    fun signUpEmail(email: String, password: String) {
        if (!isPasswordValid(password)) {
            _authState.value = AuthState.Error("Password must be at least 8 characters, include uppercase, lowercase, number, and special character.")
            return
        }
        viewModelScope.launch {
            Log.d("AUTH_TRACE", "Signup button clicked (Email)")
            authRepository.signUpWithEmail(email, password).collectLatest { resource ->
                handleAuthResource(resource, "Email Sign-Up")
            }
        }
    }

    fun isPasswordValid(password: String): Boolean {
        if (password.length < 8) return false
        if (!password.any { it.isUpperCase() }) return false
        if (!password.any { it.isLowerCase() }) return false
        if (!password.any { it.isDigit() }) return false
        if (!password.any { !it.isLetterOrDigit() }) return false
        return true
    }

    private suspend fun handleAuthResource(resource: Resource<FirebaseUser>, method: String) {
        when (resource) {
            is Resource.Loading -> {
                Log.d("AUTH_TRACE", "$method: Auth Loading state emitted")
                _authState.value = AuthState.Loading
            }
            is Resource.Success -> {
                val firebaseUser = resource.data!!
                if (BuildConfig.DEBUG) {
                    Log.d("AUTH_TRACE", "$method: Firebase Auth success, UID: ${firebaseUser.uid}")
                }
                
                // First, check if user profile exists to avoid overwriting or unnecessary writes
                Log.d("AUTH_TRACE", "$method: Checking if user profile exists in Firestore")
                val existingProfileResult = userRepository.getUserProfile(firebaseUser.uid).first { it !is Resource.Loading }
                
                if (existingProfileResult is Resource.Success && existingProfileResult.data != null) {
                    Log.d("AUTH_TRACE", "$method: Existing profile found, skipping creation")
                    _authState.value = AuthState.Authenticated
                    fetchUserProfile()
                } else {
                    Log.d("AUTH_TRACE", "$method: No existing profile, creating new one")
                    val user = User(
                        uid = firebaseUser.uid,
                        name = (firebaseUser.displayName ?: "").trim(),
                        email = (firebaseUser.email ?: "").trim(),
                        state = "bihar", // Default state normalized
                        district = "jamui", // Default district normalized
                        locality = "unknown", // Placeholder normalized
                        profileImage = firebaseUser.photoUrl?.toString(),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    userRepository.createUserProfile(user).collectLatest { createResult ->
                        when (createResult) {
                            is Resource.Loading -> Log.d("AUTH_TRACE", "$method: Firestore write started")
                            is Resource.Success -> {
                                Log.d("AUTH_TRACE", "$method: Firestore Success, Repository emitted Success")
                                Log.d("AUTH_TRACE", "$method: ViewModel received Success, transitioning to Authenticated")
                                _authState.value = AuthState.Authenticated
                                fetchUserProfile()
                            }
                            is Resource.Error -> {
                                Log.e("AUTH_TRACE", "$method: Firestore write failed: ${createResult.message}")
                                _authState.value = AuthState.Error(createResult.message ?: "Profile setup failed")
                            }
                            else -> {}
                        }
                    }
                }
            }
            is Resource.Error -> {
                Log.e("AUTH_TRACE", "$method: Firebase Auth failed: ${resource.message}")
                _authState.value = AuthState.Error(resource.message ?: "Auth failed")
            }
            is Resource.Idle -> {
                Log.d("AUTH_TRACE", "$method: Auth Idle")
                _authState.value = AuthState.Idle
            }
        }
    }

    fun fetchUserProfile() {
        val firebaseUser = authRepository.getCurrentUser()
        if (firebaseUser != null) {
            if (BuildConfig.DEBUG) {
                Log.d("AUTH_TRACE", "Fetching user profile for UID: ${firebaseUser.uid}")
            }
            viewModelScope.launch {
                userRepository.getUserProfile(firebaseUser.uid).collectLatest {
                    Log.d("AUTH_TRACE", "User profile emission received: $it")
                    _userProfile.value = it
                }
            }
        } else {
            Log.d("AUTH_TRACE", "No Firebase user, setting profile to Success(null)")
            _userProfile.value = Resource.Success(null)
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun saveProfile(
        name: String, 
        state: String, 
        district: String, 
        locality: String,
        nativeState: String = "",
        nativeDistrict: String = "",
        profession: String = "",
        company: String? = null,
        showInCommunity: Boolean = true
    ) {
        val firebaseUser = authRepository.getCurrentUser() ?: run {
            Log.e("AUTH_TRACE", "No current user during saveProfile")
            return
        }
        
        val currentProfile = (_userProfile.value as? Resource.Success)?.data
        
        val user = User(
            uid = firebaseUser.uid,
            name = name.trim(),
            email = (firebaseUser.email ?: "").trim(),
            state = state.trim().lowercase(),
            district = district.trim().lowercase(),
            locality = locality.trim().lowercase(),
            nativeState = nativeState.trim().lowercase(),
            nativeDistrict = nativeDistrict.trim().lowercase(),
            profession = profession.trim(),
            company = company?.trim(),
            showInCommunity = showInCommunity,
            profileImage = firebaseUser.photoUrl?.toString(),
            profileCompleted = true,
            isVerified = currentProfile?.isVerified ?: false,
            joinedAt = currentProfile?.joinedAt ?: System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            userRepository.saveUserProfile(user).collectLatest { resource ->
                Log.d("AUTH_TRACE", "Firestore profile save result emission: $resource")
                _profileSaved.value = resource
            }
        }
    }

    fun isUserLoggedIn() = authRepository.isUserLoggedIn()

    fun logout() {
        Log.d("AUTH_TRACE", "Logout triggered from AuthViewModel")
        authRepository.signOut()
        _userProfile.value = Resource.Idle()
        _authState.value = AuthState.Unauthenticated
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}
