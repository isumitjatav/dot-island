package com.nothing.island.ai.notifications

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * NotificationFilterUnitTest
 *
 * Verifies all mandatory exclusions, configurable preferences filtering,
 * group summary arbitration, lockscreen privacy masking, and stable ID mapping.
 */
class NotificationFilterUnitTest {

    private lateinit var filter: NotificationFilter

    @Before
    fun setUp() {
        filter = NotificationFilter(hostPackageName = "com.nothing.island.ai")
    }

    private fun createStandardRaw(
        key: String = "0|com.whatsapp|100|null|10123",
        packageName: String = "com.whatsapp",
        title: String = "Alice",
        text: String = "Hey there!",
        category: String? = null,
        isOngoing: Boolean = false,
        isGroupSummary: Boolean = false,
        groupKey: String? = "0|com.whatsapp|group1",
        importance: Int = 3, // IMPORTANCE_DEFAULT
        visibility: Int = 0, // VISIBILITY_PUBLIC
        hasMediaStyle: Boolean = false
    ): RawNotificationData {
        return RawNotificationData(
            key = key,
            packageName = packageName,
            appLabel = "WhatsApp",
            title = title,
            text = text,
            category = category,
            isOngoing = isOngoing,
            isClearable = true,
            isGroupSummary = isGroupSummary,
            groupKey = groupKey,
            importance = importance,
            visibility = visibility,
            hasMediaStyle = hasMediaStyle,
            postTime = System.currentTimeMillis()
        )
    }

    // =========================================================
    // 1. Mandatory Exclusions
    // =========================================================

    @Test
    fun testMandatorySelfPackageExclusion() {
        val raw = createStandardRaw(
            key = "0|com.nothing.island.ai|4040|null|1000",
            packageName = "com.nothing.island.ai"
        )
        val result = filter.filter(raw, NotificationPreferences.DEFAULTS)
        assertNull("Self-package notification must be unconditionally dropped", result)
    }

    @Test
    fun testMandatoryMediaCategoryExclusion() {
        val raw = createStandardRaw(
            packageName = "com.spotify.music",
            category = Notification.CATEGORY_TRANSPORT
        )
        val result = filter.filter(raw, NotificationPreferences.DEFAULTS)
        assertNull("Media CATEGORY_TRANSPORT notification must be dropped for R7", result)
    }

    @Test
    fun testMandatoryMediaStyleExclusion() {
        val raw = createStandardRaw(
            packageName = "com.google.android.apps.youtube.music",
            hasMediaStyle = true
        )
        val result = filter.filter(raw, NotificationPreferences.DEFAULTS)
        assertNull("MediaStyle notification must be dropped for R7", result)
    }

    @Test
    fun testMandatoryCallCategoryExclusion() {
        val raw = createStandardRaw(
            packageName = "com.google.android.dialer",
            category = Notification.CATEGORY_CALL
        )
        val result = filter.filter(raw, NotificationPreferences.DEFAULTS)
        assertNull("Telephony CATEGORY_CALL notification must be dropped for R9", result)
    }

    // =========================================================
    // 2. Configurable Exclusions
    // =========================================================

    @Test
    fun testConfigurableMasterToggleOff() {
        val raw = createStandardRaw()
        val prefs = NotificationPreferences.DEFAULTS.copy(enabled = false)
        val result = filter.filter(raw, prefs)
        assertNull("Notification must be dropped when master toggle is disabled", result)
    }

    @Test
    fun testConfigurablePerAppExclusionBlacklist() {
        val raw = createStandardRaw(packageName = "com.noisy.app")
        val prefs = NotificationPreferences.DEFAULTS.copy(
            excludedPackages = setOf("com.noisy.app", "com.other.junk")
        )
        val result = filter.filter(raw, prefs)
        assertNull("Notification must be dropped when package is in excludedPackages", result)
    }

