package com.nothing.island.ai.ui.components

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
import com.nothing.island.ai.ui.theme.NothingDivider
import com.nothing.island.ai.ui.theme.NothingElevatedSurface
import com.nothing.island.ai.ui.theme.NothingMutedText
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingSuccess
import com.nothing.island.ai.ui.theme.NothingWarning

enum class FeatureStatus(val label: String, val color: Color) {
    AVAILABLE("AVAILABLE", NothingSuccess),
    NOT_CONNECTED("NOT CONNECTED", NothingMutedText),
    PLANNED("PLANNED", NothingWarning),
    REQUIRES_PERMISSION("PERMISSION NEEDED", NothingPrimaryAccent),
    COMING_SOON("COMING IN M5", NothingMutedText)
}

@Composable
fun FeatureCard(
    name: String,
    description: String,
    status: FeatureStatus,
    modifier: Modifier = Modifier,
    categoryBadge: String? = null,
    iconGlyph: @Composable () -> Unit = {},
    actionLabel: String = "CONFIGURE",
    onActionClick: () -> Unit = {}
) {
    IslandCard(
        modifier = modifier,
        contentPadding = 14.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingElevatedSurface)
                            .border(1.dp, NothingDivider, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        iconGlyph()
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = name,
                            color = NothingPrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        if (categoryBadge != null) {
                            Text(
                                text = categoryBadge,
                                color = NothingMutedText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Status Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NothingElevatedSurface)
                        .border(1.dp, status.color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = status.label,
                        color = status.color,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                color = NothingSecondaryText,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isEnabled = status == FeatureStatus.AVAILABLE || status == FeatureStatus.REQUIRES_PERMISSION

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isEnabled) NothingElevatedSurface else Color.Transparent)
                        .border(
                            1.dp,
                            if (isEnabled) NothingDivider else Color(0xFF1E1E1E),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = isEnabled) { onActionClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isEnabled) actionLabel else "PENDING M4/M5",
                        color = if (isEnabled) NothingPrimaryText else NothingMutedText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
