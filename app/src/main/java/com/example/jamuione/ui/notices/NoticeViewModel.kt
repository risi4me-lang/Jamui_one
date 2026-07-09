package com.example.jamuione.ui.notices

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.Notice
import com.example.jamuione.domain.model.User
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.NoticeRepository
import com.example.jamuione.domain.repository.UserRepository
import com.example.jamuione.ui.feed.FeedScope
import com.example.jamuione.util.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class NoticeViewModel @Inject constructor(
    private val noticeRepository: NoticeRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        const val MAX_NOTICES_PER_DAY = 10
    }

    private val _notices = MutableStateFlow<Resource<List<Notice>>>(Resource.Idle())
    val notices: StateFlow<Resource<List<Notice>>> = _notices

    private val _createNoticeResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val createNoticeResult: StateFlow<Resource<Boolean>> = _createNoticeResult

    private val _deleteNoticeResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val deleteNoticeResult: StateFlow<Resource<Boolean>> = _deleteNoticeResult

    private val _reportNoticeResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val reportNoticeResult: StateFlow<Resource<Boolean>> = _reportNoticeResult

    private val _currentScope = MutableStateFlow(FeedScope.LOCALITY)
    val currentScope: StateFlow<FeedScope> = _currentScope

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _userProfile = MutableStateFlow<Resource<User?>>(Resource.Idle())
    val userProfile: StateFlow<Resource<User?>> = _userProfile

    private val _memberCount = MutableStateFlow<Resource<Long>>(Resource.Idle())
    val memberCount: StateFlow<Resource<Long>> = _memberCount

    val isGuest: Boolean
        get() = authRepository.getCurrentUser() == null

    private var currentUser: User? = null
    private var noticesJob: Job? = null

    val categories = listOf(
        "Announcement", "Jobs", "Rent/Flatmate", "Buy & Sell", 
        "Lost & Found", "Blood Donation", "Help Needed"
    )

    init {
        viewModelScope.launch {
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()
            fetchAndObserveUserProfile()
            cleanupExpiredNotices()
        }
    }

    private fun cleanupExpiredNotices() {
        viewModelScope.launch {
            noticeRepository.deleteExpiredNotices().collectLatest { }
        }
    }

    private fun fetchAndObserveUserProfile() {
        val uid = authRepository.getCurrentUser()?.uid
        if (uid == null) {
            _userProfile.value = Resource.Success(null)
            loadNotices() // Load guest notices
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
                    
                    resource.data?.district?.let { district ->
                        fetchMemberCount(district)
                    }

                    if (profileUpdated) {
                        loadNotices()
                        subscribeToLocationTopics()
                    }
                }
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

    private fun subscribeToLocationTopics() {
        val user = currentUser ?: return
        viewModelScope.launch {
            val districtTopic = "notices_${user.district.lowercase().replace(" ", "_")}"
            Log.d("NOTICE_DEBUG", "Subscribing to topic: $districtTopic")
            noticeRepository.subscribeToTopic(districtTopic).collectLatest { }
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadNotices()
    }

    fun loadNotices() {
        noticesJob?.cancel()
        noticesJob = viewModelScope.launch {
            val user = currentUser
            val category = _selectedCategory.value
            val search = _searchQuery.value
            
            if (isGuest) {
                Log.d("FIRESTORE_DEBUG", "loadNotices: guest mode")
                noticeRepository.getNotices(category, searchQuery = search).collectLatest {
                    _notices.value = it
                }
            } else if (user != null) {
                Log.d("FIRESTORE_DEBUG", "loadNotices: user mode, uid=${user.uid}")
                Log.d("FIRESTORE_DEBUG", "loadNotices: user=${user.name}, scope=${_currentScope.value}, category=$category, search=$search")
                when (_currentScope.value) {
                    FeedScope.LOCALITY -> noticeRepository.getNotices(category, locality = user.locality, searchQuery = search)
                    FeedScope.DISTRICT -> noticeRepository.getNotices(category, district = user.district, searchQuery = search)
                    FeedScope.STATE -> noticeRepository.getNotices(category, state = user.state, searchQuery = search)
                }.collectLatest {
                    _notices.value = it
                }
            }
        }
    }

    fun createNotice(
        title: String, 
        description: String, 
        category: String, 
        contact: String, 
        daysToExpiry: Int,
        pollQuestion: String? = null,
        pollOptions: List<String>? = null
    ) {
        Log.d("NOTICE_DEBUG", "createNotice button clicked in ViewModel")
        val user = currentUser
        if (user == null) {
            Log.e("NOTICE_DEBUG", "createNotice failed: currentUser is null")
            _createNoticeResult.value = Resource.Error("User profile not loaded. Please try again.")
            return
        }
        
        viewModelScope.launch {
            noticeRepository.getTodayNoticeCount(user.uid).collectLatest { resource ->
                if (resource is Resource.Success) {
                    val count = resource.data ?: 0
                    if (count >= MAX_NOTICES_PER_DAY) {
                        _createNoticeResult.value = Resource.Error("Daily posting limit reached. Please try again tomorrow.")
                        return@collectLatest
                    }
                    
                    val expiryDate = System.currentTimeMillis() + (daysToExpiry * 24 * 60 * 60 * 1000L)
                    val notice = Notice(
                        userId = user.uid,
                        userName = user.name,
                        userProfileImage = user.profileImage,
                        isVerified = user.isVerified,
                        title = title,
                        description = description,
                        category = category,
                        state = user.state,
                        district = user.district,
                        locality = user.locality,
                        expiryDate = expiryDate,
                        contactNumber = contact,
                        pollQuestion = pollQuestion,
                        pollOptions = pollOptions,
                        pollVotes = pollOptions?.associateWith { 0 }?.mapKeys { pollOptions.indexOf(it.key).toString() }
                    )
                    noticeRepository.createNotice(notice).collectLatest {
                        Log.d("NOTICE_DEBUG", "createNotice result in ViewModel: $it")
                        _createNoticeResult.value = it
                    }
                } else if (resource is Resource.Error) {
                    _createNoticeResult.value = Resource.Error(resource.message ?: "Failed to verify posting limit. Please check your connection.")
                }
            }
        }
    }

    fun resetCreateNoticeResult() {
        _createNoticeResult.value = Resource.Idle()
    }

    fun deleteNotice(noticeId: String) {
        viewModelScope.launch {
            noticeRepository.deleteNotice(noticeId).collectLatest {
                _deleteNoticeResult.value = it
            }
        }
    }

    fun resetDeleteNoticeResult() {
        _deleteNoticeResult.value = Resource.Idle()
    }

    fun reportNotice(noticeId: String, reason: String) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            noticeRepository.reportNotice(noticeId, uid, reason).collectLatest {
                _reportNoticeResult.value = it
            }
        }
    }

    fun resetReportNoticeResult() {
        _reportNoticeResult.value = Resource.Idle()
    }

    fun voteInPoll(noticeId: String, optionIndex: Int) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            noticeRepository.voteInPoll(noticeId, uid, optionIndex).collectLatest { }
        }
    }
}
