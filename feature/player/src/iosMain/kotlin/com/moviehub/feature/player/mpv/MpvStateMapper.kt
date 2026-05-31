package com.moviehub.feature.player.mpv

import com.moviehub.core.model.AudioTrack
import com.moviehub.core.model.ChapterInfo
import com.moviehub.core.model.PlayerBackend
import com.moviehub.core.model.PlayerBackendInfo
import com.moviehub.core.model.PlayerPlaybackState
import com.moviehub.core.model.SubtitleTrack
import com.moviehub.core.model.VideoTrack

/**
 * Maps an NSDictionary from the MpvPlayerBridge state callback into a [PlayerPlaybackState].
 * The dictionary keys follow the same naming as [PlayerPlaybackState] properties.
 */
@Suppress("UNCHECKED_CAST")
object MpvStateMapper {
    fun mapToPlaybackState(dict: Map<Any?, *>): PlayerPlaybackState {
        fun long(key: String): Long = (dict[key] as? Number)?.toLong() ?: 0L

        fun int(key: String): Int = (dict[key] as? Number)?.toInt() ?: 0

        fun float(key: String): Float = (dict[key] as? Number)?.toFloat() ?: 0f

        fun string(key: String): String? = (dict[key] as? String)?.takeIf { it.isNotBlank() }

        fun bool(key: String): Boolean = (dict[key] as? Boolean) ?: false

        val audioMaps = (dict["audioTracks"] as? List<Map<*, *>>) ?: emptyList()
        val subMaps = (dict["subtitleTracks"] as? List<Map<*, *>>) ?: emptyList()
        val videoMaps = (dict["videoTracks"] as? List<Map<*, *>>) ?: emptyList()
        val chapterMaps = (dict["chapters"] as? List<Map<*, *>>) ?: emptyList()

        val audioTracks = audioMaps.mapIndexed { index, m ->
            AudioTrack(
                index = index,
                id = m["id"] as? String ?: "audio/$index",
                label = m["label"] as? String ?: "Track ${index + 1}",
                language = m["language"] as? String,
                isSelected = m["isSelected"] as? Boolean ?: false,
                codec = m["codec"] as? String,
                channels = (m["channels"] as? Number)?.toInt() ?: 0,
                bitrate = (m["bitrate"] as? Number)?.toInt() ?: 0,
            )
        }
        val subtitleTracks = subMaps.mapIndexed { index, m ->
            SubtitleTrack(
                index = index,
                id = m["id"] as? String ?: "sub/$index",
                label = m["label"] as? String ?: "Track ${index + 1}",
                language = m["language"] as? String,
                isSelected = m["isSelected"] as? Boolean ?: false,
                isExternal = m["isExternal"] as? Boolean ?: false,
            )
        }
        val videoTracks = videoMaps.mapIndexed { index, m ->
            VideoTrack(
                index = index,
                id = m["id"] as? String ?: "video/$index",
                label = m["label"] as? String ?: "Video ${index + 1}",
                width = (m["width"] as? Number)?.toInt() ?: 0,
                height = (m["height"] as? Number)?.toInt() ?: 0,
                codec = m["codec"] as? String,
                bitrate = (m["bitrate"] as? Number)?.toInt() ?: 0,
                isSelected = m["isSelected"] as? Boolean ?: false,
            )
        }
        val chapters = chapterMaps.mapIndexed { index, m ->
            ChapterInfo(
                title = m["title"] as? String ?: "Chapter $index",
                startMs = (m["startMs"] as? Number)?.toLong() ?: 0L,
                endMs = (m["endMs"] as? Number)?.toLong() ?: 0L,
            )
        }

        val selectedAudioIndex = audioTracks.indexOfFirst { it.isSelected }
        val selectedSubtitleIndex = subtitleTracks.indexOfFirst { it.isSelected }

        return PlayerPlaybackState(
            isPlaying = bool("isPlaying"),
            isLoading = bool("isLoading"),
            error = string("error"),
            currentPositionMs = long("currentPositionMs"),
            durationMs = long("durationMs"),
            bufferedPositionMs = long("bufferedPositionMs"),
            playbackSpeed = float("playbackSpeed"),
            selectedAudioTrackIndex = selectedAudioIndex,
            selectedSubtitleTrackIndex = selectedSubtitleIndex,
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            videoTracks = videoTracks,
            videoWidth = int("videoWidth"),
            videoHeight = int("videoHeight"),
            videoCodec = string("videoCodec"),
            videoBitrate = int("videoBitrate"),
            audioBitrate = int("audioBitrate"),
            audioCodec = string("audioCodec"),
            currentCueCount = int("currentCueCount"),
            chapters = chapters,
            backendInfo = PlayerBackendInfo(
                backend = PlayerBackend.MPV,
                name = "mpv",
                supportsAssSubtitles = true,
                supportsShaderEffects = true,
                supportsDebanding = true,
                supportsScreenshot = true,
                supportsFrameStep = true,
            ),
            droppedFrameCount = int("droppedFrameCount"),
            decoderName = string("decoderName"),
            hardwareDecoding = bool("hardwareDecoding"),
            hdrMode = string("hdrMode"),
            demuxerName = string("demuxerName"),
            containerFormat = string("containerFormat"),
            audioSampleRate = int("audioSampleRate"),
            audioChannels = int("audioChannels"),
            displayFps = float("displayFps"),
        )
    }
}
