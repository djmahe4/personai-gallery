package com.personai.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class UsagePatternModelTest {

    @Test
    fun testHourlyPeakDetectionFromTimestamps() {
        val zoneId = ZoneId.of("UTC")
        // Generate usage events concentrated in hour 14 (2 PM) and hour 10 (10 AM)
        val timestamps = mutableListOf<Long>()
        val baseDate = ZonedDateTime.of(2026, 9, 25, 0, 0, 0, 0, zoneId)

        // 10 events at hour 14
        repeat(10) { i ->
            timestamps.add(baseDate.withHour(14).withMinute(i * 5).toInstant().toEpochMilli())
        }
        // 5 events at hour 10
        repeat(5) { i ->
            timestamps.add(baseDate.withHour(10).withMinute(i * 5).toInstant().toEpochMilli())
        }
        // 1 event at hour 8
        timestamps.add(baseDate.withHour(8).withMinute(0).toInstant().toEpochMilli())

        val model = UsagePatternModel()
        val peaks = model.detectHourlyPeaks(timestamps, zoneId = zoneId, topN = 2)

        assertEquals(2, peaks.size)
        assertEquals(14, peaks[0].hourOfDay)
        assertEquals(10, peaks[0].eventCount)
        assertEquals(10, peaks[1].hourOfDay)
        assertEquals(5, peaks[1].eventCount)
    }

    @Test
    fun testAnomalyDetectionZScore() {
        val model = UsagePatternModel()
        // Baseline hourly event counts with mean ~ 10, low std dev
        val baselineHourlyCounts = listOf(10.0, 11.0, 9.0, 10.0, 10.0, 12.0, 8.0, 10.0)

        // Count = 10 -> not anomaly
        val normalResult = model.checkAnomaly(currentCount = 11.0, baselineCounts = baselineHourlyCounts, thresholdZ = 2.0)
        assertFalse(normalResult.isAnomaly)
        assertTrue(normalResult.zScore < 2.0)

        // Count = 35 -> spike > 2.0 Z-score
        val spikeResult = model.checkAnomaly(currentCount = 35.0, baselineCounts = baselineHourlyCounts, thresholdZ = 2.0)
        assertTrue(spikeResult.isAnomaly)
        assertTrue(spikeResult.zScore > 2.0)
    }

    @Test
    fun testOptimalFocusWindowRecommendation() {
        val zoneId = ZoneId.of("UTC")
        val baseDate = ZonedDateTime.of(2026, 9, 25, 0, 0, 0, 0, zoneId)
        val timestamps = mutableListOf<Long>()

        // Heavy activity during daytime: hours 9 to 17
        for (h in 9..17) {
            repeat(15) { i ->
                timestamps.add(baseDate.withHour(h).withMinute(i * 3).toInstant().toEpochMilli())
            }
        }
        // Moderate activity in evening: hours 18 to 21
        for (h in 18..21) {
            repeat(5) { i ->
                timestamps.add(baseDate.withHour(h).withMinute(i * 10).toInstant().toEpochMilli())
            }
        }
        // Very low or 0 activity during night/early morning: hours 22 to 6
        // Some waking hours low activity: hours 6 to 8 (e.g. 1 event each)
        timestamps.add(baseDate.withHour(6).withMinute(30).toInstant().toEpochMilli())
        timestamps.add(baseDate.withHour(7).withMinute(30).toInstant().toEpochMilli())

        val model = UsagePatternModel()
        // Recommend focus window of windowDurationHours = 2 between 6 AM and 22 PM (waking hours)
        val window = model.recommendFocusWindow(
            timestamps = timestamps,
            zoneId = zoneId,
            windowDurationHours = 2,
            startHour = 6,
            endHour = 22
        )

        // Hours 6-8 or 7-9 should have minimum activity
        assertTrue("Focus window should start in low activity morning hours (6 or 7)", window.startHour in 6..7)
        assertEquals(2, window.durationHours)
        assertTrue(window.averageActivityPerHour <= 1.0)
    }
}
