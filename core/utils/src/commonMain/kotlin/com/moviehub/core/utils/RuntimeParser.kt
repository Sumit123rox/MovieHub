package com.moviehub.core.utils

/**
 * Parses common runtime string formats into milliseconds.
 * Handles "133 min", "2h 13m", "PT2H13M", plain digits (as minutes), or returns 0.
 */
fun parseRuntime(runtime: String): Long {
    val trimmed = runtime.trim().lowercase()
    // ISO 8601: PT2H13M
    val isoMatch = Regex("""PT?(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?""").find(trimmed)
    if (isoMatch != null) {
        val hours = isoMatch.groupValues[1].toLongOrNull() ?: 0L
        val minutes = isoMatch.groupValues[2].toLongOrNull() ?: 0L
        val seconds = isoMatch.groupValues[3].toLongOrNull() ?: 0L
        return ((hours * 60 + minutes) * 60 + seconds) * 1000
    }
    // "133 min" or "133m"
    val minMatch = Regex("""(\d+)\s*(?:min|m)""").find(trimmed)
    if (minMatch != null) {
        return minMatch.groupValues[1].toLongOrNull()?.let { it * 60 * 1000 } ?: 0L
    }
    // "2h 13m" or "2 hr 13 min"
    val hmMatch = Regex("""(\d+)\s*(?:h|hr)\s*(\d+)\s*(?:m|min)""").find(trimmed)
    if (hmMatch != null) {
        val hours = hmMatch.groupValues[1].toLongOrNull() ?: 0L
        val minutes = hmMatch.groupValues[2].toLongOrNull() ?: 0L
        return (hours * 60 + minutes) * 60 * 1000
    }
    // Plain digits only — treat as minutes
    trimmed.toLongOrNull()?.let { return it * 60 * 1000 }
    return 0L
}
