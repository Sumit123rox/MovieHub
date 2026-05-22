package com.moviehub.feature.player.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.moviehub.core.model.PlayerPlaybackState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.play
import platform.AVFoundation.pause
import platform.AVFoundation.seekToTime
import platform.AVFoundation.rate
import platform.AVFoundation.currentItem
import platform.AVFoundation.duration
import platform.AVFoundation.currentTime
import platform.Foundation.NSURL
import platform.UIKit.UIView
import platform.CoreMedia.CMTimeMakeWithSeconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.removeTimeObserver
import platform.CoreMedia.CMTimeGetSeconds

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    headers: Map<String, String>,
    onPlaybackStateChanged: (PlayerPlaybackState) -> Unit,
    requestedAction: PlayerAction?,
    onActionConsumed: () -> Unit,
    modifier: Modifier
) {
    val player = remember { AVPlayer(uRL = NSURL.URLWithString(url)!!) }
    val playerLayer = remember { AVPlayerLayer() }
    val avPlayerView = remember {
        UIView().apply {
            playerLayer.player = player
            layer.addSublayer(playerLayer)
        }
    }

    // Efficient position updates using native periodic observer
    DisposableEffect(player) {
        val interval = CMTimeMakeWithSeconds(1.0, 10)
        val observer = player.addPeriodicTimeObserverForInterval(
            interval = interval,
            queue = null, // main queue
            usingBlock = { time ->
                val duration = player.currentItem?.duration?.let { CMTimeGetSeconds(it) } ?: 0.0
                val currentTime = CMTimeGetSeconds(time)
                
                onPlaybackStateChanged(
                    PlayerPlaybackState(
                        isPlaying = player.rate > 0,
                        currentPositionMs = (currentTime * 1000).toLong(),
                        durationMs = (duration * 1000).toLong(),
                        playbackSpeed = player.rate
                    )
                )
            }
        )
        
        onDispose {
            player.removeTimeObserver(observer)
        }
    }

    // Handle actions
    LaunchedEffect(requestedAction) {
        requestedAction?.let { action ->
            when (action) {
                is PlayerAction.Play -> player.play()
                is PlayerAction.Pause -> player.pause()
                is PlayerAction.SeekTo -> player.seekToTime(CMTimeMakeWithSeconds(action.positionMs / 1000.0, 1000))
                is PlayerAction.SetSpeed -> player.rate = action.speed
                else -> {} // Tracks not implemented for iOS yet
            }
            onActionConsumed()
        }
    }

    UIKitView(
        factory = {
            avPlayerView
        },
        modifier = modifier,
        update = { view ->
            playerLayer.frame = view.bounds
            player.play()
        },
        onRelease = {
            player.pause()
        }
    )
}
