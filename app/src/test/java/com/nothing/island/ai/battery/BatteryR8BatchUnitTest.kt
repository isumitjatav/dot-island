package com.nothing.island.ai.battery

import com.nothing.island.ai.ExpandedViewType
import com.nothing.island.ai.IslandMode
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandPriority
import com.nothing.island.ai.engine.IslandStateRepository
import com.nothing.island.ai.switcher.IslandActivitySwitcher
import com.nothing.island.ai.switcher.IslandInterface
import com.nothing.island.ai.switcher.SwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BatteryR8BatchUnitTest
 *
 * Comprehensive JVM unit tests for Milestone R8: Battery & Charging Intelligence.
 * Covers models, telemetry calculations, honesty invariants, ETA estimation,
 * auto-expansion policies, state transitions, and activity switcher arbitration.
 */
class BatteryR8BatchUnitTest {

    // =========================================================================
    // 1. BATTERY ENUMS & MODEL MAPPING TESTS
    // =========================================================================

    @Test
    fun testChargingSourceMapping() {
        assertEquals(ChargingSource.AC, ChargingSource.fromPlugged(1))
        assertEquals(ChargingSource.USB, ChargingSource.fromPlugged(2))
        assertEquals(ChargingSource.WIRELESS, ChargingSource.fromPlugged(4))
        assertEquals(ChargingSource.DOCK, ChargingSource.fromPlugged(8))
        assertEquals(ChargingSource.NONE, ChargingSource.fromPlugged(0))
        assertEquals(ChargingSource.UNKNOWN, ChargingSource.fromPlugged(99))
    }

    @Test
    fun testChargingStatusMapping() {
        assertEquals(ChargingStatus.CHARGING, ChargingStatus.fromStatus(2))
        assertEquals(ChargingStatus.DISCHARGING, ChargingStatus.fromStatus(3))
        assertEquals(ChargingStatus.NOT_CHARGING, ChargingStatus.fromStatus(4))
        assertEquals(ChargingStatus.FULL, ChargingStatus.fromStatus(5))
        assertEquals(ChargingStatus.UNKNOWN, ChargingStatus.fromStatus(1))
        assertEquals(ChargingStatus.UNKNOWN, ChargingStatus.fromStatus(999))
    }

    @Test
    fun testBatteryHealthMapping() {
        assertEquals(BatteryHealth.GOOD, BatteryHealth.fromHealth(2))
        assertEquals(BatteryHealth.OVERHEAT, BatteryHealth.fromHealth(3))
        assertEquals(BatteryHealth.DEAD, BatteryHealth.fromHealth(4))
        assertEquals(BatteryHealth.OVER_VOLTAGE, BatteryHealth.fromHealth(5))
        assertEquals(BatteryHealth.UNSPECIFIED_FAILURE, BatteryHealth.fromHealth(6))
        assertEquals(BatteryHealth.COLD, BatteryHealth.fromHealth(7))
        assertEquals(BatteryHealth.UNKNOWN, BatteryHealth.fromHealth(1))
    }

    // =========================================================================
    // 2. BATTERY STATE & HONEST TELEMETRY INVARIANT TESTS
    // =========================================================================

    @Test
    fun testBatteryPercentageComputation() {
        val state1 = BatteryState(level = 85, scale = 100)
        assertEquals(85, state1.percentage)

        val state2 = BatteryState(level = 42, scale = 50)
        assertEquals(84, state2.percentage)

        val stateClamp = BatteryState(level = 120, scale = 100)
        assertEquals(100, stateClamp.percentage)
    }

    @Test
    fun testWattageHonestyInvariantWhenNotCharging() {
        val dischargingState = BatteryState(
            isCharging = false,
            voltageMv = 4400,
            currentMicroAmps = 500000
        )
        assertNull("Wattage must be null when discharging", dischargingState.wattageW)
        assertEquals("--", dischargingState.formattedWattage)
    }

    @Test
    fun testWattageHonestyInvariantWhenSensorsZeroOrInvalid() {
        val zeroVoltageState = BatteryState(
            isCharging = true,
            voltageMv = 0,
            currentMicroAmps = 2000000
        )
        assertNull("Wattage must be null when voltage is 0", zeroVoltageState.wattageW)

        val zeroCurrentState = BatteryState(
            isCharging = true,
            voltageMv = 4400,
            currentMicroAmps = 0
        )
        assertNull("Wattage must be null when current is 0", zeroCurrentState.wattageW)
    }

