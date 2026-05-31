package com.moviehub.feature.player.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import co.touchlab.kermit.Logger
import com.moviehub.core.model.PlayerPlaybackState
import com.moviehub.core.model.SubtitleStyle
import com.moviehub.core.model.VideoScale
import com.moviehub.core.player.MoviePlayer
import com.moviehub.core.player.MoviePlayerController
import com.moviehub.feature.player.mpv.MpvStateMapper
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpvbridge.MpvPlayerBridge
import org.koin.compose.koinInject
import platform.UIKit.UIColor
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
class IosMoviePlayer(
    private val mpvBridge: MpvPlayerBridge,
) : MoviePlayer {
    private val _playbackState = MutableStateFlow(PlayerPlaybackState())
    override val playbackState: StateFlow<PlayerPlaybackState> = _playbackState.asStateFlow()

    fun updateState(state: PlayerPlaybackState) {
        _playbackState.value = state
    }

    override fun play() {
        mpvBridge.play()
    }

    override fun pause() {
        mpvBridge.pause()
    }

    override fun seekTo(positionMs: Long) {
        mpvBridge.seekTo(positionMs / 1000.0)
    }

    override fun setSpeed(speed: Float) {
        mpvBridge.setSpeed(speed.toDouble())
    }

    override fun setVolume(volume: Float) {
        mpvBridge.setVolume(volume.toDouble())
    }

    override fun selectAudioTrack(trackIndex: Int) {
        mpvBridge.selectAudioTrackWithTrackIndex(trackIndex)
    }

    override fun selectSubtitleTrack(trackIndex: Int) {
        mpvBridge.selectSubtitleTrackWithTrackIndex(trackIndex)
    }

    override fun selectVideoTrack(trackIndex: Int) {
        // No-op for iOS target
    }

    override fun setVideoScale(scale: VideoScale) {
        mpvBridge.setVideoScale(scale.ordinal)
    }

    override fun setSubtitleStyle(style: SubtitleStyle) {
        if (style.fontColorArgb != -1) {
            mpvBridge.setSubtitleColor(style.fontColorArgb)
        }
        mpvBridge.setSubtitleBold(style.isBold)
        mpvBridge.setSubtitleShadow(style.hasDropShadow)
        mpvBridge.setSubtitleFontSize(style.fontSizeSp)
        mpvBridge.setSubtitleMargin(style.bottomMarginPercent)
    }

    override fun release() {
        mpvBridge.destroy()
    }
}

private val iosPlayerLogger = Logger.withTag("MpvVideoPlayer.iOS")

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    headers: Map<String, String>,
    onPlaybackStateChanged: (PlayerPlaybackState) -> Unit,
    forceLandscape: Boolean,
    brightness: Float,
    videoScale: VideoScale,
    subtitleBottomMargin: Int,
    subtitleStyle: SubtitleStyle,
    drmLicenseUrl: String?,
    drmScheme: String?,
    modifier: Modifier,
) {
    val playerController: MoviePlayerController = koinInject()
    if (drmLicenseUrl != null) {
        iosPlayerLogger.w { "DRM license URL provided ($drmScheme) — mpv supports this natively." }
    }

    // ═══ Force landscape orientation ═══
    DisposableEffect(forceLandscape) {
        if (forceLandscape) {
            forceLandscapeOrientation()
        }
        onDispose {
            if (forceLandscape) {
                unlockOrientation()
            }
        }
    }

    var hasStarted by remember { mutableStateOf(false) }

    val mpvBridge = remember {
        val bridge = MpvPlayerBridge()
        bridge.backgroundColor = UIColor.blackColor
        bridge
    }

    val platformPlayer = remember(mpvBridge) {
        IosMoviePlayer(mpvBridge)
    }

    DisposableEffect(platformPlayer) {
        playerController.registerPlayer(platformPlayer)
        onDispose {
            playerController.unregisterPlayer(platformPlayer)
        }
    }

    val interceptedStateChanged: (PlayerPlaybackState) -> Unit = { state ->
        onPlaybackStateChanged(state)
        platformPlayer.updateState(state)
    }

    // State callback: NSDictionary → PlayerPlaybackState.
    // Stored via remember so Kotlin/Native GC can't free the block
    // while mpv still holds it (prevents crash on back press).
    DisposableEffect(mpvBridge) {
        // Send initial state so the UI doesn't show stale defaults
        interceptedStateChanged(
            PlayerPlaybackState(
                isLoading = true,
                error = null,
            ),
        )
        mpvBridge.setFrameCallbackWithBlock { dict ->
            val m = (dict ?: emptyMap<Any?, Any?>()) as Map<Any?, *>
            val state = MpvStateMapper.mapToPlaybackState(m)
            interceptedStateChanged(state)
        }
        onDispose {
            // Don't null callback here — destroy() (called in onRelease)
            // stops the polling timer first, then nulls the callback,
            // which avoids the use-after-free race.
        }
    }

    // Load URL with optional headers
    LaunchedEffect(url, headers) {
        if (url.isNotBlank()) {
            iosPlayerLogger.i { "Loading via mpv: $url" }
            if (headers.isNotEmpty()) {
                val headerStr = headers.entries.joinToString(",") { (k, v) -> "$k:$v" }
                mpvBridge.loadURLSimple("$url|http-header-fields=$headerStr")
            } else {
                mpvBridge.loadURLSimple(url)
            }
        }
    }

    // Player actions handled reactive-style directly by IosMoviePlayer via MoviePlayerController

    // Subtitle style
    LaunchedEffect(subtitleStyle) {
        if (subtitleStyle.fontColorArgb != -1) {
            mpvBridge.setSubtitleColor(subtitleStyle.fontColorArgb)
        }
        mpvBridge.setSubtitleBold(subtitleStyle.isBold)
        mpvBridge.setSubtitleShadow(subtitleStyle.hasDropShadow)
        mpvBridge.setSubtitleFontSize(subtitleStyle.fontSizeSp)
        mpvBridge.setSubtitleMargin(subtitleStyle.bottomMarginPercent)
    }

    // Video scale
    LaunchedEffect(videoScale) {
        mpvBridge.setVideoScale(videoScale.ordinal)
    }

    // Screen brightness
    LaunchedEffect(brightness) {
        UIScreen.mainScreen.brightness = brightness.toDouble()
    }

    // Lock screen metadata
    LaunchedEffect(url) {
        val title = url.substringAfterLast("/").substringBefore("?")
        mpvBridge.setLockScreenMeta(title, "MovieHub")
    }

    UIKitView(
        factory = { mpvBridge },
        modifier = modifier,
        update = { if (!hasStarted) hasStarted = true },
        onRelease = { mpvBridge.destroy() },
    )
}
