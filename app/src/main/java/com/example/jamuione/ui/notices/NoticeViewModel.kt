package com.example.jamuione.ui.notices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.Notice
import com.example.jamuione.domain.model.User
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.NoticeRepository
import com.example.jamuione.domain.repository.UserRepository
import com.example.jamuione.ui.feed.FeedScope
import com.example.jamuione.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoticeViewModel @Inject constructor(
    private val noticeRepository: NoticeRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _notices = MutableStateFlow<Resource<List<Notice>>>(Resource.Idle())
    val notices: StateFlow<Resource<List<Notice>>> = _notices

    private val _createNoticeResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val createNoticeResult: StateFlow<Resource<Boolean>> = _createNoticeResult

    private val _currentScope = MutableStateFlow(FeedScope.LOCALITY)
    val currentScope: StateFlow<FeedScope> = _currentScope

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _userProfile = MutableStateFlow<Resource<User?>>(Resource.Idle())
    val userProfile: StateFlow<Resource<User?>> = _userProfile

    val isGuest: Boolean
        get() = authRepository.getCurrentUser() == null

    private var currentUser: User? = null
    private var noticesJob: Job? = null

    val categories = listOf(
        "Announcement", "Jobs", "Rent/Flatmate", "Buy & Sell", 
        "Lost & Found", "Blood Donation", "Help Needed"
    )

    init {
        fetchUserProfile()
        loadNotices()
        subscribeToLocationTopics()
    }

    private fun fetchUserProfile() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            userRepository.getUserProfile(uid).collectLatest {
                _userProfile.value = it
            }
        }
    }

    private fun subscribeToLocationTopics() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: return@launch
            val userResource = userRepository.getUserProfile(uid).first()
            if (userResource is Resource.Success) {
                val user = userResource.data ?: return@launch
                val districtTopic = "notices_${user.district.lowercase().replace(" ", "_")}"
                noticeRepository.subscribeToTopic(districtTopic).collectLatest { }
            }
        }
    }

    fun setScope(scope: FeedScope) {
        if (_currentScope.value != scope) {
            _currentScope.value = scope
            loadNotices()
        }
    }

    fun setCategory(category: String?) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            loadNotices()
        }
    }

    fun loadNotices() {
        noticesJob?.cancel()
        noticesJob = viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid
            if (uid == null) {
                // Guest mode: fetch global notices
                val category = _selectedCategory.value
                noticeRepository.getNotices(category).collectLatest {
                    _notices.value = it
                }
                return@launch
            }

            val userResource = userRepository.getUserProfile(uid).first()
            if (userResource is Resource.Success) {
                currentUser = userResource.data
                currentUser?.let { user ->
                    val category = _selectedCategory.value
                    when (_currentScope.value) {
                        FeedScope.LOCALITY -> noticeRepository.getNotices(category, locality = user.locality)
                        FeedScope.DISTRICT -> noticeRepository.getNotices(category, district = user.district)
                        FeedScope.STATE -> noticeRepository.getNotices(category, state = user.state)
                    }.collectLatest {
                        _notices.value = it
                    }
                }
            }
        }
    }

    fun createNotice(title: String, description: String, category: String, contact: String, daysToExpiry: Int) {
        val user = currentUser ?: return
        val expiryDate = System.currentTimeMillis() + (daysToExpiry * 24 * 60 * 60 * 1000L)
        val notice = Notice(
            userId = user.uid,
            userName = user.name,
            userProfileImage = user.profileImage,
            title = title,
            description = description,
            category = category,
            state = user.state,
            district = user.district,
            locality = user.locality,
            expiryDate = expiryDate,
            contactNumber = contact
        )
        viewModelScope.launch {
            noticeRepository.createNotice(notice).collectLatest {
                _createNoticeResult.value = it
            }
        }
    }

    fun resetCreateNoticeResult() {
        _createNoticeResult.value = Resource.Idle()
    }
}
