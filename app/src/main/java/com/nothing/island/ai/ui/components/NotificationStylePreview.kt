package com.nothing.island.ai.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nothing.island.ai.notifications.NotificationPreviewMode
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Reference-style notification presentation picker: two visual cards showing a
 * phone mockup with the resulting island popup. Selecting a card changes the
 * preview live with an animation.
 */
@Composable
fun NotificationStylePreview(
    selected: NotificationPreviewMode,
    onSelect: (NotificationPreviewMode) -> Unit,
    accentColor: Color = NothingPrimaryAccent,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        NotificationPreviewMode.FULL to "EXPANDED POPUP",
        NotificationPreviewMode.TITLE_ONLY to "SMALL POPUP"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { (mode, label) ->
                val isSelected = selected == mode
                val borderColor = if (isSelected) accentColor else NothingTokens.NothingBorderSubtle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NothingTokens.NothingSurfaceLow)
                        .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable { onSelect(mode) }
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Phone mockup with the island popup preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NothingTokens.NothingSurfaceLowest)
                            .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .animateContentSize(animationSpec = tween(240, easing = FastOutSlowInEasing)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (mode == NotificationPreviewMode.FULL) {
                                // Expanded notification card
                                Box(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0A0A0A))
                                        .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(10.dp))
                                        .padding(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF25D366)))
                                        Spacer(Modifier.width(4.dp))
                                        Column {
                                            Text("WhatsApp", color = NothingPrimaryText, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                                            Text("Meeting at 3 PM?", color = NothingSecondaryText, fontSize = 5.sp)
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Box(Modifier.height(6.dp).width(24.dp).clip(RoundedCornerShape(3.dp)).background(NothingTokens.NothingSurfaceLow))
                                        Box(Modifier.height(6.dp).width(20.dp).clip(RoundedCornerShape(3.dp)).background(NothingTokens.NothingSurfaceLow))
                                    }
                                }
                            } else {
                                // Small capsule popup
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0A0A0A))
                                        .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF25D366)))
                                        Spacer(Modifier.width(4.dp))
                                        Text("WhatsApp", color = NothingPrimaryText, fontSize = 6.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = label,
                        color = if (isSelected) NothingPrimaryText else NothingSecondaryText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    if (isSelected) {
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(accentColor))
                    }
                }
            }
        }
    }
}
