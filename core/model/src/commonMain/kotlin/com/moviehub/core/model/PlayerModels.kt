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
    val isSelected: Boolean = false,
    val codec: String? = null,
    val channels: Int = 0,
    val bitrate: Int = 0
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

@Immutable
@Serializable
data class VideoTrack(
    val index: Int,
    val id: String,
    val label: String,
    val width: Int = 0,
    val height: Int = 0,
    val codec: String? = null,
    val bitrate: Int = 0,
    val isSelected: Boolean = false
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
    val selectedVideoTrackIndex: Int = -1,
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val videoTracks: List<VideoTrack> = emptyList(),
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val videoCodec: String? = null,
    val videoBitrate: Int = 0,
    val audioBitrate: Int = 0,
    val audioCodec: String? = null,
    val currentCueCount: Int = 0,
    @kotlinx.serialization.Transient
    val chapters: List<ChapterInfo> = emptyList(),
    // MPVKit-enriched fields (all default to 0/false/null for Media3 backward compat)
    val backendInfo: PlayerBackendInfo = PlayerBackendInfo(),
    val droppedFrameCount: Int = 0,
    val decoderName: String? = null,
    val hardwareDecoding: Boolean = false,
    val hdrMode: String? = null,
    val demuxerName: String? = null,
    val containerFormat: String? = null,
    val audioSampleRate: Int = 0,
    val audioChannels: Int = 0,
    val displayFps: Float = 0f,
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
@Serializable
data class SubtitleStyle(
    val fontSizeSp: Int = 16,
    val fontColorArgb: Int = -1,       // ARGB hex, -1 = white
    val bgOpacity: Float = 0.20f,       // 0.0 - 1.0 (adjusted from 0.0-0.5)
    val bottomMarginPercent: Int = 5,   // % from bottom, 0-20
    val isBold: Boolean = false,
    val hasDropShadow: Boolean = true,  // edge style: shadow vs outline vs none

    // Advanced Customization additions
    val fontFamily: String = "Sans-Serif",
    val fontWeight: String = "Normal",
    val isItalic: Boolean = false,
    val letterSpacingSp: Float = 0f,
    val lineHeightSp: Int = 22,
    val bgColorArgb: Int = 0x00000000,
    val outlineColorArgb: Int = 0xFF000000.toInt(),
    val shadowColorArgb: Int = 0xFF000000.toInt(),
    val outlineThicknessDp: Float = 1f,
    val shadowRadiusDp: Float = 1f,
    val shadowOffsetDp: Float = 1f,
    val edgeStyle: String = "Outline", // Outline, Shadow, None, Raised, Depressed
    val verticalPositionPercent: Int = 95,
    val horizontalOffsetPercent: Int = 0,
    val safeAreaMarginDp: Int = 16,
    val highContrast: Boolean = false,
    val largeSubtitle: Boolean = false,
    val readingComfortPreset: String = "Default",
    val accessibilityPreset: String = "Default"
) {
    companion object {
        val DEFAULT = SubtitleStyle()
    }
}

/** Which video playback engine is providing the [PlayerPlaybackState]. */
@Immutable
@Serializable
enum class PlayerBackend(val label: String) {
    MEDIA3("Media3/ExoPlayer"),
    MPV("mpv/libmpv")
}

/** Capabilities and metadata about the active player backend. */
@Immutable
@Serializable
data class PlayerBackendInfo(
    val backend: PlayerBackend = PlayerBackend.MEDIA3,
    val name: String = "Media3/ExoPlayer",
    val version: String = "",
    val supportsDrm: Boolean = false,
    val supportsAssSubtitles: Boolean = false,
    val supportsShaderEffects: Boolean = false,
    val supportsDebanding: Boolean = false,
    val supportsDecoderSwitching: Boolean = false,
    val supportsScreenshot: Boolean = false,
    val supportsFrameStep: Boolean = false,
)

/** Debanding filter settings (MPV-only, no-op on Media3). */
@Immutable
@Serializable
data class DebandingSettings(
    val enabled: Boolean = false,
    val iterations: Int = 1,
    val threshold: Int = 64,
    val range: Int = 16,
    val grain: Int = 48,
)

/** A GLSL shader effect applied to the video output (MPV-only). */
@Immutable
@Serializable
data class ShaderEffect(
    val id: String,
    val name: String,
    val shaderPath: String,
    val isActive: Boolean = false,
)
