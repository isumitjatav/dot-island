package com.nothing.island.ai.ui

import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandPriority
import com.nothing.island.ai.notifications.NotificationPreviewMode
import com.nothing.island.ai.ui.components.NotificationCardResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NotificationCardUnitTest
 *
 * Verifies Milestone R6 Phase 3.1 NotificationCard visual presentation logic:
 * - Full notification data resolution and presentation state mapping.
 * - Graceful fallback handling for missing title, body, and subtext.
 * - Preview mode enforcement: FULL, TITLE_ONLY, APP_ONLY, and HIDDEN.
 * - Strict suppression under HIDDEN preview mode (shouldRender = false).
 * - Relative Nothing OS timestamp formatting.
 * - Direct bridging from IslandEvent.NotificationPosted.
 */
class NotificationCardUnitTest {

    @Test
    fun testCardRendersCorrectlyWithFullNotificationData() {
        val now = 1_000_000_000L
        val eventTime = now - 120_000L // 2 minutes ago

        val state = NotificationCardResolver.resolve(
            packageName = "com.whatsapp",
            rawTitle = "Alice Johnson",
            rawText = "Are we still meeting at 3 PM?",
            rawSubText = "Work Chat",
            timestamp = eventTime,
            previewMode = NotificationPreviewMode.FULL,
            appLabel = "WhatsApp",
            currentTimeMillis = now
        )

        assertTrue("Card must render in FULL preview mode", state.shouldRender)
        assertEquals("WhatsApp", state.appDisplayName)
        assertEquals("Alice Johnson", state.title)
        assertEquals("Are we still meeting at 3 PM?", state.body)
        assertEquals("Work Chat", state.subText)
        assertEquals("2m ago", state.formattedTime)
        assertEquals(NotificationPreviewMode.FULL, state.previewMode)
    }

    @Test
    fun testCardHandlesMissingTitleGracefully() {
        // Missing title (empty string)
        val stateEmptyTitle = NotificationCardResolver.resolve(
            packageName = "org.telegram.messenger",
            rawTitle = "",
            rawText = "Incoming audio memo",
            rawSubText = null,
            previewMode = NotificationPreviewMode.FULL,
            appLabel = "Telegram"
        )

        assertTrue(stateEmptyTitle.shouldRender)
        assertEquals("Telegram", stateEmptyTitle.appDisplayName)
        assertEquals("When raw title is empty, it must gracefully fallback to app label", "Telegram", stateEmptyTitle.title)

        // Missing title (whitespace only)
        val stateWhitespaceTitle = NotificationCardResolver.resolve(
            packageName = "com.google.android.apps.messaging",
            rawTitle = "   ",
            rawText = "Your verification code is 492019",
            previewMode = NotificationPreviewMode.FULL,
            appLabel = "Messages"
        )

        assertEquals("When raw title is whitespace, it must gracefully fallback to app label", "Messages", stateWhitespaceTitle.title)
    }

    @Test
    fun testCardHandlesMissingBodyGracefully() {
        // Empty body string
        val stateEmptyBody = NotificationCardResolver.resolve(
            packageName = "com.google.android.calendar",
            rawTitle = "Meeting with Design Team",
            rawText = "",
            rawSubText = "10:00 - 10:30",
            previewMode = NotificationPreviewMode.FULL,
            appLabel = "Calendar"
        )

        assertTrue(stateEmptyBody.shouldRender)
        assertEquals("Meeting with Design Team", stateEmptyBody.title)
        assertNull("Missing or empty body must yield null so Compose omits the body layout element", stateEmptyBody.body)
        assertEquals("10:00 - 10:30", stateEmptyBody.subText)

        // Whitespace-only body
        val stateWhitespaceBody = NotificationCardResolver.resolve(
            packageName = "com.google.android.calendar",
            rawTitle = "Standup",
            rawText = "   \n\t  ",
            previewMode = NotificationPreviewMode.FULL,
            appLabel = "Calendar"
        )

        assertNull("Whitespace-only body must yield null", stateWhitespaceBody.body)
    }

    @Test
    fun testCardHandlesMissingSubtextGracefully() {
        val stateNullSubtext = NotificationCardResolver.resolve(
            packageName = "com.slack",
            rawTitle = "General",
            rawText = "Release v1.2 deployed to staging",
            rawSubText = null,
            previewMode = NotificationPreviewMode.FULL,
            appLabel = "Slack"
        )

        assertTrue(stateNullSubtext.shouldRender)
        assertEquals("General", stateNullSubtext.title)
        assertEquals("Release v1.2 deployed to staging", stateNullSubtext.body)
        assertNull("Null subtext must yield null in presentation state", stateNullSubtext.subText)

        val stateEmptySubtext = NotificationCardResolver.resolve(
            packageName = "com.slack",
            rawTitle = "General",
            rawText = "Deployment success",
            rawSubText = "   ",
            previewMode = NotificationPreviewMode.FULL,
            appLabel = "Slack"
        )

        assertNull("Blank subtext must yield null in presentation state", stateEmptySubtext.subText)
    }

