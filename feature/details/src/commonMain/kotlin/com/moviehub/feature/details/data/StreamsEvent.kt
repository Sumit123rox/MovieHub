package com.moviehub.feature.details.data

import androidx.compose.runtime.Immutable
import com.moviehub.core.model.StreamItem

/** Per-provider stream loading status */
@Immutable
sealed interface AddonStreamStatus {
    data object Pending : AddonStreamStatus
    data object Fetching : AddonStreamStatus
    data class Completed(val streamCount: Int) : AddonStreamStatus
    data class TimedOut(val elapsedMs: Long = 0) : AddonStreamStatus
    data class Failed(val error: String?) : AddonStreamStatus
}

/**
 * Events emitted by [DetailsRepository.getStreamsFlow] during stream discovery.
 * Carries per-provider status updates alongside accumulated stream results
 * so the UI can render per-addon status pills in real time.
 */
@Immutable
sealed interface StreamsEvent {
    /** Initial event emitted as soon as providers are resolved and loading begins */
    data class LoadingStarted(val providers: Map<String, AddonStreamStatus>) : StreamsEvent

    /** Emitted when a single provider (addon/scraper) changes status */
    data class ProviderStatusChanged(
        val providerName: String,
        val status: AddonStreamStatus,
        val allProviders: Map<String, AddonStreamStatus>,
    ) : StreamsEvent

    /** Emitted when the accumulated stream list changes (new streams added) */
    data class StreamsUpdated(val streams: List<StreamItem>) : StreamsEvent

    /** Emitted when ALL providers have finished (completed, timed out, or failed) */
    data class Completed(val streams: List<StreamItem>, val providers: Map<String, AddonStreamStatus>) : StreamsEvent

    /** Emitted at the start with cached data (offline-first) */
    data class CachedStreams(val streams: List<StreamItem>) : StreamsEvent
}
