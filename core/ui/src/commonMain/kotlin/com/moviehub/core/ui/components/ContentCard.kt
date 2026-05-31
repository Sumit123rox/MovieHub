package com.moviehub.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.moviehub.core.ui.theme.MovieHubDimens

@Composable
fun ContentCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val cardShape = RoundedCornerShape(MovieHubDimens.Radius.md)

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "CardPressScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(cardShape)
            .then(
                if (onClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
                } else Modifier
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = cardShape,
            )
            .border(
                width = MovieHubDimens.Spacing.dp1,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = cardShape,
            ),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                    shape = cardShape,
                ),
        )
        content()
    }
}
