package com.moviehub.feature.addon.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moviehub.core.ui.components.GlassyBox
import com.moviehub.core.ui.components.TechnicalBadge
import kotlinx.coroutines.flow.collectLatest
import moviehub.core.ui.generated.resources.Res
import moviehub.core.ui.generated.resources.install_addon
import moviehub.core.ui.generated.resources.addon_url_hint
import moviehub.core.ui.generated.resources.install
import moviehub.core.ui.generated.resources.installed_addons
import moviehub.core.ui.generated.resources.no_addons_installed
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddonScreen(
    viewModel: AddonViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is AddonEvent.OpenUrl -> uriHandler.openUri(event.url)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Text(
                text = stringResource(Res.string.install_addon),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )

            GlassyBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = state.addonUrl,
                        onValueChange = { viewModel.onAction(AddonAction.UrlChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.addon_url_hint)) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.onAction(AddonAction.InstallAddon) },
                        modifier = Modifier.align(Alignment.End),
                        enabled = state.addonUrl.isNotBlank() && !state.isInstalling
                    ) {
                        if (state.isInstalling) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(20.dp).height(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(Res.string.install))
                        }
                    }
                }
            }

            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.installed_addons),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.installedAddons, key = { it.id }) { addon ->
                    GlassyBox(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = addon.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = addon.description ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    if (addon.behaviorHints.configurable) {
                                        IconButton(onClick = { viewModel.onAction(AddonAction.ConfigureAddon(addon.id)) }) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Configure",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    IconButton(onClick = { viewModel.onAction(AddonAction.RefreshAddon(addon.id)) }) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.onAction(AddonAction.RemoveAddon(addon.id)) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TechnicalBadge(text = "v${addon.version}")
                                addon.types.forEach { type ->
                                    TechnicalBadge(text = type.uppercase())
                                }
                            }
                        }
                    }
                }

                if (state.installedAddons.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.no_addons_installed),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
