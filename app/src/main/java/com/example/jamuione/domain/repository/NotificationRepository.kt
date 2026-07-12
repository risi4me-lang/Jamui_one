package com.example.jamuione.domain.repository

import com.example.jamuione.domain.model.AppNotification
import com.example.jamuione.util.Resource
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(userId: String): Flow<Resource<List<AppNotification>>>
    fun markAsRead(userId: String, notificationId: String): Flow<Resource<Boolean>>
    fun getUnreadCount(userId: String): Flow<Int>
    fun deleteNotification(userId: String, notificationId: String): Flow<Resource<Boolean>>
}
