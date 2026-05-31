package com.moviehub.feature.addon.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moviehub.core.ui.components.ContentCard
import com.moviehub.core.ui.components.TechnicalBadge
import com.moviehub.core.ui.theme.MovieHubColors
import kotlinx.coroutines.flow.collectLatest
import moviehub.core.ui.generated.resources.Res
import moviehub.core.ui.generated.resources.addon_url_hint
import moviehub.core.ui.generated.resources.install
import moviehub.core.ui.generated.resources.install_addon
import moviehub.core.ui.generated.resources.installed_addons
import moviehub.core.ui.generated.resources.no_addons_installed
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddonScreen(
    addonViewModel: AddonViewModel = koinViewModel(),
    pluginsViewModel: PluginsViewModel = koinViewModel(),
) {
    val addonState by addonViewModel.state.collectAsState()
    val pluginsState by pluginsViewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        addonViewModel.event.collectLatest { event ->
            when (event) {
                is AddonEvent.OpenUrl -> uriHandler.openUri(event.url)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Screen Header
            Text(
                text = "External Providers",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontWeight = FontWeight.Bold,
            )

            // Tabs to switch between Stremio Addons and JS Scrapers
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                },
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Stremio Addons",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "JS Scrapers",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // Stremio Addons Content
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            ContentCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = stringResource(Res.string.install_addon),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = addonState.addonUrl,
                                        onValueChange = { addonViewModel.onAction(AddonAction.UrlChanged(it)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(stringResource(Res.string.addon_url_hint)) },
                                        singleLine = true,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { addonViewModel.onAction(AddonAction.InstallAddon) },
                                        modifier = Modifier.align(Alignment.End),
                                        enabled = addonState.addonUrl.isNotBlank() && !addonState.isInstalling,
                                    ) {
                                        if (addonState.isInstalling) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.width(20.dp).height(20.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Text(stringResource(Res.string.install))
                                        }
                                    }
                                }
                            }
                        }

                        addonState.error?.let {
                            item {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        }

                        addonState.successMessage?.let {
                            item {
                                Text(
                                    text = it,
                                    color = MovieHubColors.Success,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        }

                        item {
                            Text(
                                text = stringResource(Res.string.installed_addons),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        if (addonState.installedAddons.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(Res.string.no_addons_installed),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        } else {
                            items(addonState.installedAddons, key = { it.id }) { addon ->
                                val addonUrl = addonState.addonUrls[addon.id]
                                ContentCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { },
                                            onLongClick = {
                                                addonUrl?.let { url ->
                                                    clipboardManager.setText(AnnotatedString(url))
                                                }
                                            },
                                        ),
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = addon.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                                var descExpanded by remember { mutableStateOf(false) }
                                                val descMaxLines = if (descExpanded) Int.MAX_VALUE else 2
                                                Text(
                                                    text = addon.description ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = descMaxLines,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                if ((addon.description?.length ?: 0) > 100) {
                                                    Text(
                                                        text = if (descExpanded) "Show less" else "Show more",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.clickable { descExpanded = !descExpanded },
                                                    )
                                                }
                                            }
                                            Row {
                                                if (addon.behaviorHints.configurable) {
                                                    IconButton(onClick = { addonViewModel.onAction(AddonAction.ConfigureAddon(addon.id)) }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Settings,
                                                            contentDescription = "Configure",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                        )
                                                    }
                                                }
                                                IconButton(onClick = { addonViewModel.onAction(AddonAction.RefreshAddon(addon.id)) }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Refresh",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                                IconButton(onClick = { addonViewModel.onAction(AddonAction.RemoveAddon(addon.id)) }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
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
                        }
                    }
                }

                1 -> {
                    // JS Scrapers Content
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Global toggle
                        item {
                            ContentCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Enable JS Scrapers",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "Use local JS scraper plugins to discover streams",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Switch(
                                        checked = pluginsState.coreState.pluginsEnabled,
                                        onCheckedChange = { pluginsViewModel.onAction(PluginsAction.TogglePluginsEnabled(it)) },
                                    )
                                }
                            }
                        }

                        // Install Repository form
                        item {
                            ContentCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Install Scraper Repository",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = pluginsState.repoUrlInput,
                                        onValueChange = { pluginsViewModel.onAction(PluginsAction.UrlInputChanged(it)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Repository Manifest URL (manifest.json)") },
                                        singleLine = true,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { pluginsViewModel.onAction(PluginsAction.InstallRepository) },
                                        modifier = Modifier.align(Alignment.End),
                                        enabled = pluginsState.repoUrlInput.isNotBlank() && !pluginsState.coreState.isInstalling,
                                    ) {
                                        if (pluginsState.coreState.isInstalling) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.width(20.dp).height(20.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Text("Install Repo")
                                        }
                                    }
                                }
                            }
                        }

                        pluginsState.coreState.error?.let {
                            item {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        }

                        pluginsState.coreState.successMessage?.let {
                            item {
                                Text(
                                    text = it,
                                    color = MovieHubColors.Success,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        }

                        // Installed Repositories list
                        item {
                            Text(
                                text = "Installed Repositories",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        if (pluginsState.coreState.repositories.isEmpty()) {
                            item {
                                Text(
                                    text = "No scraper repositories installed yet.",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        } else {
                            items(pluginsState.coreState.repositories, key = { it.manifestUrl }) { repo ->
                                ContentCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { },
                                            onLongClick = {
                                                clipboardManager.setText(AnnotatedString(repo.manifestUrl))
                                            },
                                        ),
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = repo.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                                Text(
                                                    text = repo.manifestUrl,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            Row {
                                                IconButton(
                                                    onClick = { pluginsViewModel.onAction(PluginsAction.RefreshRepository(repo.manifestUrl)) },
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Refresh",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { pluginsViewModel.onAction(PluginsAction.RemoveRepository(repo.manifestUrl)) },
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TechnicalBadge(text = "${repo.scraperCount} scrapers")
                                            repo.version?.let { TechnicalBadge(text = "v$it") }
                                            if (repo.isRefreshing) {
                                                TechnicalBadge(text = "Refreshing...")
                                            }
                                        }
                                        repo.errorMessage?.let {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = it,
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Scrapers list
                        item {
                            Text(
                                text = "Scraper Providers",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        if (pluginsState.coreState.scrapers.isEmpty()) {
                            item {
                                Text(
                                    text = "No scraper providers available.",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        } else {
                            items(pluginsState.coreState.scrapers, key = { it.id }) { scraper ->
                                ContentCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = scraper.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                                Text(
                                                    text = scraper.description.ifBlank { "No description provided." },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            Switch(
                                                checked = scraper.enabled,
                                                onCheckedChange = { pluginsViewModel.onAction(PluginsAction.ToggleScraper(scraper.id, it)) },
                                                enabled = scraper.manifestEnabled && pluginsState.coreState.pluginsEnabled,
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TechnicalBadge(text = scraper.supportedTypes.joinToString(" | ").uppercase())
                                            TechnicalBadge(text = "v${scraper.version}")
                                            if (!scraper.manifestEnabled) {
                                                TechnicalBadge(text = "Disabled by repo")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
