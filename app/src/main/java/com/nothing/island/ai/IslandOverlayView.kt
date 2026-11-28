package com.nothing.island.ai

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
import com.nothing.island.ai.ui.components.EarbudConnectionView
import com.nothing.island.ai.ui.components.EarbudDisconnectedView
import com.nothing.island.ai.ui.components.CallCompactView
import com.nothing.island.ai.ui.components.CallExpandedView
import com.nothing.island.ai.ui.components.BatteryExpandedView
import com.nothing.island.ai.ui.components.MusicCompactView
import com.nothing.island.ai.ui.components.MusicExpandedView
import com.nothing.island.ai.ui.components.NotificationCard
import com.nothing.island.ai.ui.components.NotificationCardResolver
import com.nothing.island.ai.ui.components.NotificationCompactView
import com.nothing.island.ai.ui.components.SystemAlertPill
import com.nothing.island.ai.gestures.GesturePreferences
import com.nothing.island.ai.gestures.IslandGestureActionExecutor
import com.nothing.island.ai.gestures.islandGestureDetector
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
 * Pure predicate deciding whether the idle pixel mascot (and 52.dp unclipped shoulder headroom)
 * should render on the compact default capsule.
 * Always true whenever mascotEnabled == true and no expanded card or active event pill is visible.
 */
internal fun shouldShowIdleMascot(
    isExpanded: Boolean,
    isDualPill: Boolean,
    isCallActive: Boolean,
    isEarbudEvent: Boolean,
    isSystemAlertActive: Boolean,
    hasActiveNotification: Boolean,
    isMusicPlaying: Boolean,
    isBatteryActive: Boolean,
    isChargingPillVisible: Boolean,
    hasDownloadProgress: Boolean,
    mascotEnabled: Boolean,
): Boolean {
    if (!mascotEnabled || isExpanded || isDualPill) return false
    return !(isCallActive ||
        isEarbudEvent ||
        isSystemAlertActive ||
        hasActiveNotification ||
        isMusicPlaying ||
        isBatteryActive ||
        isChargingPillVisible ||
        hasDownloadProgress)
}

