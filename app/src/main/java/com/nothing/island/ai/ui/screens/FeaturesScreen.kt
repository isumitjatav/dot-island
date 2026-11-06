package com.nothing.island.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.components.DotMatrixGlyph
import com.nothing.island.ai.ui.components.DotMatrixPatterns
import com.nothing.island.ai.ui.components.FeatureCard
import com.nothing.island.ai.ui.components.FeatureStatus
import com.nothing.island.ai.ui.components.SectionHeader
import com.nothing.island.ai.ui.theme.NothingMutedText
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSuccess
import com.nothing.island.ai.ui.theme.NothingWarning

@Composable
fun FeaturesScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingPrimaryBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "FEATURES & CONTROLS",
                color = NothingPrimaryText,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "SYSTEM INTEGRATION REGISTRY",
                color = NothingMutedText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // CATEGORY A: CORE FEATURES
        SectionHeader(title = "CATEGORY A — CORE FEATURES", badgeText = "4 MODULES")

        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard(
                name = "System Notifications",
                description = "Intercepts incoming messaging, social, and system alerts to display interactive popups around camera cutout.",
                status = FeatureStatus.NOT_CONNECTED,
                categoryBadge = "MILESTONE 4.2",
                iconGlyph = {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NothingPrimaryAccent))
                }
            )

            FeatureCard(
                name = "Music & Media Controls",
                description = "Tracks active MediaSession to provide track scrubber, play/pause, album artwork, and skip actions.",
                status = FeatureStatus.NOT_CONNECTED,
                categoryBadge = "MILESTONE 4.3",
                iconGlyph = {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NothingWarning))
                }
            )

            FeatureCard(
                name = "Charging & Battery Status",
                description = "Animates battery percentage, charging wattage indication, and low-battery warning capsules.",
                status = FeatureStatus.NOT_CONNECTED,
                categoryBadge = "MILESTONE 4.4",
                iconGlyph = {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NothingSuccess))
                }
            )

            FeatureCard(
                name = "Timer & Stopwatch",
                description = "Live countdown chronometer display with quick pause/resume controls directly in the compact pill.",
                status = FeatureStatus.PLANNED,
                categoryBadge = "MILESTONE 4.4",
                iconGlyph = {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NothingMutedText))
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CATEGORY B: ADVANCED FEATURES
        SectionHeader(title = "CATEGORY B — ADVANCED FEATURES", badgeText = "5 MODULES")

        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard(
                name = "Radial Camera Cutout Progress Ring",
                description = "Circular progress arc encircling the physical Nothing Phone (4a) punch hole for timers and battery.",
                status = FeatureStatus.PLANNED,
                categoryBadge = "MILESTONE 5",
                iconGlyph = {
                    DotMatrixGlyph(DotMatrixPatterns.IslandIcon, dotSize = 2.dp, spacing = 1.dp)
                }
            )

            FeatureCard(
                name = "Dual-Pill Multitasking",
                description = "Splits into two independent floating capsules when multiple background activities run simultaneously.",
                status = FeatureStatus.PLANNED,
                categoryBadge = "MILESTONE 5",
                iconGlyph = {
                    DotMatrixGlyph(DotMatrixPatterns.IslandIcon, dotSize = 2.dp, spacing = 1.dp)
                }
            )

            FeatureCard(
                name = "Audio Waveform Visualizer",
                description = "Dynamic 5-bar live soundwave visualizer responding to active audio streams using RECORD_AUDIO.",
                status = FeatureStatus.PLANNED,
                categoryBadge = "MILESTONE 5",
                iconGlyph = {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NothingPrimaryAccent))
                }
            )

            FeatureCard(
                name = "Gesture Navigation Controls",
                description = "Swipe left/right to dismiss notifications, vertical swipe to collapse, long-press to launch target app.",
                status = FeatureStatus.PLANNED,
                categoryBadge = "MILESTONE 4.4",
                iconGlyph = {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NothingMutedText))
                }
            )

            FeatureCard(
                name = "Quick Tool Drawer",
                description = "Swipe down on expanded island to reveal quick toggles for flashlight, camera, and system volume.",
                status = FeatureStatus.PLANNED,
                categoryBadge = "MILESTONE 5",
                iconGlyph = {
                    DotMatrixGlyph(DotMatrixPatterns.Gear, dotSize = 2.dp, spacing = 1.dp)
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CATEGORY C: AI FEATURES
        SectionHeader(title = "CATEGORY C — AI FEATURES", badgeText = "3 MODULES")

        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard(
                name = "Gemini Quick Actions",
                description = "One-tap AI summaries, notification explanations, and contextual reply suggestions powered by Gemini.",
                status = FeatureStatus.COMING_SOON,
                categoryBadge = "MILESTONE 5",
                iconGlyph = {
                    DotMatrixGlyph(DotMatrixPatterns.SparkleAI, dotSize = 2.dp, spacing = 1.dp, activeColor = NothingPrimaryAccent)
                }
            )

            FeatureCard(
                name = "Voice Interaction (Live API)",
                description = "Bidirectional streaming voice conversations with Gemini 3.8 Live directly from the floating pill.",
                status = FeatureStatus.COMING_SOON,
                categoryBadge = "MILESTONE 5",
                iconGlyph = {
                    DotMatrixGlyph(DotMatrixPatterns.SparkleAI, dotSize = 2.dp, spacing = 1.dp, activeColor = NothingPrimaryAccent)
                }
            )

            FeatureCard(
                name = "Context-Aware Assistance",
                description = "On-device heuristics prioritizing critical family and work notifications while suppressing spam.",
                status = FeatureStatus.PLANNED,
                categoryBadge = "MILESTONE 5",
                iconGlyph = {
                    DotMatrixGlyph(DotMatrixPatterns.SparkleAI, dotSize = 2.dp, spacing = 1.dp)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
