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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nothing.island.ai.ui.theme.NothingDivider
import com.nothing.island.ai.ui.theme.NothingElevatedSurface
import com.nothing.island.ai.ui.theme.NothingMutedText
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText

enum class ProminentDisclosureType {
    ACCESSIBILITY,
    NOTIFICATION_LISTENER
}

data class ProminentDisclosureSpec(
    val type: ProminentDisclosureType,
    val badge: String,
    val title: String,
    val summary: String,
    val bulletPoints: List<String>,
    val confirmLabel: String,
    val declineLabel: String = "NOT NOW"
)

/**
 * Returns the Google Play–compliant Prominent Disclosure copy shown before launching
 * sensitive Android system settings screens. Pure and unit-testable.
 */
fun prominentDisclosureSpec(type: ProminentDisclosureType): ProminentDisclosureSpec = when (type) {
    ProminentDisclosureType.ACCESSIBILITY -> ProminentDisclosureSpec(
        type = ProminentDisclosureType.ACCESSIBILITY,
        badge = "OPTIONAL SHORTCUTS & OVERLAY",
        title = "Accessibility Service Disclosure",
        summary = "Dot Island requests Android Accessibility access to anchor the floating capsule above the status bar around your front camera cutout and trigger user-initiated quick shortcuts (screen lock, global screenshot, and media controls).",
        bulletPoints = listOf(
            "Runs 100% locally on your device — zero cloud servers.",
            "canRetrieveWindowContent = false: never reads screen text, passwords, or personal messages.",
            "canPerformGestures = false: never injects touches or automates apps.",
            "Zero accessibility, diagnostic, or usage data is collected, stored, or shared."
        ),
        confirmLabel = "ACCEPT & OPEN SETTINGS"
    )
    ProminentDisclosureType.NOTIFICATION_LISTENER -> ProminentDisclosureSpec(
        type = ProminentDisclosureType.NOTIFICATION_LISTENER,
        badge = "REQUIRED FOR LIVE ALERTS",
        title = "Notification Access Disclosure",
        summary = "Dot Island requests Notification Listener access to display incoming notifications, inline reply actions, and active media playback controls inside the Dynamic Capsule.",
        bulletPoints = listOf(
            "Processed 100% on-device in transient memory only.",
            "Never logs message history to disk or transmits notification content off-device.",
            "Respects lock-screen privacy masking preferences (hide sensitive content).",
            "Zero analytics, zero tracking SDKs, and zero third-party data sharing."
        ),
        confirmLabel = "ACCEPT & OPEN SETTINGS"
    )
}

/**
 * Blocking Google Play–compliant Prominent Disclosure modal shown prior to navigating
 * to Accessibility Settings or Notification Listener Settings.
 */
@Composable
fun ProminentDisclosureDialog(
    type: ProminentDisclosureType,
    onAcceptAndContinue: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spec = prominentDisclosureSpec(type)

    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(NothingPrimaryBackground)
                .border(1.dp, NothingDivider, RoundedCornerShape(22.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NothingPrimaryAccent)
                )
                Text(
                    text = spec.badge,
                    color = NothingPrimaryAccent,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            Text(
                text = spec.title,
                color = NothingPrimaryText,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = spec.summary,
                color = NothingSecondaryText,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingElevatedSurface)
                    .border(1.dp, NothingDivider, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                spec.bulletPoints.forEach { bullet ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "•",
                            color = NothingPrimaryAccent,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bullet,
                            color = NothingPrimaryText,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NothingElevatedSurface)
                        .border(1.dp, NothingDivider, RoundedCornerShape(12.dp))
                        .clickable { onDecline() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = spec.declineLabel,
                        color = NothingMutedText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1.6f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NothingPrimaryAccent)
                        .clickable { onAcceptAndContinue() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = spec.confirmLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