    @Test
    fun testConfigurableOngoingNotificationFiltering() {
        val raw = createStandardRaw(isOngoing = true)

        // Case A: Disallowed by default
        val resultDefault = filter.filter(raw, NotificationPreferences.DEFAULTS)
        assertNull("Ongoing notification must be dropped by default", resultDefault)

        // Case B: Allowed via user preference
        val prefsAllowed = NotificationPreferences.DEFAULTS.copy(allowOngoingNotifications = true)
        val resultAllowed = filter.filter(raw, prefsAllowed)
        assertNotNull("Ongoing notification must be accepted when allowOngoingNotifications=true", resultAllowed)
    }

    @Test
    fun testConfigurableSilentLowImportanceFiltering() {
        val raw = createStandardRaw(importance = 1, category = "sys") // IMPORTANCE_MIN

        // Case A: Disallowed by default
        val resultDefault = filter.filter(raw, NotificationPreferences.DEFAULTS)
        assertNull("Low importance notification must be dropped by default", resultDefault)

        // Case B: Allowed via user preference
        val prefsAllowed = NotificationPreferences.DEFAULTS.copy(allowSilentNotifications = true)
        val resultAllowed = filter.filter(raw, prefsAllowed)
        assertNotNull("Low importance notification must be accepted when allowSilentNotifications=true", resultAllowed)
    }

    @Test
    fun testConfigurableLockscreenPolicy() {
        val raw = createStandardRaw()

        // Case A: Screen locked, showOnLockscreen = false (default)
        val resultLocked = filter.filter(raw, NotificationPreferences.DEFAULTS, isKeyguardLocked = true)
        assertNull("Notification must be dropped on lockscreen when showOnLockscreen=false", resultLocked)

        // Case B: Screen locked, showOnLockscreen = true
        val prefsAllowed = NotificationPreferences.DEFAULTS.copy(showOnLockscreen = true)
        val resultAllowed = filter.filter(raw, prefsAllowed, isKeyguardLocked = true)
        assertNotNull("Notification must be accepted on lockscreen when showOnLockscreen=true", resultAllowed)
    }

    // =========================================================
    // 3. Group Summary vs Child Arbitration
    // =========================================================

    @Test
    fun testGroupSummarySuppressionWhenChildActive() {
        val groupKey = "0|com.whatsapp|conv_123"

        // 1. Post child alert first
        val childRaw = createStandardRaw(
            key = "child_1",
            isGroupSummary = false,
            groupKey = groupKey
        )
        val childResult = filter.filter(childRaw, NotificationPreferences.DEFAULTS)
        assertNotNull("Child alert must be accepted", childResult)

        // 2. Post group summary for the same conversation
        val summaryRaw = createStandardRaw(
            key = "summary_1",
            isGroupSummary = true,
            groupKey = groupKey
        )
        val summaryResult = filter.filter(summaryRaw, NotificationPreferences.DEFAULTS)
        assertNull("Group summary must be suppressed when active child alerts exist", summaryResult)
    }

    @Test
    fun testStandaloneGroupSummaryAccepted() {
        val groupKey = "0|com.whatsapp|conv_456"

        // Post group summary with NO active children
        val summaryRaw = createStandardRaw(
            key = "summary_standalone",
            isGroupSummary = true,
            groupKey = groupKey,
            title = "3 new messages from 2 chats"
        )
        val summaryResult = filter.filter(summaryRaw, NotificationPreferences.DEFAULTS)
        assertNotNull("Standalone group summary must be accepted when no children are active", summaryResult)
        assertEquals("summary_standalone", summaryResult?.id)
    }

