package dev.gymapp.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.gymapp.R
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Base64
import dev.gymapp.BuildConfig
import dev.gymapp.GymApplication
import dev.gymapp.audio.AudioRecorder
import dev.gymapp.api.models.ApiError
import dev.gymapp.ui.chat.ChatMessage
import dev.gymapp.ui.chat.ChatRole
import dev.gymapp.ui.chat.ChatViewModel
import dev.gymapp.ui.chat.InputMode
import dev.gymapp.ui.theme.OnTrainYellow
import dev.gymapp.ui.theme.TrainYellow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    app: GymApplication = LocalContext.current.applicationContext as GymApplication,
    onOpenDashboard: () -> Unit = {},
    viewModel: ChatViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(app.api) as T
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = rememberLazyListState(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(state.messages, key = { it.id }) { msg ->
                ChatMessageItem(msg)
            }
            if (state.isLoading) {
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
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            ChatInputBar(
            inputMode = state.inputMode,
            isRecording = isRecording,
            onOpenDashboard = onOpenDashboard,
            onModeToggle = { viewModel.setInputMode(if (state.inputMode == InputMode.TEXT) InputMode.VOICE else InputMode.TEXT) },
            onSendText = { viewModel.sendText(it) },
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
    }
}

@Composable
private fun ChatMessageItem(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.role == ChatRole.USER) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .then(
                    if (msg.role == ChatRole.USER) {
                        Modifier
                            .background(
                                TrainYellow,
                                RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    } else {
                        Modifier.padding(4.dp)
                    }
                )
        ) {
            if (msg.content == "[Voice message]") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (msg.role == ChatRole.USER) OnTrainYellow else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Voice message",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (msg.role == ChatRole.USER) OnTrainYellow else MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Text(
                    text = msg.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (msg.role == ChatRole.USER) OnTrainYellow else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private val FooterButtonSize = 48.dp
private val FooterButtonRadius = 5.dp

@Composable
private fun ChatInputBar(
    inputMode: InputMode,
    isRecording: Boolean,
    onOpenDashboard: () -> Unit,
    onModeToggle: () -> Unit,
    onSendText: (String) -> Unit,
    onVoiceTap: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(FooterButtonSize)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(FooterButtonRadius)
                )
                .clickable(onClick = onOpenDashboard),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.app_icon),
                contentDescription = "Dashboard",
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.size(8.dp))

        when (inputMode) {
            InputMode.TEXT -> {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.size(8.dp))
                TextButton(
                    onClick = {
                        onSendText(text)
                        text = ""
                    }
                ) {
                    Text("Send")
                }
                Spacer(modifier = Modifier.size(8.dp))
                Box(
                    modifier = Modifier
                        .size(FooterButtonSize)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(FooterButtonRadius)
                        )
                        .clickable(onClick = onModeToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Switch to voice",
                        tint = TrainYellow,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            InputMode.VOICE -> {
                Box(
                    modifier = Modifier
                        .size(FooterButtonSize)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(FooterButtonRadius)
                        )
                        .clickable(onClick = onModeToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Switch to keyboard",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRecording) "Tap to stop" else "Tap mic to record",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(FooterButtonSize)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(FooterButtonRadius)
                        )
                        .clickable(onClick = onVoiceTap),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop recording" else "Record voice",
                        tint = if (isRecording) MaterialTheme.colorScheme.error else TrainYellow,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