    @Test
    fun testWattageComputationAccurateOnValidTelemetry() {
        // Nothing Phone (4a) verified readings: 4436mV, 2,000,000uA
        // P = (4436 * 2000000) / 10^9 = 8.872 W
        val chargingState = BatteryState(
            isCharging = true,
            voltageMv = 4436,
            currentMicroAmps = 2000000
        )
        val wattage = chargingState.wattageW
        assertNotNull(wattage)
        assertTrue("Wattage should be ~8.87W", wattage!! in 8.8f..8.9f)
        assertEquals("8.9W", chargingState.formattedWattage)
    }

    @Test
    fun testFormattedTelemetryOutputs() {
        val state = BatteryState(
            voltageMv = 4436,
            temperatureC = 35.0f,
            source = ChargingSource.AC
        )
        assertEquals("4.44V", state.formattedVoltage)
        assertEquals("35.0°C", state.formattedTemperature)
        assertEquals("AC CHARGER", state.sourceLabel)
    }

    @Test
    fun testFormattedEtaRepresentations() {
        val stateWithShortEta = BatteryState(chargeTimeRemainingMs = 45000L) // 45 seconds
        assertEquals("< 1 min", stateWithShortEta.formattedEta)

        val stateWithMinEta = BatteryState(chargeTimeRemainingMs = 1800000L) // 30 mins
        assertEquals("30 min", stateWithMinEta.formattedEta)

        val stateWithHourEta = BatteryState(chargeTimeRemainingMs = 5400000L) // 1h 30m
        assertEquals("1h 30m", stateWithHourEta.formattedEta)

        val stateWithNoEta = BatteryState(chargeTimeRemainingMs = null)
        assertEquals("--", stateWithNoEta.formattedEta)
    }

    // =========================================================================
    // 3. BATTERY ETA ESTIMATOR (ROLLING REGRESSION) TESTS
    // =========================================================================

    @Test
    fun testEtaEstimatorReturnsZeroWhenFull() {
        val estimator = BatteryEtaEstimator()
        val eta = estimator.estimateRemainingMs(currentPercentage = 100)
        assertEquals(0L, eta)
    }

    @Test
    fun testEtaEstimatorReturnsNullWhenInsufficientSamples() {
        val estimator = BatteryEtaEstimator()
        estimator.addSample(50, timestampMs = 1000L)
        assertNull("Should be null with single sample", estimator.estimateRemainingMs(50, nowMs = 2000L))
    }

    @Test
    fun testEtaEstimatorReturnsNullWhenObservationWindowTooShort() {
        val estimator = BatteryEtaEstimator(minObservationWindowMs = 60000L)
        estimator.addSample(50, timestampMs = 1000L)
        estimator.addSample(51, timestampMs = 30000L) // Only 29 seconds
        assertNull("Should be null when window < 60s", estimator.estimateRemainingMs(51, nowMs = 31000L))
    }

    @Test
    fun testEtaEstimatorCalculatesAccurateLinearRegression() {
        val estimator = BatteryEtaEstimator(minObservationWindowMs = 60000L)
        // Simulate 1% gain every 60 seconds (1 minute per 1%)
        estimator.addSample(80, timestampMs = 0L)
        estimator.addSample(81, timestampMs = 60000L)
        estimator.addSample(82, timestampMs = 120000L)

        // At 82%, remaining 18% should take ~18 minutes (1,080,000 ms)
        val etaMs = estimator.estimateRemainingMs(82, nowMs = 120000L)
        assertNotNull(etaMs)
        val minutes = etaMs!! / 60000
        assertEquals(18L, minutes)
    }

    @Test
    fun testEtaEstimatorResetsCleanly() {
        val estimator = BatteryEtaEstimator()
        estimator.addSample(80, timestampMs = 0L)
        estimator.addSample(82, timestampMs = 120000L)
        estimator.reset()
        assertNull("After reset, ETA should be null", estimator.estimateRemainingMs(82, nowMs = 130000L))
    }

    // =========================================================================
    // 4. BATTERY AUTO-EXPANSION POLICY TESTS
    // =========================================================================

