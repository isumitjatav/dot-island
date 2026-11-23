package com.nothing.island.ai.switcher

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Enumeration of available Island UI interfaces managed by the Activity Switcher.
 */
enum class IslandInterface {
    /**
     * State A: Normal Dashboard (Time, Date, Compact Weather, Recent Alerts List).
     */
    DASHBOARD,

    /**
     * State B: Notification-Focused View (Active/Selected notification details & actions).
     */
    NOTIFICATION_FOCUSED,

    /**
     * State C: Music Player (Album art, track info, visualizer, transport controls, timeline, device output).
     */
    MUSIC,

    /**
     * State D: Battery & Charging Intelligence (Live charging animation, telemetry shelf, percentage).
     */
    BATTERY
}

/**
 * Direction of horizontal navigation gesture across the Island interface.
 */
enum class SwipeDirection {
    LEFT,  // Next available or forward interface
    RIGHT, // Previous visited interface
    DOWN,  // Open quick tools drawer
    UP     // Collapse overlay or dismiss drawer
}

/**
 * IslandActivitySwitcher
 *
 * Provides a shared history-based navigation foundation across Nothing Island interfaces.
 *
 * Rules:
 * - History-based navigation (not fixed circular modulo rotation).
 * - Swipe left: Advances to forward stack or next active candidate interface.
 * - Swipe right: Traverses backward to previous visited interface in back stack.
 * - Automatic pruning: Removes ended activities (e.g. stopped media, dismissed notification) from history.
 * - Interrupt preservation: Preserves navigation state when higher-priority notifications or calls interrupt,
 *   restoring the previous interface upon interrupt completion.
 * - Strict Invariant: Exactly ONE visible Island interface at a time (no secondary music pills).
 */
class IslandActivitySwitcher {

    companion object {
        private const val TAG = "IslandActivitySwitcher"
        private const val MAX_HISTORY_DEPTH = 10
    }

    private val _currentInterface = MutableStateFlow(IslandInterface.DASHBOARD)
    val currentInterface: StateFlow<IslandInterface> = _currentInterface.asStateFlow()

    // History-based stacks
    private val backStack = ArrayDeque<IslandInterface>()
    private val forwardStack = ArrayDeque<IslandInterface>()

    // Interruption preservation memory
    private var preInterruptInterface: IslandInterface? = null

    /**
     * Explicitly navigates to an interface, pushing current onto history back stack.
     */
    /**
     * Explicitly navigates to an interface, pushing current onto history back stack.
     */
    fun navigateTo(target: IslandInterface) {
        val current = _currentInterface.value
        if (current == target) return

        if (backStack.size >= MAX_HISTORY_DEPTH) {
            backStack.removeFirst()
        }
        backStack.addLast(current)
        forwardStack.clear()
        _currentInterface.value = target
        Log.d(TAG, "Navigated to: $target | BackStack: $backStack")
    }

    /**
     * Synchronizes the switcher's active interface with an external view change (e.g. user tap or auto-expand).
     */
    fun syncCurrentInterface(target: IslandInterface) {
        val current = _currentInterface.value
        if (current == target) return

        if (backStack.isEmpty() || backStack.last() != current) {
            if (backStack.size >= MAX_HISTORY_DEPTH) {
                backStack.removeFirst()
            }
            backStack.addLast(current)
        }
        forwardStack.clear()
        _currentInterface.value = target
        Log.d(TAG, "Synced current interface to: $target | BackStack: $backStack")
    }

