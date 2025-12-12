package com.theblankstate.epmanager.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Loading : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor() {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    val currentUser: FirebaseUser?
        get() = auth.currentUser
    
    val isLoggedIn: Boolean
        get() = currentUser != null
    
    /**
     * Observe auth state changes
     */
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { 
                AuthResult.Success(it) 
            } ?: AuthResult.Error("Sign in failed")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign in failed")
        }
    }
    
    /**
     * Create account with email and password
     */
    suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { 
                AuthResult.Success(it) 
            } ?: AuthResult.Error("Sign up failed")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign up failed")
        }
    }
    
    /**
     * Sign in with Google credential (ID token from Google Sign-In)
     */
    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.let { 
                AuthResult.Success(it) 
            } ?: AuthResult.Error("Google sign in failed")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Google sign in failed")
        }
    }
    
    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign out
     */
    fun signOut() {
        auth.signOut()
    }
    
    /**
     * Delete account
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