/**
 * Main floating overlay view for Nothing Island.
 * Manages presentation switching between compact pill modes and the two distinct expanded states:
 * - State A: Normal Dashboard (Time, Date, Recent notification previews).
 * - State B: Notification-Focused View (Active notification card and actions).
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
    resumableMedia: MediaTrackState? = null,
    activeEvent: IslandEvent? = null,
    isDualPill: Boolean = false,
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
    onExpandCall: () -> Unit = onToggleExpand,
    onAnswerCall: () -> Unit = {},
    onDeclineCall: () -> Unit = {},
    onQuickTool: (String) -> Unit = {},
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
    onToggleRepeat: (Int) -> Unit = {},
    onOpenMediaApp: () -> Unit = {},
    onResumeLastMedia: () -> Unit = {},
    onQuickVolume: (Int) -> Unit = {},
    onQuickVolumePercent: (Float) -> Unit = {},
    onQuickBrightnessPercent: (Float) -> Unit = {},
    getQuickVolumePercent: () -> Float = { 0.5f },
    getQuickBrightnessPercent: () -> Float = { 0.5f },
    onQuickHistory: () -> Unit = {},
    onSwipeActivity: (SwipeDirection) -> Unit = {},
    activitySwitcherInterface: IslandInterface = IslandInterface.DASHBOARD,
    gesturePreferences: GesturePreferences = GesturePreferences(),
    stateManager: IslandStateManager = IslandStateManager.getInstance(),
    onScreenshotResult: ((com.nothing.island.ai.gestures.ScreenshotActionResult) -> Unit)? = null,
    onPillBoundsChanged: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onMascotBoundsChanged: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> }
) {
    val isEarbudEvent = activeEvent is IslandEvent.BluetoothDeviceConnected ||
            activeEvent is IslandEvent.BluetoothDeviceDisconnected
    val isExpanded = state.mode == IslandMode.EXPANDED && !isEarbudEvent
    val accentColor = Color(preferences.accentColorHex)
    val signatureTheme = remember(preferences.presetName) {
        com.nothing.island.ai.ui.theme.IslandSignatureTheme.resolve(preferences.presetName)
    }
    val activeNotification = activeEvent as? IslandEvent.NotificationPosted
    val dashboardListState = rememberLazyListState()

    val context = LocalContext.current
    val enabledQuickTools by remember {
        com.nothing.island.ai.data.IslandPreferencesRepository.getInstance(context).quickToolsFlow()
    }.collectAsState(initial = com.nothing.island.ai.ui.components.QuickTools.DEFAULT.toSet())
    val callPrefs by remember {
        com.nothing.island.ai.data.IslandPreferencesRepository.getInstance(context).callPreferencesFlow()
    }.collectAsState(initial = com.nothing.island.ai.telephony.CallPreferences.DEFAULTS)
    val isSpeakerOn by com.nothing.island.ai.telephony.CallController.isSpeakerOn.collectAsState()
    val isMuted by com.nothing.island.ai.telephony.CallController.isMuted.collectAsState()
    val isOnHold by com.nothing.island.ai.telephony.CallController.isOnHold.collectAsState()
    val isRecording by com.nothing.island.ai.telephony.CallController.isRecording.collectAsState()

    val activeCallIdentity = when (activeEvent) {
        is IslandEvent.IncomingCallRinging -> activeEvent.callId
        is IslandEvent.CallConnected -> activeEvent.callId
        else -> null
    }
    var callControlsExpanded by remember(activeCallIdentity) {
        mutableStateOf(activeEvent is IslandEvent.CallConnected)
    }
    var callNumpadExpanded by remember(activeCallIdentity) {
        mutableStateOf(false)
    }
    androidx.compose.runtime.LaunchedEffect(activeEvent) {
        if (activeEvent is IslandEvent.CallConnected && !callControlsExpanded) {
            callControlsExpanded = true
        } else if (activeEvent is IslandEvent.CallEnded || activeEvent !is IslandEvent.CallConnected && activeEvent !is IslandEvent.IncomingCallRinging) {
            callControlsExpanded = false
            callNumpadExpanded = false
        }
    }

    val onToggleSpeaker = {
        com.nothing.island.ai.telephony.CallController.toggleSpeaker(context)
        Unit
    }
    val onToggleMute = {
        com.nothing.island.ai.telephony.CallController.toggleMute(context)
        Unit
    }
    val onToggleHold = {
        com.nothing.island.ai.telephony.CallController.toggleHold(context)
        Unit
    }
    val onToggleRecord = {
        com.nothing.island.ai.telephony.CallController.toggleRecord(context)
        Unit
    }
    val onOpenDialerScreen = {
        com.nothing.island.ai.telephony.CallController.openInCallScreen(context)
        Unit
    }

    // Smart time-decayed tap priority routing
    val handleSmartCompactTap = remember(stateManager, activeEvent, currentTrack, onExpandCall, onExpandNotificationFocused, onExpandMusic, onExpandDashboard) {
        {
            val isCallActive = activeEvent is IslandEvent.IncomingCallRinging ||
                    activeEvent is IslandEvent.CallConnected ||
                    activeEvent is IslandEvent.CallEnded
            val destination = stateManager.resolveTapDestination(
                isCallActive = isCallActive,
                isMediaPlaying = currentTrack.isPlaying
            )
            when (destination) {
                ExpandedViewType.CALL -> onExpandCall()
                ExpandedViewType.NOTIFICATION_FOCUSED -> {
                    stateManager.onNotificationViewExpanded()
                    onExpandNotificationFocused()
                }
                ExpandedViewType.MUSIC -> onExpandMusic()
                else -> onExpandDashboard()
            }
        }
    }

    val handleCollapseWithAck = remember(state.expandedViewType, stateManager, onCollapse) {
        {
            if (state.expandedViewType == ExpandedViewType.NOTIFICATION_FOCUSED) {
                stateManager.onNotificationViewCollapsed()
            }
            onCollapse()
        }
    }

    val gestureExecutor = remember(context, handleSmartCompactTap, onClearHistory, onScreenshotResult) {
        IslandGestureActionExecutor(
            context = context,
            torchHandler = null,
            onToggleExpand = handleSmartCompactTap,
            onClearAllNotifications = onClearHistory,
            onScreenshotResult = onScreenshotResult
        )
    }

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

    val handleSwipeActivity: (SwipeDirection) -> Unit = remember(onSwipeActivity, handleCollapseWithAck) {
        { direction ->
            if (direction == SwipeDirection.UP) {
                handleCollapseWithAck()
            } else {
                onSwipeActivity(direction)
            }
        }
    }

    val isCallActive = activeEvent is IslandEvent.IncomingCallRinging ||
            activeEvent is IslandEvent.CallConnected ||
            activeEvent is IslandEvent.CallEnded

    val isBatteryActive = (batteryState.isCharging && activeEvent is IslandEvent.ChargingConnected) ||
            (activeEvent is IslandEvent.BatteryStateChanged && activeEvent.isLowBattery) ||
            (activeEvent is IslandEvent.BatteryWarning)

    val isSystemAlertActive = (activeEvent is IslandEvent.RingerModeChanged ||
            activeEvent is IslandEvent.TorchStateChanged ||
            activeEvent is IslandEvent.WifiStateChanged ||
            activeEvent is IslandEvent.DndStateChanged ||
            activeEvent is IslandEvent.BatteryThresholdWarning ||
            activeEvent is IslandEvent.DeviceUnlocked)

    // Unified Target Dimensions (simultaneously driving width, height, and corner radius)
    val targetWidth: Dp
    val targetHeight: Dp
    val targetCornerRadius: Dp

    if (isExpanded) {
        when (state.expandedViewType) {
            ExpandedViewType.CALL -> {
                val baseCutoutOffset = if (preferences.isCameraCutoutEnabled) 42 else 0
                val mascotRowBoost = if (preferences.mascotEnabled && activeEvent !is IslandEvent.CallEnded) 14 else 0
                val desiredCallHeight = when {
                    activeEvent is IslandEvent.CallEnded || !callControlsExpanded -> 78 + baseCutoutOffset + mascotRowBoost
                    callNumpadExpanded -> 388 + baseCutoutOffset + mascotRowBoost
                    else -> 150 + baseCutoutOffset + mascotRowBoost
                }
                targetWidth = 344.dp
                targetHeight = desiredCallHeight.dp
                targetCornerRadius = if (callControlsExpanded || callNumpadExpanded) 30.dp else if (preferences.isCameraCutoutEnabled) 36.dp else 39.dp
            }
            ExpandedViewType.QUICK_TOOLS -> {
                targetWidth = preferences.expandedWidthDp.dp
                targetHeight = 360.dp
                targetCornerRadius = 28.dp
            }
            ExpandedViewType.MUSIC -> {
                targetWidth = preferences.expandedWidthDp.dp
                targetHeight = (preferences.expandedHeightDp + 22f).dp
                targetCornerRadius = 28.dp
            }
            ExpandedViewType.NOTIFICATION_FOCUSED -> {
                targetWidth = preferences.expandedWidthDp.dp
                targetHeight = (preferences.expandedHeightDp + 24f).dp
                targetCornerRadius = 28.dp
            }
            else -> {
                targetWidth = preferences.expandedWidthDp.dp
                targetHeight = preferences.expandedHeightDp.dp
                targetCornerRadius = 28.dp
            }
        }
    } else {
        when {
            isDualPill -> {
                targetWidth = 220.dp
                targetHeight = preferences.compactHeightDp.dp
                targetCornerRadius = 18.dp
            }
            isCallActive -> {
                targetWidth = 200.dp
                targetHeight = preferences.compactHeightDp.dp
                targetCornerRadius = (preferences.compactHeightDp / 2f).dp
            }
            isEarbudEvent -> {
                targetWidth = 236.dp
                targetHeight = 38.dp
                targetCornerRadius = 19.dp
            }
            isSystemAlertActive -> {
                targetWidth = 220.dp
                targetHeight = 36.dp
                targetCornerRadius = 18.dp
            }
            currentTrack.isPlaying && !isBatteryActive && activeNotification == null -> {
                targetWidth = 224.dp
                targetHeight = preferences.compactHeightDp.dp
                targetCornerRadius = (preferences.compactHeightDp / 2f).dp
            }
            activeNotification != null || isBatteryActive -> {
                targetWidth = 200.dp
                targetHeight = preferences.compactHeightDp.dp
                targetCornerRadius = (preferences.compactHeightDp / 2f).dp
            }
            else -> {
                targetWidth = preferences.compactWidthDp.dp
                targetHeight = preferences.compactHeightDp.dp
                targetCornerRadius = preferences.cornerRadiusDp.dp
            }
        }
    }

    val safeWidth = if (targetWidth.value.isNaN() || targetWidth.value <= 0f) 124.dp else targetWidth
    val safeHeight = if (targetHeight.value.isNaN() || targetHeight.value <= 0f) 36.dp else targetHeight
    val safeCornerRadius = if (targetCornerRadius.value.isNaN() || targetCornerRadius.value <= 0f) 18.dp else targetCornerRadius

    val themeSpringSpec = spring<Dp>(
        dampingRatio = signatureTheme.springDampingRatio,
        stiffness = signatureTheme.springStiffness
    )

    val animatedWidth by animateDpAsState(
        targetValue = safeWidth,
        animationSpec = themeSpringSpec,
        label = "island_width"
    )
    val animatedHeight by animateDpAsState(
        targetValue = safeHeight,
        animationSpec = themeSpringSpec,
        label = "island_height"
    )
    val animatedCornerRadius by animateDpAsState(
        targetValue = safeCornerRadius,
        animationSpec = themeSpringSpec,
        label = "island_corner_radius"
    )

    var isPressed by remember { mutableStateOf(false) }
    val animatedTactileScale by animateFloatAsState(
        targetValue = if (isPressed && !preferences.reducedMotionEnabled) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = signatureTheme.springDampingRatio,
            stiffness = signatureTheme.springStiffness
        ),
        label = "island_tactile_scale"
    )

    // 1:1 rubber-banded drag tracking during gesture, seeded with pointer velocity on release.
    val gestureScope = rememberCoroutineScope()
    val dragTranslationX = remember { Animatable(0f) }
    val dragTranslationY = remember { Animatable(0f) }
    val handleCompactDragProgress: (Float, Float) -> Unit = remember(preferences.reducedMotionEnabled) {
        { dx, dy ->
            if (!preferences.reducedMotionEnabled) {
                val dampedX = (dx * 0.25f).coerceIn(-28f, 28f)
                val dampedY = (dy * 0.25f).coerceIn(-18f, 22f)
                gestureScope.launch {
                    dragTranslationX.snapTo(dampedX)
                    dragTranslationY.snapTo(dampedY)
                }
            }
        }
    }
    val handleCompactDragRelease: (Float, Float) -> Unit = remember {
        { vx, vy ->
            gestureScope.launch {
                dragTranslationX.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
                    initialVelocity = (vx * 0.22f).coerceIn(-1200f, 1200f)
                )
            }
            gestureScope.launch {
                dragTranslationY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
                    initialVelocity = (vy * 0.22f).coerceIn(-1200f, 1200f)
                )
            }
        }
    }

    val rawPillBgHex = preferences.pillBackgroundColorHex
    val capsuleBgColor = when (signatureTheme) {
        com.nothing.island.ai.ui.theme.IslandSignatureTheme.HIGH_CONTRAST_INVERTED -> Color(0xFFF2F2F2)
        com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLASSMORPHIC_FROSTED -> Color(0xD6222222)
        com.nothing.island.ai.ui.theme.IslandSignatureTheme.TAPE_DECK -> Color(0xFF1E1E1E)
        else -> if (rawPillBgHex == 0L || (rawPillBgHex and 0xFF000000L) == 0L) NothingBlack else Color(rawPillBgHex)
    }
    val capsuleBorderColor = when {
        !isExpanded && isEarbudEvent -> Color(0xFFD71921)
        signatureTheme.isGlassmorphic -> Color(0x45FFFFFF)
        signatureTheme.isInverted -> Color(0xFFD0D0D0)
        else -> signatureTheme.borderColor
    }

    val isChargingPillVisible = batteryState.isCharging && batteryPreferences.chargingAnimationEnabled && !currentTrack.isPlaying
    val isMascotStageActive = !isExpanded && !isDualPill && preferences.mascotEnabled

    androidx.compose.runtime.LaunchedEffect(isMascotStageActive) {
        if (!isMascotStageActive) {
            onMascotBoundsChanged(0, 0, 0, 0)
        }
    }

    val screenWidthPx = context.resources.displayMetrics.widthPixels.toFloat()
    val displayDensity = context.resources.displayMetrics.density
    var expandedCardLeftXPx by remember(screenWidthPx, animatedWidth) {
        mutableFloatStateOf((screenWidthPx - animatedWidth.value * displayDensity) / 2f)
    }
    val expandedNotchConfig = com.nothing.island.ai.ui.components.ExpandedNotchConfig(
        notchWidthDp = preferences.expandedNotchWidth,
        notchDepthDp = preferences.expandedNotchDepth,
        physicalCameraCenterXPx = geometry.centerXpx,
        expandedCardLeftXPx = expandedCardLeftXPx,
        expandedOffsetXDp = preferences.expandedOffsetX,
    )

    androidx.compose.runtime.CompositionLocalProvider(
        com.nothing.island.ai.ui.theme.LocalIslandTheme provides signatureTheme,
        com.nothing.island.ai.ui.components.LocalExpandedNotchConfig provides expandedNotchConfig,
    ) {
        val outerCapsuleShape = if (isExpanded && preferences.isCameraCutoutEnabled && state.expandedViewType != ExpandedViewType.CALL) {
            com.nothing.island.ai.ui.components.notchedCardShape(
                notchWidthDp = preferences.expandedNotchWidth,
                notchDepthDp = preferences.expandedNotchDepth,
                cornerRadiusDp = animatedCornerRadius.value,
                physicalCameraCenterXPx = geometry.centerXpx,
                expandedCardLeftXPx = expandedCardLeftXPx,
                expandedOffsetXDp = preferences.expandedOffsetX,
            )
        } else {
            RoundedCornerShape(animatedCornerRadius)
        }

        val chibiState = com.nothing.island.ai.ui.components.resolveChibiState(
            isCallActive = isCallActive,
            isMusicPlaying = currentTrack.isPlaying && !isBatteryActive,
            hasActiveNotification = activeNotification != null,
            isCharging = isBatteryActive || isChargingPillVisible,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isExpanded) {
                        Modifier
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    handleCollapseWithAck()
                                }
                            }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            // Tight stage sizing matching WindowManager.LayoutParams bounds (no 280x92 transparent touch shield)
            val compactSpec = com.nothing.island.ai.ui.components.computeCompactStageWindowSpec(
                compactWidthDp = preferences.compactWidthDp,
                compactHeightDp = preferences.compactHeightDp,
                mascotEnabled = preferences.mascotEnabled,
                isDualPill = isDualPill,
                isEarbudActive = isEarbudEvent,
                isSystemAlertActive = isSystemAlertActive,
                isCallActive = isCallActive,
                hasActiveNotification = activeNotification != null,
                isMusicPlaying = currentTrack.isPlaying,
                isChargingActive = isBatteryActive || isChargingPillVisible,
            )
            val stageWidthDp = if (!isExpanded) maxOf(compactSpec.widthDp, animatedWidth.value).dp else animatedWidth
            val stageHeightDp = if (!isExpanded) maxOf(compactSpec.heightDp, animatedHeight.value).dp else animatedHeight

            Box(
                modifier = if (isMascotStageActive) {
                    Modifier
                        .size(width = stageWidthDp, height = stageHeightDp)
                        .background(Color.Transparent)
                } else {
                    Modifier
                },
                contentAlignment = Alignment.TopCenter
            ) {
                if (isMascotStageActive && !isEarbudEvent && !isSystemAlertActive) {
                    com.nothing.island.ai.ui.components.ChibiStageCompanion(
                        state = chibiState,
                        variant = com.nothing.island.ai.ui.components.resolveChibiVariant(preferences.mascotSpriteName),
                        pillWidthDp = animatedWidth.value,
                        pillHeightDp = animatedHeight.value,
                        pillTopDp = 0f,
                        batteryPercent = batteryState.percentage,
                        isCallRinging = activeEvent is IslandEvent.IncomingCallRinging,
                        notificationKey = activeNotification?.id,
                        reducedMotionEnabled = preferences.reducedMotionEnabled,
                        onMascotBoundsChanged = onMascotBoundsChanged,
                    )
                }

            Box(
                modifier = Modifier
                    .zIndex(com.nothing.island.ai.ui.components.ChibiStageMetrics.CAPSULE_Z_INDEX)
                    .onGloballyPositioned { coordinates ->
                        val pos = coordinates.positionInRoot()
                        val size = coordinates.size
                        expandedCardLeftXPx = pos.x
                        onPillBoundsChanged(
                            pos.x.toInt(),
                            pos.y.toInt(),
                            (pos.x + size.width).toInt(),
                            (pos.y + size.height).toInt()
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            // Consumes touches inside pill bounds so they never bleed through to background apps/widgets
                        }
                    }
                    .graphicsLayer {
                        scaleX = animatedTactileScale
                        scaleY = animatedTactileScale
                        translationX = dragTranslationX.value
                        translationY = dragTranslationY.value
                    }
                    .size(width = animatedWidth, height = animatedHeight)
                    .clip(outerCapsuleShape)
                    .background(capsuleBgColor)
                    .border(1.dp, capsuleBorderColor, outerCapsuleShape),
                contentAlignment = Alignment.TopCenter
            ) {
                // Signature Theme Compact Decorations
                if (!isExpanded) {
                    when (signatureTheme) {
                        com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC -> {
                            com.nothing.island.ai.ui.theme.GlyphArcCompactDecorations(arcColor = Color.White)
                        }
                        com.nothing.island.ai.ui.theme.IslandSignatureTheme.TAPE_DECK -> {
                            val reelStartPadding = if (currentTrack.isPlaying) 34.dp else 8.dp
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = reelStartPadding, end = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.nothing.island.ai.ui.theme.TapeDeckReelsIndicator(
                                    isPlaying = currentTrack.isPlaying,
                                    sizeDp = 10.dp
                                )
                                com.nothing.island.ai.ui.theme.TapeDeckWaveform()
                            }
                        }
                        com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLASSMORPHIC_FROSTED -> {
                            com.nothing.island.ai.ui.theme.SpecularGlassShimmer(
                                cornerRadiusDp = animatedCornerRadius
                            )
                        }
                        else -> Unit
                    }
                }

                AnimatedContent(
                    targetState = if (isExpanded) state.expandedViewType else null,
                    transitionSpec = {
                        if (preferences.reducedMotionEnabled) {
                            fadeIn(animationSpec = tween(100)) togetherWith fadeOut(animationSpec = tween(80))
                        } else {
                            (fadeIn(animationSpec = tween(160)) + scaleIn(
                                initialScale = 0.93f,
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f),
                                animationSpec = spring(
                                    dampingRatio = 0.82f,
                                    stiffness = 480f
                                )
                            )).togetherWith(
                                fadeOut(animationSpec = tween(100)) + scaleOut(
                                    targetScale = 0.95f,
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f),
                                    animationSpec = tween(100)
                                )
                            )
                        }
                    },
                    label = "islandContentTransition"
                ) { currentExpandedType ->
                    if (currentExpandedType == null) {
                        if (isDualPill) {
                            com.nothing.island.ai.ui.components.DualPillContainer(
                                leftLabel = "MUSIC",
                                leftGlyph = "♪",
                                rightLabel = "TIMER",
                                rightGlyph = "◷",
                                accentColor = accentColor,
                                onLeftTap = onExpandMusic,
                                onRightTap = handleCollapseWithAck,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                        // Battery pill is gated on the LIVE charging state, so a stale
                        // ChargingConnected event can never keep it on after unplugging.
                        val isBatteryActive = (batteryState.isCharging && activeEvent is IslandEvent.ChargingConnected) ||
                                (activeEvent is IslandEvent.BatteryStateChanged && activeEvent.isLowBattery) ||
                                (activeEvent is IslandEvent.BatteryWarning)

                        val isSystemAlertActive = (activeEvent is IslandEvent.RingerModeChanged ||
                                activeEvent is IslandEvent.TorchStateChanged ||
                                activeEvent is IslandEvent.WifiStateChanged ||
                                activeEvent is IslandEvent.DndStateChanged ||
                                activeEvent is IslandEvent.BatteryThresholdWarning ||
                                activeEvent is IslandEvent.DeviceUnlocked)

                        val isCallActive = activeEvent is IslandEvent.IncomingCallRinging ||
                                activeEvent is IslandEvent.CallConnected ||
                                activeEvent is IslandEvent.CallEnded

                        val isEarbudEvent = activeEvent is IslandEvent.BluetoothDeviceConnected ||
                                activeEvent is IslandEvent.BluetoothDeviceDisconnected

                        val mascotIdleReactionState = com.nothing.island.ai.ui.components.resolveMascotReactionState(
                            isCallOrAlertActive = false,
                            isMusicPlaying = false,
                            isCharging = batteryState.isCharging
                        )

                        if (isCallActive) {
                            // R9: telephony capsule with breathing red accent
                            CallCompactView(
                                event = activeEvent!!,
                                onTap = onExpandCall,
                                onEndCall = onDeclineCall,
                                showEndButton = callPrefs.doubleIncomingCallActions,
                                isSpeakerOn = isSpeakerOn,
                                onToggleSpeaker = onToggleSpeaker,
                            )
                        } else if (isEarbudEvent) {
                            // Compact centered Bluetooth headphones pill; never toggles an expanded blank container
                            when (val e = activeEvent) {
                                is IslandEvent.BluetoothDeviceConnected -> EarbudConnectionView(
                                    event = e,
                                    onTap = handleCollapseWithAck,
                                )
                                is IslandEvent.BluetoothDeviceDisconnected -> EarbudDisconnectedView(
                                    event = e,
                                    onTap = handleCollapseWithAck,
                                )
                                else -> Unit
                            }
                        } else if (isSystemAlertActive) {
                            // SYSTEM ALERT PILL (REQ-36, REQ-37, REQ-60, REQ-61, REQ-62, REQ-64, REQ-75)
                            SystemAlertPill(
                                event = activeEvent!!,
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(36.dp)
                                    .clickable { onToggleExpand() }
                            )
                        } else if (activeNotification != null) {
                            // COMPACT NOTIFICATION PREVIEW (Milestone R6 Phase 3)
                            // 200dp symmetrical pill centered across camera punch-hole
                            NotificationCompactView(
                                event = activeNotification,
                                onTap = {
                                    Log.d("IslandTouch", "NotificationCompactView tapped -> expand notification-focused view (State B)")
                                    stateManager.onNotificationViewExpanded()
                                    onExpandNotificationFocused()
                                },
                                onSwipeDismiss = if (notificationPreferences.swipeToDismissEnabled) {
                                    {
                                        Log.d("IslandTouch", "NotificationCompactView swiped -> dismiss event ${activeNotification.id}")
                                        stateManager.acknowledgeLatestNotification()
                                        onDismissEvent(activeNotification.id)
                                    }
                                } else null
                            )
                        } else if (currentTrack.isPlaying && !isBatteryActive) {
                            // COMPACT MUSIC PLAYER takes priority over idle battery when music plays.
                            // Swipe-left shifts the whole window beside the camera (see controller).
                            MusicCompactView(
                                trackState = currentTrack,
                                preferences = musicPreferences,
                                onTap = {
                                    Log.d("IslandTouch", "MusicCompactView tapped -> handleSmartCompactTap")
                                    handleSmartCompactTap()
                                },
                                onSwipe = { direction ->
                                    Log.d("IslandTouch", "MusicCompactView swiped $direction")
                                    onSwipeActivity(direction)
                                },
                                modifier = Modifier.islandGestureDetector(
                                    preferences = gesturePreferences,
                                    executor = gestureExecutor,
                                    isMediaIslandActive = true,
                                    onTap = handleSmartCompactTap,
                                    onSwipeActivity = onSwipeActivity,
                                    onPressChange = { isPressed = it },
                                    onDragProgress = handleCompactDragProgress,
                                    onDragRelease = handleCompactDragRelease
                                )
                            )
                        } else if (isBatteryActive || (batteryState.isCharging && batteryPreferences.chargingAnimationEnabled && !currentTrack.isPlaying)) {
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
                                },
                                modifier = Modifier.islandGestureDetector(
                                    preferences = gesturePreferences,
                                    executor = gestureExecutor,
                                    isMediaIslandActive = false,
                                    onTap = onExpandBattery,
                                    onSwipeActivity = onSwipeActivity,
                                    onPressChange = { isPressed = it },
                                    onDragProgress = handleCompactDragProgress,
                                    onDragRelease = handleCompactDragRelease
                                )
                            )
                        } else {
                            // COMPACT PILL MODE: Calibrated capsule dimensions and corner radius in DP
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

                            val rawPillBgHex = preferences.pillBackgroundColorHex
                            val pillBgColor = when (signatureTheme) {
                                com.nothing.island.ai.ui.theme.IslandSignatureTheme.HIGH_CONTRAST_INVERTED -> Color(0xFFF2F2F2)
                                com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLASSMORPHIC_FROSTED -> Color(0xD1222222)
                                com.nothing.island.ai.ui.theme.IslandSignatureTheme.TAPE_DECK -> Color(0xFF1E1E1E)
                                else -> if (rawPillBgHex == 0L || (rawPillBgHex and 0xFF000000L) == 0L) NothingBlack else Color(rawPillBgHex)
                            }

                            val haptics = LocalHapticFeedback.current
                            val resumeSwipeThresholdPx = with(LocalDensity.current) { 28.dp.toPx() }
                            val canSwipeToResume = !isExpanded &&
                                activeEvent == null &&
                                !currentTrack.isPlaying &&
                                resumableMedia != null &&
                                resumableMedia.packageName.isNotBlank() &&
                                resumableMedia.trackTitle.isNotBlank()

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .islandProgressBorder(
                                        shape = pillShape,
                                        baseBorderColor = capsuleBorderColor,
                                        progressState = downloadState,
                                        progress = downloadProg,
                                        maxProgress = downloadMax,
                                        accentColor = accentColor
                                    )
                                    .then(
                                        if (canSwipeToResume) {
                                            Modifier.pointerInput(canSwipeToResume, resumeSwipeThresholdPx) {
                                                var dragX = 0f
                                                var resumed = false
                                                detectHorizontalDragGestures(
                                                    onDragStart = {
                                                        dragX = 0f
                                                        resumed = false
                                                    },
                                                    onHorizontalDrag = { change, dragAmount ->
                                                        dragX += dragAmount
                                                        if (!resumed && shouldTriggerIdleSwipeResume(
                                                                hasResumableMedia = true,
                                                                isMusicPlaying = false,
                                                                isExpanded = false,
                                                                hasActivePriorityEvent = false,
                                                                dragDistancePx = dragX,
                                                                thresholdPx = resumeSwipeThresholdPx,
                                                            )
                                                        ) {
                                                            change.consume()
                                                            resumed = true
                                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            onResumeLastMedia()
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        dragX = 0f
                                                        resumed = false
                                                    },
                                                    onDragCancel = {
                                                        dragX = 0f
                                                        resumed = false
                                                    }
                                                )
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .islandGestureDetector(
                                        preferences = gesturePreferences,
                                        executor = gestureExecutor,
                                        isMediaIslandActive = false,
                                        onTap = handleSmartCompactTap,
                                        onSwipeActivity = { direction ->
                                            if (canSwipeToResume &&
                                                (direction == SwipeDirection.LEFT || direction == SwipeDirection.RIGHT)
                                            ) {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onResumeLastMedia()
                                            } else {
                                                onSwipeActivity(direction)
                                            }
                                        },
                                        onPressChange = { isPressed = it },
                                        onDragProgress = handleCompactDragProgress,
                                        onDragRelease = handleCompactDragRelease
                                    )
                            )
                        }
                    } // end Box
                } // end else (non-dual compact)
            } else {
                    // EXPANDED PRESENTATION LAYER
                    when (currentExpandedType) {
                        ExpandedViewType.NOTIFICATION_FOCUSED -> {
                    // =========================================================================
                    // STATE B: NOTIFICATION-FOCUSED VIEW
                    // Non-destructive horizontal swipe pagination across active/recent notifications.
                    // STRICT INVARIANT: ZERO clock or weather displayed!
                    // =========================================================================
                    val totalPages = projectedHistory.size
                    var notificationPageIndex by remember(projectedHistory.size) { mutableIntStateOf(0) }
                    val safeIndex = if (totalPages > 0) notificationPageIndex.coerceIn(0, totalPages - 1) else 0

                    val presentationToRender = if (projectedHistory.isNotEmpty()) {
                        projectedHistory[safeIndex]
                    } else null

                    val pageIndicatorText = if (totalPages > 1) "${safeIndex + 1}/$totalPages" else null

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Reference-style slide-down: the card drops out of the
                        // capsule with a spring, and fades/slides in. Skipped when
                        // reduced motion is enabled.
                        val reveal by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = if (preferences.reducedMotionEnabled) {
                                tween(1)
                            } else {
                                spring(
                                    dampingRatio = 0.85f,
                                    stiffness = 480f,
                                )
                            },
                            label = "notifSlideDown",
                        )
                        val slideOffsetDp = (1f - reveal) * -24f
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset { IntOffset(0, slideOffsetDp.dp.roundToPx()) }
                                .alpha(reveal)
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
                                    handleCollapseWithAck()
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
                                isCameraCutoutEnabled = preferences.isCameraCutoutEnabled,
                                pageIndicator = pageIndicatorText,
                                onPageNext = {
                                    if (totalPages > 1) {
                                        notificationPageIndex = (safeIndex + 1) % totalPages
                                    }
                                },
                                onPagePrevious = {
                                    if (totalPages > 1) {
                                        notificationPageIndex = (safeIndex - 1 + totalPages) % totalPages
                                    }
                                }
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
                                    handleCollapseWithAck()
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
                                isCameraCutoutEnabled = preferences.isCameraCutoutEnabled,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            NotificationEmptyFocusedCard(
                                onCollapse = handleCollapseWithAck,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
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
                        onCollapse = handleCollapseWithAck,
                        onSwipe = onSwipeActivity,
                        listState = dashboardListState,
                        isCameraCutoutEnabled = preferences.isCameraCutoutEnabled,
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
                        onCollapse = handleCollapseWithAck,
                        onSwipe = onSwipeActivity,
                        onToggleRepeat = onToggleRepeat,
                        onOpenMediaApp = onOpenMediaApp,
                        isCameraCutoutEnabled = preferences.isCameraCutoutEnabled,
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
                        onCollapse = handleCollapseWithAck,
                        onSwipe = onSwipeActivity,
                        isCameraCutoutEnabled = preferences.isCameraCutoutEnabled,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ExpandedViewType.CALL -> {
                    // =========================================================================
                    // STATE E: CALL CARD (Milestone R9)
                    // Caller identity, video badge, duration timer and real dialer actions
                    // =========================================================================
                    val callEvent = activeEvent
                    if (callEvent is IslandEvent.IncomingCallRinging ||
                        callEvent is IslandEvent.CallConnected ||
                        callEvent is IslandEvent.CallEnded
                    ) {
                        val elapsed = if (callEvent is IslandEvent.CallConnected) callEvent.durationSeconds else 0L
                        CallExpandedView(
                            event = callEvent,
                            elapsedSeconds = elapsed,
                            onAnswer = onAnswerCall,
                            onDecline = onDeclineCall,
                            onToggleSpeaker = onToggleSpeaker,
                            isSpeakerOn = isSpeakerOn,
                            isCameraCutoutEnabled = preferences.isCameraCutoutEnabled,
                            isMuted = isMuted,
                            onToggleMute = onToggleMute,
                            isOnHold = isOnHold,
                            onToggleHold = onToggleHold,
                            isRecording = isRecording,
                            onToggleRecord = onToggleRecord,
                            onOpenDialerScreen = onOpenDialerScreen,
                            controlsExpanded = callControlsExpanded,
                            onToggleControlsExpanded = {
                                val next = !callControlsExpanded
                                callControlsExpanded = next
                                if (!next) callNumpadExpanded = false
                            },
                            numpadExpanded = callNumpadExpanded,
                            onToggleNumpadExpanded = {
                                if (!callControlsExpanded) callControlsExpanded = true
                                callNumpadExpanded = !callNumpadExpanded
                            },
                            showMascot = preferences.mascotEnabled,
                            mascotSpriteName = preferences.mascotSpriteName,
                            reducedMotionEnabled = preferences.reducedMotionEnabled,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                ExpandedViewType.QUICK_TOOLS -> {
                    val qt = com.nothing.island.ai.ui.components.QuickTools
                    var vol by remember { mutableFloatStateOf(getQuickVolumePercent()) }
                    var bri by remember { mutableFloatStateOf(getQuickBrightnessPercent()) }
                    val overlayScope = rememberCoroutineScope()
                    val prefsRepo = remember(context) { com.nothing.island.ai.data.IslandPreferencesRepository.getInstance(context) }

                    // Live ContentObserver for system brightness changes (e.g. from native Quick Settings slider)
                    DisposableEffect(context) {
                        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                            override fun onChange(selfChange: Boolean) {
                                super.onChange(selfChange)
                                bri = getQuickBrightnessPercent()
                            }
                        }
                        try {
                            context.contentResolver.registerContentObserver(
                                android.provider.Settings.System.getUriFor(android.provider.Settings.System.SCREEN_BRIGHTNESS),
                                false,
                                observer
                            )
                        } catch (e: Exception) {
                            Log.w("IslandOverlayView", "Failed to register brightness observer", e)
                        }
                        onDispose {
                            try {
                                context.contentResolver.unregisterContentObserver(observer)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }

                    com.nothing.island.ai.ui.components.QuickToolsDrawer(
                        tools = qt.ALL.filter { id -> id in enabledQuickTools }.map { id ->
                            val spec = com.nothing.island.ai.ui.components.QuickToolExecutor.describeTool(id)
                            com.nothing.island.ai.ui.components.QuickTool(
                                id = id,
                                label = spec.label,
                                glyph = qt.glyph(id),
                                actionType = spec.actionType,
                                contentDescription = spec.contentDescription,
                                fallbackTag = spec.fallbackTag,
                                action = { onQuickTool(id) },
                            )
                        },
                        onCollapse = handleCollapseWithAck,
                        volumePercent = vol,
                        onVolumeChange = { newVol ->
                            vol = newVol
                            onQuickVolumePercent(newVol)
                        },
                        brightnessPercent = bri,
                        onBrightnessChange = { newBri ->
                            bri = newBri
                            onQuickBrightnessPercent(newBri)
                        },
                        allAvailableToolIds = qt.ALL,
                        enabledToolIds = enabledQuickTools,
                        onToggleToolEnabled = { toolId ->
                            val next = if (toolId in enabledQuickTools) enabledQuickTools - toolId else enabledQuickTools + toolId
                            overlayScope.launch {
                                prefsRepo.updateQuickTools(next)
                            }
                        },
                        accentColor = accentColor,
                        isCameraCutoutEnabled = preferences.isCameraCutoutEnabled,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
}
}
}
}

/**
 * State A: Normal Dashboard Card.
 * Displays Time, Date, and Recent Notification Previews.
 * Features a bounded, vertically scrollable LazyColumn allowing navigation through all history entries (up to 30),
 * with pinned header and clock/date elements, smooth Nothing OS scrollbar, and stable key preservation.
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
    modifier: Modifier = Modifier,
    isCameraCutoutEnabled: Boolean = false
) {
    val cardShape = if (isCameraCutoutEnabled) com.nothing.island.ai.ui.components.currentNotchedCardShape() else RoundedCornerShape(26.dp)
    var isConfirmingClear by remember { mutableStateOf(false) }

    var totalHeaderDrag by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = 60f

    // Live clock ticker updating on exact seconds (12-hour format with AM/PM)
    val currentTimeText by produceState(
        initialValue = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    ) {
        while (true) {
            val now = System.currentTimeMillis()
            value = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now))
            val delayMs = 1000L - (now % 1000L)
            kotlinx.coroutines.delay(delayMs)
        }
    }

    val currentDateText = remember {
        SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date()).uppercase()
    }

    val theme = com.nothing.island.ai.ui.theme.LocalIslandTheme.current
    val cardBg = theme.pillBackgroundColor
    val cardBorder = theme.borderColor

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(cardBg)
            .border(width = 1.dp, color = cardBorder, shape = cardShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Region with Activity Switcher swipe detection (Punch-hole clearance spacer + Clock/Date/Weather)
            var totalHeaderDragX by remember { mutableFloatStateOf(0f) }
            var totalHeaderDragY by remember { mutableFloatStateOf(0f) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                totalHeaderDragX = 0f
                                totalHeaderDragY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalHeaderDragX += dragAmount.x
                                totalHeaderDragY += dragAmount.y
                            },
                            onDragEnd = {
                                val absX = abs(totalHeaderDragX)
                                val absY = abs(totalHeaderDragY)
                                if (absY > absX && totalHeaderDragY >= swipeThresholdPx) {
                                    Log.d("IslandTouch", "DashboardCard header swiped DOWN -> Quick Tools")
                                    onSwipe(SwipeDirection.DOWN)
                                } else if (absY > absX && totalHeaderDragY <= -swipeThresholdPx) {
                                    Log.d("IslandTouch", "DashboardCard header swiped UP -> Collapse")
                                    onCollapse()
                                } else if (absX > absY && absX >= swipeThresholdPx) {
                                    val dir = if (totalHeaderDragX < 0) SwipeDirection.LEFT else SwipeDirection.RIGHT
                                    Log.d("IslandTouch", "DashboardCard header swiped $dir (drag: $totalHeaderDragX px)")
                                    onSwipe(dir)
                                }
                                totalHeaderDragX = 0f
                                totalHeaderDragY = 0f
                            },
                            onDragCancel = {
                                totalHeaderDragX = 0f
                                totalHeaderDragY = 0f
                            }
                        )
                    }
            ) {
                // 1. Top Punch-hole clearance spacer (36dp camera punch hole + 6dp safety margin)
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                )

                // 2. Header Row: Clock & Date (Left), COLLAPSE (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Clock & Date
                    Column {
                        Text(
                            text = currentTimeText,
                            color = theme.textColorPrimary,
                            fontSize = 22.sp,
                            fontFamily = theme.displayFont,
                            fontWeight = theme.displayFontWeight,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = currentDateText,
                            color = theme.textColorSecondary,
                            fontSize = 10.sp,
                            fontFamily = theme.bodyFont,
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
            val recentAlertsBg = when {
                theme.isInverted -> Color(0xFFE4E4E4)
                theme.isGlassmorphic -> Color(0xB3333333)
                else -> NothingDarkGray
            }
            val recentAlertsBorder = when {
                theme.isInverted -> Color(0xFFCCCCCC)
                theme.isGlassmorphic -> Color(0x28FFFFFF)
                else -> Color.Transparent
            }
            val alertsHeaderColor = when {
                theme.isInverted -> Color(0xFF000000)
                else -> NothingMutedText
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(recentAlertsBg)
                    .then(if (recentAlertsBorder != Color.Transparent) Modifier.border(1.dp, recentAlertsBorder, RoundedCornerShape(12.dp)) else Modifier)
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
                        color = alertsHeaderColor,
                        fontSize = 10.5.sp,
                        fontFamily = theme.bodyFont,
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
                                .simpleVerticalScrollbar(listState)
                                .padding(end = 6.dp),
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
                                fallbackChar = com.nothing.island.ai.ui.components.TextBreakUtils.extractSafeFallbackChar(
                                    activeNotification.title,
                                    'N'
                                ),
                                size = 20.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activeNotification.title,
                                color = NothingWhite,
                                fontSize = 12.5.sp,
                                lineHeight = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
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
                    text = "DOT ISLAND • DASHBOARD",
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

        // Draggable foreground item: 1:1 finger tracking + velocity-seeded interruptible spring release
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(key) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)
                        var initialDelta = 0f
                        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                            change.consume()
                            initialDelta = over
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                        }
                        if (drag == null) {
                            if (offsetX.value != 0f) {
                                coroutineScope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f)
                                    )
                                }
                            }
                            return@awaitEachGesture
                        }
                        if (initialDelta != 0f) {
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + initialDelta)
                            }
                        }
                        val completed = horizontalDrag(drag.id) { change ->
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val dx = change.positionChange().x
                            change.consume()
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dx)
                            }
                        }
                        val vx = velocityTracker.calculateVelocity().x
                        val shouldDismiss = completed && (
                            abs(offsetX.value) >= dismissThresholdPx ||
                                (abs(vx) >= 900f && abs(offsetX.value) >= dismissThresholdPx * 0.35f)
                            )
                        if (shouldDismiss) {
                            coroutineScope.launch {
                                val target = if (vx != 0f && abs(offsetX.value) < dismissThresholdPx) {
                                    if (vx > 0f) size.width.toFloat() else -size.width.toFloat()
                                } else {
                                    if (offsetX.value >= 0f) size.width.toFloat() else -size.width.toFloat()
                                }
                                offsetX.animateTo(
                                    targetValue = target,
                                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
                                    initialVelocity = vx
                                )
                                onDismiss()
                            }
                        } else {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
                                    initialVelocity = vx
                                )
                            }
                        }
                    }
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
            modifier = Modifier.weight(1f)
        ) {
            AppNotificationIcon(
                packageName = item.packageName,
                fallbackChar = com.nothing.island.ai.ui.components.TextBreakUtils.extractSafeFallbackChar(
                    item.title,
                    'N'
                ),
                size = 18.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = NothingWhite.copy(alpha = alpha),
                        fontSize = 12.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 17.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!item.isSystemActive) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(REMOVED)",
                            color = NothingMutedText,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                if (item.body.isNotBlank()) {
                    Text(
                        text = item.body,
                        color = NothingWhite.copy(alpha = 0.85f * alpha),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.wrapContentWidth()
        ) {
            Text(
                text = relativeTime,
                color = NothingMutedText,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false
            )
            if (onDismiss != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
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
    var dragX by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(NothingBlack)
            .border(width = 1.dp, color = NothingBorder, shape = cardShape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onCollapse() })
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragX += dragAmount
                    },
                    onDragEnd = {
                        if (abs(dragX) > 40f) {
                            onCollapse()
                        }
                        dragX = 0f
                    },
                    onDragCancel = { dragX = 0f }
                )
            }
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
                    text = "DOT ISLAND • NOTIFICATION",
                    color = NothingMutedText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Resolves the notification card to display in [ExpandedViewType.NOTIFICATION_FOCUSED]:
 * prefers an explicitly selected historical entry, then the active pill notification,
 * and falls back to the most recent alert in [projectedHistory] so tapping the pill
 * after a banner's 4.5s auto-collapse still surfaces recent alerts instead of an empty state.
 */
internal fun resolveFocusedNotificationPresentation(
    selectedNotificationKey: String?,
    activeNotificationId: String?,
    projectedHistory: List<com.nothing.island.ai.notifications.PrivacySafeNotificationPresentation>
): com.nothing.island.ai.notifications.PrivacySafeNotificationPresentation? {
    if (selectedNotificationKey != null) {
        return projectedHistory.firstOrNull { it.key == selectedNotificationKey }
    }
    if (activeNotificationId != null) {
        return projectedHistory.firstOrNull { it.key == activeNotificationId }
    }
    return projectedHistory.firstOrNull()
}

/**
 * Pure policy gating compact idle-pill swipe-to-resume behavior (`>= 28.dp` horizontal drag).
 */
internal fun shouldTriggerIdleSwipeResume(
    hasResumableMedia: Boolean,
    isMusicPlaying: Boolean,
    isExpanded: Boolean,
    hasActivePriorityEvent: Boolean,
    dragDistancePx: Float,
    thresholdPx: Float,
): Boolean {
    if (!hasResumableMedia || isMusicPlaying || isExpanded || hasActivePriorityEvent) return false
    return abs(dragDistancePx) >= thresholdPx
}
