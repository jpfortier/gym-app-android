package dev.gymapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.gymapp.update.UpdateHelper
import dev.gymapp.update.UpdateResult
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.gymapp.BuildConfig
import dev.gymapp.PrTracksApplication
import dev.gymapp.api.models.ApiError
import dev.gymapp.api.models.Pr
import dev.gymapp.api.models.Session
import dev.gymapp.ui.dashboard.DashboardViewModel
import dev.gymapp.ui.chat.PrImageModal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    app: PrTracksApplication = LocalContext.current.applicationContext as PrTracksApplication,
    onBack: () -> Unit,
    viewModel: DashboardViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(app.api) as T
            }
        }
    )
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showErrorDialog by remember { mutableStateOf<ApiError?>(null) }
    var updateCheckResult by remember { mutableStateOf<UpdateResult?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val updateHelper = remember { UpdateHelper(context) }

    LaunchedEffect(Unit) {
        updateCheckResult = updateHelper.checkForUpdate()
    }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.semantics { contentDescription = "Refresh" }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(
                        onClick = { app.authRepository.signOut() }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
                    }
                    when (val result = updateCheckResult) {
                        is UpdateResult.UpToDate -> {
                            Text(
                                text = "Up to date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .semantics { contentDescription = "Up to date" }
                                    .clickable {
                                        scope.launch {
                                            updateCheckResult = null
                                            updateCheckResult = updateHelper.checkForUpdate()
                                            when (val r = updateCheckResult) {
                                                is UpdateResult.Available -> {
                                                    snackbarHostState.showSnackbar(
                                                        "Update available: ${r.info.versionName}. Downloading...",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                    updateHelper.downloadAndInstall()
                                                        .onSuccess {
                                                            snackbarHostState.showSnackbar(
                                                                "Install when ready",
                                                                duration = SnackbarDuration.Short
                                                            )
                                                        }
                                                        .onFailure {
                                                            snackbarHostState.showSnackbar(
                                                                "Download failed: ${it.message}"
                                                            )
                                                        }
                                                }
                                                is UpdateResult.Error -> {
                                                    snackbarHostState.showSnackbar("Update check failed: ${r.message}")
                                                }
                                                else -> { }
                                            }
                                        }
                                    }
                            )
                        }
                        is UpdateResult.Available, is UpdateResult.Error -> {
                            IconButton(
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
                                                        snackbarHostState.showSnackbar(
                                                            "Install when ready",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                    .onFailure {
                                                        snackbarHostState.showSnackbar(
                                                            "Download failed: ${it.message}"
                                                        )
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
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardTile(
                        modifier = Modifier.weight(1f),
                        title = "Latest PR",
                        content = state.latestPr?.let { "${it.exerciseName} ${it.weight}×${it.reps}" } ?: "—",
                        imageBytes = state.latestPrImage,
                        onClick = state.latestPr?.let { pr ->
                            { viewModel.showPrModal(pr, state.latestPrImage) }
                        }
                    )
                    DashboardTile(
                        modifier = Modifier.weight(1f),
                        title = "Streak",
                        content = if (state.streakDays > 0) "${state.streakDays} day${if (state.streakDays == 1) "" else "s"}" else "—",
                        leadingIcon = Icons.Default.Whatshot
                    )
                }
            }

            if (state.recentPrsByType.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent PRs",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.recentPrsByType.forEach { (prType, prs) ->
                            prs.forEach { pr ->
                                RecentPrRow(
                                    pr = pr,
                                    prTypeLabel = formatPrType(prType),
                                    onClick = { viewModel.showPrModal(pr) }
                                )
                            }
                        }
                    }
                }
            }

            if (state.exerciseCategories.isNotEmpty()) {
                item {
                    Text(
                        text = "Exercise types",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(state.exerciseCategories) { category ->
                            ExerciseCategoryCard(categoryName = category)
                        }
                    }
                }
            }

            if (state.sessions.isNotEmpty()) {
                item {
                    Text(
                        text = "Activity timeline",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.sessions.take(15)) { session ->
                    SessionTimelineRow(session = session)
                }
            }

            item {
                Text(
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        state.selectedPrModal?.let { prWithImage ->
            PrImageModal(
                prs = listOf(prWithImage),
                onDismiss = { viewModel.dismissPrModal() }
            )
        }
        }
    }
}

private fun formatPrType(prType: String): String =
    prType.split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

@Composable
private fun RecentPrRow(
    pr: Pr,
    prTypeLabel: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pr.exerciseName + if (pr.variantName != "standard") " (${pr.variantName})" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = prTypeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${pr.weight}×${pr.reps}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ExerciseCategoryCard(categoryName: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = categoryName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SessionTimelineRow(session: Session) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = session.date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (!session.entries.isNullOrEmpty()) {
                Text(
                    text = session.entries.joinToString { it.exerciseName },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DashboardTile(
    modifier: Modifier = Modifier,
    title: String,
    content: String,
    imageBytes: ByteArray? = null,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.size(160.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (imageBytes != null) {
                AsyncImage(
                    model = imageBytes,
                    contentDescription = "PR image",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
