package com.example.jamuione.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.jamuione.BuildConfig
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.util.Resource
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override fun signInWithGoogle(context: Context): Flow<Resource<FirebaseUser>> = flow {
        emit(Resource.Loading())
        Log.d("AUTH_TRACE", "Google signup started")

        val credentialManager = CredentialManager.create(context)
        val webClientId = "651764912534-4ipea7aiql5c5j61fomp1r4ou7ph89nj.apps.googleusercontent.com"

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            Log.d("AUTH_TRACE", "Credential received: ${credential::class.java.simpleName}")
            
            when {
                credential is GoogleIdTokenCredential -> {
                    val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    authResult.user?.let {
                        Log.d("AUTH_TRACE", "Firebase Google signup success, UID: ${it.uid}")
                        emit(Resource.Success(it))
                    } ?: run {
                        Log.e("AUTH_TRACE", "Firebase Google signup failed: user is null")
                        emit(Resource.Error("User not found after sign-in"))
                    }
                }
                credential is CustomCredential && 
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    Log.d("AUTH_TRACE", "Google credential parsed from CustomCredential")

                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    authResult.user?.let {
                        Log.d("AUTH_TRACE", "Firebase Google signup success, UID: ${it.uid}")
                        emit(Resource.Success(it))
                    } ?: run {
                        emit(Resource.Error("User not found after sign-in"))
                    }
                }
                else -> {
                    Log.e("AUTH_TRACE", "Unexpected credential type: ${credential::class.java.name}")
                    emit(Resource.Error("Unexpected credential type: ${credential::class.java.simpleName}"))
                }
            }
        } catch (e: Exception) {
            Log.e("AUTH_TRACE", "Firebase Google signup failed", e)
            emit(Resource.Error(e.message ?: "Google Sign-In failed"))
        }
    }

    override fun signInWithEmail(email: String, password: String): Flow<Resource<FirebaseUser>> = flow {
        emit(Resource.Loading())
        if (BuildConfig.DEBUG) {
            Log.d("AUTH_TRACE", "Firebase login started for: $email")
        }
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                Log.d("AUTH_TRACE", "Firebase login success, UID: ${it.uid}")
                emit(Resource.Success(it))
            } ?: run {
                Log.e("AUTH_TRACE", "Firebase login failed: user is null")
                emit(Resource.Error("Login failed: User is null"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_TRACE", "Firebase login failed", e)
            emit(Resource.Error(e.message ?: "Login failed"))
        }
    }

    override fun signUpWithEmail(email: String, password: String): Flow<Resource<FirebaseUser>> = flow {
        emit(Resource.Loading())
        if (BuildConfig.DEBUG) {
            Log.d("AUTH_TRACE", "Firebase signup started for: $email")
        }
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                Log.d("AUTH_TRACE", "Firebase signup success, UID: ${it.uid}")
                emit(Resource.Success(it))
            } ?: run {
                Log.e("AUTH_TRACE", "Firebase signup failed: user is null")
                emit(Resource.Error("Registration failed: User is null"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_TRACE", "Firebase signup failed", e)
            emit(Resource.Error(e.message ?: "Registration failed"))
        }
    }

    override fun signOut() {
        Log.d("AUTH_TRACE", "Logout triggered")
        auth.signOut()
    }

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override fun isUserLoggedIn(): Boolean = auth.currentUser != null
}
