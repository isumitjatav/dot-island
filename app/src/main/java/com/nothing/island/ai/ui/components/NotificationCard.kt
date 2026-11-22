package com.nothing.island.ai.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.NothingBlack
import com.nothing.island.ai.NothingBorder
import com.nothing.island.ai.NothingDarkGray
import com.nothing.island.ai.NothingMutedText
import com.nothing.island.ai.NothingRed
import com.nothing.island.ai.NothingWhite
import com.nothing.island.ai.engine.DownloadProgressState
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.notifications.NotificationActionBridge
import com.nothing.island.ai.notifications.NotificationPreviewMode
import com.nothing.island.ai.notifications.ReplyResult
import kotlinx.coroutines.delay

/**
 * Immutable display state computed for NotificationCard.
 * Completely testable in JVM unit tests without Android Context or WindowManager dependencies.
 */
data class NotificationCardState(
    val shouldRender: Boolean,
    val appDisplayName: String,
    val title: String?,
    val body: String?,
    val subText: String?,
    val formattedTime: String?,
    val previewMode: NotificationPreviewMode,
    val progress: Int = 0,
    val maxProgress: Int = 0,
    val isIndeterminate: Boolean = false,
    val hasProgress: Boolean = false,
    val progressState: DownloadProgressState = DownloadProgressState.NONE
)

/**
 * Pure Kotlin state resolver for NotificationCard presentation.
 * Enforces Nothing OS typography, privacy masking, download progress, and preview mode rules.
 */
object NotificationCardResolver {

    /**
     * Resolves raw notification fields into a sanitized NotificationCardState.
     */
    fun resolve(
        packageName: String,
        rawTitle: String,
        rawText: String,
        rawSubText: String? = null,
        timestamp: Long = 0L,
        previewMode: NotificationPreviewMode = NotificationPreviewMode.FULL,
        appLabel: String? = null,
        currentTimeMillis: Long = System.currentTimeMillis(),
        progress: Int = 0,
        maxProgress: Int = 0,
        isIndeterminate: Boolean = false,
        hasProgress: Boolean = false,
        progressState: DownloadProgressState = DownloadProgressState.NONE
    ): NotificationCardState {
        // Rule 1: HIDDEN preview mode must suppress all visual presentation
        if (previewMode == NotificationPreviewMode.HIDDEN) {
            return NotificationCardState(
                shouldRender = false,
                appDisplayName = "",
                title = null,
                body = null,
                subText = null,
                formattedTime = null,
                previewMode = NotificationPreviewMode.HIDDEN,
                progress = progress,
                maxProgress = maxProgress,
                isIndeterminate = isIndeterminate,
                hasProgress = hasProgress,
                progressState = progressState
            )
        }

        // Rule 2: App display name resolution with fallback
        val resolvedAppName = if (!appLabel.isNullOrBlank()) {
            appLabel.trim()
        } else {
            packageName.substringAfterLast('.').ifBlank { "APP" }
        }

        // Rule 3: Reliable timestamp formatting
        val formattedTime = if (timestamp > 0L) {
            formatTimestamp(timestamp, currentTimeMillis)
        } else {
            null
        }

        // Rule 4: Preview mode mapping
        return when (previewMode) {
            NotificationPreviewMode.HIDDEN -> NotificationCardState(
                shouldRender = false,
                appDisplayName = "",
                title = null,
                body = null,
                subText = null,
                formattedTime = null,
                previewMode = NotificationPreviewMode.HIDDEN,
                progress = progress,
                maxProgress = maxProgress,
                isIndeterminate = isIndeterminate,
                hasProgress = hasProgress,
                progressState = progressState
            )
            NotificationPreviewMode.APP_ONLY -> NotificationCardState(
                shouldRender = true,
                appDisplayName = resolvedAppName,
                title = "New notification",
                body = null,
                subText = null,
                formattedTime = formattedTime,
                previewMode = NotificationPreviewMode.APP_ONLY,
                progress = progress,
                maxProgress = maxProgress,
                isIndeterminate = isIndeterminate,
                hasProgress = hasProgress,
                progressState = progressState
            )
            NotificationPreviewMode.TITLE_ONLY -> NotificationCardState(
                shouldRender = true,
                appDisplayName = resolvedAppName,
                title = rawTitle.ifBlank { resolvedAppName },
                body = null,
                subText = null,
                formattedTime = formattedTime,
                previewMode = NotificationPreviewMode.TITLE_ONLY,
                progress = progress,
                maxProgress = maxProgress,
                isIndeterminate = isIndeterminate,
                hasProgress = hasProgress,
                progressState = progressState
            )
            NotificationPreviewMode.FULL -> NotificationCardState(
                shouldRender = true,
                appDisplayName = resolvedAppName,
                title = rawTitle.ifBlank { resolvedAppName },
                body = rawText.takeIf { it.isNotBlank() },
                subText = rawSubText?.takeIf { it.isNotBlank() },
                formattedTime = formattedTime,
                previewMode = NotificationPreviewMode.FULL,
                progress = progress,
                maxProgress = maxProgress,
                isIndeterminate = isIndeterminate,
                hasProgress = hasProgress,
                progressState = progressState
            )
        }
    }

