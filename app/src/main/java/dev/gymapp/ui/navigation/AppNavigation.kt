package dev.gymapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.gymapp.GymApplication
import dev.gymapp.ui.screens.ChatScreen
import dev.gymapp.ui.screens.SignInScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(app: GymApplication) {
    var isSignedIn by remember { mutableStateOf(app.authRepository.isSignedIn()) }
    var signedOutMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (isSignedIn) {
        ChatScreen()
    } else {
        SignInScreen(
            signedOutMessage = signedOutMessage,
            onSignIn = {
                signedOutMessage = null
                scope.launch {
                    val result = app.authRepository.signIn()
                    if (result.isSuccess) {
                        isSignedIn = true
                    } else {
                        signedOutMessage = result.exceptionOrNull()?.message ?: "Sign-in failed"
                    }
                }
            }
        )
    }
}
