package dev.gymapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import dev.gymapp.BuildConfig
import dev.gymapp.PrTracksApplication
import dev.gymapp.ui.screens.ChatScreen
import dev.gymapp.ui.screens.DashboardScreen
import dev.gymapp.ui.screens.SignInScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppNavigation(app: PrTracksApplication) {
    val authState by app.authRepository.authState.collectAsState(initial = app.authRepository.authState.value)
    val scope = rememberCoroutineScope()
    var showDashboard by remember { mutableStateOf(false) }
    var isValidated by remember { mutableStateOf(false) }
    var cameFromSignIn by remember { mutableStateOf(false) }
    var hasAttemptedSilentSignIn by remember { mutableStateOf(false) }
    var isAttemptingSilentSignIn by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(authState.isSignedIn, authState.signedOutMessage) {
        if (!authState.isSignedIn) {
            isValidated = false
            showDashboard = false
            if (authState.signedOutMessage != null) {
                hasAttemptedSilentSignIn = true
            } else if (!hasAttemptedSilentSignIn) {
                hasAttemptedSilentSignIn = true
                isAttemptingSilentSignIn = true
                app.authRepository.refreshToken()
                isAttemptingSilentSignIn = false
            }
        } else {
            hasAttemptedSilentSignIn = false
        }
    }

    LaunchedEffect(authState.isSignedIn) {
        if (authState.isSignedIn && !isValidated) {
            val response = withContext(Dispatchers.IO) { app.api.me() }
            if (response.isSuccessful) {
                isValidated = true
            } else {
                val msg = "Server validation failed: HTTP ${response.code()}. Check server is running and reachable."
                app.authRepository.signOutDueToAuthFailure(msg)
            }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        snapshotFlow { lifecycleOwner.lifecycle.currentState }
            .collect { state ->
                if (state == Lifecycle.State.RESUMED && authState.isSignedIn && isValidated) {
                    runCatching {
                        val response = withContext(Dispatchers.IO) { app.api.me() }
                        if (!response.isSuccessful) {
                            app.authRepository.signOutDueToAuthFailure(
                                "Session expired or server unreachable (HTTP ${response.code()})"
                            )
                            isValidated = false
                        }
                    }.onFailure {
                        app.authRepository.signOutDueToAuthFailure(
                            "Session check failed: ${it.message ?: "Unknown error"}"
                        )
                        isValidated = false
                    }
                }
            }
    }

    if (authState.isSignedIn) {
        if (!isValidated) {
            if (cameFromSignIn) {
                SignInScreen(
                    signedOutMessage = authState.signedOutMessage,
                    isValidating = true,
                    onSignIn = { },
                    onDevSignIn = null
                )
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
        } else if (showDashboard) {
            DashboardScreen(app = app, onBack = { showDashboard = false })
        } else {
            LaunchedEffect(Unit) { cameFromSignIn = false }
            ChatScreen(app = app, onOpenDashboard = { showDashboard = true })
        }
    } else {
        LaunchedEffect(Unit) { cameFromSignIn = true }
        SignInScreen(
            signedOutMessage = authState.signedOutMessage,
            isValidating = isAttemptingSilentSignIn,
            onSignIn = {
                app.authRepository.clearSignedOutMessage()
                scope.launch {
                    val result = app.authRepository.signIn()
                    if (result.isFailure) {
                        app.authRepository.signOutDueToAuthFailure(
                            result.exceptionOrNull()?.message ?: "Sign-in failed"
                        )
                    }
                }
            },
            onDevSignIn = if (BuildConfig.DEBUG) {
                {
                    scope.launch {
                        val response = app.api.devToken()
                        if (response.isSuccessful) {
                            response.body()?.token?.let { app.authRepository.setTokenForTesting(it) }
                        } else {
                            app.authRepository.signOutDueToAuthFailure(
                                "Dev token failed: HTTP ${response.code()}. Is GYM_DEV_MODE=true?"
                            )
                        }
                    }
                }
            } else null
        )
    }
}
