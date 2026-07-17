package com.example.jamuione.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.Comment
import com.example.jamuione.domain.model.HelpfulVote
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

    private val _helpfulUsers = MutableStateFlow<Resource<List<HelpfulVote>>>(Resource.Idle())
    val helpfulUsers: StateFlow<Resource<List<HelpfulVote>>> = _helpfulUsers

    private val _isHelpful = MutableStateFlow(false)
    val isHelpful: StateFlow<Boolean> = _isHelpful

    private val _reportPostResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val reportPostResult: StateFlow<Resource<Boolean>> = _reportPostResult

    private val _reportCommentResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val reportCommentResult: StateFlow<Resource<Boolean>> = _reportCommentResult

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
            postRepository.observeIsHelpfulByUser(postId, uid).collectLatest {
                _isHelpful.value = it
            }
        }

        viewModelScope.launch {
            postRepository.getComments(postId).collectLatest {
                _comments.value = it
            }
        }
    }

    fun fetchHelpfulUsers(postId: String) {
        viewModelScope.launch {
            postRepository.getHelpfulUsers(postId).collectLatest {
                _helpfulUsers.value = it
            }
        }
    }

    fun toggleHelpful(postId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            postRepository.toggleHelpful(postId, user.uid, user.name, user.profileImage, user.isVerified).collectLatest { }
        }
    }

    fun addComment(postId: String, content: String, parentCommentId: String? = null) {
        val user = _currentUser.value ?: return
        val comment = Comment(
            userId = user.uid,
            userName = user.name,
            userProfileImage = user.profileImage,
            isVerified = user.isVerified,
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
