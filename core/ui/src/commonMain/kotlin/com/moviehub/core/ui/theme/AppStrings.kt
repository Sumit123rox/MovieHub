package com.moviehub.core.ui.theme

/**
 * App-wide display strings. Single source of truth for all user-facing text.
 *
 * For localized strings, use Compose Resources (res/values/strings.xml).
 * This object contains English defaults and keys used across screens.
 */
object AppStrings {

    // ── Settings ───────────────────────────────────────────────────────────

    const val settingsTitle = "Settings"
    const val appearanceTitle = "Appearance"
    const val playbackTitle = "Playback"
    const val subtitlesTitle = "Subtitles"
    const val metadataTitle = "Metadata Enrichment"
    const val downloadsTitle = "Downloads"

    const val profileManagementSection = "Profile Management"
    const val appearanceSection = "Appearance"
    const val playbackSection = "Playback"
    const val dataStorageSection = "Data & Storage"
    const val externalSection = "External"

    const val switchProfile = "Switch Profile"
    const val activeProfile = "Active Profile"
    const val themeAccent = "Theme & Accent"

    const val seekForwardBackward = "Seek Forward/Backward"
    const val defaultPlaybackSpeed = "Default Playback Speed"
    const val autoPlayNext = "Auto-play Next Episode"
    const val autoPlayNextDesc = "Start next episode automatically"
    const val resumePlayback = "Resume Playback"
    const val resumePlaybackDesc = "Continue from last saved position"

    const val navigationSection = "Navigation"
    const val speedSection = "Speed"
    const val behaviorSection = "Behavior"

    const val fontSection = "Font Size"
    const val colorSection = "Color"
    const val styleSection = "Style"
    const val boldLabel = "Bold"
    const val shadowLabel = "Shadow"

    const val cloudSync = "Cloud Sync"
    const val cloudSyncDesc = "Backup & restore"
    const val externalProviders = "External Providers"
    const val externalProvidersDesc = "Manage addons & scrapers"
    const val downloadsDesc = "Manage offline downloads"
    const val metadataDesc = "TMDB API key for cast & ratings"

    const val metadataApiKeyLabel = "TMDB API Key"
    const val metadataApiKeyDesc = "Get a free API key at themoviedb.org to enable cast photos, ratings, and person details."
    const val enterApiKeyHint = "Enter TMDB API Key"
    const val show = "Show"
    const val hide = "Hide"
    const val saveKey = "Save Key"

    const val logout = "Logout"
    const val cancel = "Cancel"
    const val apply = "Apply"

    // ── Player ─────────────────────────────────────────────────────────────

    const val noStreamsFound = "No streams found"
    const val noStreamsFoundHint = "Try adding more addons or check back later"
    const val noTracksAvailable = "No track information available"
    const val noAudioTracks = "No audio tracks available"
    const val noSubtitleTracks = "No subtitle tracks available"
    const val noVideoTracks = "No video tracks available"
    const val searchingForStreams = "Searching for streams..."
    const val invalidStreamUrl = "Invalid Stream URL"
    const val tapToDismiss = "Tap anywhere to dismiss"
    const val debugInfoTitle = "Debug Info"
    const val noPlayableSource = "No Playable Source"
    const val noPlayableSourceDesc = "This stream has no direct playable URL. Try another source or copy the link for external playback."

    // ── Playback Speed ─────────────────────────────────────────────────────

    const val playbackSpeed = "Playback Speed"
    const val fineControlLabel = "Fine Control"

    // ── Tracks ──────────────────────────────────────────────────────────────

    const val tracksTitle = "Tracks"
    const val audioLabel = "Audio"
    const val videoLabel = "Video"
    const val subtitleLabel = "Subs"
    const val off = "Off"
    const val sourceLabel = "Source"
    const val zoomLabel = "Zoom"
    const val timerLabel = "Timer"
    const val sleepLabel = "Sleep"

    // ── Gesture Hints ──────────────────────────────────────────────────────

    const val gestureBrightness = "Left swipe\nBrightness"
    const val gestureVolume = "Right swipe\nVolume"
    const val gestureSeek = "Tap edges\nto seek"
    const val gestureControls = "Tap center\nControls"
    const val gestureTapDismiss = "Tap to dismiss"

    // ── Search ─────────────────────────────────────────────────────────────

    const val searchHint = "Search movies & shows..."
    const val clearSearch = "Clear"
    const val noResults = "No results found"
    const val searchFailed = "Search failed"

    // ── Continue Watching ──────────────────────────────────────────────────

    const val continueWatching = "Continue Watching"
    const val recentlyWatched = "Recently Watched"

    // ── Subtitle Style ─────────────────────────────────────────────────────

    val subtitleFontSizes = listOf(12 to "Small", 16 to "Med", 20 to "Large", 28 to "XL")
    val subtitleBgOpacities = listOf(0.0f to "None", 0.15f to "Light", 0.30f to "Med", 0.50f to "Heavy")

    // ── Playback Speed Presets ─────────────────────────────────────────────

    val speedPresets = listOf(0.5f, 1.0f, 1.5f, 2.0f)
    val speedRangeLabels = "0.25x" to "4.0x"

    // ── Done count ──────────────────────────────────────────────────────────

    const val doneCountFormat = "done"
}
