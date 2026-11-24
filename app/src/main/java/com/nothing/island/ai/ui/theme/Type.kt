package com.nothing.island.ai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Nothing OS inspired typography hierarchy.
 *
 * Utilizes Monospace for technical telemetry, numbers, badges, and NDot styling;
 * and clean Sans-Serif for primary headers and readable body copy.
 */
val NdotFontFamily: FontFamily by lazy {
    try {
        FontFamily(androidx.compose.ui.text.font.Font(com.nothing.island.ai.R.font.ndot_matrix))
    } catch (_: Throwable) {
        FontFamily.Monospace
    }
}
val MonospaceFamily = FontFamily.Monospace
val SansSerifFamily = FontFamily.SansSerif

val NothingTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SansSerifFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = NothingPrimaryText
    ),
    headlineMedium = TextStyle(
        fontFamily = SansSerifFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = NothingPrimaryText
    ),
    titleLarge = TextStyle(
        fontFamily = MonospaceFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 1.sp,
        color = NothingPrimaryText
    ),
    titleMedium = TextStyle(
        fontFamily = MonospaceFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 1.2.sp,
        color = NothingPrimaryText
    ),
    bodyLarge = TextStyle(
        fontFamily = SansSerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = NothingPrimaryText
    ),
    bodyMedium = TextStyle(
        fontFamily = SansSerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = NothingSecondaryText
    ),
    labelSmall = TextStyle(
        fontFamily = MonospaceFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp,
        color = NothingMutedText
    )
)
