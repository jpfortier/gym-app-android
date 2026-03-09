package dev.gymapp.ui.navigation

import dev.gymapp.BuildConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.gymapp.GymApplication
import dev.gymapp.ui.screens.ChatScreen
import dev.gymapp.ui.screens.DashboardScreen
import dev.gymapp.ui.screens.SignInScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(app: GymApplication) {
    val authState by app.authRepository.authState.collectAsState(initial = app.authRepository.authState.value)
    val scope = rememberCoroutineScope()
    var showDashboard by remember { mutableStateOf(false) }

    if (authState.isSignedIn) {
        if (showDashboard) {
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
                        }
                    }
                }
            } else null
        )
    }
}
