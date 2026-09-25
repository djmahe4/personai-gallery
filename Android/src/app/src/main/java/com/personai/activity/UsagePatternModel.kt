package com.personai.activity

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.sqrt

data class HourlyUsage(
    val hourOfDay: Int,
    val eventCount: Int
)

data class AnomalyResult(
    val isAnomaly: Boolean,
    val zScore: Double,
    val currentCount: Double,
    val mean: Double,
    val standardDeviation: Double
)

data class FocusWindow(
    val startHour: Int,
    val durationHours: Int,
    val averageActivityPerHour: Double
)

class UsagePatternModel {

    fun detectHourlyPeaks(
        timestamps: List<Long>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        topN: Int = 3
    ): List<HourlyUsage> {
        val hourlyHistogram = IntArray(24)
        for (ts in timestamps) {
            val hour = Instant.ofEpochMilli(ts).atZone(zoneId).hour
            hourlyHistogram[hour]++
        }

        return hourlyHistogram.indices
            .map { hour -> HourlyUsage(hourOfDay = hour, eventCount = hourlyHistogram[hour]) }
            .sortedByDescending { it.eventCount }
            .take(topN)
    }

    fun checkAnomaly(
        currentCount: Double,
        baselineCounts: List<Double>,
        thresholdZ: Double = 2.0
    ): AnomalyResult {
        if (baselineCounts.isEmpty()) {
            return AnomalyResult(
                isAnomaly = false,
                zScore = 0.0,
                currentCount = currentCount,
                mean = currentCount,
                standardDeviation = 0.0
            )
        }

        val mean = baselineCounts.average()
        val variance = baselineCounts.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)

        val zScore = if (stdDev > 0.00001) {
            (currentCount - mean) / stdDev
        } else {
            0.0
        }

        return AnomalyResult(
            isAnomaly = zScore > thresholdZ,
            zScore = zScore,
            currentCount = currentCount,
            mean = mean,
            standardDeviation = stdDev
        )
    }

    fun recommendFocusWindow(
        timestamps: List<Long>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        windowDurationHours: Int = 2,
        startHour: Int = 6,
        endHour: Int = 22
    ): FocusWindow {
        val hourlyHistogram = DoubleArray(24)
        for (ts in timestamps) {
            val hour = Instant.ofEpochMilli(ts).atZone(zoneId).hour
            hourlyHistogram[hour] += 1.0
        }

        var minAvg = Double.MAX_VALUE
        var bestStartHour = startHour

        val maxSearchStart = endHour - windowDurationHours
        for (h in startHour..maxSearchStart) {
            var sum = 0.0
            for (offset in 0 until windowDurationHours) {
                sum += hourlyHistogram[h + offset]
            }
            val avg = sum / windowDurationHours
            if (avg < minAvg) {
                minAvg = avg
                bestStartHour = h
            }
        }

        return FocusWindow(
            startHour = bestStartHour,
            durationHours = windowDurationHours,
            averageActivityPerHour = if (minAvg == Double.MAX_VALUE) 0.0 else minAvg
        )
    }
}
