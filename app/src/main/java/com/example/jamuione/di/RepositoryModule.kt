package com.example.jamuione.di

import com.example.jamuione.data.repository.AuthRepositoryImpl
import com.example.jamuione.data.repository.NoticeRepositoryImpl
import com.example.jamuione.data.repository.NotificationRepositoryImpl
import com.example.jamuione.data.repository.OrganizationRepositoryImpl
import com.example.jamuione.data.repository.PostRepositoryImpl
import com.example.jamuione.data.repository.UserRepositoryImpl
import com.example.jamuione.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindPostRepository(
        postRepositoryImpl: PostRepositoryImpl
    ): PostRepository

    @Binds
    @Singleton
    abstract fun bindNoticeRepository(
        noticeRepositoryImpl: NoticeRepositoryImpl
    ): NoticeRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindOrganizationRepository(
        organizationRepositoryImpl: OrganizationRepositoryImpl
    ): OrganizationRepository
}
