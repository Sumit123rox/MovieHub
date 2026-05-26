package com.moviehub.feature.details.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.model.StreamItem
import com.moviehub.core.model.DownloadItem
import com.moviehub.core.model.DownloadState
import com.moviehub.core.network.DownloadsRepository
import com.moviehub.core.ui.components.GlassyBox
import com.moviehub.core.ui.components.TechnicalBadge
import com.moviehub.core.ui.components.shimmerEffect
import com.moviehub.core.ui.theme.MovieHubColors
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StreamDiscoveryIndicator(processed: Int, total: Int) {
    GlassyBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Searching Addons",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (total > 0) processed.toFloat() / total else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.15f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Searching $processed of $total addons...",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun StreamsScreen(
    id: String,
    type: String,
    mediaId: String? = null,
    onPlayClick: (stream: StreamItem, streams: List<StreamItem>, title: String?, posterUrl: String?) -> Unit,
    onBackClick: () -> Unit,
    viewModel: DetailsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val downloadsRepository: DownloadsRepository = koinInject()
    val profileRepository: com.moviehub.core.database.ProfileRepository = koinInject()
    val scope = rememberCoroutineScope()
    var activeTorrentStream by remember { mutableStateOf<StreamItem?>(null) }

    LaunchedEffect(id, type, mediaId) {
        viewModel.onAction(DetailsAction.LoadDetails(mediaId ?: id, type, null))
        viewModel.onAction(DetailsAction.LoadStreams(id, type))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        // Try backgroundUrl, fallback to posterUrl
        val backdropUrl = state.mediaItem?.backgroundUrl
            ?: state.mediaItem?.posterUrl
        val hasBackdrop = backdropUrl != null

        Box(modifier = Modifier.fillMaxSize()) {
            // ===== FULLSCREEN BACKDROP =====
            if (hasBackdrop) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.TopCenter)
                ) {
                    KamelImage(
                        resource = { asyncPainterResource(data = backdropUrl) },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onLoading = { Box(Modifier.fillMaxSize().shimmerEffect()) }
                    )
                    // Deep gradient — dark at edges, very transparent center
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Black.copy(alpha = 0.7f),
                                    )
                                )
                            )
                    )
                    // Additional radial gradient to keep edges dark while center is visible
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Black.copy(alpha = 0.7f),
                                    )
                                )
                            )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            ) {
                // ===== HEADER =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.background(
                            Color.Black.copy(alpha = 0.4f),
                            CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Choose Stream",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        state.mediaItem?.title?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1
                            )
                        }
                    }
                }

                // ===== STREAM CONTENT =====
                if (state.isSearchingStreams && state.streams.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Searching for streams...",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            repeat(5) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .shimmerEffect()
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 8.dp,
                            bottom = paddingValues.calculateBottomPadding() + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.isSearchingStreams) {
                            item {
                                StreamDiscoveryIndicator(
                                    processed = state.processedStreamAddons,
                                    total = state.totalStreamAddons
                                )
                            }
                        }

                        items(state.streams, key = { it.url ?: it.infoHash ?: it.name ?: "" }) { stream ->
                            StreamItemCard(
                                stream = stream,
                                onClick = {
                                    if (stream.isTorrentStream) {
                                        activeTorrentStream = stream
                                    } else {
                                        onPlayClick(stream, state.streams, state.mediaItem?.title, state.mediaItem?.posterUrl)
                                    }
                                },
                                onDownloadClick = if (stream.url != null || stream.externalUrl != null) { {
                                    val url = stream.url ?: stream.externalUrl ?: return@StreamItemCard
                                    val profileId = profileRepository.activeProfile.value?.id ?: return@StreamItemCard
                                    scope.launch {
                                        downloadsRepository.startDownload(
                                            DownloadItem(
                                                id = "${mediaId ?: id}_${url.hashCode().let { if (it < 0) -it else it }}",
                                                profileId = profileId,
                                                mediaId = mediaId ?: id,
                                                title = state.mediaItem?.title ?: stream.name ?: "Unknown",
                                                posterUrl = state.mediaItem?.posterUrl,
                                                url = url,
                                                headers = stream.behaviorHints.proxyHeaders?.request ?: emptyMap(),
                                                state = DownloadState.QUEUED
                                            )
                                        )
                                    }
                                } } else { null }
                            )
                        }

                        if (state.streams.isEmpty() && !state.isSearchingStreams) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.TheaterComedy,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.25f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No streams found",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Try adding more addons or check back later",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.35f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            activeTorrentStream?.let { torrentStream ->
                TorrentStreamDialog(
                    stream = torrentStream,
                    onDismiss = { activeTorrentStream = null }
                )
            }
        }
    }
}

