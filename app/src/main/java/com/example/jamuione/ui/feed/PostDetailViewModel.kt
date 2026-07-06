package com.example.jamuione.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.Comment
import com.example.jamuione.domain.model.Like
import com.example.jamuione.domain.model.User
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.PostRepository
import com.example.jamuione.domain.repository.UserRepository
import com.example.jamuione.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _comments = MutableStateFlow<Resource<List<Comment>>>(Resource.Idle())
    val comments: StateFlow<Resource<List<Comment>>> = _comments

    private val _likers = MutableStateFlow<Resource<List<Like>>>(Resource.Idle())
    val likers: StateFlow<Resource<List<Like>>> = _likers

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked

    private val _reportPostResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val reportPostResult: StateFlow<Resource<Boolean>> = _reportPostResult

    private val _currentUser = MutableStateFlow<User?>(null)

    init {
        val uid = authRepository.getCurrentUser()?.uid
        if (uid != null) {
            viewModelScope.launch {
                userRepository.getUserProfile(uid).first { it !is Resource.Loading }.let { resource ->
                    if (resource is Resource.Success) {
                        _currentUser.value = resource.data
                    }
                }
            }
        }
    }

    fun loadPostDetails(postId: String) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        
        viewModelScope.launch {
            postRepository.observeIsLikedByUser(postId, uid).collectLatest {
                _isLiked.value = it
            }
        }

        viewModelScope.launch {
            postRepository.getComments(postId).collectLatest {
                _comments.value = it
            }
        }
    }

    fun fetchLikers(postId: String) {
        viewModelScope.launch {
            postRepository.getLikers(postId).collectLatest {
                _likers.value = it
            }
        }
    }

    fun toggleLike(postId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            postRepository.toggleLike(postId, user.uid, user.name, user.profileImage).collectLatest { }
        }
    }

    fun addComment(postId: String, content: String, parentCommentId: String? = null) {
        val user = _currentUser.value ?: return
        val comment = Comment(
            userId = user.uid,
            userName = user.name,
            userProfileImage = user.profileImage,
            content = content,
            parentCommentId = parentCommentId
        )
        viewModelScope.launch {
            postRepository.addComment(postId, comment).collectLatest { }
        }
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
