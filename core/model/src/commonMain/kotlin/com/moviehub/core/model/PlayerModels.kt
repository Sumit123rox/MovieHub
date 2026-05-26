package com.moviehub.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AudioTrack(
    val index: Int,
    val id: String,
    val label: String,
    val language: String? = null,
    val isSelected: Boolean = false
)

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

data class ChapterInfo(
    val title: String,
    val startMs: Long,
    val endMs: Long
)

enum class VideoScale(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    ZOOM("Zoom"),
    STRETCH("Stretch")
}

data class SubtitleStyle(
    val fontSizeSp: Int = 16,
    val fontColorArgb: Int = -1,
    val bgOpacity: Float = 0.20f,
)