    /**
     * Formats notification timestamp into a concise, relative Nothing OS indicator.
     */
    fun formatTimestamp(timestamp: Long, currentTimeMillis: Long = System.currentTimeMillis()): String {
        val diffMs = currentTimeMillis - timestamp
        return when {
            diffMs < 0L -> "NOW"
            diffMs < 60_000L -> "NOW"
            diffMs < 3_600_000L -> "${(diffMs / 60_000L).coerceAtLeast(1)}m ago"
            diffMs < 86_400_000L -> "${(diffMs / 3_600_000L).coerceAtLeast(1)}h ago"
            else -> "RECENT"
        }
    }
}

/**
 * NotificationCard
 *
 * Polished, readable visual presentation card for active notifications in Nothing Island:
 * - Pure AMOLED Black (#000000) with 1dp NothingBorder (#262626) and 26dp rounded corners.
 * - Symmetrical punch-hole camera clearance spacer (48dp).
 * - Header Row: App icon (22dp), uppercase monospace app source name (12sp), and COLLAPSE / DISMISS chips.
 * - Body: Inner dark gray container (#141414) displaying readable title (14sp bold, maxLines=2),
 *   body (12sp, maxLines=3), subtext (10.5sp), progress indicator (if downloading), and interactive action chips.
 * - Inline Reply: Direct reply support with RemoteInput without persistence.
 * - Footer: Minimalist Nothing OS brand & telemetry row.
 */
