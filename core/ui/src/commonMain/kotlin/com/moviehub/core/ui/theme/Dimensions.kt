package com.moviehub.core.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Unified design system for MovieHub — single source of truth for ALL sizing,
 * spacing, typography, radii, and timing values across every screen.
 *
 * RULES (enforced by code review):
 * 1. NEVER use raw dp or sp values in any @Composable or screen file.
 * 2. ALWAYS reference MovieHubDimens.<Category>.<name>.
 * 3. If a value doesn't exist, add it to the correct category first.
 */
object MovieHubDimens {

    /** Top app bar sizing */
    object TopBar {
        val elevationTonal = 1.dp
        val elevationShadow = 0.dp
        val backIconSize = 24.dp
        val actionIconSize = 24.dp
        val containerHeight = 56.dp
        val containerHeightCompact = 48.dp
    }

    /** Spacing / padding */
    object Spacing {
        val dp0 = 0.dp
        val dp1 = 1.dp
        val dp2 = 2.dp
        val dp3 = 3.dp

        /** 4dp — icon-to-text, chip gaps */
        val xxs = 4.dp

        /** 6dp — compact gap */
        val xs = 6.dp

        /** 8dp — small gap, internal card padding */
        val sm = 8.dp

        /** 10dp — compact card spacing */
        val ms = 10.dp

        /** 12dp — section spacing, card spacing */
        val md = 12.dp

        /** 14dp — list item vertical */
        val ml = 14.dp

        /** 16dp — standard horizontal padding */
        val lg = 16.dp

        /** 20dp — section group spacing */
        val xl = 20.dp

        /** 24dp — large section spacing */
        val xxl = 24.dp

        /** 32dp — screen bottom */
        val xxxl = 32.dp
    }

    /** Corner Radii */
    object Radius {
        /** 8dp — chips, small cards */
        val sm = 8.dp

        /** 12dp — buttons, cards, dialogs, list items */
        val md = 12.dp

        /** 14dp — screen lock indicator */
        val screenLock = 14.dp

        /** 16dp — content cards, sheets */
        val lg = 16.dp

        /** 22dp — side slider */
        val sideSlider = 22.dp

        /** 24dp — alert dialogs */
        val xl = 24.dp

        /** 28dp — bottom sheets, overlay sheets */
        val xxl = 28.dp
    }

    /** Icon sizes */
    object Icon {
        val xxs = 12.dp
        val xs = 16.dp
        val sm = 20.dp
        val md = 22.dp
        val lg = 24.dp
        val xl = 28.dp
        val xxl = 32.dp
        val xxxl = 48.dp
        val jumbo = 64.dp
    }

    /** Avatar / profile sizes */
    object Avatar {
        val sm = 38.dp
        val md = 52.dp
        val lg = 56.dp
    }

    /** Cast Section dimensions */
    object Cast {
        val shimmerNameWidth = 70.dp
        val shimmerNameHeight = 14.dp
        val shimmerRoleWidth = 50.dp
        val shimmerRoleHeight = 10.dp
    }

    /** Player-specific dimensions */
    object Player {
        val controlSize = 48.dp
        val topBarHeight = 56.dp
        val pillHeight = 36.dp
        val seekBarThumb = 18.dp
        val seekBarActive = 4.dp
        val seekBarInactive = 2.dp
        val loadingBar = 3.dp
        val chapterDot = 6.dp
        val chapterDotRadius = 2.5f
        val seekBarLabelWidth = 46.dp
        /** Width of the current/total time Text next to the seek bar */
        val seekBarTimeWidth = 42.dp
        val overlaySheetMaxWidth = 420.dp
        val subtitleMarginControls = 110.dp
        val sideSliderWidth = 44.dp
        val shimmerHeight = 80.dp
        /** Controls focus indicator border widths */
        val controlsFocusBorder = 2.dp
        val controlsFocusBorderLarge = 3.dp
        /** Blur radius for the poster backdrop on the loading overlay */
        val posterBlurRadius = 24.dp
        /** Blur radius for the controls-visible video background */
        val controlsBlurRadius = 12.dp

