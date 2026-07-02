package com.example.jamuione.di

import android.content.Context
import androidx.room.Room
import com.example.jamuione.data.local.AppDatabase
import com.example.jamuione.data.local.dao.PostDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "jamui_one_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun providePostDao(database: AppDatabase): PostDao {
        return database.postDao()
    }
}
