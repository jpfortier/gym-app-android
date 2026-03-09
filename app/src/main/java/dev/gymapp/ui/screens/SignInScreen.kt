package dev.gymapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.gymapp.BuildConfig
import dev.gymapp.R
import androidx.compose.runtime.LaunchedEffect
import dev.gymapp.update.UpdateHelper
import dev.gymapp.update.UpdateResult
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    signedOutMessage: String? = null,
    onSignIn: () -> Unit,
    onDevSignIn: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val updateHelper = remember { UpdateHelper(context) }
    var updateCheckResult by remember { mutableStateOf<UpdateResult?>(null) }

    LaunchedEffect(Unit) {
        updateCheckResult = updateHelper.checkForUpdate()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "Gym App logo",
                    modifier = Modifier.size(120.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (signedOutMessage != null) {
                    Text(
                        text = signedOutMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(onClick = onSignIn) {
                    Text("Sign in with Google")
                }

                if (onDevSignIn != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDevSignIn,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Dev sign-in")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (val result = updateCheckResult) {
                is UpdateResult.UpToDate -> {
                    Text(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        text = "Up to date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is UpdateResult.Available, is UpdateResult.Error -> {
                    IconButton(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        onClick = {
                            scope.launch {
                                when (val r = updateHelper.checkForUpdate()) {
                                    is UpdateResult.UpToDate -> {
                                        updateCheckResult = r
                                        snackbarHostState.showSnackbar("App is up to date")
                                    }
                                    is UpdateResult.Available -> {
                                        snackbarHostState.showSnackbar(
                                            "Update available: ${r.info.versionName}. Downloading...",
                                            duration = SnackbarDuration.Short
                                        )
                                        updateHelper.downloadAndInstall()
                                            .onSuccess {
                                                snackbarHostState.showSnackbar("Install when ready", duration = SnackbarDuration.Short)
                                            }
                                            .onFailure {
                                                snackbarHostState.showSnackbar("Download failed: ${it.message}")
                                            }
                                    }
                                    is UpdateResult.Error -> {
                                        snackbarHostState.showSnackbar("Update check failed: ${r.message}")
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = "Check for update")
                    }
                }
                null -> { }
            }
        }
    }
}
