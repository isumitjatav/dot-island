package com.nothing.island.ai

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.data.IslandPreferences
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.engine.DownloadProgressState
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.media.MediaTrackState
import com.nothing.island.ai.media.MusicPreferences
import com.nothing.island.ai.notifications.NotificationPreferences
import com.nothing.island.ai.notifications.PrivacySafeNotificationPresentation
import com.nothing.island.ai.notifications.ReplyResult
import com.nothing.island.ai.notifications.UndoNotificationState
import com.nothing.island.ai.battery.BatteryPreferences
import com.nothing.island.ai.battery.BatteryState
import com.nothing.island.ai.switcher.IslandInterface
import com.nothing.island.ai.switcher.SwipeDirection
import com.nothing.island.ai.ui.components.AppNotificationIcon
import com.nothing.island.ai.ui.components.BatteryCompactView
import com.nothing.island.ai.ui.components.BatteryExpandedView
import com.nothing.island.ai.ui.components.MusicCompactView
import com.nothing.island.ai.ui.components.MusicExpandedView
import com.nothing.island.ai.ui.components.NotificationCard
import com.nothing.island.ai.ui.components.NotificationCardResolver
import com.nothing.island.ai.ui.components.NotificationCompactView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Color palette inspired by Nothing OS design language
val NothingBlack = Color(0xFF000000)
val NothingDarkGray = Color(0xFF141414)
val NothingBorder = Color(0xFF262626)
val NothingWhite = Color(0xFFEEEEEE)
val NothingMutedText = Color(0xFF888888)
val NothingRed = Color(0xFFD71920)

/**
 * Renders an animated or styled progress border on the outer edge of an Island pill or card.
 */
