package com.theblankstate.epmanager.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class for Google Sign-In using Credential Manager API
 */
class GoogleSignInHelper(private val context: Context) {
    
    private val credentialManager = CredentialManager.create(context)
    
    // Web client ID from google-services.json (client_type: 3)
    // This is the OAuth 2.0 Web Client ID from Firebase Console
    companion object {
        const val WEB_CLIENT_ID = "433679771016-og24d0m53ke0csvf5te23krs5d63gc6s.apps.googleusercontent.com"
    }
    
    /**
     * Launches Google Sign-In flow and returns the ID token on success
     */
    suspend fun signIn(): Result<String> = withContext(Dispatchers.Main) {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Allow any Google account
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false) // Don't auto-select, let user choose
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            val result = credentialManager.getCredential(
                request = request,
                context = context as android.app.Activity
            )
            
            handleSignInResult(result)
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Google Sign-In failed: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Google Sign-In error: ${e.message}"))
        }
    }
    
    private fun handleSignInResult(result: GetCredentialResponse): Result<String> {
        return when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        Result.success(googleIdTokenCredential.idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Result.failure(Exception("Failed to parse Google ID token: ${e.message}"))
                    }
                } else {
                    Result.failure(Exception("Unexpected credential type"))
                }
            }
            else -> Result.failure(Exception("Unexpected credential type"))
        }
    }
}
