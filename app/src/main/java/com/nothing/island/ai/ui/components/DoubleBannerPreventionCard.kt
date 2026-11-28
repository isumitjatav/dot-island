package com.nothing.island.ai.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.notifications.SystemHeadsUpController
import com.nothing.island.ai.ui.screens.InstalledAppItem
import com.nothing.island.ai.ui.theme.NothingDivider
import com.nothing.island.ai.ui.theme.NothingElevatedSurface
import com.nothing.island.ai.ui.theme.NothingMutedText
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import kotlinx.coroutines.launch

/**
 * Auto-detects installed chat and call apps (Phone, Messages, WhatsApp, Telegram).
 */
fun detectInstalledChatAndCallApps(context: Context): List<InstalledAppItem> {
    val pm = context.packageManager
    val result = mutableListOf<InstalledAppItem>()

    // 1. Phone / Dialer
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    val defaultDialer = telecomManager?.defaultDialerPackage
    val phoneCandidates = listOfNotNull(
        defaultDialer,
        "com.google.android.dialer",
        "com.android.dialer",
        "com.samsung.android.dialer"
    ).distinct()

    for (pkg in phoneCandidates) {
        val label = getAppLabel(pm, pkg)
        if (label != null) {
            result.add(InstalledAppItem("Phone", pkg, label))
            break
        }
    }

    // 2. Messages / SMS
    val defaultSms = try {
        android.provider.Telephony.Sms.getDefaultSmsPackage(context)
    } catch (_: Exception) {
        null
    }
    val messagesCandidates = listOfNotNull(
        defaultSms,
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging"
    ).distinct()

    for (pkg in messagesCandidates) {
        val label = getAppLabel(pm, pkg)
        if (label != null) {
            result.add(InstalledAppItem("Messages", pkg, label))
            break
        }
    }

    // 3. WhatsApp
    val whatsappCandidates = listOf("com.whatsapp", "com.whatsapp.w4b")
    for (pkg in whatsappCandidates) {
        val label = getAppLabel(pm, pkg)
        if (label != null) {
            result.add(InstalledAppItem("WhatsApp", pkg, label))
            break
        }
    }

    // 4. Telegram
    val telegramCandidates = listOf(
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.telegram.messenger.beta"
    )
    for (pkg in telegramCandidates) {
        val label = getAppLabel(pm, pkg)
        if (label != null) {
            result.add(InstalledAppItem("Telegram", pkg, label))
            break
        }
    }

    return result
}

private fun getAppLabel(pm: PackageManager, packageName: String): String? {
    return try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }
        pm.getApplicationLabel(info).toString()
    } catch (_: Exception) {
        null
    }
}

/**
 * Prevent Double Banners Component
 *
 * Auto-detects installed chat and call apps (WhatsApp, Phone, Messages, Telegram),
 * renders their app icons, provides a one-tap button to open system notification settings,
 * and displays the 1-line hint: "Turn off 'Pop on screen' so only Dot Island shows alerts."
 */
@Composable
fun DoubleBannerPreventionCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val detectedApps = remember { detectInstalledChatAndCallApps(context) }
    val headsUpController = remember { SystemHeadsUpController(context) }
    val hasSecurePerm = remember { headsUpController.hasSecureSettingsPermission() }
    var isMutedLive by remember { mutableStateOf(hasSecurePerm && headsUpController.isSystemHeadsUpMuted()) }
    val coroutineScope = rememberCoroutineScope()

    IslandCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prevent Double Banners",
                    color = NothingPrimaryText,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NothingElevatedSurface)
                        .border(1.dp, NothingDivider, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${detectedApps.size} APPS DETECTED",
                        color = NothingMutedText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 1-line plain-English hint
            Text(
                text = "Turn off 'Pop on screen' so only Dot Island shows alerts.",
                color = NothingSecondaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (hasSecurePerm) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingPrimaryBackground)
                        .border(1.dp, NothingDivider, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic Banner Silence",
                            color = NothingPrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isMutedLive) "System popups muted automatically" else "System popups active",
                            color = NothingMutedText,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isMutedLive,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                if (headsUpController.setSystemHeadsUpMuted(checked)) {
                                    isMutedLive = checked
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NothingPrimaryAccent,
                            uncheckedThumbColor = NothingMutedText,
                            uncheckedTrackColor = NothingElevatedSurface
                        )
                    )
                }
            }

            if (detectedApps.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    detectedApps.forEach { appItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingPrimaryBackground)
                                .border(1.dp, NothingDivider, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                AppNotificationIcon(
                                    packageName = appItem.packageName,
                                    fallbackChar = appItem.categoryName.firstOrNull() ?: 'A',
                                    size = 28.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = appItem.appLabel,
                                        color = NothingPrimaryText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = appItem.categoryName,
                                        color = NothingMutedText,
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingElevatedSurface)
                                    .border(1.dp, NothingDivider, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, appItem.packageName)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            try {
                                                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.parse("package:${appItem.packageName}")
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(fallback)
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "APP SETTINGS",
                                    color = NothingPrimaryText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "No compatible messaging apps detected.",
                    color = NothingMutedText,
                    fontSize = 11.sp
                )
            }
        }
    }
}
