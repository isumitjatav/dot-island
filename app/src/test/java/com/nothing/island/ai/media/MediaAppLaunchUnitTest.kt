package com.nothing.island.ai.media

import android.content.Intent
import com.nothing.island.ai.ui.components.dispatchOpenMediaApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for media app launch intent resolution (FLAG_ACTIVITY_NEW_TASK +
 * FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) and MusicExpandedView open-app tap dispatch.
 */
class MediaAppLaunchUnitTest {

    @Test
    fun resolveMediaLaunchIntent_appliesNewTaskAndResetTaskFlags() {
        val fakeIntent = Intent()
        var queriedPkg: String? = null
        var appliedFlags = 0

        val resolved = MediaSessionTracker.resolveMediaLaunchIntentWith(
            packageName = "com.metrolist.music",
            lookupLaunchIntent = { pkg ->
                queriedPkg = pkg
                fakeIntent
            },
            applyFlags = { _, flags -> appliedFlags = flags }
        )

        assertNotNull(resolved)
        assertEquals("com.metrolist.music", queriedPkg)
        assertTrue(
            "Launch intent must include FLAG_ACTIVITY_NEW_TASK",
            (appliedFlags and Intent.FLAG_ACTIVITY_NEW_TASK) == Intent.FLAG_ACTIVITY_NEW_TASK
        )
        assertTrue(
            "Launch intent must include FLAG_ACTIVITY_RESET_TASK_IF_NEEDED",
            (appliedFlags and Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) == Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
    }

    @Test
    fun resolveMediaLaunchIntent_ignoresBlankPackageAndNullLaunchIntent() {
        var lookupCalled = false
        val blankResult = MediaSessionTracker.resolveMediaLaunchIntentWith(
            packageName = "   ",
            lookupLaunchIntent = {
                lookupCalled = true
                Intent()
            }
        )
        assertNull(blankResult)
        assertFalse(lookupCalled)

        val missingAppResult = MediaSessionTracker.resolveMediaLaunchIntentWith(
            packageName = "com.unknown.player",
            lookupLaunchIntent = { null }
        )
        assertNull(missingAppResult)
    }

    @Test
    fun musicExpandedView_trackAndHeaderTap_triggersOpenMediaAppCallback() {
        var controllerOpened = false
        var fallbackOpened = false

        dispatchOpenMediaApp(
            onOpenMediaApp = { controllerOpened = true },
            fallbackLaunch = { fallbackOpened = true }
        )

        assertTrue(controllerOpened)
        assertFalse(fallbackOpened)
    }

    @Test
    fun musicExpandedView_withoutCallback_usesFallbackLaunch() {
        var fallbackOpened = false

        dispatchOpenMediaApp(
            onOpenMediaApp = null,
            fallbackLaunch = { fallbackOpened = true }
        )

        assertTrue(fallbackOpened)
    }
}
