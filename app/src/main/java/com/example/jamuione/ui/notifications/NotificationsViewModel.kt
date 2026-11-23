package com.example.jamuione.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.AppNotification
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.NotificationRepository
import com.example.jamuione.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<Resource<List<AppNotification>>>(Resource.Idle())
    val notifications: StateFlow<Resource<List<AppNotification>>> = _notifications

    val hasUnread: StateFlow<Boolean> = authRepository.getCurrentUser()?.uid?.let { uid ->
        notificationRepository.getUnreadCount(uid)
            .map { count -> count > 0 }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )
    } ?: MutableStateFlow(false)

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            notificationRepository.getNotifications(uid).collectLatest {
                _notifications.value = it
            }
        }
    }

    fun markAsRead(notificationId: String) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            notificationRepository.markAsRead(uid, notificationId).collectLatest { }
        }
    }

    fun deleteNotification(notificationId: String) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            notificationRepository.deleteNotification(uid, notificationId).collectLatest { }
        }
    }
}