    @Test
    fun testCardTruncatesLongTextWithoutClippingOrOverflow() {
        val veryLongTitle = "A".repeat(500)
        val veryLongBody = "B".repeat(2000)
        val veryLongSubtext = "C".repeat(300)

        val state = NotificationCardResolver.resolve(
            packageName = "com.example.chat",
            rawTitle = veryLongTitle,
            rawText = veryLongBody,
            rawSubText = veryLongSubtext,
            previewMode = NotificationPreviewMode.FULL,
            appLabel = "Example Chat"
        )

        assertTrue(state.shouldRender)
        assertNotNull(state.title)
        assertNotNull(state.body)
        assertNotNull(state.subText)
        assertEquals("Title preserves content for Compose TextOverflow.Ellipsis clipping", 500, state.title?.length)
        assertEquals("Body preserves content for Compose TextOverflow.Ellipsis clipping", 2000, state.body?.length)
        assertEquals("Subtext preserves content for Compose TextOverflow.Ellipsis clipping", 300, state.subText?.length)
    }

    @Test
    fun testCardRespectsTitleOnlyPreviewMode() {
        val state = NotificationCardResolver.resolve(
            packageName = "com.whatsapp",
            rawTitle = "Secret Group",
            rawText = "Confidential chat contents that must be hidden",
            rawSubText = "Group info",
            previewMode = NotificationPreviewMode.TITLE_ONLY,
            appLabel = "WhatsApp"
        )

        assertTrue("TITLE_ONLY mode must be displayed", state.shouldRender)
        assertEquals("WhatsApp", state.appDisplayName)
        assertEquals("Secret Group", state.title)
        assertNull("Body content must strictly be null/hidden in TITLE_ONLY mode", state.body)
        assertNull("SubText must strictly be null/hidden in TITLE_ONLY mode", state.subText)
        assertEquals(NotificationPreviewMode.TITLE_ONLY, state.previewMode)
    }

    @Test
    fun testCardRespectsAppOnlyPreviewMode() {
        val state = NotificationCardResolver.resolve(
            packageName = "com.whatsapp",
            rawTitle = "Alice Johnson",
            rawText = "Private message text",
            rawSubText = "Direct Message",
            previewMode = NotificationPreviewMode.APP_ONLY,
            appLabel = "WhatsApp"
        )

        assertTrue("APP_ONLY mode must be displayed", state.shouldRender)
        assertEquals("WhatsApp", state.appDisplayName)
        assertEquals("Generic 'New notification' placeholder must be shown", "New notification", state.title)
        assertNull("Body content must strictly be null/hidden in APP_ONLY mode", state.body)
        assertNull("SubText must strictly be null/hidden in APP_ONLY mode", state.subText)
        assertEquals(NotificationPreviewMode.APP_ONLY, state.previewMode)
    }

    @Test
    fun testCardRespectsHiddenPreviewModeRendersNothing() {
        val state = NotificationCardResolver.resolve(
            packageName = "com.whatsapp",
            rawTitle = "Private Alert",
            rawText = "Sensitive message",
            rawSubText = "Secret",
            previewMode = NotificationPreviewMode.HIDDEN,
            appLabel = "WhatsApp"
        )

        assertFalse("HIDDEN mode must strictly have shouldRender = false", state.shouldRender)
        assertNull("Title must be null in HIDDEN mode", state.title)
        assertNull("Body must be null in HIDDEN mode", state.body)
        assertNull("SubText must be null in HIDDEN mode", state.subText)
        assertNull("FormattedTime must be null in HIDDEN mode", state.formattedTime)
    }

    @Test
    fun testCardTimestampFormatting() {
        val now = 10_000_000L

        // Under 1 minute -> NOW
        assertEquals("NOW", NotificationCardResolver.formatTimestamp(now - 15_000L, now))
        assertEquals("NOW", NotificationCardResolver.formatTimestamp(now, now))

        // Minutes ago
        assertEquals("1m ago", NotificationCardResolver.formatTimestamp(now - 60_000L, now))
        assertEquals("5m ago", NotificationCardResolver.formatTimestamp(now - 300_000L, now))
        assertEquals("45m ago", NotificationCardResolver.formatTimestamp(now - 2_700_000L, now))

        // Hours ago
        assertEquals("1h ago", NotificationCardResolver.formatTimestamp(now - 3_600_000L, now))
        assertEquals("3h ago", NotificationCardResolver.formatTimestamp(now - 10_800_000L, now))

        // Older than 24 hours
        assertEquals("RECENT", NotificationCardResolver.formatTimestamp(now - 90_000_000L, now))

        // Future or invalid timestamp
        assertEquals("NOW", NotificationCardResolver.formatTimestamp(now + 5_000L, now))
    }

    @Test
    fun testCardDirectEventBridging() {
        val event = IslandEvent.NotificationPosted(
            id = "0|com.twitter.android|1|null|1000",
            packageName = "com.twitter.android",
            title = "Nothing",
            text = "Introducing Phone (4a)",
            subText = "@nothing",
            timestamp = 5_000_000L,
            priority = IslandPriority.NOTIFICATION,
            timeoutMs = 4500L
        )

        val state = NotificationCardResolver.resolve(
            packageName = event.packageName,
            rawTitle = event.title,
            rawText = event.text,
            rawSubText = event.subText,
            timestamp = event.timestamp,
            previewMode = NotificationPreviewMode.FULL,
            appLabel = "X"
        )

        assertTrue(state.shouldRender)
        assertEquals("X", state.appDisplayName)
        assertEquals("Nothing", state.title)
        assertEquals("Introducing Phone (4a)", state.body)
        assertEquals("@nothing", state.subText)
    }

    @Test
    fun testFallbackAppDisplayNameFromPackageSuffix() {
        val state = NotificationCardResolver.resolve(
            packageName = "com.example.supercalculator",
            rawTitle = "Calculation finished",
            rawText = "42",
            appLabel = null
        )

        assertEquals("supercalculator", state.appDisplayName)
    }
}
