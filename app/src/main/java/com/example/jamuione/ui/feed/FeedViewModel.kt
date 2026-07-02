package com.example.jamuione.ui.feed

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.Post
import com.example.jamuione.domain.model.User
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.PostRepository
import com.example.jamuione.domain.repository.UserRepository
import com.example.jamuione.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FeedScope {
    LOCALITY, DISTRICT, STATE
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _posts = MutableStateFlow<Resource<List<Post>>>(Resource.Idle())
    val posts: StateFlow<Resource<List<Post>>> = _posts

    private val _cachedPosts = MutableStateFlow<List<Post>>(emptyList())
    val cachedPosts: StateFlow<List<Post>> = _cachedPosts

    private val _createPostResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val createPostResult: StateFlow<Resource<Boolean>> = _createPostResult

    private val _currentScope = MutableStateFlow(FeedScope.LOCALITY)
    val currentScope: StateFlow<FeedScope> = _currentScope

    private val _userProfile = MutableStateFlow<Resource<User?>>(Resource.Idle())
    val userProfile: StateFlow<Resource<User?>> = _userProfile

    val isGuest: Boolean
        get() = authRepository.getCurrentUser() == null

    private var currentUser: User? = null
    private var postsJob: Job? = null

    init {
        observeCachedPosts()
        fetchUserProfile()
        loadPosts()
    }

    private fun fetchUserProfile() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            userRepository.getUserProfile(uid).collectLatest {
                _userProfile.value = it
            }
        }
    }

    fun setScope(scope: FeedScope) {
        if (_currentScope.value != scope) {
            _currentScope.value = scope
            loadPosts()
        }
    }

    fun loadPosts() {
        postsJob?.cancel()
        postsJob = viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid
            if (uid == null) {
                // Guest mode: fetch global feed
                postRepository.getPosts().collectLatest {
                    _posts.value = it
                }
                return@launch
            }

            val userResource = userRepository.getUserProfile(uid).first()
            if (userResource is Resource.Success) {
                currentUser = userResource.data
                currentUser?.let { user ->
                    when (_currentScope.value) {
                        FeedScope.LOCALITY -> postRepository.getPosts(locality = user.locality)
                        FeedScope.DISTRICT -> postRepository.getPosts(district = user.district)
                        FeedScope.STATE -> postRepository.getPosts(state = user.state)
                    }.collectLatest {
                        _posts.value = it
                    }
                }
            }
        }
    }

    private fun observeCachedPosts() {
        viewModelScope.launch {
            postRepository.getCachedPosts().collectLatest {
                _cachedPosts.value = it
            }
        }
    }

    fun createPost(content: String, imageUri: Uri?) {
        val user = currentUser ?: return
        val post = Post(
            userId = user.uid,
            userName = user.name,
            userProfileImage = user.profileImage,
            content = content,
            state = user.state,
            district = user.district,
            locality = user.locality
        )
        viewModelScope.launch {
            postRepository.createPost(post, imageUri).collectLatest {
                _createPostResult.value = it
            }
        }
    }

    fun resetCreatePostResult() {
        _createPostResult.value = Resource.Idle()
    }
}
