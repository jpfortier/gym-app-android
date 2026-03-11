package dev.gymapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.gymapp.ui.chat.ChatMessage
import dev.gymapp.ui.chat.ChatRole
import dev.gymapp.ui.theme.OnTrainYellow
import dev.gymapp.ui.theme.TrainYellow

@Composable
fun ChatMessageItem(
    msg: ChatMessage,
    showSpinner: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.role == ChatRole.USER) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(
                    if (msg.role == ChatRole.USER) {
                        Modifier
                            .background(
                                TrainYellow,
                                RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                            )
                            .padding(12.dp)
                    } else {
                        Modifier.padding(4.dp)
                    }
                )
        ) {
            if (msg.content == "[Voice message]" || showSpinner) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showSpinner) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = if (msg.role == ChatRole.USER) {
                                OnTrainYellow
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (msg.role == ChatRole.USER) {
                                OnTrainYellow
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    Text(
                        text = if (showSpinner) "Sending…" else "Voice message",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (msg.role == ChatRole.USER) {
                            OnTrainYellow
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            } else {
                Text(
                    text = msg.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (msg.role == ChatRole.USER) {
                        OnTrainYellow
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
fun BottomBarMic(
    isRecording: Boolean,
    onVoiceTap: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onVoiceTap()
        },
        modifier = Modifier
            .size(56.dp)
            .background(TrainYellow, CircleShape)
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = if (isRecording) "Stop recording" else "Record voice",
            modifier = Modifier.size(28.dp),
            tint = if (isRecording) OnTrainYellow else OnTrainYellow
        )
    }
}

