package com.nothing.island.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.NothingBlack
import com.nothing.island.ai.NothingBorder
import com.nothing.island.ai.NothingWhite
import com.nothing.island.ai.engine.DownloadProgressState
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.islandProgressBorder

/**
 * NotificationCompactView
 *
 * Symmetrical 200dp capsule preview centered on the Nothing Phone (4a) punch-hole:
 * [ Left Wing: 73dp ] [ Punch-Hole Clearance: 54dp ] [ Right Wing: 73dp ] = 200dp
 *
 * Left Wing: App icon (18dp x 18dp) with 12dp start margin.
 * Center Zone: 54dp empty clearance (physical camera).
 * Right Wing: Truncated sender name / title / download percentage (Monospace, 11sp, NothingWhite).
 * Border: Dynamically reflects active download progress tracking around outer capsule perimeter.
 */
@Composable
fun NotificationCompactView(
    event: IslandEvent.NotificationPosted,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(18.dp)

    val rightWingText = if (event.hasProgress && event.progressState == DownloadProgressState.DOWNLOADING && event.maxProgress > 0) {
        val pct = ((event.progress.toFloat() / event.maxProgress.toFloat()) * 100).toInt().coerceIn(0, 100)
        "$pct%"
    } else if (event.hasProgress && event.progressState == DownloadProgressState.INDETERMINATE) {
        "..."
    } else if (event.hasProgress && event.progressState == DownloadProgressState.PAUSED) {
        "PAUSED"
    } else if (event.hasProgress && event.progressState == DownloadProgressState.COMPLETED) {
        "DONE"
    } else {
        event.title.ifBlank { event.packageName.substringAfterLast('.') }
    }

    Box(
        modifier = modifier
            .width(200.dp)
            .height(36.dp)
            .clip(pillShape)
            .background(NothingBlack)
            .islandProgressBorder(
                shape = pillShape,
                progressState = event.progressState,
                progress = event.progress,
                maxProgress = event.maxProgress,
                accentColor = NothingWhite
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onTap()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Left Wing: 73dp (App Icon / Glyph)
            Box(
                modifier = Modifier
                    .width(73.dp)
                    .fillMaxHeight()
                    .padding(start = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                AppNotificationIcon(
                    packageName = event.packageName,
                    fallbackChar = event.title.firstOrNull()?.uppercaseChar() ?: 'N',
                    size = 18.dp
                )
            }

            // 2. Centered Punch-Hole Clearance Zone: 54dp
            Spacer(modifier = Modifier.width(54.dp))

            // 3. Right Wing: 73dp (Sender / Title / Percentage Truncated)
            Box(
                modifier = Modifier
                    .width(73.dp)
                    .fillMaxHeight()
                    .padding(end = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = rightWingText,
                    color = NothingWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
