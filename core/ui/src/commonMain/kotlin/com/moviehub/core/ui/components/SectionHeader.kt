package com.moviehub.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.moviehub.core.ui.theme.MovieHubDimens

/**
 * Nuvio-style section header — accent color, bold, with proper padding.
 * Use above groups of settings rows or list sections.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = MovieHubDimens.Spacing.xxs),
    )
}
