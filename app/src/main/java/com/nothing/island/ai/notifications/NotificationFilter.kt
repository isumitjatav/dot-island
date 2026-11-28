package com.nothing.island.ai.notifications

import android.app.Notification
import android.service.notification.StatusBarNotification
import android.util.Log
import android.content.Context
import com.nothing.island.ai.engine.DownloadProgressState
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandPriority
import java.util.concurrent.ConcurrentHashMap

/**
 * Normalized notification business payload container.
 * Independent of Android binder / framework classes for 100% JVM unit testability.
 */
data class RawNotificationData(
    val key: String,
    val packageName: String,
    val appLabel: String? = null,
    val title: String,
    val text: String,
    val subText: String? = null,
    val category: String? = null,
    val isOngoing: Boolean = false,
    val isClearable: Boolean = true,
    val isGroupSummary: Boolean = false,
    val groupKey: String? = null,
    val priority: Int = 0,
    val importance: Int = 3, // NotificationListenerService.Ranking.IMPORTANCE_DEFAULT = 3
    val visibility: Int = 0, // Notification.VISIBILITY_PUBLIC = 0
    val hasMediaStyle: Boolean = false,
    val postTime: Long = System.currentTimeMillis(),
    val progress: Int = 0,
    val maxProgress: Int = 0,
    val isIndeterminate: Boolean = false,
    val hasProgress: Boolean = false,
    val progressState: DownloadProgressState = DownloadProgressState.NONE,
    val channelId: String? = null,
)

/**
 * NotificationFilter
 *
 * Implements the multi-stage notification processing and filtering pipeline for Nothing Island.
 * Filters system noise, ongoing services, media/telephony categories, and group summaries,
 * while extracting metadata into platform-independent IslandEvent.NotificationPosted.
 */
