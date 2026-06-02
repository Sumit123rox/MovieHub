package com.moviehub.feature.sync.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SyncDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.moviehub.core.ui.components.MovieHubTopBar
import com.moviehub.core.ui.components.SmartStatusBar
import androidx.compose.ui.graphics.luminance
import com.moviehub.core.ui.theme.MovieHubDimens
import com.moviehub.core.utils.currentTimeMillis
import com.moviehub.feature.sync.SyncState
import org.koin.compose.koinInject

@Composable
fun SyncScreen(
    onNavigateToAuth: () -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    val viewModel: SyncViewModel = koinInject()
    val currentState: SyncState by viewModel.syncState.collectAsState()
    val isConnected by viewModel.isAccountConnected.collectAsState()

    val isSystemDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    SmartStatusBar(
        isDark = isSystemDark,
        color = MaterialTheme.colorScheme.background,
    )

    Scaffold(
        topBar = {
            MovieHubTopBar(
                title = "Cloud Sync",
                onBackClick = null,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        if (!isConnected) {
            SyncNotConnectedContent(
                modifier = Modifier.padding(paddingValues),
                onConnectAccount = onNavigateToAuth,
            )
        } else {
            SyncContent(
                modifier = Modifier.padding(paddingValues),
                syncState = currentState,
                onSyncNow = viewModel::onSyncNow,
            )
        }
    }
}

@Composable
private fun SyncNotConnectedContent(
    modifier: Modifier = Modifier,
    onConnectAccount: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MovieHubDimens.Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(MovieHubDimens.Icon.jumbo),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        )
        Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.lg))
        Text(
            text = "Not Connected",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.xs))
        Text(
            text = "Select a Sync Provider\nConnect Trakt or Real-Debrid in settings to synchronize your library, history, and watch progress across all your devices.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MovieHubDimens.Spacing.lg),
        )
        Spacer(modifier = Modifier.height(MovieHubDimens.Spacing.xxl))
        Button(
            onClick = onConnectAccount,
            shape = RoundedCornerShape(MovieHubDimens.Radius.md),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            contentPadding = PaddingValues(horizontal = MovieHubDimens.Spacing.xl, vertical = MovieHubDimens.Spacing.md),
        ) {
            Icon(imageVector = Icons.Rounded.Link, contentDescription = null)
            Spacer(modifier = Modifier.width(MovieHubDimens.Spacing.xs))
            Text("Connect Account", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SyncContent(
    modifier: Modifier = Modifier,
    syncState: SyncState,
    onSyncNow: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MovieHubDimens.Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MovieHubDimens.Spacing.lg, Alignment.CenterVertically),
    ) {
        Spacer(Modifier.weight(1f))

        // Status icon
        Icon(
            imageVector = if (syncState.isSyncing) {
                Icons.Rounded.Sync
            } else if (syncState.lastSyncError != null) {
                Icons.Rounded.SyncDisabled
            } else {
                Icons.Rounded.CloudSync
            },
            contentDescription = null,
            modifier = Modifier.size(MovieHubDimens.Icon.jumbo),
            tint = if (syncState.lastSyncError != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )

        Text(
            text = if (syncState.isSyncing) {
                "Syncing..."
            } else if (syncState.lastSyncError != null) {
                "Sync Error"
            } else {
                "Sync with Trakt & Real-Debrid"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )

        // Last sync time
        val timeMs = syncState.lastSyncTime
        if (timeMs != null) {
            val secondsAgo = (currentTimeMillis() - timeMs) / 1000
            val timeText: String = if (secondsAgo < 60L) {
                "Just now"
            } else if (secondsAgo < 3600L) {
                "${secondsAgo / 60} min ago"
            } else if (secondsAgo < 86400L) {
                "${secondsAgo / 3600} h ago"
            } else {
                "${secondsAgo / 86400} d ago"
            }
            Text(
                text = "Last synced: $timeText  -  ${syncState.itemsSynced} items",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Error message
        if (syncState.lastSyncError != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = syncState.lastSyncError,
                    modifier = Modifier.padding(MovieHubDimens.Spacing.md),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // Sync button
        Button(
            onClick = onSyncNow,
            enabled = !syncState.isSyncing,
            modifier = Modifier.fillMaxWidth(0.6f),
        ) {
            if (syncState.isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(MovieHubDimens.Icon.xs),
                    strokeWidth = MovieHubDimens.Spacing.dp2,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(MovieHubDimens.Spacing.sm))
            }
            Text(if (syncState.isSyncing) "Syncing..." else "Sync Now")
        }

        Spacer(Modifier.weight(1f))
    }
}
