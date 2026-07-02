package com.example.jamuione.domain.repository

import com.example.jamuione.domain.model.Notice
import com.example.jamuione.util.Resource
import kotlinx.coroutines.flow.Flow

interface NoticeRepository {
    fun getNotices(
        category: String? = null,
        locality: String? = null,
        district: String? = null,
        state: String? = null
    ): Flow<Resource<List<Notice>>>
    
    fun createNotice(notice: Notice): Flow<Resource<Boolean>>
    fun subscribeToTopic(topic: String): Flow<Resource<Unit>>
}
