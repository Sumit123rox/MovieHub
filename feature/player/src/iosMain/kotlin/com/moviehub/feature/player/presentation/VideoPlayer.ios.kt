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
import com.moviehub.core.model.PlayerPlaybackState
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.AVPlayerItemStatusUnknown
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.play
import platform.AVFoundation.pause
import platform.AVFoundation.seekToTime
import platform.AVFoundation.rate
import platform.AVFoundation.volume
import platform.AVFoundation.currentItem
import platform.AVFoundation.duration
import platform.AVFoundation.currentTime
import platform.AVFoundation.isPlaybackBufferEmpty
import platform.AVFoundation.isPlaybackLikelyToKeepUp
import platform.Foundation.NSURL
import platform.UIKit.UIView
import platform.UIKit.UIColor
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.CoreMedia.CMTimeGetSeconds
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.AVMediaCharacteristicAudible
import platform.AVFoundation.AVMediaCharacteristicLegible
import platform.AVFoundation.presentationSize
import platform.AVFoundation.asset
import platform.AVFoundation.availableMediaCharacteristicsWithMediaSelectionOptions
import platform.AVFoundation.mediaSelectionGroupForMediaCharacteristic
import platform.AVFoundation.selectedMediaOptionInMediaSelectionGroup
import platform.AVFoundation.selectMediaOption
import platform.AVFoundation.accessLog
import platform.UIKit.UIScreen
import com.moviehub.core.model.SubtitleStyle
import com.moviehub.core.model.VideoScale
import platform.AVFoundation.canUseNetworkResourcesForLiveStreamingWhilePaused
import platform.AVFoundation.preferredForwardBufferDuration
import platform.AVFoundation.preferredPeakBitRate