    @Test
    fun testPolicySkipsWhenMasterBatteryDisabled() {
        val outcome = BatteryAutoExpansionPolicy.evaluateExpansion(
            event = IslandEvent.ChargingConnected(id = "c1", batteryLevel = 80, chargeType = "AC"),
            presentationId = "pres_1",
            lastHandledPresentationId = null,
            mode = IslandMode.COLLAPSED,
            expandedViewType = ExpandedViewType.DASHBOARD,
            preferences = BatteryPreferences(enabled = false),
            isUserInteracting = false
        )
        assertTrue(outcome is BatteryAutoExpansionPolicy.ExpansionOutcome.Skip)
        assertEquals(BatteryAutoExpansionPolicy.Reasons.BATTERY_DISABLED, (outcome as BatteryAutoExpansionPolicy.ExpansionOutcome.Skip).reason)
    }

    @Test
    fun testPolicySkipsWhenAutoExpandOnPluggedDisabled() {
        val outcome = BatteryAutoExpansionPolicy.evaluateExpansion(
            event = IslandEvent.ChargingConnected(id = "c1", batteryLevel = 80, chargeType = "AC"),
            presentationId = "pres_1",
            lastHandledPresentationId = null,
            mode = IslandMode.COLLAPSED,
            expandedViewType = ExpandedViewType.DASHBOARD,
            preferences = BatteryPreferences(enabled = true, autoExpandOnPlugged = false),
            isUserInteracting = false
        )
        assertTrue(outcome is BatteryAutoExpansionPolicy.ExpansionOutcome.Skip)
        assertEquals(BatteryAutoExpansionPolicy.Reasons.AUTO_EXPAND_DISABLED, (outcome as BatteryAutoExpansionPolicy.ExpansionOutcome.Skip).reason)
    }

    @Test
    fun testPolicyExpandsOnPluggedWithClampedDuration() {
        val outcome = BatteryAutoExpansionPolicy.evaluateExpansion(
            event = IslandEvent.ChargingConnected(id = "c1", batteryLevel = 80, chargeType = "AC"),
            presentationId = "pres_1",
            lastHandledPresentationId = null,
            mode = IslandMode.COLLAPSED,
            expandedViewType = ExpandedViewType.DASHBOARD,
            preferences = BatteryPreferences(enabled = true, autoExpandOnPlugged = true, displayDurationMs = 4500L),
            isUserInteracting = false
        )
        assertTrue(outcome is BatteryAutoExpansionPolicy.ExpansionOutcome.Expand)
        assertEquals(4500L, (outcome as BatteryAutoExpansionPolicy.ExpansionOutcome.Expand).durationMs)
    }

    @Test
    fun testPolicyExpandsOnLowBatterySafetyAlert() {
        val outcome = BatteryAutoExpansionPolicy.evaluateExpansion(
            event = IslandEvent.BatteryStateChanged(id = "low_1", batteryLevel = 15, isCharging = false, isLowBattery = true),
            presentationId = "pres_low",
            lastHandledPresentationId = null,
            mode = IslandMode.COLLAPSED,
            expandedViewType = ExpandedViewType.DASHBOARD,
            preferences = BatteryPreferences(enabled = true),
            isUserInteracting = false
        )
        assertTrue(outcome is BatteryAutoExpansionPolicy.ExpansionOutcome.Expand)
    }

    @Test
    fun testPolicyExpandsOnOverheatSafetyAlert() {
        val outcome = BatteryAutoExpansionPolicy.evaluateExpansion(
            event = IslandEvent.BatteryWarning(id = "hot_1", batteryLevel = 80, temperatureC = 46.5f, isOverheat = true),
            presentationId = "pres_hot",
            lastHandledPresentationId = null,
            mode = IslandMode.COLLAPSED,
            expandedViewType = ExpandedViewType.DASHBOARD,
            preferences = BatteryPreferences(enabled = true),
            isUserInteracting = false
        )
        assertTrue(outcome is BatteryAutoExpansionPolicy.ExpansionOutcome.Expand)
    }