class NotificationFilter(
    private val hostPackageName: String = "com.nothing.island.ai"
) {
    constructor(context: Context) : this(context.packageName)
    companion object {
        private const val TAG = "NotificationFilter"

        private val SYSTEM_UTILITY_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.server.telecom",
            "com.android.bluetooth",
            "com.android.nfc",
            "com.android.mtp",
            "com.android.usb",
            "com.android.shell",
        )

        private val SYSTEM_UTILITY_PREFIXES = listOf(
            "com.android.providers.",
        )

        private val HARDWARE_NOISE_CHANNELS = listOf(
            "usb",
            "adb",
            "developer",
            "debugging",
            "charging",
            "battery",
            "vpn",
            "screen_record",
            "screenrecord",
            "screenshot",
            "do_not_disturb",
            "zen_mode",
        )

        private val HARDWARE_NOISE_PHRASES = listOf(
            "usb debugging",
            "debugging connected",
            "charging this device via usb",
            "usb for file transfer",
            "tap for more options",
            "screen recording",
            "recording screen",
            "screenshot captured",
            "vpn is active",
            "vpn activated",
            "battery saver is on",
            "battery saver turned on",
        )
    }

    // Active child notification keys indexed by group key
    // Map: groupKey -> Set<childNotificationKeys>
    private val activeGroupChildren = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * Filters a raw notification data model against preferences and keyguard state.
     * Pure JVM testable method without requiring Android framework binder mocks.
     */
    fun filter(
        raw: RawNotificationData,
        preferences: NotificationPreferences,
        isKeyguardLocked: Boolean = false
    ): IslandEvent.NotificationPosted? {
        val key = raw.key
        val pkg = raw.packageName

        // =========================================================
        // STAGE 1: MANDATORY EXCLUSIONS (Inviolable System Invariants)
        // =========================================================

        // 1.1 Self-package exclusion (suppress Island's own foreground notification ID 4040)
        if (pkg == hostPackageName) {
            Log.d(TAG, "MANDATORY DROP: Self package $pkg (key=$key)")
            return null
        }

        // 1.2 Media Transport exclusion (Reserved for Milestone R7 Media Engine)
        if (raw.category == Notification.CATEGORY_TRANSPORT ||
            raw.hasMediaStyle
        ) {
            Log.d(TAG, "MANDATORY DROP: Media playback category/style from $pkg (key=$key)")
            return null
        }

        // 1.3 Telephony Call & Dialer exclusion (Reserved for Milestone R9 Telephony Engine)
        val isDialerOrTelecom = raw.category == Notification.CATEGORY_CALL ||
            raw.packageName == "com.android.server.telecom" ||
            raw.packageName == "com.google.android.dialer" ||
            raw.packageName == "com.android.dialer"
        if (isDialerOrTelecom) {
            Log.d(TAG, "MANDATORY DROP: Telephony call category or dialer from $pkg (key=$key)")
            return null
        }

        // 1.4 System OS & Hardware utility noise exclusion (USB debugging/charging, Screen Record, VPN, Battery Saver)
        if (isSystemOrHardwareNoise(raw)) {
            Log.d(TAG, "MANDATORY DROP: System/hardware utility notification from $pkg (channel=${raw.channelId}, key=$key)")
            return null
        }

        // =========================================================
        // STAGE 2: CONFIGURABLE EXCLUSIONS (User Preferences Governed)
        // =========================================================

        // 2.1 Master Toggle
        if (!preferences.enabled) {
            Log.d(TAG, "CONFIGURABLE DROP: Notifications disabled globally")
            return null
        }

        // 2.2 Per-App Exclusions (Blacklist)
        if (preferences.excludedPackages.contains(pkg)) {
            Log.d(TAG, "CONFIGURABLE DROP: App $pkg is excluded via blacklist (key=$key)")
            return null
        }

        // 2.3 Ongoing / Foreground Service Notifications
        // Active download/progress notifications are exempted from generic ongoing suppression!
        val isProgressNotification = raw.hasProgress || raw.category == Notification.CATEGORY_PROGRESS
        if (raw.isOngoing && !preferences.allowOngoingNotifications && !isProgressNotification) {
            Log.d(TAG, "CONFIGURABLE DROP: Ongoing notification from $pkg suppressed (key=$key)")
            return null
        }

        // 2.4 Silent / Low-Importance Notifications
        val isLowImportance = raw.importance < 3 || raw.priority < 0 // < IMPORTANCE_DEFAULT or < PRIORITY_DEFAULT
        if (isLowImportance && !preferences.allowSilentNotifications) {
            Log.d(TAG, "CONFIGURABLE DROP: Silent / low importance notification from $pkg (key=$key)")
            return null
        }

        // 2.5 Lockscreen Presentation Policy & Secret Visibility
        if (isKeyguardLocked) {
            if (!preferences.showOnLockscreen) {
                Log.d(TAG, "CONFIGURABLE DROP: Lockscreen presentation disabled (key=$key)")
                return null
            }
            // VISIBILITY_SECRET (-1) must NEVER be exposed on the lock screen under any circumstances
            if (raw.visibility == Notification.VISIBILITY_SECRET) {
                Log.d(TAG, "MANDATORY PRIVACY DROP: Secret visibility (-1) notification suppressed on keyguard (key=$key)")
                return null
            }
        }

        // 2.6 Preview Mode Hidden
        // HIDDEN must suppress all visible notification presentation (no "New Notification", app names, icons, badges or preview text)
        if (preferences.previewMode == NotificationPreviewMode.HIDDEN) {
            // Internal child tracking continues so future group summaries are properly arbitrated
            val groupKey = raw.groupKey ?: pkg
            if (!raw.isGroupSummary) {
                val children = activeGroupChildren.computeIfAbsent(groupKey) { ConcurrentHashMap.newKeySet() }
                children.add(key)
            }
            Log.d(TAG, "SUPPRESS PRESENTATION: Preview mode is HIDDEN; visual presentation completely suppressed (key=$key)")
            return null
        }

        // =========================================================
        // STAGE 3: GROUP SUMMARY VS CHILD NOTIFICATION ARBITRATION
        // =========================================================
        val groupKey = raw.groupKey ?: pkg

        if (!raw.isGroupSummary) {
            // Child notification: track in active group children
            val children = activeGroupChildren.computeIfAbsent(groupKey) { ConcurrentHashMap.newKeySet() }
            children.add(key)
        } else {
            // Group summary: if active children already exist, suppress summary to prevent duplicate alert
            val children = activeGroupChildren[groupKey]
            if (children != null && children.isNotEmpty()) {
                Log.d(TAG, "GROUP DROP: Group summary suppressed for groupKey=$groupKey because ${children.size} child alerts are active")
                return null
            }
            // If no child alerts exist, allow standalone summary (e.g. "3 new messages from 2 chats")
            Log.d(TAG, "GROUP ALLOW: Standalone group summary allowed for groupKey=$groupKey")
        }

        // =========================================================
        // STAGE 4: PRIVACY MASKING & METADATA EXTRACTION
        // =========================================================
        val isPrivateOnLockscreen = isKeyguardLocked && (preferences.hideSensitiveContent || raw.visibility == Notification.VISIBILITY_PRIVATE)
        val appDisplayName = raw.appLabel ?: pkg

        // Ensure sensitive content cannot leak through alternate preview modes
        val (finalTitle, finalText, finalSubText) = when {
            isPrivateOnLockscreen -> {
                Log.d(TAG, "PRIVACY MASK: Masking sensitive notification on keyguard for $pkg")
                Triple(appDisplayName, NotificationPlaceholderFormatter.MASKED_GENERIC_LABEL, null)
            }
            preferences.previewMode == NotificationPreviewMode.APP_ONLY -> {
                Triple(appDisplayName, NotificationPlaceholderFormatter.MASKED_GENERIC_LABEL, null)
            }
            preferences.previewMode == NotificationPreviewMode.TITLE_ONLY -> {
                Triple(raw.title.ifBlank { appDisplayName }, "", null)
            }
            else -> { // NotificationPreviewMode.FULL
                Triple(raw.title.ifBlank { appDisplayName }, raw.text, raw.subText)
            }
        }

        val clampedDuration = preferences.displayDurationMs.coerceIn(
            NotificationPreferences.MIN_DURATION_MS,
            NotificationPreferences.MAX_DURATION_MS
        )

        // Active downloads remain persistent (0L timeout) until completed, paused, or user dismissed
        val effectiveTimeout = if (raw.progressState == DownloadProgressState.DOWNLOADING || raw.progressState == DownloadProgressState.INDETERMINATE) {
            0L
        } else {
            clampedDuration
        }

        return IslandEvent.NotificationPosted(
            id = key,
            timestamp = raw.postTime.takeIf { it > 0L } ?: System.currentTimeMillis(),
            source = "NotificationEngine",
            priority = IslandPriority.NOTIFICATION,
            timeoutMs = effectiveTimeout,
            packageName = pkg,
            title = finalTitle,
            text = finalText,
            subText = finalSubText,
            category = raw.category,
            isClearable = raw.isClearable,
            progress = raw.progress,
            maxProgress = raw.maxProgress,
            isIndeterminate = raw.isIndeterminate,
            hasProgress = raw.hasProgress,
            progressState = raw.progressState
        )
    }

    /**
     * Adapts an Android StatusBarNotification into RawNotificationData and executes the pipeline.
     */
    fun filter(
        sbn: StatusBarNotification,
        preferences: NotificationPreferences,
        isKeyguardLocked: Boolean = false
    ): IslandEvent.NotificationPosted? {
        val raw = extractRawData(sbn)
        return filter(raw, preferences, isKeyguardLocked)
    }

    /**
     * Cleans up group tracking when a notification is removed.
     */
    fun onNotificationRemoved(key: String, groupKey: String?) {
        if (groupKey != null) {
            val children = activeGroupChildren[groupKey]
            children?.remove(key)
            if (children != null && children.isEmpty()) {
                activeGroupChildren.remove(groupKey)
            }
        } else {
            // Scan and remove from all groups
            for ((_, children) in activeGroupChildren) {
                children.remove(key)
            }
        }
    }

    /**
     * Clears all group tracking caches (e.g. on service reset).
     */
    fun reset() {
        activeGroupChildren.clear()
    }

    internal fun extractRawData(
        sbn: StatusBarNotification
    ): RawNotificationData {
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras?.getCharSequence("android.title")?.toString()
            ?: ""
        val text = IslandNotificationListenerService.extractNotificationText(extras).ifEmpty {
            extras?.getCharSequence("android.text")?.toString() ?: ""
        }
        val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val isOngoing = sbn.isOngoing ||
                (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
                (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0

        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

        // Determine importance from priority
        var importance = 3 // IMPORTANCE_DEFAULT
        if (notification.priority <= Notification.PRIORITY_MIN) {
            importance = 1
        } else if (notification.priority <= Notification.PRIORITY_LOW) {
            importance = 2
        }

        // Check media style
        val hasMediaStyle = extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true ||
                notification.category == Notification.CATEGORY_TRANSPORT

        // Extract download / progress metadata
        val hasProgress = extras?.containsKey(Notification.EXTRA_PROGRESS) == true ||
                extras?.containsKey(Notification.EXTRA_PROGRESS_MAX) == true ||
                notification.category == Notification.CATEGORY_PROGRESS

        val progress = extras?.getInt(Notification.EXTRA_PROGRESS, 0) ?: 0
        val maxProgress = extras?.getInt(Notification.EXTRA_PROGRESS_MAX, 0) ?: 0
        val isIndeterminate = extras?.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) ?: false

        val progressState = when {
            !hasProgress -> DownloadProgressState.NONE
            isIndeterminate -> DownloadProgressState.INDETERMINATE
            maxProgress > 0 && progress >= maxProgress -> DownloadProgressState.COMPLETED
            text.contains("fail", ignoreCase = true) || text.contains("error", ignoreCase = true) -> DownloadProgressState.FAILED
            text.contains("pause", ignoreCase = true) -> DownloadProgressState.PAUSED
            maxProgress > 0 -> DownloadProgressState.DOWNLOADING
            else -> DownloadProgressState.NONE
        }

        val channelId = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification.channelId
        } else {
            null
        }

        return RawNotificationData(
            key = sbn.key ?: "${sbn.packageName}_${sbn.id}",
            packageName = sbn.packageName,
            title = title,
            text = text,
            subText = subText,
            category = notification.category,
            isOngoing = isOngoing,
            isClearable = sbn.isClearable,
            isGroupSummary = isGroupSummary,
            groupKey = sbn.groupKey,
            priority = notification.priority,
            importance = importance,
            visibility = notification.visibility,
            hasMediaStyle = hasMediaStyle,
            postTime = sbn.postTime,
            progress = progress,
            maxProgress = maxProgress,
            isIndeterminate = isIndeterminate,
            hasProgress = hasProgress,
            progressState = progressState,
            channelId = channelId,
        )
    }

    private fun isSystemOrHardwareNoise(raw: RawNotificationData): Boolean {
        val pkgLower = raw.packageName.lowercase()
        val isActiveDownloadManager = pkgLower == "com.android.providers.downloads" &&
            (raw.hasProgress || raw.category == Notification.CATEGORY_PROGRESS)
        if (!isActiveDownloadManager &&
            (pkgLower in SYSTEM_UTILITY_PACKAGES || SYSTEM_UTILITY_PREFIXES.any { pkgLower.startsWith(it) })
        ) {
            return true
        }
        val channelLower = raw.channelId?.lowercase().orEmpty()
        if (channelLower.isNotEmpty() && HARDWARE_NOISE_CHANNELS.any { channelLower.contains(it) }) {
            return true
        }
        val combinedText = "${raw.title} ${raw.text} ${raw.subText.orEmpty()}".lowercase()
        return HARDWARE_NOISE_PHRASES.any { combinedText.contains(it) }
    }

    internal fun getActiveChildrenForGroup(groupKey: String): Set<String> {
        return activeGroupChildren[groupKey]?.toSet() ?: emptySet()
    }
}
