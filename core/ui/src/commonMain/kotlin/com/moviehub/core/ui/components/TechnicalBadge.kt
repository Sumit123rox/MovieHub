package com.moviehub.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.moviehub.core.ui.theme.MovieHubDimens

@Composable
fun TechnicalBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    val accentPrimary = MaterialTheme.colorScheme.primary
    val badgeShape = RoundedCornerShape(MovieHubDimens.Spacing.xxs)

    Surface(
        modifier = modifier
            .border(
                width = MovieHubDimens.Spacing.dp1,
                color = accentPrimary.copy(alpha = 0.35f),
                shape = badgeShape,
            ),
        color = accentPrimary.copy(alpha = 0.08f),
        shape = badgeShape,
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = MovieHubDimens.Spacing.xs,
                vertical = MovieHubDimens.Spacing.dp2
            ),
        ) {
            Text(
                text = text.uppercase(),
                color = accentPrimary,
                fontSize = MovieHubDimens.Font.xs,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = MovieHubDimens.Font.trackingUltraWide,
            )
        }
    }
}
