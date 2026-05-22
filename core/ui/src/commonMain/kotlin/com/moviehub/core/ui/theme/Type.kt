package com.moviehub.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import moviehub.core.ui.generated.resources.Res
import moviehub.core.ui.generated.resources.inter_bold
import moviehub.core.ui.generated.resources.inter_medium
import moviehub.core.ui.generated.resources.inter_regular
import moviehub.core.ui.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun InterFontFamily() = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold)
)

@Composable
fun MovieHubTypography() = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    displayMedium = TextStyle(
        fontFamily = InterFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    displaySmall = TextStyle(
        fontFamily = InterFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily(),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily(),
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.4.sp
    )
)
