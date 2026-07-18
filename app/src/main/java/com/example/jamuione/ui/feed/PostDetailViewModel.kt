package com.example.jamuione.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.Comment
import com.example.jamuione.domain.model.Like
import com.example.jamuione.domain.model.Post
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.PostRepository
import com.example.jamuione.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _comments = MutableStateFlow<Resource<List<Comment>>>(Resource.Idle())
    val comments: StateFlow<Resource<List<Comment>>> = _comments

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked

    private val _likers = MutableStateFlow<Resource<List<Like>>>(Resource.Idle())
    val likers: StateFlow<Resource<List<Like>>> = _likers

    private val _reportPostResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val reportPostResult: StateFlow<Resource<Boolean>> = _reportPostResult

    private val _reportCommentResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val reportCommentResult: StateFlow<Resource<Boolean>> = _reportCommentResult

    fun loadPostDetails(postId: String) {
        val userId = authRepository.getCurrentUser()?.uid
        if (userId != null) {
            viewModelScope.launch {
                postRepository.observeIsLikedByUser(postId, userId).collectLatest {
                    _isLiked.value = it
                }
            }
        }
        fetchComments(postId)
    }

    fun fetchComments(postId: String) {
        viewModelScope.launch {
            postRepository.getComments(postId).collectLatest {
                _comments.value = it
            }
        }
    }

    fun addComment(postId: String, content: String, parentCommentId: String? = null) {
        val user = authRepository.getCurrentUser() ?: return
        // Fetch user profile first for name and image
        // Or pass from UI
        val comment = Comment(
            userId = user.uid,
            userName = user.displayName ?: "User",
            userProfileImage = user.photoUrl?.toString(),
            content = content,
            parentCommentId = parentCommentId
        )
        viewModelScope.launch {
            postRepository.addComment(postId, comment).collectLatest {
                // Handle result if needed
            }
        }
    }

    fun toggleLike(postId: String) {
        val user = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            postRepository.toggleLike(
                postId = postId,
                userId = user.uid,
                userName = user.displayName ?: "User",
                userProfileImage = user.photoUrl?.toString(),
                isVerified = false // TODO: fetch from actual profile
            ).collectLatest { }
        }
    }

    fun fetchLikers(postId: String) {
        viewModelScope.launch {
            postRepository.getLikers(postId).collectLatest {
                _likers.value = it
            }
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

    fun reportComment(postId: String, commentId: String, reason: String) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            postRepository.reportComment(postId, commentId, uid, reason).collectLatest {
                _reportCommentResult.value = it
            }
        }
    }

    fun resetReportCommentResult() {
        _reportCommentResult.value = Resource.Idle()
    }
}
