package com.example.jamuione.ui.feed

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.Post
import com.example.jamuione.domain.model.User
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.NotificationRepository
import com.example.jamuione.domain.repository.PostRepository
import com.example.jamuione.domain.repository.UserRepository
import com.example.jamuione.util.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class FeedScope {
    LOCALITY, DISTRICT, STATE, NATIVE_DISTRICT
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _posts = MutableStateFlow<Resource<List<Post>>>(Resource.Idle())
    val posts: StateFlow<Resource<List<Post>>> = _posts

    private val _helpfulPosts = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val helpfulPosts: StateFlow<Map<String, Boolean>> = _helpfulPosts

    private val _cachedPosts = MutableStateFlow<List<Post>>(emptyList())
    val cachedPosts: StateFlow<List<Post>> = _cachedPosts

    private val _createPostResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val createPostResult: StateFlow<Resource<Boolean>> = _createPostResult

    private val _deletePostResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val deletePostResult: StateFlow<Resource<Boolean>> = _deletePostResult

    private val _reportPostResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val reportPostResult: StateFlow<Resource<Boolean>> = _reportPostResult

    private val _savedPosts = MutableStateFlow<Resource<List<Post>>>(Resource.Idle())
    val savedPosts: StateFlow<Resource<List<Post>>> = _savedPosts

    private val _isSavedMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isSavedMap: StateFlow<Map<String, Boolean>> = _isSavedMap

    private val _currentScope = MutableStateFlow(FeedScope.LOCALITY)
    val currentScope: StateFlow<FeedScope> = _currentScope

    private val _userProfile = MutableStateFlow<Resource<User?>>(Resource.Idle())
    val userProfile: StateFlow<Resource<User?>> = _userProfile

    private val _memberCount = MutableStateFlow<Resource<Long>>(Resource.Idle())
    val memberCount: StateFlow<Resource<Long>> = _memberCount

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    val isGuest: Boolean
        get() = authRepository.getCurrentUser() == null

    private var currentUser: User? = null
    private var postsJob: Job? = null
    private var lastPostTimestamp: Long = 0L
    private val POST_COOLDOWN_MS = 30_000L

    init {
        viewModelScope.launch {
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()
            observeCachedPosts()
            fetchAndObserveUserProfile()
        }
    }

    private fun fetchAndObserveUserProfile() {
        val uid = authRepository.getCurrentUser()?.uid
        if (uid == null) {
            _userProfile.value = Resource.Success(null)
            loadPosts() // Load guest feed
            return
        }

        viewModelScope.launch {
            userRepository.getUserProfile(uid).collectLatest { resource ->
                _userProfile.value = resource
                if (resource is Resource.Success) {
                    val profileUpdated = currentUser?.uid != resource.data?.uid || 
                                       currentUser?.locality != resource.data?.locality ||
                                       currentUser?.district != resource.data?.district
                    
                    currentUser = resource.data
                    
                    // Fetch member count for current district
                    resource.data?.district?.let { district ->
                        fetchMemberCount(district)
                    }

                    // Observe unread notifications
                    observeUnreadCount(uid)

                    // Reload posts if profile data changed (locality/district) or first load
                    if (profileUpdated) {
                        loadPosts()
                    }
                }
            }
        }
    }

    private fun observeUnreadCount(userId: String) {
        viewModelScope.launch {
            notificationRepository.getUnreadCount(userId).collectLatest {
                _unreadCount.value = it
            }
        }
    }

    private fun fetchMemberCount(district: String) {
        viewModelScope.launch {
            userRepository.getDistrictMemberCount(district).collectLatest {
                _memberCount.value = it
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
            val user = currentUser
            if (isGuest) {
                Log.d("FIRESTORE_DEBUG", "loadPosts: guest mode")
                postRepository.getPosts().collectLatest {
                    _posts.value = it
                }
            } else if (user != null) {
                Log.d("FIRESTORE_DEBUG", "loadPosts: user mode, uid=${user.uid}")
                Log.d("FIRESTORE_DEBUG", "loadPosts: user=${user.name}, scope=${_currentScope.value}")
                when (_currentScope.value) {
                    FeedScope.LOCALITY -> postRepository.getPosts(locality = user.locality)
                    FeedScope.DISTRICT -> postRepository.getPosts(district = user.district)
                    FeedScope.STATE -> postRepository.getPosts(state = user.state)
                    FeedScope.NATIVE_DISTRICT -> postRepository.getPosts(district = user.nativeDistrict)
                }.collectLatest { resource ->
                    _posts.value = resource
                    if (resource is Resource.Success) {
                        resource.data?.forEach { post ->
                            observeHelpfulState(post.id, user.uid)
                            observeSaveState(post.id, user.uid)
                        }
                    }
                }
            }
        }
    }

    private fun observeHelpfulState(postId: String, userId: String) {
        if (_helpfulPosts.value.containsKey(postId)) return
        viewModelScope.launch {
            postRepository.observeIsHelpfulByUser(postId, userId).collectLatest { isHelpful ->
                _helpfulPosts.value = _helpfulPosts.value + (postId to isHelpful)
            }
        }
    }

    private fun observeSaveState(postId: String, userId: String) {
        if (_isSavedMap.value.containsKey(postId)) return
        viewModelScope.launch {
            postRepository.observeIsSavedByUser(postId, userId).collectLatest { isSaved ->
                _isSavedMap.value = _isSavedMap.value + (postId to isSaved)
            }
        }
    }

    fun loadSavedPosts() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            postRepository.getSavedPosts(uid).collectLatest { resource ->
                _savedPosts.value = resource
                if (resource is Resource.Success) {
                    resource.data?.forEach { post ->
                        observeHelpfulState(post.id, uid)
                        observeSaveState(post.id, uid)
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
        Log.d("POST_DEBUG", "createPost button clicked in ViewModel")
        val user = currentUser
        if (user == null) {
            Log.e("POST_DEBUG", "createPost failed: currentUser is null")
            _createPostResult.value = Resource.Error("User profile not loaded. Please try again.")
            return
        }
        
        val now = System.currentTimeMillis()
        if (now - lastPostTimestamp < POST_COOLDOWN_MS) {
            val remainingSeconds = ((POST_COOLDOWN_MS - (now - lastPostTimestamp)) / 1000) + 1
            _createPostResult.value = Resource.Error("Please wait ${remainingSeconds}s before posting again.")
            return
        }

        _createPostResult.value = Resource.Loading()
        val post = Post(
            userId = user.uid,
            userName = user.name,
            userProfileImage = user.profileImage,
            isVerified = user.isVerified,
            content = content,
            state = user.state,
            district = user.district,
            locality = user.locality
        )
        viewModelScope.launch {
            postRepository.createPost(post, imageUri).collectLatest { result ->
                Log.d("POST_DEBUG", "createPost result in ViewModel: $result")
                if (result is Resource.Success) lastPostTimestamp = System.currentTimeMillis()
                _createPostResult.value = result
            }
        }
    }

    fun toggleHelpful(postId: String) {
        val user = currentUser ?: return
        viewModelScope.launch {
            postRepository.toggleHelpful(postId, user.uid, user.name, user.profileImage, user.isVerified).collectLatest { }
        }
    }

    fun toggleSavePost(postId: String) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            postRepository.toggleSavePost(postId, uid).collectLatest { }
        }
    }

    fun resetCreatePostResult() {
        _createPostResult.value = Resource.Idle()
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            postRepository.deletePost(postId).collectLatest {
                _deletePostResult.value = it
            }
        }
    }

    fun resetDeletePostResult() {
        _deletePostResult.value = Resource.Idle()
    }

    fun reportPost(postId: String, reason: String) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            postRepository.reportPost(postId, uid, reason).collectLatest {
                _reportPostResult.value = it
            }
        }
    }

    fun resetReportPostResult() {
        _reportPostResult.value = Resource.Idle()
    }
}
