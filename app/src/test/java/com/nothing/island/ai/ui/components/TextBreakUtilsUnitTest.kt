package com.nothing.island.ai.ui.components

import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.notifications.NotificationPreviewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TextBreakUtilsUnitTest {

    @Test
    fun extractAvatarInitials_handlesEmojiAndMultiWordWithoutBrokenSurrogates() {
        assertEquals("RS", TextBreakUtils.extractAvatarInitials("🎧 Rohit Sharma"))
        assertEquals("🎧", TextBreakUtils.extractAvatarInitials("🎧🔥"))
        assertEquals("B", TextBreakUtils.extractAvatarInitials("bartholomew.fitzgerald"))
        assertEquals("BF", TextBreakUtils.extractAvatarInitials("Bartholomew Fitzgerald"))
        assertEquals("C", TextBreakUtils.extractAvatarInitials("   "))
        assertEquals("AW", TextBreakUtils.extractAvatarInitials("Aleksandra Wiśniewska-Kowalczyk"))
    }

    @Test
    fun extractAvatarInitials_handlesDevanagariWithoutBreakingClusters() {
        val initials = TextBreakUtils.extractAvatarInitials("अभिषेक शर्मा")
        assertEquals(2, initials.length)
        assertFalse(initials.any { it.isSurrogate() })
    }

    @Test
    fun extractSafeFallbackChar_skipsSurrogateEmojiAndReturnsValidChar() {
        assertEquals('R', TextBreakUtils.extractSafeFallbackChar("🎧🔥 Rohit", 'N'))
        assertEquals('N', TextBreakUtils.extractSafeFallbackChar("🎧🔥", 'N'))
        assertEquals('M', TextBreakUtils.extractSafeFallbackChar("", 'M'))
        val devanagariChar = TextBreakUtils.extractSafeFallbackChar("नया संदेश", 'N')
        assertFalse(devanagariChar.isSurrogate())
    }

    @Test
    fun notificationCardResolver_andEarbudFormatter_handleExtremeAndMissingData() {
        val ultraLongUnbreakable = "bartholomew_fitzgerald_superlong_identifier_without_any_spaces_at_all_1234567890"
        val resolved = NotificationCardResolver.resolve(
            packageName = "com.example.chat",
            rawTitle = "🎧 $ultraLongUnbreakable",
            rawText = "नया संदेश: कृपया तुरंत देखें 🔥🎧",
            appLabel = ultraLongUnbreakable,
            previewMode = NotificationPreviewMode.FULL,
            timestamp = 0L,
        )
        assertEquals(ultraLongUnbreakable, resolved.appDisplayName)
        assertNotNull(resolved.title)
        assertNull(resolved.formattedTime)

        val earbudDetail = formatEarbudBatteryDetailText(
            IslandEvent.BluetoothDeviceConnected(
                id = "eb_edge",
                deviceName = "🎧 Nothing_Ear_Pro_Ultra_Long_50_Char_Unbreakable_Device_Name_Test",
                deviceAddress = "AA:BB:CC:DD:EE:FF",
                batteryLevelPercentage = -1,
                leftBatteryPercentage = 100,
                rightBatteryPercentage = null,
                caseBatteryPercentage = 250,
            )
        )
        assertEquals("L 100%", earbudDetail)
    }
}
