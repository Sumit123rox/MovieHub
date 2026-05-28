package com.moviehub.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class AudioTrack(
    val index: Int,
    val id: String,
    val label: String,
    val language: String? = null,
    val isSelected: Boolean = false
)

@Immutable
@Serializable
data class SubtitleTrack(
    val index: Int,
    val id: String,
    val label: String,
    val language: String? = null,
    val isSelected: Boolean = false,
    val isExternal: Boolean = false
)

@Serializable
@Immutable
data class PlayerPlaybackState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val selectedAudioTrackIndex: Int = -1,
    val selectedSubtitleTrackIndex: Int = -1,
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val videoCodec: String? = null,
    val videoBitrate: Int = 0,
    val audioBitrate: Int = 0,
    val currentCueCount: Int = 0,
    @kotlinx.serialization.Transient
    val chapters: List<ChapterInfo> = emptyList()
)

@Immutable
data class ChapterInfo(
    val title: String,
    val startMs: Long,
    val endMs: Long
)

@Immutable
enum class VideoScale(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    ZOOM("Zoom"),
    STRETCH("Stretch")
}

@Immutable
data class SubtitleStyle(
    val fontSizeSp: Int = 16,
    val fontColorArgb: Int = -1,
    val bgOpacity: Float = 0.20f,
)
