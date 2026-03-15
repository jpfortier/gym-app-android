package dev.gymapp.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Base64
import dev.gymapp.BuildConfig
import dev.gymapp.R
import dev.gymapp.ui.theme.TrainYellow
import dev.gymapp.PrTracksApplication
import dev.gymapp.audio.AudioRecorder
import dev.gymapp.ui.components.AppSnackbarHost
import dev.gymapp.api.models.ApiError
import dev.gymapp.ui.chat.ChatRole
import dev.gymapp.ui.chat.ChatViewModel
import dev.gymapp.ui.chat.SafePrImageModal
import dev.gymapp.ui.chat.PrWithImage
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    app: PrTracksApplication = LocalContext.current.applicationContext as PrTracksApplication,
    onOpenDashboard: () -> Unit = {},
    viewModel: ChatViewModel = viewModel(
        key = "chat_${app.authRepository.currentToken ?: "signedout"}",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(app.api, app.chatRepository) as T
            }
        }
    )
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val audioRecorder = remember { AudioRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf<ApiError?>(null) }

    LaunchedEffect(state.lastError) {
        state.lastError?.let { err ->
            val result = snackbarHostState.showSnackbar(
                message = err.error,
                actionLabel = "Details",
                duration = SnackbarDuration.Indefinite
            )
            when (result) {
                SnackbarResult.ActionPerformed -> showErrorDialog = err
                SnackbarResult.Dismissed -> viewModel.clearError()
            }
        }
    }

    if (showErrorDialog != null) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = null
                viewModel.clearError()
            },
            title = { Text("Error details") },
            text = {
                val err = showErrorDialog!!
                Column {
                    Text("error: ${err.error}")
                    Text("code: ${err.code}")
                    Text("error_token: ${err.errorToken ?: "—"}")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showErrorDialog = null
                        viewModel.clearError()
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && isRecording) {
            scope.launch {
                audioRecorder.startRecording()
                if (!audioRecorder.isRecording()) return@launch
                delay(1000)
                var silenceCount = 0
                while (currentCoroutineContext().isActive && audioRecorder.isRecording()) {
                    delay(100)
                    val amp = audioRecorder.getMaxAmplitude()
                    if (amp in 1..800) {
                        silenceCount++
                        if (silenceCount >= 15) {
                            audioRecorder.stopAndGetBase64().onSuccess { base64 ->
                                viewModel.sendAudio(base64)
                            }
                            isRecording = false
                            return@launch
                        }
                    } else {
                        silenceCount = 0
                    }
                }
            }
        } else if (!granted) {
            isRecording = false
        }
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(69.dp)
                    .graphicsLayer { clip = false }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .graphicsLayer { clip = false },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(48.dp))
                        BottomBarMic(
                            isRecording = isRecording,
                            onVoiceTap = {
                                if (isRecording) {
                                    scope.launch {
                                        audioRecorder.stopAndGetBase64().onSuccess { base64 ->
                                            viewModel.sendAudio(base64)
                                        }
                                        isRecording = false
                                    }
                                } else {
                                    isRecording = true
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )
                    }
                }
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "Dashboard",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 16.dp)
                        .offset(y = 10.dp)
                        .size(158.dp)
                        .clickable { onOpenDashboard() }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(TrainYellow)
            )
            val lastMsg = state.messages.lastOrNull()
            val isPendingVoice = state.isLoading &&
                lastMsg != null &&
                lastMsg.role == ChatRole.USER &&
                lastMsg.content == "[Voice message]"

            val listState = rememberLazyListState()
            LaunchedEffect(state.messages.size) {
                if (state.messages.isNotEmpty()) {
                    listState.animateScrollToItem(state.messages.size - 1)
                }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    ChatMessageItem(
                        msg = msg,
                        showSpinner = msg == lastMsg && isPendingVoice
                    )
                }
                if (state.isLoading && !isPendingVoice && !state.messages.any { it.isPlaceholder }) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                val sampleLabels = mapOf(
                    "20260306_133927.m4a" to "Close grip bench 130",
                    "20260306_133935.m4a" to "Query close grip",
                    "20260306_133944.m4a" to "RDL + shoulder press",
                    "20260306_134002.m4a" to "Query deadlift",
                    "20260309_142607.m4a" to "John",
                    "20260309_142616.m4a" to "My name's Rocky",
                    "20260309_142623.m4a" to "Other"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    context.assets.list("samples")?.filter { it.endsWith(".m4a") }?.sorted()?.forEach { name ->
                        TextButton(
                            onClick = {
                                scope.launch {
                                    context.assets.open("samples/$name").use { it.readBytes() }.let { bytes ->
                                        viewModel.sendAudio(Base64.encodeToString(bytes, Base64.NO_WRAP))
                                    }
                                }
                            }
                        ) {
                            Text(sampleLabels[name] ?: name.removeSuffix(".m4a"))
                        }
                    }
                }
            }
            }

            state.pendingPrModal?.let { prs ->
                SafePrImageModal(
                    prs = prs,
                    onDismiss = { viewModel.dismissPrModal() },
                    onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                    onRetry = { prId -> viewModel.retryPrImage(prId) }
                )
            }
        }
    }
}
