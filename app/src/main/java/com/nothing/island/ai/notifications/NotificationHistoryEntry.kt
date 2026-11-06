package com.nothing.island.ai.notifications

import android.app.Notification
import com.nothing.island.ai.engine.DownloadProgressState

/**
 * Immutable in-memory notification record held in NotificationHistoryRepository.
 * Resides exclusively in volatile heap memory (max 30 entries). NEVER written to DataStore or disk.
 */
data class NotificationHistoryEntry(
    val key: String,                     // StatusBarNotification.key (stable AOSP identity)
    val packageName: String,             // Originating package name
    val appDisplayName: String,          // Resolved human-readable app label
    val rawTitle: String,                // Sanitized raw title
    val rawText: String,                 // Sanitized raw body
    val rawSubText: String? = null,      // Optional subtext (e.g. group chat name, channel)
    val category: String? = null,        // AOSP category (e.g. msg, promo)
    val firstPostTime: Long,             // Original arrival timestamp (for relative age formatting)
    val lastUpdateTime: Long,            // Timestamp of most recent update
    val sourcePostTime: Long = 0L,       // sbn.postTime used for event freshness arbitration
    val ingestSequence: Long = 0L,       // Monotonic sequence assigned at listener ingress
    val lastRemovalPostTime: Long = 0L,  // sbn.postTime of most recent removal event
    val lastRemovalSequence: Long = 0L,  // Ingress sequence of most recent removal event
    val isSystemActive: Boolean = true,  // True if still active in Android status bar shade
    val isOngoing: Boolean = false,      // Whether notification was posted as ongoing
    val isClearable: Boolean = true,     // Whether notification can be dismissed by user
    val visibility: Int = 0,             // Notification.VISIBILITY_PUBLIC (0), PRIVATE (1), SECRET (-1)
    val previewMode: NotificationPreviewMode = NotificationPreviewMode.FULL,
    val actionGeneration: Long = 0L,     // Generation token assigned by NotificationActionBridge
    val updateCount: Int = 0,            // Number of content updates applied to this entry
    val progress: Int = 0,
    val maxProgress: Int = 0,
    val isIndeterminate: Boolean = false,
    val hasProgress: Boolean = false,
    val progressState: DownloadProgressState = DownloadProgressState.NONE
) {
    /**
     * Projects this entry into a privacy-safe presentation based on current lock-screen
     * state and user preferences. Returns null if the notification must be suppressed
     * (e.g. VISIBILITY_SECRET on lock screen, or global HIDDEN preview mode).
     */
    fun toPrivacySafePresentation(
        isKeyguardLocked: Boolean,
        preferences: NotificationPreferences
    ): PrivacySafeNotificationPresentation? {
        // Invariant 1: Global preview mode HIDDEN suppresses presentation completely
        if (preferences.previewMode == NotificationPreviewMode.HIDDEN) {
            return null
        }

        // Invariant 2: VISIBILITY_SECRET (-1) is TEMPORARILY SUPPRESSED on lock screen (not deleted from memory)
        if (isKeyguardLocked && visibility == Notification.VISIBILITY_SECRET) {
            return null
        }

        // Invariant 3: Sensitive content masking
        val shouldMask = isKeyguardLocked && (preferences.hideSensitiveContent || visibility == Notification.VISIBILITY_PRIVATE)

        return when {
            shouldMask -> PrivacySafeNotificationPresentation(
                key = key,
                packageName = packageName,
                appDisplayName = appDisplayName,
                title = appDisplayName,
                body = "New notification",
                subText = null,
                isMasked = true,
                isSystemActive = isSystemActive,
                timestamp = lastUpdateTime,
                previewMode = previewMode,
                progress = progress,
                maxProgress = maxProgress,
                isIndeterminate = isIndeterminate,
                hasProgress = hasProgress,
                progressState = progressState
            )
            previewMode == NotificationPreviewMode.APP_ONLY -> PrivacySafeNotificationPresentation(
                key = key,
                packageName = packageName,
                appDisplayName = appDisplayName,
                title = appDisplayName,
                body = "New notification",
                subText = null,
                isMasked = true,
                isSystemActive = isSystemActive,
                timestamp = lastUpdateTime,
                previewMode = previewMode,
                progress = progress,
                maxProgress = maxProgress,
                isIndeterminate = isIndeterminate,
                hasProgress = hasProgress,
                progressState = progressState
            )
            previewMode == NotificationPreviewMode.TITLE_ONLY -> PrivacySafeNotificationPresentation(
                key = key,
                packageName = packageName,
                appDisplayName = appDisplayName,
                title = rawTitle.ifBlank { appDisplayName },
                body = "",
                subText = null,
                isMasked = false,
                isSystemActive = isSystemActive,
                timestamp = lastUpdateTime,
                previewMode = previewMode,
                progress = progress,
                maxProgress = maxProgress,
                isIndeterminate = isIndeterminate,
                hasProgress = hasProgress,
                progressState = progressState
            )
            else -> PrivacySafeNotificationPresentation(
                key = key,
                packageName = packageName,
                appDisplayName = appDisplayName,
                title = rawTitle.ifBlank { appDisplayName },
                body = rawText,
                subText = rawSubText,
                isMasked = false,
                isSystemActive = isSystemActive,
                timestamp = lastUpdateTime,
                previewMode = previewMode,
                progress = progress,
                maxProgress = maxProgress,
                isIndeterminate = isIndeterminate,
                hasProgress = hasProgress,
                progressState = progressState
            )
        }
    }
}

/**
 * Privacy-safe UI projection consumed by Compose views (Dashboard & NotificationCard).
 * Dynamically generated; never statically cached across lock-state transitions.
 */
data class PrivacySafeNotificationPresentation(
    val key: String,
    val packageName: String,
    val appDisplayName: String,
    val title: String,
    val body: String,
    val subText: String?,
    val isMasked: Boolean,
    val isSystemActive: Boolean,
    val timestamp: Long,
    val previewMode: NotificationPreviewMode,
    val progress: Int = 0,
    val maxProgress: Int = 0,
    val isIndeterminate: Boolean = false,
    val hasProgress: Boolean = false,
    val progressState: DownloadProgressState = DownloadProgressState.NONE
)