    @Test
    fun testPolicySuppressesDuplicatePresentation() {
        val outcome = BatteryAutoExpansionPolicy.evaluateExpansion(
            event = IslandEvent.ChargingConnected(id = "c1", batteryLevel = 80, chargeType = "AC"),
            presentationId = "pres_1",
            lastHandledPresentationId = "pres_1",
            mode = IslandMode.COLLAPSED,
            expandedViewType = ExpandedViewType.DASHBOARD,
            preferences = BatteryPreferences(enabled = true),
            isUserInteracting = false
        )
        assertTrue(outcome is BatteryAutoExpansionPolicy.ExpansionOutcome.Skip)
        assertEquals(BatteryAutoExpansionPolicy.Reasons.ALREADY_HANDLED, (outcome as BatteryAutoExpansionPolicy.ExpansionOutcome.Skip).reason)
    }

    @Test
    fun testPolicySuppressesWhenUserInteracting() {
        val outcome = BatteryAutoExpansionPolicy.evaluateExpansion(
            event = IslandEvent.ChargingConnected(id = "c1", batteryLevel = 80, chargeType = "AC"),
            presentationId = "pres_1",
            lastHandledPresentationId = null,
            mode = IslandMode.COLLAPSED,
            expandedViewType = ExpandedViewType.DASHBOARD,
            preferences = BatteryPreferences(enabled = true),
            isUserInteracting = true
        )
        assertTrue(outcome is BatteryAutoExpansionPolicy.ExpansionOutcome.Skip)
        assertEquals(BatteryAutoExpansionPolicy.Reasons.USER_INTERACTING, (outcome as BatteryAutoExpansionPolicy.ExpansionOutcome.Skip).reason)
    }

    @Test
    fun testPolicyCollapseEvaluation() {
        // Safe to collapse
        val canCollapse = BatteryAutoExpansionPolicy.evaluateCollapse(
            currentPresentationId = "pres_1",
            expectedPresentationId = "pres_1",
            mode = IslandMode.EXPANDED,
            expandedViewType = ExpandedViewType.BATTERY,
            isUserInteracting = false,
            hasActiveBatteryEvent = true
        )
        assertTrue("Should allow collapse", canCollapse)

        // Block collapse if presentation changed
        assertFalse(
            BatteryAutoExpansionPolicy.evaluateCollapse(
                currentPresentationId = "pres_other",
                expectedPresentationId = "pres_1",
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.BATTERY,
                isUserInteracting = false,
                hasActiveBatteryEvent = true
            )
        )

        // Block collapse if user interacting
        assertFalse(
            BatteryAutoExpansionPolicy.evaluateCollapse(
                currentPresentationId = "pres_1",
                expectedPresentationId = "pres_1",
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.BATTERY,
                isUserInteracting = true,
                hasActiveBatteryEvent = true
            )
        )

        // Block collapse if user navigated away from BATTERY
        assertFalse(
            BatteryAutoExpansionPolicy.evaluateCollapse(
                currentPresentationId = "pres_1",
                expectedPresentationId = "pres_1",
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.MUSIC,
                isUserInteracting = false,
                hasActiveBatteryEvent = true
            )
        )
    }

    // =========================================================================
    // 5. ISLAND STATE REPOSITORY & ACTIVITY SWITCHER INTEGRATION
    // =========================================================================

    @Test
    fun testIslandStateRepositoryBatteryTransitions() {
        val repo = IslandStateRepository()
        assertEquals(IslandMode.COLLAPSED, repo.state.value.mode)

        repo.expandBattery(auto = true)
        assertEquals(IslandMode.EXPANDED, repo.state.value.mode)
        assertEquals(ExpandedViewType.BATTERY, repo.state.value.expandedViewType)
        assertTrue(repo.state.value.isAutoExpanded)

        repo.collapse()
        assertEquals(IslandMode.COLLAPSED, repo.state.value.mode)
        assertFalse(repo.state.value.isAutoExpanded)
    }

    @Test
    fun testActivitySwitcherNavigatesToAndFromBattery() {
        val switcher = IslandActivitySwitcher()
        val candidates = setOf(IslandInterface.DASHBOARD, IslandInterface.MUSIC, IslandInterface.BATTERY)

        switcher.syncCurrentInterface(IslandInterface.DASHBOARD)
        assertEquals(IslandInterface.DASHBOARD, switcher.currentInterface.value)

        // Navigate to BATTERY
        switcher.navigateTo(IslandInterface.BATTERY)
        assertEquals(IslandInterface.BATTERY, switcher.currentInterface.value)

        // Swipe RIGHT goes backward to DASHBOARD
        val handled = switcher.handleSwipe(SwipeDirection.RIGHT, candidates)
        assertTrue(handled)
        assertEquals(IslandInterface.DASHBOARD, switcher.currentInterface.value)

        // Swipe LEFT goes forward to BATTERY
        val handledForward = switcher.handleSwipe(SwipeDirection.LEFT, candidates)
        assertTrue(handledForward)
        assertEquals(IslandInterface.BATTERY, switcher.currentInterface.value)
    }

