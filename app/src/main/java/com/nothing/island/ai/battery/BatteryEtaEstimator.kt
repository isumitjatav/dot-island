package com.nothing.island.ai.battery

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Pure JVM helper to estimate remaining battery charging time
 * based strictly on real observed battery level changes over time.
 *
 * Requirements:
 * - Requires at least 2 samples with increasing percentage.
 * - Requires at least 60 seconds of observed charging to establish confidence.
 * - Never fabricates fake values. Returns null if confidence criteria are not met.
 */
class BatteryEtaEstimator(
    private val minObservationWindowMs: Long = 60_000L,
    private val maxSamples: Int = 10
) {
    data class Sample(val timestampMs: Long, val percentage: Int)

    private val samples = CopyOnWriteArrayList<Sample>()

    /**
     * Resets observed samples upon charging connection/disconnection.
     */
    fun reset() {
        samples.clear()
    }

    /**
     * Adds an observation sample.
     */
    fun addSample(percentage: Int, timestampMs: Long = System.currentTimeMillis()) {
        val last = samples.lastOrNull()
        if (last != null && last.percentage == percentage && (timestampMs - last.timestampMs) < 15_000L) {
            // Drop redundant high-frequency duplicate timestamps with identical percentage
            return
        }

        samples.add(Sample(timestampMs, percentage))
        while (samples.size > maxSamples) {
            samples.removeAt(0)
        }
    }

    /**
     * Estimates remaining charging time in milliseconds to 100%.
     * Returns null if insufficient observation time or negative/zero charging rate.
     */
    fun estimateRemainingMs(currentPercentage: Int, nowMs: Long = System.currentTimeMillis()): Long? {
        if (currentPercentage >= 100) return 0L
        if (samples.size < 2) return null

        val first = samples.first()
        val last = samples.last()

        val timeSpanMs = last.timestampMs - first.timestampMs
        if (timeSpanMs < minObservationWindowMs) {
            // Insufficient observation window
            return null
        }

        val percentageDelta = last.percentage - first.percentage
        if (percentageDelta <= 0) {
            // Not increasing (could be trickle charge, discharging, or stalled)
            return null
        }

        // Rate in percentage per millisecond
        val ratePerMs = percentageDelta.toDouble() / timeSpanMs.toDouble()
        if (ratePerMs <= 0.0) return null

        val remainingPercentage = 100 - currentPercentage
        val estimatedMs = (remainingPercentage / ratePerMs).toLong()

        // Sanity clamp: between 1 minute and 12 hours
        return if (estimatedMs in 60_000L..43_200_000L) estimatedMs else null
    }
}
