package com.nothing.island.ai.notifications

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * IslandNotificationListenerUnitTest
 *
 * Verifies Milestone R6 Phase 1 headless notification listener service lifecycle,
 * connection status reporting, silent reconciliation tracking, and safe disconnection cleanup.
 */
class IslandNotificationListenerUnitTest {

    private lateinit var service: IslandNotificationListenerService

    @Before
    fun setUp() {
        service = IslandNotificationListenerService()
        service.cleanupListenerState()
    }

    @After
    fun tearDown() {
        service.cleanupListenerState()
    }

    @Test
    fun testInitialServiceStateIsDisconnected() {
        assertFalse("Service should be disconnected initially", IslandNotificationListenerService.isConnected)
        assertNull("Instance should be null initially", IslandNotificationListenerService.instance)
        assertFalse("isServiceBound should be false", IslandNotificationListenerService.isServiceBound)
        assertFalse("connectionState Flow should emit false", IslandNotificationListenerService.connectionState.value)
        assertEquals(0, service.reconciledHistoricalCount)
    }

    @Test
    fun testSimulatedConnectionUpdatesStateFlowAndFlags() {
        service.setConnectedForTesting(true)

        assertTrue("isConnected should be true after connection", IslandNotificationListenerService.isConnected)
        assertEquals("instance should match current service", service, IslandNotificationListenerService.instance)
        assertTrue("isServiceBound should be true", IslandNotificationListenerService.isServiceBound)
        assertTrue("connectionState Flow should emit true", IslandNotificationListenerService.connectionState.value)
    }

    @Test
    fun testSilentReconciliationIndexesHistoricalKeysWithoutFlooding() {
        service.setConnectedForTesting(true)

        // Simulate 3 historical status bar notifications present at connection time
        val key1 = "0|com.whatsapp|101|null|10001"
        val key2 = "0|org.telegram.messenger|202|null|10002"
        val key3 = "0|com.google.android.gm|303|null|10003"

        service.addHistoricalKeyForTesting(key1)
        service.addHistoricalKeyForTesting(key2)
        service.addHistoricalKeyForTesting(key3)

        assertEquals("Should index exactly 3 historical notifications", 3, service.reconciledHistoricalCount)
        assertTrue("key1 should be recognized as historical", service.isHistorical(key1))
        assertTrue("key2 should be recognized as historical", service.isHistorical(key2))
        assertTrue("key3 should be recognized as historical", service.isHistorical(key3))

        // A new alert arriving post-connection is NOT historical
        val newKey = "0|com.whatsapp|102|null|10004"
        assertFalse("New alert key should not be historical", service.isHistorical(newKey))
    }

    @Test
    fun testHistoricalKeyRemovalUponNotificationDismissal() {
        val key = "0|com.whatsapp|101|null|10001"
        service.addHistoricalKeyForTesting(key)
        assertEquals(1, service.reconciledHistoricalCount)

        // Disconnection/cleanup or manual removal clears historical record
        service.clearHistoricalKeysForTesting()
        assertEquals(0, service.reconciledHistoricalCount)
        assertFalse(service.isHistorical(key))
    }

    @Test
    fun testDisconnectionCleansUpStateAndCachesSafely() {
        service.setConnectedForTesting(true)
        service.addHistoricalKeyForTesting("0|com.example|1|null|100")
        assertEquals(1, service.reconciledHistoricalCount)

        // Trigger disconnection cleanup
        service.cleanupListenerState()

        assertFalse("isConnected should be false after cleanup", IslandNotificationListenerService.isConnected)
        assertNull("instance should be null after cleanup", IslandNotificationListenerService.instance)
        assertFalse("isServiceBound should be false", IslandNotificationListenerService.isServiceBound)
        assertFalse("connectionState Flow should be false", IslandNotificationListenerService.connectionState.value)
        assertEquals("Historical cache should be empty after cleanup", 0, service.reconciledHistoricalCount)
    }

    @Test
    fun testRepeatedConnectionCyclesAreIdempotentAndSafe() {
        for (i in 1..5) {
            service.setConnectedForTesting(true)
            assertTrue(IslandNotificationListenerService.isConnected)
            assertEquals(service, IslandNotificationListenerService.instance)

            service.cleanupListenerState()
            assertFalse(IslandNotificationListenerService.isConnected)
            assertNull(IslandNotificationListenerService.instance)
        }
    }

    @Test
    fun testCancelSystemNotificationWhenDisconnectedReturnsFalseWithoutCrashing() {
        assertNull(IslandNotificationListenerService.instance)
        val result = IslandNotificationListenerService.cancelSystemNotification("0|com.example|1|null|100")
        assertFalse("Calling cancelSystemNotification when unbound should return false safely", result)
    }
}
