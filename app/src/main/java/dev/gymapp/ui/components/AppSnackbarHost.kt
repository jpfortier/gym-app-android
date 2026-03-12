package dev.gymapp.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val InstallWhenReadyBackground = Color(0xFF181818)
private val InstallWhenReadyBorder = Color.Yellow

@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data ->
            val isInstallWhenReady = data.visuals.message == "Install when ready"
            if (isInstallWhenReady) {
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                        .border(2.dp, InstallWhenReadyBorder),
                    containerColor = InstallWhenReadyBackground
                )
            } else {
                Snackbar(snackbarData = data)
            }
        }
    )
}
