package com.nothing.island.ai.gestures

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IslandGestureDetectorUnitTest {

    private var toggleExpandCalled = false
    private var clearAllCalled = false
    private lateinit var executor: IslandGestureActionExecutor

    @Before
    fun setUp() {
        toggleExpandCalled = false
        clearAllCalled = false

        val context = object : android.content.ContextWrapper(null) {}
        executor = IslandGestureActionExecutor(
            context = context,
            torchHandler = null,
            onToggleExpand = { toggleExpandCalled = true },
            onClearAllNotifications = { clearAllCalled = true }
        )
    }

    @Test
    fun execute_expandCollapse_invokesCallback() {
        assertFalse(toggleExpandCalled)
        executor.execute(GestureAction.EXPAND_COLLAPSE)
        assertTrue(toggleExpandCalled)
    }

    @Test
    fun execute_clearAll_invokesCallback() {
        assertFalse(clearAllCalled)
        executor.execute(GestureAction.CLEAR_ALL)
        assertTrue(clearAllCalled)
    }

    @Test
    fun execute_none_doesNotTriggerCallbacks() {
        executor.execute(GestureAction.NONE)
        assertFalse(toggleExpandCalled)
        assertFalse(clearAllCalled)
    }

    @Test
    fun gestureActionEnum_containsAllEightStandardActions() {
        val actions = GestureAction.values()
        assertEquals(8, actions.size)
        assertTrue(actions.contains(GestureAction.NONE))
        assertTrue(actions.contains(GestureAction.EXPAND_COLLAPSE))
        assertTrue(actions.contains(GestureAction.NOTIFICATIONS_HUB))
        assertTrue(actions.contains(GestureAction.TORCH))
        assertTrue(actions.contains(GestureAction.CAMERA))
        assertTrue(actions.contains(GestureAction.SETTINGS))
        assertTrue(actions.contains(GestureAction.SCREENSHOT))
        assertTrue(actions.contains(GestureAction.CLEAR_ALL))
    }

    @Test
    fun notchDisappearModeEnum_containsExpectedModes() {
        val modes = NotchDisappearMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(NotchDisappearMode.AUTO))
        assertTrue(modes.contains(NotchDisappearMode.NEVER))
        assertTrue(modes.contains(NotchDisappearMode.ON_READ))
    }
}