    /**
     * Handles horizontal swipe gestures across the Nothing Island interface.
     *
     * @param direction Swipe direction (LEFT = Next / Forward, RIGHT = Previous / Backward).
     * @param availableCandidates Set of currently available active interfaces (e.g. DASHBOARD, MUSIC, NOTIFICATION).
     * @return True if navigation occurred, false if no alternate interface was reachable.
     */
    fun handleSwipe(direction: SwipeDirection, availableCandidates: Set<IslandInterface>): Boolean {
        pruneInvalidEntries(availableCandidates)
        val current = _currentInterface.value

        val standardOrder = listOf(
            IslandInterface.DASHBOARD,
            IslandInterface.NOTIFICATION_FOCUSED,
            IslandInterface.MUSIC,
            IslandInterface.BATTERY
        )
        val candidateList = standardOrder.filter { it in availableCandidates }.ifEmpty { availableCandidates.toList() }

        return when (direction) {
            SwipeDirection.RIGHT -> {
                // Navigate backward in history if available
                if (backStack.isNotEmpty()) {
                    val prev = backStack.removeLast()
                    forwardStack.addLast(current)
                    _currentInterface.value = prev
                    Log.d(TAG, "Swipe RIGHT (Back Stack) -> $prev | Forward: $forwardStack")
                    true
                } else {
                    // Fallback when back stack is empty: traverse to previous available candidate in reverse order
                    val currentIndex = candidateList.indexOf(current)
                    val prevCandidate = if (currentIndex >= 0 && candidateList.size > 1) {
                        candidateList[(currentIndex - 1 + candidateList.size) % candidateList.size]
                    } else {
                        candidateList.lastOrNull { it != current }
                    }

                    if (prevCandidate != null) {
                        forwardStack.addLast(current)
                        _currentInterface.value = prevCandidate
                        Log.d(TAG, "Swipe RIGHT (Candidate Fallback) -> $prevCandidate")
                        true
                    } else {
                        Log.d(TAG, "Swipe RIGHT ignored: no alternate candidates available")
                        false
                    }
                }
            }

            SwipeDirection.LEFT -> {
                // Navigate forward in history if available
                if (forwardStack.isNotEmpty()) {
                    val next = forwardStack.removeLast()
                    backStack.addLast(current)
                    _currentInterface.value = next
                    Log.d(TAG, "Swipe LEFT (Forward Stack) -> $next | Back: $backStack")
                    true
                } else {
                    // Look for next active candidate not currently displayed in ordered sequence
                    val currentIndex = candidateList.indexOf(current)
                    val nextCandidate = if (currentIndex >= 0 && candidateList.size > 1) {
                        candidateList[(currentIndex + 1) % candidateList.size]
                    } else {
                        candidateList.firstOrNull { it != current }
                    }

                    if (nextCandidate != null) {
                        navigateTo(nextCandidate)
                        Log.d(TAG, "Swipe LEFT (Next Candidate) -> $nextCandidate")
                        true
                    } else {
                        Log.d(TAG, "Swipe LEFT ignored: no further active candidates")
                        false
                    }
                }
            }

            SwipeDirection.DOWN,
            SwipeDirection.UP -> false // handled by controller as quick tools / collapse
        }
    }

    /**
     * Saves the current active interface before a higher-priority event takes over.
     */
    fun onInterruptStarted() {
        preInterruptInterface = _currentInterface.value
        Log.d(TAG, "Interruption started. Preserving state: $preInterruptInterface")
    }

    /**
     * Restores the preserved interface after a higher-priority interruption concludes.
     *
     * @param availableCandidates Currently active interfaces to ensure restored target is still valid.
     */
    fun onInterruptEnded(availableCandidates: Set<IslandInterface>) {
        val saved = preInterruptInterface
        preInterruptInterface = null

        if (saved != null && saved in availableCandidates) {
            _currentInterface.value = saved
            Log.d(TAG, "Interruption ended. Restored preserved interface: $saved")
        } else {
            // Default to DASHBOARD if preserved interface ended during interrupt
            _currentInterface.value = IslandInterface.DASHBOARD
            Log.d(TAG, "Interruption ended. Fallback to DASHBOARD")
        }
        pruneInvalidEntries(availableCandidates)
    }

    /**
     * Prunes ended activities from both back and forward stacks.
     */
    fun pruneInvalidEntries(activeCandidates: Set<IslandInterface>) {
        val current = _currentInterface.value

        backStack.removeAll { it !in activeCandidates && it != IslandInterface.DASHBOARD }
        forwardStack.removeAll { it !in activeCandidates && it != IslandInterface.DASHBOARD }

        if (current !in activeCandidates && current != IslandInterface.DASHBOARD) {
            val fallback = backStack.removeLastOrNull() ?: IslandInterface.DASHBOARD
            _currentInterface.value = fallback
            Log.i(TAG, "Current interface $current ended. Auto-fallback to $fallback")
        }
    }

    /**
     * Resets navigation state to base Dashboard.
     */
    fun resetToDashboard() {
        backStack.clear()
        forwardStack.clear()
        preInterruptInterface = null
        _currentInterface.value = IslandInterface.DASHBOARD
    }
}
