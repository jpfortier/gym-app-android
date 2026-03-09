package dev.gymapp.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dev.gymapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {

    private val credentialManager: CredentialManager? by lazy {
        runCatching { CredentialManager.create(context) }.getOrNull()
    }
    private var cachedToken: String? = null

    private val _signedOutMessage = MutableStateFlow<String?>(null)
    val signedOutMessage: StateFlow<String?> = _signedOutMessage.asStateFlow()

    private val _authState = MutableStateFlow(AuthState(cachedToken != null, null))
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentToken: String?
        get() = cachedToken

    private fun emitAuthState() {
        _authState.value = AuthState(cachedToken != null, _signedOutMessage.value)
    }

    data class AuthState(val isSignedIn: Boolean, val signedOutMessage: String?)

    suspend fun signIn(): Result<String> = signInInternal(filterByAuthorizedAccounts = false)

    suspend fun refreshToken(): Result<String> = signInInternal(filterByAuthorizedAccounts = true)

    private suspend fun signInInternal(filterByAuthorizedAccounts: Boolean): Result<String> = withContext(Dispatchers.IO) {
        val cm = credentialManager
        if (cm == null) {
            return@withContext Result.failure(
                IllegalStateException("Credential Manager not available (e.g. emulator without Google Play)")
            )
        }
        val serverClientId = BuildConfig.GOOGLE_CLIENT_ID_WEB
        if (serverClientId.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("GOOGLE_CLIENT_ID_WEB not configured. Add to build.gradle or local config.")
            )
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(serverClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = cm.getCredential(context, request)
            val credential = result.credential
            val idToken = when (credential) {
                is CustomCredential -> {
                    require(credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        "Unexpected credential type: ${credential.type}"
                    }
                    GoogleIdTokenCredential.createFrom(credential.data).idToken
                }
                else -> throw IllegalArgumentException("Unexpected credential type")
            }
            cachedToken = idToken
            _signedOutMessage.value = null
            emitAuthState()
            Result.success(idToken)
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(e)
        }
    }

    fun signOut() {
        cachedToken = null
        _signedOutMessage.value = null
        emitAuthState()
    }

    fun signOutDueToAuthFailure(message: String) {
        cachedToken = null
        _signedOutMessage.value = message
        emitAuthState()
    }

    fun clearSignedOutMessage() {
        _signedOutMessage.value = null
        emitAuthState()
    }

    fun isSignedIn(): Boolean = cachedToken != null

    fun setTokenForTesting(token: String) {
        cachedToken = token
        _signedOutMessage.value = null
        emitAuthState()
    }
}
