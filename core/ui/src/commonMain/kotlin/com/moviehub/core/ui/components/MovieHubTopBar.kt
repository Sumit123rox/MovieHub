package com.moviehub.core.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.moviehub.core.ui.theme.MovieHubDimens

/**
 * MovieHub top app bar — matches Nuvio styling with elevated surface,
 * Inter Bold title at 20sp, and compact 24dp back icon.
 *
 * Applied everywhere EXCEPT the HomeScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieHubTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        tonalElevation = MovieHubDimens.TopBar.elevationTonal,
        shadowElevation = MovieHubDimens.TopBar.elevationShadow,
        color = MaterialTheme.colorScheme.surface,
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )
            },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = androidx.compose.ui.Modifier.size(MovieHubDimens.TopBar.backIconSize),
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            ),
        )
    }
}
