package dev.gymapp.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
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
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(authState.isSignedIn) {
        if (!authState.isSignedIn) {
            isValidated = false
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
                    val response = withContext(Dispatchers.IO) { app.api.me() }
                    if (!response.isSuccessful) {
                        app.authRepository.signOutDueToAuthFailure(
                            "Session expired or server unreachable (HTTP ${response.code()})"
                        )
                        isValidated = false
                    }
                }
            }
    }

    if (authState.isSignedIn) {
        if (!isValidated) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Text(
                        text = "Validating...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else if (showDashboard) {
            DashboardScreen(app = app, onBack = { showDashboard = false })
        } else {
            ChatScreen(app = app, onOpenDashboard = { showDashboard = true })
        }
    } else {
        SignInScreen(
            signedOutMessage = authState.signedOutMessage,
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
