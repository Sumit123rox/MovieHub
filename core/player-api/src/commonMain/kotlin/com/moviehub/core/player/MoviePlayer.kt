package com.moviehub.core.player

import com.moviehub.core.model.PlayerPlaybackState
import com.moviehub.core.model.SubtitleStyle
import com.moviehub.core.model.VideoScale
import kotlinx.coroutines.flow.StateFlow

interface MoviePlayer {
    val playbackState: StateFlow<PlayerPlaybackState>

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun setSpeed(speed: Float)

    fun setVolume(volume: Float)

    fun selectAudioTrack(trackIndex: Int)

    fun selectSubtitleTrack(trackIndex: Int)

    fun setVideoScale(scale: VideoScale)

    fun setSubtitleStyle(style: SubtitleStyle)

    fun release()
}
