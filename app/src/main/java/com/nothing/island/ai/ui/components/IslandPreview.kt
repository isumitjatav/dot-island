package com.nothing.island.ai.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.theme.NothingDivider
import com.nothing.island.ai.ui.theme.NothingElevatedSurface
import com.nothing.island.ai.ui.theme.NothingMutedText
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryBackground
import com.nothing.island.ai.ui.theme.NothingSecondaryText

@Composable
fun IslandPreview(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    pillWidthDp: Float = 124f,
    pillHeightDp: Float = 36f,
    cornerRadiusDp: Float = 18f,
    topOffsetDp: Float = 9f,
    horizontalOffsetDp: Float = 0f,
    accentColor: Color = NothingPrimaryAccent,
    title: String = "NOTHING ISLAND",
    statusText: String = "A069 (FroggerIND)"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PreviewPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PreviewPulseAlpha"
    )

    IslandCard(
        modifier = modifier,
        backgroundColor = NothingSecondaryBackground,
        borderColor = NothingDivider,
        contentPadding = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Header with Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE UI PREVIEW",
                    color = NothingMutedText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                // Interactive mode switcher pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NothingElevatedSurface)
                        .border(1.dp, NothingDivider, RoundedCornerShape(12.dp))
                        .clickable { onToggleExpanded() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "MODE: EXPANDED" else "MODE: COMPACT",
                        color = NothingPrimaryText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simulated Phone Bezel Screen Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isExpanded) 220.dp else 120.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF050505))
                    .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(22.dp))
                    .animateContentSize(animationSpec = tween(durationMillis = 250)),
                contentAlignment = Alignment.TopCenter
            ) {
                // Physical Punch-Hole Camera Representation (Centered at x=0, y=9dp)
                Box(
                    modifier = Modifier
                        .offset(x = 0.dp, y = (topOffsetDp + 6).dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF030303))
                        .border(1.dp, Color(0xFF181818), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Camera lens reflection hint
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F2937))
                    )
                }

                // Nothing Island Floating Overlay Preview
                val previewWidth = if (isExpanded) 310.dp else pillWidthDp.dp
                val previewHeight = if (isExpanded) 180.dp else pillHeightDp.dp
                val shape = if (isExpanded) RoundedCornerShape(24.dp) else RoundedCornerShape(cornerRadiusDp.dp)

                Box(
                    modifier = Modifier
                        .offset(x = horizontalOffsetDp.dp, y = topOffsetDp.dp)
                        .width(previewWidth)
                        .height(previewHeight)
                        .clip(shape)
                        .background(NothingPrimaryBackground)
                        .border(1.dp, Color(0xFF262626), shape)
                        .clickable { onToggleExpanded() }
                        .animateContentSize(animationSpec = tween(durationMillis = 250))
                ) {
                    if (!isExpanded) {
                        // Compact Pill Preview - R4.6.1 Clean idle appearance
                        // No decorative red dot, no white/gray glyph dots
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(pillHeightDp.dp)
                        )
                    } else {
                        // Expanded Card Preview
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Clearance for physical camera
                            Spacer(modifier = Modifier.height(28.dp))

                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = pulseAlpha))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = title,
                                        color = NothingPrimaryText,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Text(
                                    text = "TAP TO CLOSE",
                                    color = NothingMutedText,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Telemetry Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NothingElevatedSurface)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "DEVICE", color = NothingMutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        Text(text = statusText, color = NothingPrimaryText, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "CALIBRATION", color = NothingMutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        Text(text = "204dp x 27.2dp", color = accentColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Footer info
                            Text(
                                text = "NOTHING ISLAND • PREVIEW MODE",
                                color = NothingMutedText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Tap the preview island to toggle between compact and expanded presentation.",
                color = NothingSecondaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
