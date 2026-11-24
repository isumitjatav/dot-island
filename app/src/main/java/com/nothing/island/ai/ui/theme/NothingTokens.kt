package com.nothing.island.ai.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nothing OS 3.x Inspired Design System Tokens
 *
 * Implements pure AMOLED black surfaces, subtle grayscale elevations,
 * surgical Nothing Red accents, and authentic Nothing design metrics
 * for the Nothing Phone (4a) companion interface.
 *
 * Note: An original interpretation inspired by Nothing OS aesthetic principles;
 * does not copy or include proprietary assets or brand claims.
 */
object NothingTokens {

    // --- AMOLED & Grayscale Surface Hierarchy ---
    val NothingBlack = Color(0xFF000000)             // 100% True AMOLED Black Background
    val NothingSurfaceLowest = Color(0xFF080808)     // Deep Surface / Simulated Preview Well
    val NothingSurfaceLow = Color(0xFF101010)        // Base Card Surface (16dp radius)
    val NothingSurfaceMedium = Color(0xFF161616)     // Elevated Controls, Buttons, Segment Track
    val NothingSurfaceHigh = Color(0xFF202020)       // Interactive Pressed / Hovered / Focused
    val NothingSurfaceHighest = Color(0xFF2A2A2A)    // Slider Inactive Track / Modal Borders

    // --- Border & Divider Tokens ---
    val NothingBorderSubtle = Color(0x1AFFFFFF)      // 10% White Card Stroke (1.dp)
    val NothingBorderProminent = Color(0x33FFFFFF)   // 20% White Active / Selected Outline
    val NothingBorderActive = Color(0x66FFFFFF)      // 40% White Focused State
    val NothingDividerColor = Color(0xFF1C1C1E)      // Subtle Section Separator

    // --- Signature Accent Hierarchy ---
    val NothingRed = Color(0xFFD71920)               // Surgical Signature Red
    val NothingRedDark = Color(0xFF8A0F14)           // Subdued Accent Fill
    val NothingRedGlow = Color(0x33D71920)           // Focused Glow Backing
    val NothingRedSubtle = Color(0x1AD71920)         // Ambient Tint Fill

    // --- Semantic Functional Accents ---
    val NothingGreen = Color(0xFF30D158)             // Service Active / Battery Charging
    val NothingAmber = Color(0xFFFFD60A)             // Low Battery Warning / Calibration Alert
    val NothingBlue = Color(0xFF0A84FF)              // Bluetooth Connected / Equalizer Waveform
    val NothingCyan = Color(0xFF32D74B)

    // --- Text & Content Hierarchy ---
    val NothingTextPrimary = Color(0xFFFFFFFF)       // 100% White for Headlines & Titles
    val NothingTextSecondary = Color(0xFF9E9E9E)     // 62% Gray for Descriptions & Subtexts
    val NothingTextTertiary = Color(0xFF5E5E5E)      // 37% Dimmed Gray for Timestamps & Badges
    val NothingTextDisabled = Color(0xFF383838)      // Disabled Controls & Inactive Steppers

    // --- Typography Families ---
    val NdotFamily get() = NdotFontFamily
    val MonospaceFamily get() = androidx.compose.ui.text.font.FontFamily.Monospace
    val SansSerifFamily get() = androidx.compose.ui.text.font.FontFamily.SansSerif

    // --- Dimensional Grid & Radii ---
    val CardRadius: Dp = 16.dp                       // Standard Card Corner Radius
    val ControlRadius: Dp = 12.dp                    // Button / Stepper / Switch Radius
    val BadgeRadius: Dp = 6.dp                       // Monospace Badge Radius
    val PillRadius: Dp = 50.dp                       // Fully Rounded Capsule Radius

    val CardPadding: Dp = 16.dp                      // Inner Card Padding
    val ItemSpacing: Dp = 12.dp                      // Spacing Between Cards
    val ScreenHorizontalPadding: Dp = 16.dp          // Global Screen Margin
    val SectionHeaderSpacing: Dp = 8.dp              // Space Below Section Titles

    // --- Nothing Phone (4a) Cutout Baseline ---
    val CutoutWidthDp: Float = 124f
    val CutoutHeightDp: Float = 36f
    val CutoutTopOffsetDp: Float = 9f
    val CutoutCornerRadiusDp: Float = 18f
}