@Composable
fun Modifier.islandProgressBorder(
    shape: RoundedCornerShape,
    borderWidth: Dp = 1.5.dp,
    baseBorderColor: Color = NothingBorder,
    progressState: DownloadProgressState = DownloadProgressState.NONE,
    progress: Int = 0,
    maxProgress: Int = 100,
    accentColor: Color = NothingWhite
): Modifier {
    if (progressState == DownloadProgressState.NONE) {
        return this.border(width = 1.dp, color = baseBorderColor, shape = shape)
    }

    val fraction = if (maxProgress > 0) (progress.toFloat() / maxProgress.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "border_progress_fraction"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "border_indeterminate")
    val indeterminatePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_sweep"
    )

    return this
        .border(width = 1.dp, color = baseBorderColor, shape = shape)
        .drawWithContent {
            drawContent()

            val strokeWidthPx = borderWidth.toPx()
            val rPx = size.height / 2f

            val roundRectPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(
                            offset = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f),
                            size = Size(
                                size.width - strokeWidthPx,
                                size.height - strokeWidthPx
                            )
                        ),
                        cornerRadius = CornerRadius(rPx, rPx)
                    )
                )
            }

            val pathMeasure = PathMeasure()
            pathMeasure.setPath(roundRectPath, false)
            val totalLength = pathMeasure.length

            if (totalLength > 0f) {
                when (progressState) {
                    DownloadProgressState.DOWNLOADING -> {
                        val segLength = totalLength * animatedFraction
                        if (segLength > 0f) {
                            val dst = Path()
                            pathMeasure.getSegment(0f, segLength, dst, true)
                            drawPath(
                                path = dst,
                                color = accentColor,
                                style = Stroke(
                                    width = strokeWidthPx,
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                    }
                    DownloadProgressState.INDETERMINATE -> {
                        val cometLength = totalLength * 0.25f
                        val start = (indeterminatePhase * totalLength)
                        val end = start + cometLength

                        val dst = Path()
                        if (end <= totalLength) {
                            pathMeasure.getSegment(start, end, dst, true)
                        } else {
                            pathMeasure.getSegment(start, totalLength, dst, true)
                            pathMeasure.getSegment(0f, end - totalLength, dst, true)
                        }
                        drawPath(
                            path = dst,
                            color = accentColor,
                            style = Stroke(
                                width = strokeWidthPx,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                    DownloadProgressState.PAUSED -> {
                        drawPath(
                            path = roundRectPath,
                            color = Color(0xFFE5A93C),
                            style = Stroke(width = strokeWidthPx)
                        )
                    }
                    DownloadProgressState.COMPLETED -> {
                        drawPath(
                            path = roundRectPath,
                            color = accentColor,
                            style = Stroke(width = strokeWidthPx)
                        )
                    }
                    DownloadProgressState.FAILED -> {
                        drawPath(
                            path = roundRectPath,
                            color = NothingRed,
                            style = Stroke(width = strokeWidthPx)
                        )
                    }
                    DownloadProgressState.NONE -> {}
                }
            }
        }
}

/**
 * Main floating overlay view for Nothing Island.
 * Manages presentation switching between compact pill modes and the two distinct expanded states:
 * - State A: Normal Dashboard (Time, Date, Compact Weather, Recent notification previews).
 * - State B: Notification-Focused View (Active notification card, actions; strictly NO clock/weather).
 */
@Composable
fun IslandOverlayView(
    state: IslandState,
    geometry: CutoutGeometry,
    preferences: IslandPreferences = IslandPreferencesRepository.HARDWARE_DEFAULTS,
    notificationPreferences: NotificationPreferences = NotificationPreferences.DEFAULTS,
    musicPreferences: MusicPreferences = MusicPreferences.DEFAULTS,
    batteryPreferences: BatteryPreferences = BatteryPreferences(),
    batteryState: BatteryState = BatteryState.INITIAL,
    currentTrack: MediaTrackState = MediaTrackState.EMPTY,
    activeEvent: IslandEvent? = null,
    projectedHistory: List<PrivacySafeNotificationPresentation> = emptyList(),
    selectedNotificationKey: String? = null,
    activeUndo: UndoNotificationState? = null,
    onSelectHistoricalNotification: (String) -> Unit = {},
    onDismissHistoryEntry: (String) -> Unit = {},
    onUndoDismiss: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onToggleExpand: () -> Unit,
    onExpandDashboard: () -> Unit = onToggleExpand,
    onExpandNotificationFocused: () -> Unit = onToggleExpand,
    onExpandMusic: () -> Unit = onToggleExpand,
    onExpandBattery: () -> Unit = onToggleExpand,
    onCollapse: () -> Unit = onToggleExpand,
    onTogglePulse: () -> Unit = {},
    onOpenContent: (String) -> Unit = {},
    onDismissEvent: (String) -> Unit = {},
    onExecuteAction: (String, Int) -> Unit = { _, _ -> },
    onSendReply: ((String, Int, String) -> ReplyResult)? = null,
    onInputFocusChanged: ((Boolean) -> Unit)? = null,
    onPlayPause: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    onSwipeActivity: (SwipeDirection) -> Unit = {},
    activitySwitcherInterface: IslandInterface = IslandInterface.DASHBOARD
) {
    val isExpanded = state.mode == IslandMode.EXPANDED
    val accentColor = Color(preferences.accentColorHex)
    val activeNotification = activeEvent as? IslandEvent.NotificationPosted
    val dashboardListState = rememberLazyListState()

    // Multi-download support with primary selection:
    // 1. Active notification if downloading/indeterminate
    // 2. Or most recently updated active download in projected history
    val primaryDownload = remember(activeNotification, projectedHistory) {
        if (activeNotification?.hasProgress == true && activeNotification.progressState != DownloadProgressState.NONE) {
            activeNotification
        } else {
            projectedHistory.firstOrNull {
                it.hasProgress && (it.progressState == DownloadProgressState.DOWNLOADING || it.progressState == DownloadProgressState.INDETERMINATE)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (!isExpanded) {
            val isBatteryActive = (activeEvent is IslandEvent.ChargingConnected ||
                    activeEvent is IslandEvent.ChargingDisconnected ||
                    (activeEvent is IslandEvent.BatteryStateChanged && activeEvent.isLowBattery) ||
                    (activeEvent is IslandEvent.BatteryWarning) ||
                    (activitySwitcherInterface == IslandInterface.BATTERY))

            if (activeNotification != null) {
                // COMPACT NOTIFICATION PREVIEW (Milestone R6 Phase 3)
                // 200dp symmetrical pill centered across camera punch-hole
                NotificationCompactView(
                    event = activeNotification,
                    onTap = {
                        Log.d("IslandTouch", "NotificationCompactView tapped -> expand notification-focused view (State B)")
                        onExpandNotificationFocused()
                    },
                    onSwipeDismiss = if (notificationPreferences.swipeToDismissEnabled) {
                        {
                            Log.d("IslandTouch", "NotificationCompactView swiped -> dismiss event ${activeNotification.id}")
                            onDismissEvent(activeNotification.id)
                        }
                    } else null
                )
            } else if (isBatteryActive || (batteryState.isCharging && batteryPreferences.chargingAnimationEnabled && (!currentTrack.isPlaying || activitySwitcherInterface == IslandInterface.BATTERY))) {
                // COMPACT BATTERY & CHARGING (Milestone R8)
                // 200dp symmetrical capsule with animated bolt, punch-hole clearance, percentage & source tag
                BatteryCompactView(
                    batteryState = batteryState,
                    preferences = batteryPreferences,
                    onTap = {
                        Log.d("IslandTouch", "BatteryCompactView tapped -> expand battery view (State D)")
                        onExpandBattery()
                    },
                    onSwipe = { direction ->
                        Log.d("IslandTouch", "BatteryCompactView swiped $direction")
                        onSwipeActivity(direction)
                    }
                )
            } else if (currentTrack.isPlaying || (!currentTrack.isHistorical && currentTrack != MediaTrackState.EMPTY)) {
                // COMPACT MUSIC PLAYER (Milestone R7)
                // 200dp symmetrical capsule with rotating artwork, 54dp punch-hole clearance, and live audio-reactive visualizer
                MusicCompactView(
                    trackState = currentTrack,
                    preferences = musicPreferences,
                    onTap = {
                        Log.d("IslandTouch", "MusicCompactView tapped -> expand music player (State C)")
                        onExpandMusic()
                    },
                    onSwipe = { direction ->
                        Log.d("IslandTouch", "MusicCompactView swiped $direction")
                        onSwipeActivity(direction)
                    }
                )
            } else {
                // COMPACT PILL MODE: Calibrated capsule dimensions and corner radius in DP
                // Symmetrically wraps the centered punch-hole camera without any rectangular box distortion
                val pillShape = RoundedCornerShape(preferences.cornerRadiusDp.dp)

                val downloadState = when (primaryDownload) {
                    is IslandEvent.NotificationPosted -> primaryDownload.progressState
                    is PrivacySafeNotificationPresentation -> primaryDownload.progressState
                    else -> DownloadProgressState.NONE
                }
                val downloadProg = when (primaryDownload) {
                    is IslandEvent.NotificationPosted -> primaryDownload.progress
                    is PrivacySafeNotificationPresentation -> primaryDownload.progress
                    else -> 0
                }
                val downloadMax = when (primaryDownload) {
                    is IslandEvent.NotificationPosted -> primaryDownload.maxProgress
                    is PrivacySafeNotificationPresentation -> primaryDownload.maxProgress
                    else -> 100
                }

                Box(
                    modifier = Modifier
                        .width(preferences.compactWidthDp.dp)
                        .height(preferences.compactHeightDp.dp)
                        .clip(pillShape)
                        .background(NothingBlack)
                        .islandProgressBorder(
                            shape = pillShape,
                            progressState = downloadState,
                            progress = downloadProg,
                            maxProgress = downloadMax,
                            accentColor = accentColor
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            Log.d("IslandTouch", "IslandOverlayView root idle Box clicked -> expand dashboard (State A)")
                            onExpandDashboard()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Clean idle state with outer border progress tracking if background download active
                }
            }
        } else {
            // EXPANDED PRESENTATION LAYER
            when (state.expandedViewType) {
                ExpandedViewType.NOTIFICATION_FOCUSED -> {
                    // =========================================================================
                    // STATE B: NOTIFICATION-FOCUSED VIEW
                    // Triggered by tapping an active notification or selecting from Dashboard history.
                    // STRICT INVARIANT: ZERO clock or weather displayed!
                    // =========================================================================
                    val presentationToRender = remember(selectedNotificationKey, activeNotification, projectedHistory) {
                        if (selectedNotificationKey != null) {
                            projectedHistory.firstOrNull { it.key == selectedNotificationKey }
                        } else if (activeNotification != null) {
                            projectedHistory.firstOrNull { it.key == activeNotification.id }
                        } else {
                            null
                        }
                    }

                    var totalNotifDrag by remember { mutableFloatStateOf(0f) }
                    val swipeThresholdPx = 60f

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (!notificationPreferences.swipeToDismissEnabled) {
                                    Modifier.pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (abs(totalNotifDrag) >= swipeThresholdPx) {
                                                    val dir = if (totalNotifDrag < 0) SwipeDirection.LEFT else SwipeDirection.RIGHT
                                                    onSwipeActivity(dir)
                                                }
                                                totalNotifDrag = 0f
                                            },
                                            onDragCancel = {
                                                totalNotifDrag = 0f
                                            },
                                            onHorizontalDrag = { _, dragAmount ->
                                                totalNotifDrag += dragAmount
                                            }
                                        )
                                    }
                                } else Modifier
                            )
                    ) {
                        if (presentationToRender != null) {
                            NotificationCard(
                                packageName = presentationToRender.packageName,
                                title = presentationToRender.title,
                                text = presentationToRender.body,
                                subText = presentationToRender.subText,
                                timestamp = presentationToRender.timestamp,
                                modifier = Modifier.fillMaxSize(),
                                previewMode = presentationToRender.previewMode,
                                accentColor = accentColor,
                                appName = presentationToRender.appDisplayName,
                                notificationKey = presentationToRender.key,
                                isSystemActive = presentationToRender.isSystemActive,
                                hasProgress = presentationToRender.hasProgress,
                                progress = presentationToRender.progress,
                                maxProgress = presentationToRender.maxProgress,
                                isIndeterminate = presentationToRender.isIndeterminate,
                                progressState = presentationToRender.progressState,
                                swipeToDismissEnabled = notificationPreferences.swipeToDismissEnabled,
                                onCollapse = {
                                    Log.d("IslandTouch", "NotificationCard COLLAPSE tapped")
                                    onCollapse()
                                },
                                onDismiss = { key ->
                                    Log.d("IslandTouch", "NotificationCard DISMISS tapped for key=$key")
                                    onDismissHistoryEntry(key)
                                },
                                onClearAll = if (projectedHistory.size > 1) onClearHistory else null,
                                onOpenContent = onOpenContent,
                                onExecuteAction = onExecuteAction,
                                onSendReply = onSendReply,
                                onInputFocusChanged = onInputFocusChanged
                            )
                        } else if (activeNotification != null) {
                            NotificationCard(
                                event = activeNotification,
                                previewMode = notificationPreferences.previewMode,
                                accentColor = accentColor,
                                notificationKey = activeNotification.id,
                                isSystemActive = true,
                                swipeToDismissEnabled = notificationPreferences.swipeToDismissEnabled,
                                onCollapse = {
                                    Log.d("IslandTouch", "NotificationCard COLLAPSE tapped")
                                    onCollapse()
                                },
                                onDismiss = { key ->
                                    Log.d("IslandTouch", "NotificationCard DISMISS tapped for key=$key")
                                    onDismissHistoryEntry(key)
                                },
                                onClearAll = if (projectedHistory.size > 1) onClearHistory else null,
                                onOpenContent = onOpenContent,
                                onExecuteAction = onExecuteAction,
                                onSendReply = onSendReply,
                                onInputFocusChanged = onInputFocusChanged,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Fallback when notification expired or unavailable
                            NotificationEmptyFocusedCard(
                                onCollapse = onCollapse,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                ExpandedViewType.DASHBOARD -> {
                    // =========================================================================
                    // STATE A: NORMAL DASHBOARD
                    // Triggered by tapping the idle compact island.
                    // Displays Time, Date, Compact Weather, and Recent Notification Previews.
                    // =========================================================================
                    DashboardCard(
                        projectedHistory = projectedHistory,
                        activeNotification = activeNotification,
                        activeUndo = activeUndo,
                        onSelectHistoricalNotification = onSelectHistoricalNotification,
                        onDismissHistoryEntry = onDismissHistoryEntry,
                        onUndoDismiss = onUndoDismiss,
                        onClearHistory = onClearHistory,
                        syncDismissWithAndroid = notificationPreferences.syncDismissWithAndroid,
                        onCollapse = onCollapse,
                        onSwipe = onSwipeActivity,
                        listState = dashboardListState,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ExpandedViewType.MUSIC -> {
                    // =========================================================================
                    // STATE C: MUSIC EXPANDED PLAYER (Milestone R7)
                    // Full interactive music card with artwork, visualizer, timeline & controls
                    // =========================================================================
                    MusicExpandedView(
                        trackState = currentTrack,
                        preferences = musicPreferences,
                        onPlayPause = onPlayPause,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        onSeekTo = onSeekTo,
                        onCollapse = onCollapse,
                        onSwipe = onSwipeActivity,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ExpandedViewType.BATTERY -> {
                    // =========================================================================
                    // STATE D: BATTERY & CHARGING EXPANDED (Milestone R8)
                    // Full interactive charging intelligence card with active visualizer & honest telemetry
                    // =========================================================================
                    BatteryExpandedView(
                        batteryState = batteryState,
                        preferences = batteryPreferences,
                        onCollapse = onCollapse,
                        onSwipe = onSwipeActivity,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * State A: Normal Dashboard Card.
 * Displays Time, Date, Compact Weather, and Recent Notification Previews.
 * Features a bounded, vertically scrollable LazyColumn allowing navigation through all history entries (up to 30),
 * with pinned header and clock/date/weather elements, smooth Nothing OS scrollbar, and stable key preservation.
 * Phase 3.4/3.5 adds horizontal swipe-to-dismiss, visible 'x' dismiss affordance, CLEAR ALL confirmation,
 * and compact Nothing OS-style Undo Snackbar with live countdown ticker.
 */
@Composable
fun DashboardCard(
    projectedHistory: List<PrivacySafeNotificationPresentation> = emptyList(),
    activeNotification: IslandEvent.NotificationPosted? = null,
    activeUndo: UndoNotificationState? = null,
    onSelectHistoricalNotification: (String) -> Unit = {},
    onDismissHistoryEntry: (String) -> Unit = {},
    onUndoDismiss: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    syncDismissWithAndroid: Boolean = false,
    onCollapse: () -> Unit,
    onSwipe: (SwipeDirection) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(26.dp)
    var isConfirmingClear by remember { mutableStateOf(false) }

    var totalHeaderDrag by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = 60f

    // Live clock ticker updating on exact seconds
    val currentTimeText by produceState(
        initialValue = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    ) {
        while (true) {
            val now = System.currentTimeMillis()
            value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
            val delayMs = 1000L - (now % 1000L)
            kotlinx.coroutines.delay(delayMs)
        }
    }

    val currentDateText = remember {
        SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date()).uppercase()
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Region with Activity Switcher swipe detection (Punch-hole clearance spacer + Clock/Date/Weather)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (abs(totalHeaderDrag) >= swipeThresholdPx) {
                                    val dir = if (totalHeaderDrag < 0) SwipeDirection.LEFT else SwipeDirection.RIGHT
                                    Log.d("IslandTouch", "DashboardCard header swiped $dir (drag: $totalHeaderDrag px)")
                                    onSwipe(dir)
                                }
                                totalHeaderDrag = 0f
                            },
                            onDragCancel = {
                                totalHeaderDrag = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                totalHeaderDrag += dragAmount
                            }
                        )
                    }
            ) {
                // 1. Top Punch-hole clearance spacer (48dp / 144px below status bar)
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                // 2. Header Row: Clock & Date (Left), Weather (Center), COLLAPSE (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Clock & Date
                    Column {
                        Text(
                            text = currentTimeText,
                            color = NothingWhite,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = currentDateText,
                            color = NothingMutedText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Neutral Weather Placeholder (Weather integration deferred to Phase 3.7)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingDarkGray)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "WEATHER UNAVAILABLE",
                            color = NothingMutedText,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Collapse Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingDarkGray)
                            .clickable {
                                Log.d("IslandTouch", "Dashboard COLLAPSE button clicked")
                                onCollapse()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
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
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Body: Recent Notifications Preview Section
            // Strictly bounded with weight(1f) to prevent content clipping or infinite-height conflicts
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingDarkGray)
                    .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 6.dp)
            ) {
                // Pinned Header row for Recent Alerts (always visible above the list)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RECENT ALERTS",
                        color = NothingMutedText,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    if (isConfirmingClear) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CLEAR ALL?",
                                color = NothingRed,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NothingBlack)
                                    .clickable { isConfirmingClear = false }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CANCEL",
                                    color = NothingWhite,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NothingRed)
                                    .clickable {
                                        isConfirmingClear = false
                                        onClearHistory()
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CONFIRM",
                                    color = NothingWhite,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (projectedHistory.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { isConfirmingClear = true }
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "CLEAR ALL",
                                        color = NothingRed,
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = if (projectedHistory.isEmpty()) "EMPTY" else "${projectedHistory.size}",
                                color = NothingMutedText,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                if (projectedHistory.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .simpleVerticalScrollbar(listState),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                items = projectedHistory,
                                key = { it.key }
                            ) { item ->
                                SwipeableHistoryItem(
                                    key = item.key,
                                    onDismiss = { onDismissHistoryEntry(item.key) }
                                ) {
                                    DashboardHistoryItemRow(
                                        item = item,
                                        onClick = { onSelectHistoricalNotification(item.key) },
                                        onDismiss = { onDismissHistoryEntry(item.key) }
                                    )
                                }
                            }
                        }
                    }
                } else if (activeNotification != null) {
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
                                packageName = activeNotification.packageName,
                                fallbackChar = activeNotification.title.firstOrNull()?.uppercaseChar() ?: 'N',
                                size = 20.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activeNotification.title,
                                color = NothingWhite,
                                fontSize = 12.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "NOW",
                            color = NothingMutedText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (activeNotification.text.isNotBlank()) {
                        Text(
                            text = activeNotification.text,
                            color = NothingWhite.copy(alpha = 0.85f),
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "No recent notifications",
                        color = NothingWhite.copy(alpha = 0.7f),
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Compact Nothing OS-style Undo Snackbar Banner (3.5-second recovery window)
                if (activeUndo != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    UndoSnackbarBanner(
                        activeUndo = activeUndo,
                        syncDismissWithAndroid = syncDismissWithAndroid,
                        onUndo = onUndoDismiss
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Footer: Minimalist Nothing OS brand status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOTHING ISLAND • DASHBOARD",
                    color = NothingMutedText,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Horizontal swipe-to-dismiss wrapper for history entries.
 * Horizontal swipes beyond ~80dp dismiss the notification with red background reveal.
 * Vertical drag gestures naturally flow to the parent LazyColumn for smooth scrolling.
 */
@Composable
fun SwipeableHistoryItem(
    key: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Red background revealed when swiping
        if (offsetX.value != 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(NothingRed.copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp),
                contentAlignment = if (offsetX.value > 0) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Text(
                    text = "DISMISS",
                    color = NothingWhite,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Draggable foreground item
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(key) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (abs(offsetX.value) >= dismissThresholdPx) {
                                coroutineScope.launch {
                                    val target = if (offsetX.value > 0) size.width.toFloat() else -size.width.toFloat()
                                    offsetX.animateTo(target)
                                    onDismiss()
                                }
                            } else {
                                coroutineScope.launch {
                                    offsetX.animateTo(0f, spring())
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, spring())
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}

/**
 * Compact Nothing OS-style Undo Snackbar banner displayed at the bottom of the Recent Alerts list.
 * Features a real-time countdown ticker for the 3.5-second recovery window and explicit note when
 * Android shade synchronization is active.
 */
@Composable
fun UndoSnackbarBanner(
    activeUndo: UndoNotificationState,
    syncDismissWithAndroid: Boolean,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingMs by produceState(
        initialValue = (activeUndo.expirationTimeMs - System.currentTimeMillis()).coerceAtLeast(0L),
        key1 = activeUndo
    ) {
        while (true) {
            val rem = (activeUndo.expirationTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)
            value = rem
            if (rem <= 0L) break
            kotlinx.coroutines.delay(100L)
        }
    }

    val remainingSecs = (remainingMs / 1000f).coerceAtLeast(0f)
    val titleText = if (activeUndo.isBatch) {
        "Cleared ${activeUndo.count} alerts"
    } else {
        "${activeUndo.title} dismissed"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(NothingRed)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = titleText,
                    color = NothingWhite,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = String.format(Locale.US, "%.1fs undo", remainingSecs) +
                            if (syncDismissWithAndroid) " (Shade un-dismiss unsupported)" else "",
                    color = NothingMutedText,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(NothingRed)
                .clickable { onUndo() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "UNDO",
                color = NothingWhite,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Single historical alert item row within the Dashboard LazyColumn.
 */
@Composable
fun DashboardHistoryItemRow(
    item: PrivacySafeNotificationPresentation,
    onClick: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val relativeTime = NotificationCardResolver.formatTimestamp(item.timestamp)
    val alpha = if (item.isSystemActive) 1.0f else 0.5f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NothingDarkGray)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            AppNotificationIcon(
                packageName = item.packageName,
                fallbackChar = item.title.firstOrNull()?.uppercaseChar() ?: 'N',
                size = 18.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = NothingWhite.copy(alpha = alpha),
                        fontSize = 12.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!item.isSystemActive) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(REMOVED)",
                            color = NothingMutedText,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                if (item.body.isNotBlank()) {
                    Text(
                        text = item.body,
                        color = NothingWhite.copy(alpha = 0.85f * alpha),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Download Progress Mini Bar
                if (item.hasProgress && item.progressState != DownloadProgressState.NONE) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF262626))
                        ) {
                            if (item.progressState == DownloadProgressState.DOWNLOADING && item.maxProgress > 0) {
                                val fraction = (item.progress.toFloat() / item.maxProgress.toFloat()).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxSize()
                                        .background(NothingRed)
                                )
                            } else if (item.progressState == DownloadProgressState.INDETERMINATE) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.4f)
                                        .fillMaxSize()
                                        .background(NothingRed)
                                )
                            } else if (item.progressState == DownloadProgressState.COMPLETED) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(NothingRed)
                                )
                            }
                        }

                        val pctText = if (item.progressState == DownloadProgressState.DOWNLOADING && item.maxProgress > 0) {
                            "${((item.progress.toFloat() / item.maxProgress.toFloat()) * 100).toInt()}%"
                        } else if (item.progressState == DownloadProgressState.INDETERMINATE) {
                            "..."
                        } else if (item.progressState == DownloadProgressState.PAUSED) {
                            "PAUSED"
                        } else if (item.progressState == DownloadProgressState.COMPLETED) {
                            "DONE"
                        } else ""

                        if (pctText.isNotEmpty()) {
                            Text(
                                text = pctText,
                                color = NothingMutedText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = relativeTime,
                color = NothingMutedText,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace
            )
            if (onDismiss != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E1E1E))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        color = NothingMutedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Compact, minimalist vertical scroll indicator for Nothing Island's LazyColumn.
 * Draws a subtle monospace-inspired scroll thumb when content exceeds the viewport.
 */
fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    width: Dp = 2.dp,
    color: Color = NothingMutedText.copy(alpha = 0.5f)
): Modifier = drawWithContent {
    drawContent()
    val totalItems = state.layoutInfo.totalItemsCount
    val visibleItems = state.layoutInfo.visibleItemsInfo.size
    if (totalItems > visibleItems && totalItems > 0) {
        val firstVisible = state.firstVisibleItemIndex
        val scrollbarHeight = (size.height * (visibleItems.toFloat() / totalItems)).coerceAtLeast(16f)
        val maxOffset = size.height - scrollbarHeight
        val maxScrollIndex = (totalItems - visibleItems).coerceAtLeast(1)
        val scrollbarY = maxOffset * (firstVisible.toFloat() / maxScrollIndex).coerceIn(0f, 1f)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - width.toPx(), scrollbarY),
            size = Size(width.toPx(), scrollbarHeight),
            cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
        )
    }
}

/**
 * State B Fallback Card when no notification is currently active.
 * STRICT INVARIANT: ZERO clock or weather displayed!
 */
@Composable
fun NotificationEmptyFocusedCard(
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(26.dp)
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
            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "NOTIFICATIONS",
                    color = NothingWhite,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
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
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingDarkGray)
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No active notifications",
                    color = NothingMutedText,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOTHING ISLAND • NOTIFICATION",
                    color = NothingMutedText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
