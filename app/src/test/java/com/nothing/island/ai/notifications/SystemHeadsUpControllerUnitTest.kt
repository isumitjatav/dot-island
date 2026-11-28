package com.nothing.island.ai.notifications

import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SystemHeadsUpControllerUnitTest
 *
 * Verifies SystemHeadsUpController logic:
 * - Permission checking (granted vs denied)
 * - Mute state reading
 * - Setting mute state to true (0) and false (1)
 * - Automatic safety restoration (restoreSystemHeadsUp -> 1)
 * - Behavior when permission is missing (safe no-op, returns false)
 * - Intent creation for fallback settings
 */
class SystemHeadsUpControllerUnitTest {

    private fun createMockContext(): Context {
        return object : ContextWrapper(null) {
            override fun getPackageName(): String = "com.nothing.island.ai"
        }
    }

    @Test
    fun testHasSecureSettingsPermissionWhenGranted() {
        val context = createMockContext()
        val controller = SystemHeadsUpController(
            context = context,
            permissionChecker = { true }
        )

        assertTrue("Should return true when permission checker returns true", controller.hasSecureSettingsPermission())
    }

    @Test
    fun testHasSecureSettingsPermissionWhenDenied() {
        val context = createMockContext()
        val controller = SystemHeadsUpController(
            context = context,
            permissionChecker = { false }
        )

        assertFalse("Should return false when permission checker returns false", controller.hasSecureSettingsPermission())
    }

    @Test
    fun testIsSystemHeadsUpMutedWhenSettingIsZero() {
        val context = createMockContext()
        val controller = SystemHeadsUpController(
            context = context,
            settingsReader = { 0 }
        )

        assertTrue("Should return true (muted) when setting value is 0", controller.isSystemHeadsUpMuted())
    }

    @Test
    fun testIsSystemHeadsUpMutedWhenSettingIsOne() {
        val context = createMockContext()
        val controller = SystemHeadsUpController(
            context = context,
            settingsReader = { 1 }
        )

        assertFalse("Should return false (unmuted) when setting value is 1", controller.isSystemHeadsUpMuted())
    }

    @Test
    fun testSetSystemHeadsUpMutedWritesZeroWhenMuted() {
        val context = createMockContext()
        var writtenValue: Int? = null

        val controller = SystemHeadsUpController(
            context = context,
            permissionChecker = { true },
            settingsWriter = { _, value ->
                writtenValue = value
                true
            }
        )

        val result = controller.setSystemHeadsUpMuted(true)

        assertTrue("Write should succeed when permission is held", result)
        assertEquals("Muting heads-up must write 0 to Settings.Global", 0, writtenValue)
    }

    @Test
    fun testSetSystemHeadsUpMutedWritesOneWhenUnmuted() {
        val context = createMockContext()
        var writtenValue: Int? = null

        val controller = SystemHeadsUpController(
            context = context,
            permissionChecker = { true },
            settingsWriter = { _, value ->
                writtenValue = value
                true
            }
        )

        val result = controller.setSystemHeadsUpMuted(false)

        assertTrue("Write should succeed when permission is held", result)
        assertEquals("Unmuting heads-up must write 1 to Settings.Global", 1, writtenValue)
    }

    @Test
    fun testSetSystemHeadsUpMutedFailsSafelyWithoutPermission() {
        val context = createMockContext()
        var writeAttempted = false

        val controller = SystemHeadsUpController(
            context = context,
            permissionChecker = { false },
            settingsWriter = { _, _ ->
                writeAttempted = true
                true
            }
        )

        val result = controller.setSystemHeadsUpMuted(true)

        assertFalse("Should return false without WRITE_SECURE_SETTINGS permission", result)
        assertFalse("Should not attempt to write setting without permission", writeAttempted)
    }

    @Test
    fun testRestoreSystemHeadsUpAlwaysWritesOne() {
        val context = createMockContext()
        var writtenValue: Int? = null

        val controller = SystemHeadsUpController(
            context = context,
            permissionChecker = { true },
            settingsWriter = { _, value ->
                writtenValue = value
                true
            }
        )

        val result = controller.restoreSystemHeadsUp()

        assertTrue("Restoration should succeed when permission is held", result)
        assertEquals("Safety restoration must strictly write 1 (enabled)", 1, writtenValue)
    }

    @Test
    fun testRestoreSystemHeadsUpFailsSafelyWithoutPermission() {
        val context = createMockContext()
        var writeAttempted = false

        val controller = SystemHeadsUpController(
            context = context,
            permissionChecker = { false },
            settingsWriter = { _, _ ->
                writeAttempted = true
                true
            }
        )

        val result = controller.restoreSystemHeadsUp()

        assertFalse("Restoration should return false when permission is missing", result)
        assertFalse("Should not attempt write without permission", writeAttempted)
    }

    @Test
    fun testIntentCreationHelpers() {
        val context = createMockContext()
        val controller = SystemHeadsUpController(context)

        val phoneIntent = controller.createPhoneNotificationSettingsIntent()
        assertNotNull(phoneIntent)

        val messagesIntent = controller.createMessagesNotificationSettingsIntent()
        assertNotNull(messagesIntent)

        val generalIntent = controller.createGeneralNotificationSettingsIntent()
        assertNotNull(generalIntent)
    }
}