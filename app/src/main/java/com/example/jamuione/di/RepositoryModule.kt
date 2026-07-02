package com.example.jamuione.di

import com.example.jamuione.data.repository.AuthRepositoryImpl
import com.example.jamuione.data.repository.NoticeRepositoryImpl
import com.example.jamuione.data.repository.PostRepositoryImpl
import com.example.jamuione.data.repository.UserRepositoryImpl
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.NoticeRepository
import com.example.jamuione.domain.repository.PostRepository
import com.example.jamuione.domain.repository.UserRepository
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
}