private val iosPlayerLogger = Logger.withTag("VideoPlayer.iOS")

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    headers: Map<String, String>,
    onPlaybackStateChanged: (PlayerPlaybackState) -> Unit,
    requestedAction: PlayerAction?,
    onActionConsumed: () -> Unit,
    forceLandscape: Boolean,
    brightness: Float,
    videoScale: VideoScale,
    subtitleBottomMargin: Int,
    subtitleStyle: SubtitleStyle,
    drmLicenseUrl: String?,
    drmScheme: String?,
    modifier: Modifier
) {
    if (drmLicenseUrl != null) {
        iosPlayerLogger.w { "DRM license URL provided ($drmScheme) but iOS AVPlayer does not support Widevine. DRM will be ignored." }
    }

    val player = remember(url, headers) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            val asset = if (headers.isNotEmpty()) {
                val opts = mutableMapOf<Any?, Any>()
                opts["AVURLAssetHTTPHeaderFieldsKey" as Any] = headers
                AVURLAsset(uRL = nsUrl, options = opts)
            } else {
                AVURLAsset(uRL = nsUrl, options = null)
            }
            val playerItem = AVPlayerItem(asset = asset)
            // Adaptive buffer config for smoother playback
            playerItem.preferredForwardBufferDuration = 30.0
            playerItem.preferredPeakBitRate = 5_000_000f
            playerItem.automaticallyWaitsToMinimizeStalling = true
            playerItem.canUseNetworkResourcesForLiveStreamingWhilePaused = true
            AVPlayer(playerItem = playerItem)
        } else null
    }
    val playerLayer = remember { AVPlayerLayer().apply { backgroundColor = UIColor.blackColor.CGColor } }
    val avPlayerView = remember(player) {
        UIView().apply {
            setBackgroundColor(UIColor.blackColor)
            playerLayer.player = player
            if (player != null) {
                layer.addSublayer(playerLayer)
            }
        }
    }

    // Efficient position updates using native periodic observer
    DisposableEffect(player) {
        val p = player
        val observer = if (p != null) {
            val interval = CMTimeMakeWithSeconds(1.0, 10)
            p.addPeriodicTimeObserverForInterval(
                interval = interval,
                queue = null, // main queue
                usingBlock = { time ->
                    val item = p.currentItem
                    val avDuration = item?.duration?.let { CMTimeGetSeconds(it) } ?: 0.0
                    val currentTime = CMTimeGetSeconds(time)

                    // Determine loading state from AVPlayerItem status
                    val itemStatus = item?.status ?: AVPlayerItemStatusUnknown
                    val isCurrentlyPlaying = p.rate > 0
                    val isCurrentlyLoading = when (itemStatus) {
                        AVPlayerItemStatusUnknown -> true
                        AVPlayerItemStatusReadyToPlay -> {
                            // Still loading if buffer is empty and not yet playing
                            !isCurrentlyPlaying && (item?.isPlaybackLikelyToKeepUp() != true)
                        }
                        AVPlayerItemStatusFailed -> false
                        else -> false
                    }

                    // Check for player item errors
                    val playerError = if (itemStatus == AVPlayerItemStatusFailed) {
                        "Playback failed: ${item?.error?.localizedDescription ?: "Unknown error"}"
                    } else null

                    // Extract audio/subtitle tracks, video dimensions, bitrate
                    val audioTrackList = mutableListOf<AudioTrack>()
                    val subtitleTrackList = mutableListOf<SubtitleTrack>()
                    var vWidth = 0
                    var vHeight = 0
                    var videoBitrate = 0
                    var audioBitrate = 0

                    if (item != null) {
                        // Video dimensions from presentationSize
                        val presSize = item.presentationSize
                        presSize.use { size ->
                            if (size.width > 0 && size.height > 0) {
                                vWidth = size.width.toInt()
                                vHeight = size.height.toInt()
                            }
                        }

                        // Estimated bitrate from access log
                        val logEvents = item.accessLog()?.events
                        val lastEvent = logEvents?.lastOrNull()
                        if (lastEvent != null) {
                            videoBitrate = lastEvent.observedBitrate.toInt()
                        }

                        // Audio and subtitle tracks via AVMediaSelectionGroup
                        val avAsset = item.asset
                        val characteristics = avAsset.availableMediaCharacteristicsWithMediaSelectionOptions()
                        for (characteristic in characteristics) {
                            val charDesc = characteristic.toString()
                            if (charDesc == AVMediaCharacteristicAudible.toString()) {
                                val group = avAsset.mediaSelectionGroupForMediaCharacteristic(characteristic)
                                if (group != null) {
                                    val selectedOpt = item.selectedMediaOptionInMediaSelectionGroup(group)
                                    group.options.forEachIndexed { index, option ->
                                        audioTrackList.add(
                                            AudioTrack(
                                                index = 0,
                                                id = index.toString(),
                                                label = option.displayName?.toString()
                                                    ?: option.extendedLanguageTag?.toString()
                                                    ?: "Audio ${index + 1}",
                                                language = option.extendedLanguageTag?.toString(),
                                                isSelected = option == selectedOpt
                                            )
                                        )
                                    }
                                }
                            } else if (charDesc == AVMediaCharacteristicLegible.toString()) {
                                val group = avAsset.mediaSelectionGroupForMediaCharacteristic(characteristic)
                                if (group != null) {
                                    val selectedOpt = item.selectedMediaOptionInMediaSelectionGroup(group)
                                    group.options.forEachIndexed { index, option ->
                                        subtitleTrackList.add(
                                            SubtitleTrack(
                                                index = 0,
                                                id = index.toString(),
                                                label = option.displayName?.toString()
                                                    ?: option.extendedLanguageTag?.toString()
                                                    ?: "Subtitle ${index + 1}",
                                                language = option.extendedLanguageTag?.toString(),
                                                isSelected = option == selectedOpt
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    onPlaybackStateChanged(
                        PlayerPlaybackState(
                            isPlaying = isCurrentlyPlaying,
                            isLoading = isCurrentlyLoading,
                            error = playerError,
                            currentPositionMs = (currentTime * 1000).toLong(),
                            durationMs = (avDuration * 1000).toLong(),
                            playbackSpeed = p.rate,
                            audioTracks = audioTrackList,
                            subtitleTracks = subtitleTrackList,
                            videoWidth = vWidth,
                            videoHeight = vHeight,
                            videoBitrate = videoBitrate,
                            audioBitrate = audioBitrate
                        )
                    )
                }
            )
        } else {
            onPlaybackStateChanged(
                PlayerPlaybackState(
                    isPlaying = false,
                    isLoading = false,
                    error = "Failed to create player: invalid URL"
                )
            )
            null
        }

        onDispose {
            val p = player
            if (observer != null && p != null) {
                p.removeTimeObserver(observer)
            }
        }
    }

    // Handle actions
    LaunchedEffect(requestedAction) {
        val p = player ?: return@LaunchedEffect
        requestedAction?.let { action ->
            when (action) {
                is PlayerAction.Play -> p.play()
                is PlayerAction.Pause -> p.pause()
                is PlayerAction.SeekTo -> p.seekToTime(CMTimeMakeWithSeconds(action.positionMs / 1000.0, 1000))
                is PlayerAction.SetSpeed -> p.rate = action.speed
                is PlayerAction.SetVolume -> {
                    p.volume = action.volume.coerceIn(0f, 1f)
                }
                is PlayerAction.SelectAudioTrack -> {
                    val audioItem = p.currentItem
                    if (audioItem != null) {
                        val avAsset = audioItem.asset
                        val group = avAsset.mediaSelectionGroupForMediaCharacteristic(AVMediaCharacteristicAudible)
                        if (group != null && action.trackIndex in 0 until group.options.size) {
                            audioItem.selectMediaOption(group.options[action.trackIndex], group)
                        }
                    }
                }
                is PlayerAction.SelectSubtitleTrack -> {
                    val subItem = p.currentItem
                    if (subItem != null) {
                        val avAsset = subItem.asset
                        val group = avAsset.mediaSelectionGroupForMediaCharacteristic(AVMediaCharacteristicLegible)
                        if (group != null) {
                            if (action.groupIndex == -1) {
                                subItem.selectMediaOption(null, group)
                            } else if (action.trackIndex in 0 until group.options.size) {
                                subItem.selectMediaOption(group.options[action.trackIndex], group)
                            }
                        }
                    }
                }
                is PlayerAction.SetScale -> {
                    // Handled via videoScale state in LaunchedEffect(videoScale)
                }
                is PlayerAction.ResetZoom -> {
                    // Handled via freeZoomScale/freeZoomOffset state in PlayerScreen
                }
            }
            onActionConsumed()
        }
    }

    // Apply screen brightness
    LaunchedEffect(brightness) {
        UIScreen.mainScreen.brightness = brightness.toDouble()
    }

    // Apply video scale to AVPlayerLayer
    LaunchedEffect(videoScale) {
        playerLayer.videoGravity = when (videoScale) {
            VideoScale.FIT -> platform.AVFoundation.AVLayerVideoGravityResizeAspect
            VideoScale.FILL -> platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
            VideoScale.ZOOM -> platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
            VideoScale.STRETCH -> platform.AVFoundation.AVLayerVideoGravityResize
        }
    }

    // Apply subtitle bottom margin to AVPlayerLayer to prevent overlap with controls
    LaunchedEffect(subtitleBottomMargin, avPlayerView) {
        if (subtitleBottomMargin > 0) {
            val w = avPlayerView.bounds.use { size.width.toDouble() }
            val h = avPlayerView.bounds.use { size.height.toDouble() }
            if (w > 0 && h > 0) {
                playerLayer.frame = platform.CoreGraphics.CGRectMake(
                    0.0, 0.0, w,
                    (h - subtitleBottomMargin.toDouble()).coerceAtLeast(0.0)
                )
            }
        } else {
            playerLayer.frame = avPlayerView.bounds
        }
    }

    var hasStartedPlayback by remember(player) { mutableStateOf(false) }

    UIKitView(
        factory = {
            avPlayerView
        },
        modifier = modifier,
        update = { view ->
            playerLayer.frame = view.bounds
            if (!hasStartedPlayback) {
                hasStartedPlayback = true
                player?.play()
            }
        },
        onRelease = {
            player?.pause()
        }
    )
}