    @Test
    fun testGroupChildRemovalAllowsSubsequentSummary() {
        val groupKey = "0|com.whatsapp|conv_789"
        val childKey = "child_789"

        val childRaw = createStandardRaw(key = childKey, isGroupSummary = false, groupKey = groupKey)
        filter.filter(childRaw, NotificationPreferences.DEFAULTS)

        // Child is removed
        filter.onNotificationRemoved(childKey, groupKey)

        // Now summary should be accepted
        val summaryRaw = createStandardRaw(key = "summary_789", isGroupSummary = true, groupKey = groupKey)
        val summaryResult = filter.filter(summaryRaw, NotificationPreferences.DEFAULTS)
        assertNotNull("Summary should be accepted after all children are cleared", summaryResult)
    }

    // =========================================================
    // 4. Preview Modes & Privacy Masking
    // =========================================================

    @Test
    fun testPreviewModeFull() {
        val raw = createStandardRaw(title = "Alice", text = "Are you available?")
        val prefs = NotificationPreferences.DEFAULTS.copy(previewMode = NotificationPreviewMode.FULL)
        val result = filter.filter(raw, prefs)

        assertNotNull(result)
        assertEquals("Alice", result?.title)
        assertEquals("Are you available?", result?.text)
    }

    @Test
    fun testPreviewModeTitleOnly() {
        val raw = createStandardRaw(title = "Bob", text = "Secret message content")
        val prefs = NotificationPreferences.DEFAULTS.copy(previewMode = NotificationPreviewMode.TITLE_ONLY)
        val result = filter.filter(raw, prefs)

        assertNotNull(result)
        assertEquals("Bob", result?.title)
        assertEquals("", result?.text)
    }

    @Test
    fun testPreviewModeAppOnly() {
        val raw = createStandardRaw(title = "Charlie", text = "Confidential text")
        val prefs = NotificationPreferences.DEFAULTS.copy(previewMode = NotificationPreviewMode.APP_ONLY)
        val result = filter.filter(raw, prefs)

        assertNotNull(result)
        assertEquals("WhatsApp", result?.title)
        assertEquals("New notification", result?.text)
    }

    @Test
    fun testPreviewModeHiddenSuppressesPresentationAndTracksChild() {
        val groupKey = "0|com.whatsapp|conv_hidden"
        val rawChild = createStandardRaw(key = "child_hidden", groupKey = groupKey, isGroupSummary = false)
        val prefs = NotificationPreferences.DEFAULTS.copy(previewMode = NotificationPreviewMode.HIDDEN)
        val resultChild = filter.filter(rawChild, prefs)

        // Visual presentation must be completely suppressed
        assertNull("HIDDEN mode must suppress all visual notification presentation", resultChild)

        // Internal child tracking must still occur so subsequent group summaries are properly arbitrated
        val rawSummary = createStandardRaw(key = "summary_hidden", groupKey = groupKey, isGroupSummary = true)
        val resultSummary = filter.filter(rawSummary, prefs)
        assertNull("Group summary must also be suppressed because active child was tracked", resultSummary)
    }

    @Test
    fun testSensitiveContentMaskingPreventsLeakThroughTitleOnly() {
        val raw = createStandardRaw(title = "Dr. Smith - Lab Results", text = "Confidential medical text")
        val prefs = NotificationPreferences.DEFAULTS.copy(
            showOnLockscreen = true,
            hideSensitiveContent = true,
            previewMode = NotificationPreviewMode.TITLE_ONLY
        )
        val result = filter.filter(raw, prefs, isKeyguardLocked = true)

        assertNotNull(result)
        // Sensitive title must NOT leak through TITLE_ONLY preview mode on lockscreen
        assertEquals("WhatsApp", result?.title)
        assertEquals("New notification", result?.text)
        assertNull(result?.subText)
    }

    @Test
    fun testSensitiveContentMaskingPreventsLeakThroughFullMode() {
        val raw = createStandardRaw(title = "Bank OTP", text = "Your secret code is 884920")
        val prefs = NotificationPreferences.DEFAULTS.copy(
            showOnLockscreen = true,
            hideSensitiveContent = true,
            previewMode = NotificationPreviewMode.FULL
        )
        val result = filter.filter(raw, prefs, isKeyguardLocked = true)

        assertNotNull(result)
        assertEquals("WhatsApp", result?.title)
        assertEquals("New notification", result?.text)
        assertNull(result?.subText)
    }

