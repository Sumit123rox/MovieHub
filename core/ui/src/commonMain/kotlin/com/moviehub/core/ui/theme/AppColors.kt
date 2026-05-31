package com.moviehub.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * App-level shared colors that are NOT theme-dependent.
 * Theme-dependent colors (primary, surface, background, etc.) live in
 * Color.kt via ThemePalette/AccentPalette/MaterialTheme.colorScheme.
 *
 * USE THESE instead of raw Color(0xFF...) anywhere in the app.
 */
object AppColors {

    // ── Semi-transparent overlays ──────────────────────────────────────────

    /** 80% black — debug overlay, error background */
    val overlayHeavy = Color.Black.copy(alpha = 0.80f)

    /** 85% black — error overlay */
    val overlayError = Color.Black.copy(alpha = 0.85f)

    /** 50% black — settings sheet scrim, tap-to-dismiss area */
    val overlayScrim = Color.Black.copy(alpha = 0.50f)

    /** 45% black — loading overlay */
    val overlayLoading = Color.Black.copy(alpha = 0.45f)

    /** 40% black — side slider background, lock indicator background */
    val overlayMedium = Color.Black.copy(alpha = 0.40f)

    /** 35% black — screen lock indicator, gradient end */
    val overlayLight = Color.Black.copy(alpha = 0.35f)

    /** 30% black — gradient mid in poster */
    val overlayPoster = Color.Black.copy(alpha = 0.30f)

    /** 25% black — seek bar track */
    val overlaySeekTrack = Color.Black.copy(alpha = 0.25f)

    /** 15% black — gradient light, accent background, shimmer */
    val overlayAccent = Color.Black.copy(alpha = 0.15f)

    /** 12% black — accent overlay subtle */
    val overlayAccentSubtle = Color.Black.copy(alpha = 0.12f)

    /** 80% white — icon tint, primary text on dark */
    val whitePrimary = Color.White.copy(alpha = 0.80f)

    /** 70% white — section header, secondary text */
    val whiteSecondary = Color.White.copy(alpha = 0.70f)

    /** 60% white — hint text */
    val whiteHint = Color.White.copy(alpha = 0.60f)

    /** 50% white — disabled text, placeholder */
    val whiteDisabled = Color.White.copy(alpha = 0.50f)

    /** 40% white — very subtle text, range labels */
    val whiteSubtle = Color.White.copy(alpha = 0.40f)

    /** 35% white — extremely subtle */
    val whiteExtraSubtle = Color.White.copy(alpha = 0.35f)

    /** 25% white — empty state icons */
    val whiteMuted = Color.White.copy(alpha = 0.25f)

    /** 20% white — chevron, divider */
    val whiteFaded = Color.White.copy(alpha = 0.20f)

    /** 15% white — seek track, separator */
    val whiteSeparator = Color.White.copy(alpha = 0.15f)

    /** 12% white — seek track inactive */
    val whiteInactive = Color.White.copy(alpha = 0.12f)

    /** 10% white — subtle divider */
    val whiteDivider = Color.White.copy(alpha = 0.10f)

    /** 6% white — minimal divider */
    val whiteDividerMinimal = Color.White.copy(alpha = 0.06f)

    // ── Static Colors ──────────────────────────────────────────────────────

    /** Content card background on dark themes */
    val cardSurface = Color(0xFF161616)

    /** Card surface variant */
    val cardSurfaceVariant = Color(0xFF1E1E1E)

    /** Seek thumb ring */
    val seekThumbRing = Color(0xFF424242)

    // ── Functional Colors ──────────────────────────────────────────────────

    /** Error tint for delete/remove actions */
    val errorRed = Color(0xFFCF6679)

    /** Success green */
    val successGreen = Color(0xFF4CAF50)

    /** Seek increment badge */
    val badgeBlue = Color(0xFF2196F3)

    /** Watched badge background */
    val watchedGreen = Color(0xFF4CAF50)

    // ── Subtitle Color Presets (ARGB) ──────────────────────────────────────

    val subtitleWhite = -1
    val subtitleYellow = -0x100 // 0xFFFFFF00
    val subtitleRed = -0x10000 // 0xFFFF0000
    val subtitleGreen = 0xFF00FF00.toInt()
    val subtitleCyan = 0xFF00FFFF.toInt()

    val subtitleColorOptions = listOf(
        subtitleWhite to "White",
        subtitleYellow to "Yellow",
        subtitleRed to "Red",
        subtitleGreen to "Green",
        subtitleCyan to "Cyan",
    )

    // ── Poster quality badge colors ────────────────────────────────────────

    val quality8k = Color(0xFFE91E63)
    val quality4k = Color(0xFF9C27B0)
    val quality1080p = Color(0xFF2196F3)
    val quality720p = Color(0xFF4CAF50)
    val qualitySD = Color(0xFFFF9800)
    val qualityDefault = Color(0xFF757575)
}
