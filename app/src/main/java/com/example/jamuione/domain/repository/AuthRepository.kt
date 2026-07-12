package com.example.jamuione.domain.repository

import android.content.Context
import com.example.jamuione.util.Resource
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun signInWithGoogle(context: Context): Flow<Resource<FirebaseUser>>
    fun signInWithEmail(email: String, password: String): Flow<Resource<FirebaseUser>>
    fun signUpWithEmail(email: String, password: String): Flow<Resource<FirebaseUser>>
    fun sendPasswordResetEmail(email: String): Flow<Resource<Boolean>>
    fun sendEmailVerification(): Flow<Resource<Boolean>>
    fun signOut()
    fun getCurrentUser(): FirebaseUser?
    fun isUserLoggedIn(): Boolean
}