    @Test
    fun testVisibilityPrivateMaskedOnLockscreen() {
        // Notification marked with framework VISIBILITY_PRIVATE (0)
        val raw = createStandardRaw(
            title = "Private Sender",
            text = "Private Message",
            visibility = 0 // Notification.VISIBILITY_PRIVATE
        )
        val prefs = NotificationPreferences.DEFAULTS.copy(showOnLockscreen = true)
        val result = filter.filter(raw, prefs, isKeyguardLocked = true)

        assertNotNull(result)
        assertEquals("WhatsApp", result?.title)
        assertEquals("New notification", result?.text)
    }

    @Test
    fun testVisibilitySecretDroppedEvenIfShowOnLockscreenTrue() {
        val raw = createStandardRaw(visibility = -1) // Notification.VISIBILITY_SECRET
        val prefs = NotificationPreferences.DEFAULTS.copy(showOnLockscreen = true)
        val result = filter.filter(raw, prefs, isKeyguardLocked = true)

        assertNull("VISIBILITY_SECRET notification must be dropped unconditionally on keyguard", result)
    }

    // =========================================================
    // 5. Stable Key Identity & Duration Clamping
    // =========================================================

    @Test
    fun testStableNotificationKeyMappedToEventId() {
        val rawKey = "0|com.example.messenger|42|tag_conv|10099"
        val raw = createStandardRaw(key = rawKey)
        val result = filter.filter(raw, NotificationPreferences.DEFAULTS)

        assertNotNull(result)
        assertEquals("Event id must strictly match sbn.key", rawKey, result?.id)
    }

    @Test
    fun testDisplayDurationClamping() {
        val raw = createStandardRaw()

        // Under min duration (1000ms -> clamped to 2000ms)
        val underMin = filter.filter(raw, NotificationPreferences.DEFAULTS.copy(displayDurationMs = 1000L))
        assertEquals(2000L, underMin?.timeoutMs)

        // Normal duration (6000ms)
        val normal = filter.filter(raw, NotificationPreferences.DEFAULTS.copy(displayDurationMs = 6000L))
        assertEquals(6000L, normal?.timeoutMs)

        // Over max duration (15000ms -> clamped to 10000ms)
        val overMax = filter.filter(raw, NotificationPreferences.DEFAULTS.copy(displayDurationMs = 15000L))
        assertEquals(10000L, overMax?.timeoutMs)
    }

    @Test
    fun testSystemAndHardwareUtilityNoiseDropped() {
        val blockedSamples = listOf(
            createStandardRaw(key = "sys_android", packageName = "android", title = "USB debugging connected", text = "Tap to turn off USB debugging"),
            createStandardRaw(key = "sys_ui", packageName = "com.android.systemui", title = "Screen Recording", text = "Recording screen"),
            createStandardRaw(key = "sys_settings", packageName = "com.android.settings", title = "Battery Saver", text = "Battery Saver is on"),
            createStandardRaw(key = "sys_provider", packageName = "com.android.providers.contacts", title = "Contacts", text = "Syncing"),
            RawNotificationData(
                key = "usb_channel",
                packageName = "com.nothing.os.utility",
                title = "USB connection",
                text = "Tap for more options",
                channelId = "usb_debugging_channel"
            ),
            RawNotificationData(
                key = "vpn_channel",
                packageName = "com.example.vpnhost",
                title = "VPN",
                text = "VPN is active",
                channelId = "vpn_status"
            ),
        )
        blockedSamples.forEach { sample ->
            val result = filter.filter(sample, NotificationPreferences.DEFAULTS)
            assertNull("System/hardware noise should be dropped: ${sample.key}", result)
        }
    }
}