@Composable
fun StreamItemCard(
    stream: StreamItem,
    onClick: () -> Unit,
    onDownloadClick: (() -> Unit)? = null
) {
    GlassyBox(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ===== FLAG / SOURCE ICON =====
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getFlagEmoji(stream.name ?: stream.description),
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // ===== DETAILS =====
            Column(modifier = Modifier.weight(1f)) {
                // Source name & file size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stream.name ?: "Unknown Source",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // File size badge
                    stream.behaviorHints.videoSize?.let { size ->
                        if (size > 0) {
                            Text(
                                text = formatBytes(size),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Addon name
                stream.addonName?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Technical badges row
                Spacer(modifier = Modifier.height(6.dp))
                val specs = extractTechnicalSpecs(stream.description)
                if (specs.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        specs.forEach { spec ->
                            TechnicalBadge(spec)
                        }
                    }
                }

                // Description / filename shown if no specs extracted
                if (specs.isEmpty() && !stream.description.isNullOrBlank()) {
                    Text(
                        text = stream.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ===== PLAY BUTTON =====
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // ===== DOWNLOAD BUTTON =====
            if (onDownloadClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onDownloadClick)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun getFlagEmoji(text: String?): String {
    if (text == null) return "🌐"
    val t = text.uppercase()
    return when {
        t.contains("INDIA") || t.contains(" IN ") || t.contains("[IN]") -> "🇮🇳"
        t.contains("USA") || t.contains(" US ") || t.contains("[US]") || t.contains("UNITED STATES") -> "🇺🇸"
        t.contains("UK") || t.contains("UNITED KINGDOM") -> "🇬🇧"
        t.contains("FR") || t.contains("FRANCE") -> "🇫🇷"
        t.contains("ES") || t.contains("SPAIN") -> "🇪🇸"
        t.contains("DE") || t.contains("GERMANY") -> "🇩🇪"
        t.contains("IT") || t.contains("ITALY") -> "🇮🇹"
        t.contains("BR") || t.contains("BRAZIL") -> "🇧🇷"
        t.contains("JP") || t.contains("JAPAN") -> "🇯🇵"
        t.contains("KR") || t.contains("SOUTH KOREA") -> "🇰🇷"
        else -> "🌐"
    }
}

private fun extractTechnicalSpecs(title: String?): List<String> {
    if (title == null) return emptyList()
    val specs = mutableListOf<String>()
    val lowerTitle = title.lowercase()

    if (lowerTitle.contains("4k") || lowerTitle.contains("2160p") || lowerTitle.contains("uhd")) specs.add("4K")
    if (lowerTitle.contains("1080p") || lowerTitle.contains("fhd")) specs.add("1080P")
    if (lowerTitle.contains("720p") || lowerTitle.contains("hd")) {
        if (!specs.contains("4K") && !specs.contains("1080P")) specs.add("720P")
    }

    if (lowerTitle.contains("hdr")) specs.add("HDR")
    if (lowerTitle.contains("dv") || lowerTitle.contains("dolby vision")) specs.add("DV")

    if (lowerTitle.contains("dts")) specs.add("DTS")
    if (lowerTitle.contains("atmos")) specs.add("ATMOS")
    if (lowerTitle.contains("5.1")) specs.add("5.1")
    if (lowerTitle.contains("7.1")) specs.add("7.1")

    if (lowerTitle.contains("hevc") || lowerTitle.contains("x265")) specs.add("HEVC")

    return specs
}

@Composable
fun TorrentStreamDialog(
    stream: StreamItem,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val magnetUrl = remember(stream) {
        when {
            stream.url?.trimStart()?.startsWith("magnet:", ignoreCase = true) == true -> stream.url
            stream.externalUrl?.trimStart()
                ?.startsWith("magnet:", ignoreCase = true) == true -> stream.externalUrl

            !stream.infoHash.isNullOrBlank() -> {
                var link = "magnet:?xt=urn:btih:${stream.infoHash}"
                val nameOrFilename = stream.behaviorHints.filename ?: stream.name
                if (!nameOrFilename.isNullOrBlank()) {
                    val encodedName = nameOrFilename
                        .replace(" ", "%20")
                        .replace("[", "%5B")
                        .replace("]", "%5D")
                    link += "&dn=$encodedName"
                }
                val trackers = listOf(
                    "udp://tracker.coppersurfer.tk:6969/announce",
                    "udp://tracker.openbittorrent.com:6969/announce",
                    "udp://tracker.opentrackr.org:1337/announce",
                    "udp://tracker.leechers-paradise.org:6969/announce",
                    "udp://tracker.internetwarriors.net:1337/announce",
                    "udp://open.demonii.com:1337/announce",
                    "udp://tracker.torrent.eu.org:451/announce",
                    "udp://tracker.cyberia.is:6969/announce",
                    "wss://tracker.openwebtorrent.com",
                    "wss://tracker.btorrent.xyz",
                    "wss://tracker.fastcast.nz"
                )
                trackers.forEach { tracker ->
                    val encodedTracker = tracker
                        .replace(":", "%3A")
                        .replace("/", "%2F")
                    link += "&tr=$encodedTracker"
                }
                link
            }

            else -> null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "P2P Torrent Stream",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Standard media players cannot play peer-to-peer torrent or magnet links directly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val fileName =
                            stream.behaviorHints.filename ?: stream.name ?: "Unknown File"
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sizeStr = formatBytes(stream.behaviorHints.videoSize)
                            TechnicalBadge(sizeStr)

                            stream.addonName?.let {
                                TechnicalBadge(it)
                            }
                        }

                        stream.infoHash?.let { hash ->
                            val displayHash = if (hash.length > 12) {
                                "${hash.take(6)}...${hash.takeLast(6)}"
                            } else {
                                hash
                            }
                            Text(
                                text = "Hash: $displayHash",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                    }
                }

                Text(
                    text = "Open this stream in an external player like VLC, Stremio, or TorrServe, or copy the magnet link for a Debrid / Torrent manager.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )

                AnimatedVisibility(visible = !errorMessage.isNullOrBlank(), enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                AnimatedVisibility(visible = !successMessage.isNullOrBlank(), enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = successMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MovieHubColors.Success,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (magnetUrl != null) {
                        try {
                            errorMessage = null
                            uriHandler.openUri(magnetUrl)
                        } catch (e: Exception) {
                            errorMessage =
                                "No compatible external player found. Try VLC or copy the magnet link."
                        }
                    } else {
                        errorMessage = "Failed to construct magnet URL."
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Play Externally", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (magnetUrl != null) {
                            clipboardManager.setText(AnnotatedString(magnetUrl))
                            successMessage = "Magnet link copied!"
                            errorMessage = null
                        } else {
                            errorMessage = "Failed to construct magnet URL."
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                ) {
                    Text("Copy Magnet")
                }

                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return "Unknown Size"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "${(gb * 10.0).toInt() / 10.0} GB"
        mb >= 1.0 -> "${(mb * 10.0).toInt() / 10.0} MB"
        else -> "$bytes B"
    }
}