        /** Binge-watching / auto-play countdown card */
        val bingeCardWidth = 280.dp
        val bingeCardCornerRadius = 20.dp
        val bingeCardPadding = 16.dp
        val bingeCardItemSpacing = 10.dp
        val bingeCardButtonSpacing = 8.dp
        val bingeCardButtonCorner = 10.dp
        val bingeCardSafeAreaEnd = 48.dp
        val bingeCardSafeAreaBottom = 48.dp
        val bingeCountdownBubbleSize = 24.dp

        /** Subtitle WYSIWYG preview box */
        val subtitlePreviewHeight = 64.dp
        val subtitlePreviewCorner = 16.dp
        val subtitlePreviewBgPaddingInner = 4.dp
    }

    /** Hero Carousel Sizing */
    object Carousel {
        val containerHeight = 650.dp
        val gradientStartY = 180.dp
        val horizontalPadding = 48.dp
        val bottomPadding = 48.dp
        val pageSpacing = 16.dp
        val itemCornerRadius = 24.dp
        val indicatorSpacing = 6.dp
        val indicatorHeight = 6.dp
        val indicatorDotMin = 8.dp
        val indicatorDotMax = 24.dp
    }

    /** List & Scroll */
    object List {
        val loadMoreThreshold = 8
        val headerCollapseOffset = 64.dp
        val sectionSpacing = 20.dp
        val itemSpacing = 10.dp
    }

    /** Loading / Progress indicators */
    object Progress {
        val indicatorSize = 48.dp
        val indicatorStroke = 4.dp
        val indicatorStrokeThin = 2.dp
    }

    /** Shimmer effect dimensions */
    object Shimmer {
        val heroHeight = 450.dp
        val titleWidth = 140.dp
        val cardWidth = 130.dp
        val cardHeight = 190.dp
    }

    /** Dialog / Sheet sizing */
    object Sheet {
        val maxHeightFraction = 0.85f
        val maxWidthFraction = 0.92f
        val dialogWidthLegacy = 340.dp
        val minWidth = 320.dp
        val maxWidth = 420.dp
        val maxWidthWide = 640.dp
        val minHeight = 200.dp
    }

    /** Emptystate */
    object EmptyState {
        val iconSize = 60.dp
        val iconSizeLarge = 64.dp
    }

    /** Poster / Card */
    object Poster {
        const val aspectRatio = 0.72f
        val homeWidth = 160.dp
        val homeHeight = 220.dp
        val continueWatchingWidth = 175.dp
        val continueWatchingHeight = 215.dp
        val bottomSheetThumbnailWidth = 64.dp
        val bottomSheetThumbnailHeight = 88.dp
    }

    /** Font sizes (supplementing MaterialTheme typography) */
    object Font {
        val xxs = 9.sp
        val xs = 10.sp
        val sm = 11.sp
        val md = 12.sp
        val lg = 14.sp
        val xl = 16.sp
        val xxl = 18.sp
        val title = 20.sp
        val headline = 22.sp
        val display = 26.sp
        val hero = 28.sp
        val jumbo = 32.sp
        val trackingWide = 0.5.sp
        val trackingUltraWide = 1.5.sp
    }

    /** Animation durations in ms */
    object Anim {
        const val fast = 200
        const val medium = 350
        const val slow = 500
    }

    /** Player timing in ms */
    object PlayerTiming {
        const val controlsAutoHide = 3_000L
        const val loadingMinDisplay = 3_000L
        const val controlsForceShow = 8_000L
        const val rebufferDebounce = 500L
        const val rebufferMinDisplay = 800L
        const val errorAutoSwitch = 4_000L
        const val saveInterval = 5_000L
        const val saveFirstDelay = 3_000L
    }

    /** Network timing in ms */
    object NetworkTiming {
        const val retryTimeout = 15_000L
        const val searchTimeout = 15_000L
    }

    /** Catalog pre-fetch timing in ms */
    object PrefetchTiming {
        /** Hover/focus duration before triggering background stream pre-warm on a catalog item */
        const val catalogItemHoverMs = 800L
    }
}