    @Test
    fun testActivitySwitcherMultiCandidateCycling() {
        val switcher = IslandActivitySwitcher()
        val candidates = setOf(IslandInterface.DASHBOARD, IslandInterface.NOTIFICATION_FOCUSED, IslandInterface.BATTERY)

        assertEquals(IslandInterface.DASHBOARD, switcher.currentInterface.value)

        // Swipe LEFT from DASHBOARD -> NOTIFICATION_FOCUSED
        assertTrue(switcher.handleSwipe(SwipeDirection.LEFT, candidates))
        assertEquals(IslandInterface.NOTIFICATION_FOCUSED, switcher.currentInterface.value)

        // Swipe LEFT from NOTIFICATION_FOCUSED -> BATTERY
        assertTrue(switcher.handleSwipe(SwipeDirection.LEFT, candidates))
        assertEquals(IslandInterface.BATTERY, switcher.currentInterface.value)

        // Swipe RIGHT from BATTERY traverses back stack: -> NOTIFICATION_FOCUSED
        assertTrue(switcher.handleSwipe(SwipeDirection.RIGHT, candidates))
        assertEquals(IslandInterface.NOTIFICATION_FOCUSED, switcher.currentInterface.value)

        // Swipe RIGHT from NOTIFICATION_FOCUSED traverses back stack: -> DASHBOARD
        assertTrue(switcher.handleSwipe(SwipeDirection.RIGHT, candidates))
        assertEquals(IslandInterface.DASHBOARD, switcher.currentInterface.value)
    }

    @Test
    fun testActivitySwitcherPrunesBatteryWhenUnpluggedAndInactive() {
        val switcher = IslandActivitySwitcher()
        val fullCandidates = setOf(IslandInterface.DASHBOARD, IslandInterface.BATTERY)

        switcher.navigateTo(IslandInterface.BATTERY)
        assertEquals(IslandInterface.BATTERY, switcher.currentInterface.value)

        // Battery is no longer candidate (unplugged and user disabled)
        val prunedCandidates = setOf(IslandInterface.DASHBOARD)
        switcher.pruneInvalidEntries(prunedCandidates)

        assertEquals("Should auto-fallback to DASHBOARD when BATTERY pruned", IslandInterface.DASHBOARD, switcher.currentInterface.value)
    }

    @Test
    fun testIslandPriorityArbitrationForBatteryEvents() {
        val chargingEvent = IslandEvent.ChargingConnected(
            id = "charge_1",
            batteryLevel = 90,
            chargeType = "AC"
        )
        assertEquals(IslandPriority.SYSTEM_HARDWARE, chargingEvent.priority)
        assertEquals(15, chargingEvent.priority)

        val lowBatteryEvent = IslandEvent.BatteryStateChanged(
            id = "low_1",
            batteryLevel = 15,
            isCharging = false,
            isLowBattery = true
        )
        assertEquals(IslandPriority.SAFETY_ALERT, lowBatteryEvent.priority)
        assertEquals(70, lowBatteryEvent.priority)

        val overheatEvent = IslandEvent.BatteryWarning(
            id = "hot_1",
            batteryLevel = 85,
            temperatureC = 47.0f,
            isOverheat = true
        )
        assertEquals(IslandPriority.SAFETY_ALERT, overheatEvent.priority)
        assertEquals(70, overheatEvent.priority)

        // Verify Safety Alerts (P70) outrank Notifications (P55) and Media (P40)
        assertTrue(lowBatteryEvent.priority > IslandPriority.NOTIFICATION)
        assertTrue(lowBatteryEvent.priority > IslandPriority.MEDIA_PLAYBACK)
        assertTrue(overheatEvent.priority > IslandPriority.NOTIFICATION)
    }
}
