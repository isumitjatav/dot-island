package com.nothing.island.ai.notifications

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * NotificationActionBridgeUnitTest
 *
 * Verifies decoupled action registration, stable key lookup, bounds checking,
 * safe execution fallback, and memory cache cleanup.
 */
class NotificationActionBridgeUnitTest {

    @Before
    fun setUp() {
        NotificationActionBridge.clearAll()
    }

    @After
    fun tearDown() {
        NotificationActionBridge.clearAll()
    }

    @Test
    fun testInitialCacheIsEmpty() {
        assertEquals(0, NotificationActionBridge.getCachedCount())
    }

    @Test
    fun testRegisterAndLookup() {
        val key = "0|com.whatsapp|100|null|1001"
        val actions = listOf(
            NotificationActionItem(title = "Reply", actionIntent = null),
            NotificationActionItem(title = "Mark as read", actionIntent = null)
        )

        NotificationActionBridge.register(
            key = key,
            packageName = "com.whatsapp",
            contentIntent = null,
            actions = actions
        )

        assertEquals(1, NotificationActionBridge.getCachedCount())
        val cached = NotificationActionBridge.get(key)
        assertNotNull(cached)
        assertEquals(key, cached?.key)
        assertEquals("com.whatsapp", cached?.packageName)
        assertEquals(2, cached?.actions?.size)
        assertEquals("Reply", cached?.actions?.get(0)?.title)
    }

    @Test
    fun testUnregisterRemovesKey() {
        val key = "0|com.example|1|null|1002"
        NotificationActionBridge.register(
            key = key,
            packageName = "com.example",
            contentIntent = null,
            actions = listOf(NotificationActionItem("Open", null))
        )
        assertEquals(1, NotificationActionBridge.getCachedCount())

        NotificationActionBridge.unregister(key)
        assertEquals(0, NotificationActionBridge.getCachedCount())
        assertNull(NotificationActionBridge.get(key))
    }

    @Test
    fun testClearAll() {
        for (i in 1..5) {
            NotificationActionBridge.register(
                key = "key_$i",
                packageName = "pkg_$i",
                contentIntent = null,
                actions = listOf(NotificationActionItem("Action_$i", null))
            )
        }
        assertEquals(5, NotificationActionBridge.getCachedCount())

        NotificationActionBridge.clearAll()
        assertEquals(0, NotificationActionBridge.getCachedCount())
    }

    @Test
    fun testExecuteContentIntentNullSafety() {
        // Calling executeContentIntent when unregistered or null returns false without throwing
        assertFalse(NotificationActionBridge.executeContentIntent("non_existent_key"))

        NotificationActionBridge.register("null_intent_key", "pkg", null, emptyList())
        assertFalse(NotificationActionBridge.executeContentIntent("null_intent_key"))
    }

    @Test
    fun testExecuteActionInvalidIndexReturnsFalse() {
        val key = "key_test_action"
        NotificationActionBridge.register(
            key = key,
            packageName = "pkg",
            contentIntent = null,
            actions = listOf(NotificationActionItem("Archive", null))
        )

        assertFalse("Negative index should return false", NotificationActionBridge.executeAction(key, -1))
        assertFalse("Out of bounds index should return false", NotificationActionBridge.executeAction(key, 5))
        assertFalse("Null action intent should return false", NotificationActionBridge.executeAction(key, 0))
    }

    @Test
    fun testUnregisterIfTokenMatchesReleasesMatchingToken() {
        val key = "key_token_match"
        val token = 1000L
        NotificationActionBridge.register(
            key = key,
            packageName = "com.test",
            contentIntent = null,
            actions = listOf(NotificationActionItem("Action", null)),
            token = token
        )

        assertEquals(1, NotificationActionBridge.getCachedCount())

        // Calling unregisterIfTokenMatches with the same token releases it
        val unregistered = NotificationActionBridge.unregisterIfTokenMatches(key, token)
        assertTrue("Matching token must successfully unregister", unregistered)
        assertEquals(0, NotificationActionBridge.getCachedCount())
        assertNull(NotificationActionBridge.get(key))
    }

    @Test
    fun testUnregisterIfTokenMatchesPreservesNewerToken() {
        val key = "key_token_preserve"
        val oldToken = 1000L
        val newToken = 2000L

        // Initial notification
        NotificationActionBridge.register(
            key = key,
            packageName = "com.test",
            contentIntent = null,
            actions = listOf(NotificationActionItem("Old Action", null)),
            token = oldToken
        )

        // Notification is updated with new content and newer token
        NotificationActionBridge.register(
            key = key,
            packageName = "com.test",
            contentIntent = null,
            actions = listOf(NotificationActionItem("New Action", null)),
            token = newToken
        )

        // Attempting to unregister with oldToken must fail and preserve newToken
        val unregistered = NotificationActionBridge.unregisterIfTokenMatches(key, oldToken)
        assertFalse("Old token must not unregister newer notification update", unregistered)
        assertEquals(1, NotificationActionBridge.getCachedCount())

        val current = NotificationActionBridge.get(key)
        assertNotNull(current)
        assertEquals(newToken, current?.token)
        assertEquals("New Action", current?.actions?.firstOrNull()?.title)
    }

    @Test
    fun testPruneOlderThan() {
        val now = 100000L
        NotificationActionBridge.register("old_1", "pkg", null, emptyList(), token = now - 50000L)
        NotificationActionBridge.register("old_2", "pkg", null, emptyList(), token = now - 40000L)
        NotificationActionBridge.register("recent_1", "pkg", null, emptyList(), token = now - 5000L)

        assertEquals(3, NotificationActionBridge.getCachedCount())

        // Prune entries older than 30000ms
        val pruned = NotificationActionBridge.pruneOlderThan(maxAgeMs = 30000L, currentTimeMs = now)
        assertEquals(2, pruned)
        assertEquals(1, NotificationActionBridge.getCachedCount())
        assertNotNull(NotificationActionBridge.get("recent_1"))
        assertNull(NotificationActionBridge.get("old_1"))
        assertNull(NotificationActionBridge.get("old_2"))
    }
}