@Composable
fun NotificationCard(
    event: IslandEvent.NotificationPosted,
    modifier: Modifier = Modifier,
    previewMode: NotificationPreviewMode = NotificationPreviewMode.FULL,
    accentColor: Color = NothingRed,
    appName: String? = null,
    topClearanceDp: Dp = 48.dp,
    notificationKey: String? = event.id,
    isSystemActive: Boolean = true,
    swipeToDismissEnabled: Boolean = true,
    onCollapse: (() -> Unit)? = null,
    onDismiss: ((String) -> Unit)? = null,
    onClearAll: (() -> Unit)? = null,
    onOpenContent: ((String) -> Unit)? = null,
    onExecuteAction: ((String, Int) -> Unit)? = null,
    onSendReply: ((String, Int, String) -> ReplyResult)? = null,
    onInputFocusChanged: ((Boolean) -> Unit)? = null,
    isCameraCutoutEnabled: Boolean = false
) {
    val context = LocalContext.current
    val resolvedAppLabel = appName ?: remember(event.packageName) {
        try {
            val appInfo = context.packageManager.getApplicationInfo(event.packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Throwable) {
            null
        }
    }

    NotificationCard(
        packageName = event.packageName,
        title = event.title,
        text = event.text,
        subText = event.subText,
        timestamp = event.timestamp,
        modifier = modifier,
        previewMode = previewMode,
        accentColor = accentColor,
        appName = resolvedAppLabel,
        topClearanceDp = topClearanceDp,
        notificationKey = notificationKey,
        isSystemActive = isSystemActive,
        hasProgress = event.hasProgress,
        progress = event.progress,
        maxProgress = event.maxProgress,
        isIndeterminate = event.isIndeterminate,
        progressState = event.progressState,
        swipeToDismissEnabled = swipeToDismissEnabled,
        onCollapse = onCollapse,
        onDismiss = onDismiss,
        onClearAll = onClearAll,
        onOpenContent = onOpenContent,
        onExecuteAction = onExecuteAction,
        onSendReply = onSendReply,
        onInputFocusChanged = onInputFocusChanged,
        isCameraCutoutEnabled = isCameraCutoutEnabled
    )
}

/**
 * Raw-field overload for NotificationCard, enabling direct Compose previews and test fixtures.
 */
@Composable
fun NotificationCard(
    packageName: String,
    title: String,
    text: String,
    subText: String? = null,
    timestamp: Long = 0L,
    modifier: Modifier = Modifier,
    previewMode: NotificationPreviewMode = NotificationPreviewMode.FULL,
    accentColor: Color = NothingRed,
    appName: String? = null,
    topClearanceDp: Dp = 48.dp,
    notificationKey: String? = null,
    isSystemActive: Boolean = true,
    hasProgress: Boolean = false,
    progress: Int = 0,
    maxProgress: Int = 0,
    isIndeterminate: Boolean = false,
    progressState: DownloadProgressState = DownloadProgressState.NONE,
    swipeToDismissEnabled: Boolean = true,
    onCollapse: (() -> Unit)? = null,
    onDismiss: ((String) -> Unit)? = null,
    onClearAll: (() -> Unit)? = null,
    onOpenContent: ((String) -> Unit)? = null,
    onExecuteAction: ((String, Int) -> Unit)? = null,
    onSendReply: ((String, Int, String) -> ReplyResult)? = null,
    onInputFocusChanged: ((Boolean) -> Unit)? = null,
    isCameraCutoutEnabled: Boolean = false
) {
    val state = remember(packageName, title, text, subText, timestamp, previewMode, appName, progress, maxProgress, isIndeterminate, hasProgress, progressState) {
        NotificationCardResolver.resolve(
            packageName = packageName,
            rawTitle = title,
            rawText = text,
            rawSubText = subText,
            timestamp = timestamp,
            previewMode = previewMode,
            appLabel = appName,
            progress = progress,
            maxProgress = maxProgress,
            isIndeterminate = isIndeterminate,
            hasProgress = hasProgress,
            progressState = progressState
        )
    }

    // Suppress visual layout completely when preview mode is HIDDEN
    if (!state.shouldRender) {
        return
    }

    val actionEntry = remember(notificationKey) {
        if (notificationKey != null) NotificationActionBridge.get(notificationKey) else null
    }

    // Inline reply state (zero persistent storage)
    var replyingActionIndex by remember { mutableStateOf<Int?>(null) }
    var replyText by remember { mutableStateOf("") }
    var replyStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(replyingActionIndex) {
        if (replyingActionIndex != null) {
            onInputFocusChanged?.invoke(true)
        }
    }

    LaunchedEffect(replyStatus) {
        if (replyStatus == "SENT") {
            delay(1200L)
            replyText = ""
            replyStatus = null
            replyingActionIndex = null
            onInputFocusChanged?.invoke(false)
        }
    }

    val cardShape = if (isCameraCutoutEnabled) notchedCardShape() else RoundedCornerShape(26.dp)
    var totalDrag by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = 50f

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(NothingBlack)
            .border(width = 1.dp, color = NothingBorder, shape = cardShape)
            .then(
                if (swipeToDismissEnabled && onDismiss != null && notificationKey != null) {
                    Modifier.pointerInput(notificationKey) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDrag += dragAmount
                            },
                            onDragEnd = {
                                if (kotlin.math.abs(totalDrag) > swipeThresholdPx) {
                                    onDismiss(notificationKey)
                                }
                                totalDrag = 0f
                            },
                            onDragCancel = { totalDrag = 0f }
                        )
                    }
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Punch-hole clearance spacer
            val effectiveTopClearance = if (isCameraCutoutEnabled) 18.dp else 24.dp
            Spacer(modifier = Modifier.height(effectiveTopClearance))

            // 2. Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    AppNotificationIcon(
                        packageName = packageName,
                        fallbackChar = state.title?.firstOrNull()?.uppercaseChar()
                            ?: state.appDisplayName.firstOrNull()?.uppercaseChar()
                            ?: 'N',
                        size = 22.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.appDisplayName.uppercase(),
                        color = NothingWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Header controls (CLEAR ALL, DISMISS, COLLAPSE, or formatted timestamp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onClearAll != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingDarkGray)
                                .clickable { onClearAll() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "CLEAR ALL",
                                color = NothingRed,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (onDismiss != null && notificationKey != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingDarkGray)
                                .clickable { onDismiss(notificationKey) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "DISMISS",
                                color = NothingWhite,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (onCollapse != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingDarkGray)
                                .clickable {
                                    replyingActionIndex = null
                                    replyText = ""
                                    onInputFocusChanged?.invoke(false)
                                    onCollapse()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "COLLAPSE",
                                color = NothingWhite,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (state.formattedTime != null) {
                        Text(
                            text = state.formattedTime,
                            color = NothingMutedText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 3. Body: Message Container (clickable if contentIntent is available and not replying)
            val isBodyClickable = notificationKey != null && actionEntry?.contentIntent != null && onOpenContent != null && replyingActionIndex == null
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingDarkGray)
                    .then(
                        if (isBodyClickable) {
                            Modifier.clickable { onOpenContent?.invoke(notificationKey!!) }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                if (!state.title.isNullOrBlank()) {
                    Text(
                        text = state.title,
                        color = NothingWhite,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (state.previewMode == NotificationPreviewMode.APP_ONLY) FontWeight.Normal else FontWeight.Bold,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Body content (only rendered in FULL preview mode when non-blank)
                if (!state.body.isNullOrBlank()) {
                    Text(
                        text = state.body,
                        color = NothingWhite.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Subtext
                if (!state.subText.isNullOrBlank()) {
                    Text(
                        text = state.subText,
                        color = NothingMutedText,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Download Progress Tracking Section
                if (state.hasProgress && state.progressState != DownloadProgressState.NONE) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val stateLabel = when (state.progressState) {
                                DownloadProgressState.DOWNLOADING -> "DOWNLOADING"
                                DownloadProgressState.INDETERMINATE -> "DOWNLOADING..."
                                DownloadProgressState.PAUSED -> "PAUSED"
                                DownloadProgressState.COMPLETED -> "DOWNLOAD COMPLETE"
                                DownloadProgressState.FAILED -> "DOWNLOAD FAILED"
                                else -> "PROGRESS"
                            }
                            Text(
                                text = stateLabel,
                                color = when (state.progressState) {
                                    DownloadProgressState.FAILED -> NothingRed
                                    DownloadProgressState.PAUSED -> Color(0xFFE5A93C)
                                    else -> NothingWhite
                                },
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            if (state.progressState == DownloadProgressState.DOWNLOADING && state.maxProgress > 0) {
                                val pct = ((state.progress.toFloat() / state.maxProgress.toFloat()) * 100).toInt().coerceIn(0, 100)
                                Text(
                                    text = "$pct%",
                                    color = NothingWhite,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Progress track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF262626))
                        ) {
                            if (state.progressState == DownloadProgressState.DOWNLOADING && state.maxProgress > 0) {
                                val fraction = (state.progress.toFloat() / state.maxProgress.toFloat()).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxSize()
                                        .background(NothingRed)
                                )
                            } else if (state.progressState == DownloadProgressState.INDETERMINATE) {
                                val infiniteTransition = rememberInfiniteTransition(label = "notif_indeterminate")
                                val offsetFraction by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "sweep_progress"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.35f)
                                        .fillMaxSize()
                                        .offset(x = (200 * offsetFraction).dp)
                                        .background(NothingRed)
                                )
                            } else if (state.progressState == DownloadProgressState.COMPLETED) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(NothingRed)
                                )
                            }
                        }
                    }
                }

                // Inline Reply UI or Action Chips
                if (replyingActionIndex != null && notificationKey != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E1E))
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = NothingWhite,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            cursorBrush = SolidColor(NothingRed),
                            decorationBox = { innerTextField ->
                                if (replyText.isEmpty()) {
                                    Text(
                                        text = "Type reply...",
                                        color = NothingMutedText,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                innerTextField()
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (replyText.isNotBlank() && replyingActionIndex != null) {
                                    replyStatus = "SENDING"
                                    val res = onSendReply?.invoke(notificationKey, replyingActionIndex!!, replyText)
                                    if (res == ReplyResult.SUCCESS) {
                                        replyStatus = "SENT"
                                    } else if (res == ReplyResult.CANCELED) {
                                        replyStatus = "EXPIRED"
                                    } else {
                                        replyStatus = "FAILED"
                                    }
                                }
                            })
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        if (replyStatus != null) {
                            Text(
                                text = replyStatus!!,
                                color = if (replyStatus == "SENT") NothingWhite else NothingRed,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        // SEND button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (replyText.isNotBlank()) NothingRed else NothingBorder)
                                .clickable(enabled = replyText.isNotBlank() && replyStatus != "SENDING") {
                                    if (replyingActionIndex != null) {
                                        replyStatus = "SENDING"
                                        val res = onSendReply?.invoke(notificationKey, replyingActionIndex!!, replyText)
                                        if (res == ReplyResult.SUCCESS) {
                                            replyStatus = "SENT"
                                        } else if (res == ReplyResult.CANCELED) {
                                            replyStatus = "EXPIRED"
                                        } else {
                                            replyStatus = "FAILED"
                                        }
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "SEND",
                                color = NothingWhite,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // CANCEL button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NothingDarkGray)
                                .clickable {
                                    replyText = ""
                                    replyStatus = null
                                    replyingActionIndex = null
                                    onInputFocusChanged?.invoke(false)
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "×",
                                color = NothingMutedText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (actionEntry != null && actionEntry.actions.isNotEmpty() && notificationKey != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        actionEntry.actions.take(3).forEachIndexed { index, actionItem ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingBorder)
                                    .clickable {
                                        if (actionItem.isReply) {
                                            replyingActionIndex = index
                                            replyStatus = null
                                            replyText = ""
                                            onInputFocusChanged?.invoke(true)
                                        } else {
                                            onExecuteAction?.invoke(notificationKey, index)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = actionItem.title.uppercase(),
                                    color = NothingWhite,
                                    fontSize = 10.5.sp,
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

            // 4. Footer: Minimalist Nothing OS brand & telemetry row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSystemActive) "NOTHING ISLAND • NOTIFICATION" else "NOTHING ISLAND • (REMOVED)",
                    color = NothingMutedText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (state.formattedTime != null && onCollapse != null) {
                    Text(
                        text = state.formattedTime,
                        color = NothingMutedText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else if (state.previewMode != NotificationPreviewMode.FULL) {
                    Text(
                        text = state.previewMode.name.replace('_', ' '),
                        color = NothingMutedText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
