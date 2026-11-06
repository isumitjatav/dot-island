package com.nothing.island.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import com.nothing.island.ai.NothingBlack
import com.nothing.island.ai.NothingBorder
import com.nothing.island.ai.NothingDarkGray
import com.nothing.island.ai.NothingMutedText
import com.nothing.island.ai.NothingWhite
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.notifications.NotificationActionBridge

/**
 * NotificationExpandedCard
 *
 * Full 350dp x 210dp expanded presentation card for active notifications:
 * - 48dp top spacer clearance below physical camera cutout and status bar.
 * - Header Row: App icon, app label/package, and COLLAPSE chip.
 * - Body: Message title, message text, and optional subText with tap-to-open.
 * - Footer Row: OPEN, DISMISS, and dynamic notification action buttons.
 */
@Composable
fun NotificationExpandedCard(
    event: IslandEvent.NotificationPosted,
    accentColor: Color,
    onCollapse: () -> Unit,
    onOpenContent: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onExecuteAction: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(26.dp)
    val cachedActions = NotificationActionBridge.get(event.id)?.actions ?: emptyList()
    val context = LocalContext.current
    val appLabel = remember(event.packageName) {
        try {
            val appInfo = context.packageManager.getApplicationInfo(event.packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Throwable) {
            event.packageName.substringAfterLast('.')
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(NothingBlack)
            .border(width = 1.dp, color = NothingBorder, shape = cardShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Punch-hole clearance spacer (48dp / 144px below status bar)
            Spacer(modifier = Modifier.height(48.dp))

            // 2. Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppNotificationIcon(
                        packageName = event.packageName,
                        fallbackChar = event.title.firstOrNull()?.uppercaseChar() ?: 'N',
                        size = 20.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = appLabel.uppercase(),
                        color = NothingWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // COLLAPSE chip button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingDarkGray)
                        .clickable { onCollapse() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "COLLAPSE",
                        color = NothingWhite,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. Body: Message Container (Tap to Open)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingDarkGray)
                    .clickable { onOpenContent(event.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = event.title,
                    color = NothingWhite,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (event.text.isNotBlank()) {
                    Text(
                        text = event.text,
                        color = NothingWhite.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!event.subText.isNullOrBlank()) {
                    Text(
                        text = event.subText,
                        color = NothingMutedText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. Footer Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary OPEN Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { onOpenContent(event.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "OPEN",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // DISMISS Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingDarkGray)
                        .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                        .clickable { onDismiss(event.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "DISMISS",
                        color = NothingMutedText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dynamic Action Buttons (e.g. Reply, Mark Read)
                cachedActions.take(2).forEachIndexed { index, action ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingDarkGray)
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                            .clickable { onExecuteAction(event.id, index) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = action.title.take(12).uppercase(),
                            color = NothingWhite,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
