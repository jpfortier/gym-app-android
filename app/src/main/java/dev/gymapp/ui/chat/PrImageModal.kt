package dev.gymapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.gymapp.api.models.PersonalRecord

private val FOOTER_BG = Color(0xFF181818)
private const val WIDTH_FRACTION = 0.9f

data class PrWithImage(
    val pr: PersonalRecord,
    val imageBytes: ByteArray? = null,
    val imageLoadFailed: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PrWithImage
        if (pr != other.pr) return false
        if (imageBytes != null) {
            if (other.imageBytes == null) return false
            if (!imageBytes.contentEquals(other.imageBytes)) return false
        } else if (other.imageBytes != null) return false
        if (imageLoadFailed != other.imageLoadFailed) return false
        return true
    }

    override fun hashCode(): Int {
        var result = pr.hashCode()
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        result = 31 * result + imageLoadFailed.hashCode()
        return result
    }
}

@Composable
fun SafePrImageModal(
    prs: List<PrWithImage>,
    onDismiss: () -> Unit,
    onError: ((String) -> Unit)? = null,
    onRetry: ((prId: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    try {
        PrImageModal(prs = prs, onDismiss = onDismiss, onRetry = onRetry, modifier = modifier)
    } catch (e: Throwable) {
        onError?.invoke(e.message ?: "Could not display image")
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(enabled = true, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(24.dp),
                onClick = { },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Could not display image",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
        }
    }
}

@Composable
fun PrImageModal(
    prs: List<PrWithImage>,
    onDismiss: () -> Unit,
    onRetry: ((prId: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val pr = prs.getOrNull(currentIndex) ?: return
    val hasImage = pr.imageBytes != null
    val loadFailed = pr.imageLoadFailed

    val config = LocalConfiguration.current
    val screenWidthDp = config.screenWidthDp.dp
    val modalWidth = screenWidthDp * WIDTH_FRACTION

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = modalWidth)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp, pressedElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (prs.size > 1) {
                            IconButton(
                                onClick = {
                                    currentIndex = (currentIndex - 1 + prs.size) % prs.size
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous"
                                )
                            }
                        } else {
                            Box(modifier = Modifier.size(40.dp))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FOOTER_BG),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasImage) {
                                val context = LocalContext.current
                                val imageRequest = remember(pr.imageBytes) {
                                    pr.imageBytes?.let { bytes ->
                                        ImageRequest.Builder(context)
                                            .data(bytes)
                                            .size(1024)
                                            .build()
                                    }
                                }
                                if (imageRequest != null) {
                                    AsyncImage(
                                        model = imageRequest,
                                        contentDescription = "${pr.pr.exerciseName} PR image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        error = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(FOOTER_BG),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Could not load image",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    )
                                }
                            } else if (loadFailed) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Image not available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    onRetry?.let { retry ->
                                        TextButton(onClick = { retry(pr.pr.id) }) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        if (prs.size > 1) {
                            IconButton(
                                onClick = {
                                    currentIndex = (currentIndex + 1) % prs.size
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next"
                                )
                            }
                        } else {
                            Box(modifier = Modifier.size(40.dp))
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FOOTER_BG)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = formatPrCaption(pr.pr),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .padding(top = (-8).dp, end = (-8).dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun formatPrCaption(pr: PersonalRecord): String = runCatching {
    val variant = if (pr.variantName == "standard") "" else " (${pr.variantName})"
    "${pr.exerciseName}$variant ${pr.weight.toInt()}×${pr.reps}"
}.getOrElse { "PR" }
