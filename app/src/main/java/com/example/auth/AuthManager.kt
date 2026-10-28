package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {

    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUser.value = firebaseAuth.currentUser
    }

    init {
        try {
            _currentUser.value = auth.currentUser
            auth.addAuthStateListener(authStateListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Auth listener: ${e.message}", e)
        }
    }

    fun clearError() {
        _authErrorMessage.value = null
    }

    fun getWebClientId(): String? {
        return try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.w(TAG, "default_web_client_id string not found in resources: ${e.message}")
            null
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> {
        val clientId = getWebClientId()
            ?: return Result.failure(IllegalStateException("Google Sign-In Web Client ID is not configured in resources."))

        _isAuthenticating.value = true
        _authErrorMessage.value = null

        return try {
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                    ?: return Result.failure(IllegalStateException("Firebase user was null after Google sign-in."))

                _currentUser.value = user
                _isAuthenticating.value = false
                Log.i(TAG, "Successfully signed in user: ${user.displayName} (${user.email})")
                Result.success(user)
            } else {
                val err = "Unexpected credential format returned: ${credential::class.java.name}"
                _authErrorMessage.value = err
                _isAuthenticating.value = false
                Result.failure(IllegalStateException(err))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed by user: ${e.message}")
            _isAuthenticating.value = false
            Result.failure(e)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Google Sign-In failed"
            Log.e(TAG, "Google Sign-In failed: $msg", e)
            _authErrorMessage.value = msg
            _isAuthenticating.value = false
            Result.failure(e)
        }
    }

    suspend fun attemptAutoSignIn(): Result<FirebaseUser?> {
        val current = auth.currentUser
        if (current != null) {
            _currentUser.value = current
            return Result.success(current)
        }

        val clientId = getWebClientId() ?: return Result.success(null)

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            // Expected when user is not previously signed in on this device
            Log.d(TAG, "Silent auto-sign-in skipped: ${e.message}")
            Result.success(null)
        }
    }

    suspend fun signOut(): Result<Unit> {
        _isAuthenticating.value = true
        return try {
            auth.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _currentUser.value = null
            _isAuthenticating.value = false
            Log.i(TAG, "Successfully signed out user and cleared credential state.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear credential state during sign out: ${e.message}", e)
            _currentUser.value = null
            _isAuthenticating.value = false
            Result.success(Unit) // Local sign-out still completed
        }
    }

    companion object {
        private const val TAG = "AuthManager"
    }
}
