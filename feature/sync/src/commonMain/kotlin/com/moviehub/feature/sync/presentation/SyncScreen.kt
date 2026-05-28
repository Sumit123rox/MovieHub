package com.moviehub.feature.sync.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SyncDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moviehub.core.utils.currentTimeMillis
import com.moviehub.feature.sync.SyncState
import org.koin.compose.koinInject

@Composable
fun SyncScreen() {
    val viewModel: SyncViewModel = koinInject()
    val currentState: SyncState by viewModel.syncState.collectAsState()

    SyncContent(
        syncState = currentState,
        onSyncNow = viewModel::onSyncNow
    )
}

@Composable
private fun SyncContent(
    syncState: SyncState,
    onSyncNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Spacer(Modifier.weight(1f))

        // Status icon
        Icon(
            imageVector = if (syncState.isSyncing) Icons.Rounded.Sync
            else if (syncState.lastSyncError != null) Icons.Rounded.SyncDisabled
            else Icons.Rounded.CloudSync,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = if (syncState.lastSyncError != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
        )

        Text(
            text = if (syncState.isSyncing) "Syncing..."
            else if (syncState.lastSyncError != null) "Sync Error"
            else "Sync with Trakt & Real-Debrid",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        // Last sync time
        val timeMs = syncState.lastSyncTime
        if (timeMs != null) {
            val secondsAgo = (currentTimeMillis() - timeMs) / 1000
            val timeText: String = if (secondsAgo < 60L) "Just now"
            else if (secondsAgo < 3600L) "${secondsAgo / 60} min ago"
            else if (secondsAgo < 86400L) "${secondsAgo / 3600} h ago"
            else "${secondsAgo / 86400} d ago"
            Text(
                text = "Last synced: $timeText  -  ${syncState.itemsSynced} items",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Error message
        if (syncState.lastSyncError != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = syncState.lastSyncError,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Sync button
        Button(
            onClick = onSyncNow,
            enabled = !syncState.isSyncing,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            if (syncState.isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(if (syncState.isSyncing) "Syncing..." else "Sync Now")
        }

        Spacer(Modifier.weight(1f))
    }
}
