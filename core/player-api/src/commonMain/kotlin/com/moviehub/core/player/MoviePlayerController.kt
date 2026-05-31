package com.moviehub.core.player

import com.moviehub.core.model.PlayerPlaybackState
import com.moviehub.core.model.SubtitleStyle
import com.moviehub.core.model.VideoScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class MoviePlayerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _currentPlayer = MutableStateFlow<MoviePlayer?>(null)
    val currentPlayer: StateFlow<MoviePlayer?> = _currentPlayer.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val playbackState: StateFlow<PlayerPlaybackState> =
        _currentPlayer
            .flatMapLatest { player ->
                player?.playbackState ?: flowOf(PlayerPlaybackState())
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = PlayerPlaybackState(),
            )

    fun registerPlayer(player: MoviePlayer) {
        _currentPlayer.value = player
    }

    fun unregisterPlayer(player: MoviePlayer) {
        if (_currentPlayer.value == player) {
            _currentPlayer.value = null
        }
    }

    fun play() {
        _currentPlayer.value?.play()
    }

    fun pause() {
        _currentPlayer.value?.pause()
    }

    fun seekTo(positionMs: Long) {
        _currentPlayer.value?.seekTo(positionMs)
    }

    fun setSpeed(speed: Float) {
        _currentPlayer.value?.setSpeed(speed)
    }

    fun setVolume(volume: Float) {
        _currentPlayer.value?.setVolume(volume)
    }

    fun selectAudioTrack(index: Int) {
        _currentPlayer.value?.selectAudioTrack(index)
    }

    fun selectSubtitleTrack(index: Int) {
        _currentPlayer.value?.selectSubtitleTrack(index)
    }

    fun selectVideoTrack(index: Int) {
        _currentPlayer.value?.selectVideoTrack(index)
    }

    fun setVideoScale(scale: VideoScale) {
        _currentPlayer.value?.setVideoScale(scale)
    }

    fun setSubtitleStyle(style: SubtitleStyle) {
        _currentPlayer.value?.setSubtitleStyle(style)
    }

    fun enterPip() {
        _currentPlayer.value?.enterPip()
    }
}
