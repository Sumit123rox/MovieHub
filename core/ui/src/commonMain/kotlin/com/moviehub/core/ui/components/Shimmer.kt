package com.moviehub.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/**
 * Theme-aware shimmer loading modifier.
 * Uses drawBehind (backed by Modifier.Node in Compose 1.7+) instead of
 * Modifier.composed, avoiding subcomposition overhead.
 */
@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerTranslation"
    )

    val base = MaterialTheme.colorScheme.onSurface
    val brush = remember(base, translateAnim) {
        Brush.linearGradient(
            colors = listOf(
                base.copy(alpha = 0.06f),
                base.copy(alpha = 0.12f),
                base.copy(alpha = 0.06f)
            ),
            start = Offset.Zero,
            end = Offset(x = translateAnim, y = translateAnim)
        )
    }

    return this.drawBehind { drawRect(brush) }
}
