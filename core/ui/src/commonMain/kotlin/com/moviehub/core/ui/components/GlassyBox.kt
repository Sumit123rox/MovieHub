package com.moviehub.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A utility component for blurred, semi-transparent overlays.
 * Uses a blurred background layer to achieve a "glassy" effect while keeping content sharp.
 *
 * @param baseAlpha Controls the opacity of the background fill. Higher values (0.3-0.5)
 *   make content more legible against dark backdrops. Default is balanced for general use.
 */
@Composable
fun GlassyBox(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 16.dp,
    baseAlpha: Float = 0.35f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // Solid base layer for legibility
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
        )

        // Blurred background layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(blurRadius)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = baseAlpha),
                            MaterialTheme.colorScheme.onSurface.copy(alpha = baseAlpha * 0.5f)
                        )
                    )
                )
        )

        // Content layer (not blurred)
        Box(modifier = Modifier) {
            content()
        }
    }
}
