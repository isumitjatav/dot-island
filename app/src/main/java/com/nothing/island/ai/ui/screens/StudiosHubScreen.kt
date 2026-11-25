package com.nothing.island.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.navigation.IslandDestination
import com.nothing.island.ai.ui.components.NothingCard
import com.nothing.island.ai.ui.components.NothingCardVariant
import com.nothing.island.ai.ui.components.NothingSectionHeader
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Data item representing a feature studio gateway card.
 */
data class StudioCardItem(
    val destination: IslandDestination,
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val glyphSymbol: String,
    val accentColor: Color
)

/**
 * Nothing OS 3.x Studios Hub Screen
 *
 * Central launchpad providing navigation access to specialized feature laboratories:
 * Theme Studio, Animation Studio, Gesture Studio, Peripherals Studio, Geometry Calibration,
 * and System Settings & Diagnostics.
 */
@Composable
fun StudiosHubScreen(
    onNavigateToDestination: (IslandDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val studiosList = listOf(
        StudioCardItem(
            destination = IslandDestination.ThemeStudio,
            title = "THEME STUDIO",
            subtitle = "5 Signature Themes: Classic, Glyph Arc, Tape Deck & More",
            badgeText = "5 THEMES",
            glyphSymbol = "🎨",
            accentColor = NothingTokens.NothingRed
        ),
        StudioCardItem(
            destination = IslandDestination.AnimationStudio,
            title = "ANIMATION STUDIO",
            subtitle = "Liquid, Bouncy & Droplet Curves, Multi-Color Border Glow",
            badgeText = "PHYSICS",
            glyphSymbol = "✨",
            accentColor = NothingTokens.NothingAmber
        ),
        StudioCardItem(
            destination = IslandDestination.GestureStudio,
            title = "GESTURE STUDIO",
            subtitle = "Bi-Directional Activity Switcher, Volume Swipe & Tutorial",
            badgeText = "TOUCH",
            glyphSymbol = "👆",
            accentColor = NothingTokens.NothingBlue
        ),
        StudioCardItem(
            destination = IslandDestination.PeripheralsStudio,
            title = "PERIPHERALS & SYSTEM",
            subtitle = "Bluetooth Audio Connection, Ringer Alerts & Device Unlock",
            badgeText = "EVENTS",
            glyphSymbol = "🎧",
            accentColor = NothingTokens.NothingGreen
        ),
        StudioCardItem(
            destination = IslandDestination.MascotStudio,
            title = "MASCOT STUDIO",
            subtitle = "Pixel Companions That Walk & Jump Inside The Island",
            badgeText = "PIXEL",
            glyphSymbol = "🐱",
            accentColor = NothingTokens.NothingRed
        ),
        StudioCardItem(
            destination = IslandDestination.GeometryStudio,
            title = "GEOMETRY CALIBRATION",
            subtitle = "Cutout Alignment, Width/Height DP & Corner Radii",
            badgeText = "HARDWARE",
            glyphSymbol = "📐",
            accentColor = NothingTokens.NothingTextSecondary
        ),
        StudioCardItem(
            destination = IslandDestination.CallStudio,
            title = "CALL STUDIO",
            subtitle = "System & VoIP Call Notch, End Button & Live Preview",
            badgeText = "CALL",
            glyphSymbol = "📞",
            accentColor = NothingTokens.NothingGreen
        ),
        StudioCardItem(
            destination = IslandDestination.Settings,
            title = "SYSTEM SETTINGS & GATEWAYS",
            subtitle = "AOSP Layer 24000 Anchor, Permissions & Hardware Info",
            badgeText = "GATEWAYS",
            glyphSymbol = "⚙️",
            accentColor = NothingTokens.NothingTextSecondary
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingTokens.NothingBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = NothingTokens.ScreenHorizontalPadding, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hub Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NothingTokens.NothingRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STUDIOS HUB",
                        color = NothingTokens.NothingTextPrimary,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = "DESIGN LABORATORIES & SYSTEM GATEWAYS",
                    color = NothingTokens.NothingTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        NothingSectionHeader(title = "FEATURE STUDIOS & WORKSHOPS", isAccent = true)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            studiosList.forEach { studio ->
                NothingCard(
                    variant = NothingCardVariant.STANDARD,
                    onClick = { onNavigateToDestination(studio.destination) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Studio Icon Box
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(NothingTokens.NothingSurfaceMedium),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = studio.glyphSymbol, fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = studio.title,
                                    color = NothingTokens.NothingTextPrimary,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = studio.badgeText,
                                    color = studio.accentColor,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = studio.subtitle,
                                color = NothingTokens.NothingTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = ">",
                            color = NothingTokens.NothingTextTertiary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
