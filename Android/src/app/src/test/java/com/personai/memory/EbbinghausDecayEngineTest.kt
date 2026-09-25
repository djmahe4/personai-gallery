package com.personai.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class EbbinghausDecayEngineTest {

    private val engine = EbbinghausDecayEngine(timeUnitMillis = 86_400_000.0) // 1 day unit

    @Test
    fun retentionAtZeroElapsedIsOne() {
        val retention = engine.calculateRetention(elapsedMillis = 0L, stability = 1.0)
        assertEquals(1.0, retention, 0.0001)
    }

    @Test
    fun retentionDecaysExponentiallyWithElapsedDays() {
        val oneDayMillis = 86_400_000L
        val retentionDay1 = engine.calculateRetention(elapsedMillis = oneDayMillis, stability = 1.0)
        // e^(-1) ~ 0.367879
        assertTrue("Retention after 1 day should be ~0.368, was $retentionDay1", abs(retentionDay1 - 0.3678) < 0.01)

        val retentionDay2 = engine.calculateRetention(elapsedMillis = 2 * oneDayMillis, stability = 1.0)
        // e^(-2) ~ 0.1353
        assertTrue("Retention after 2 days should be lower than day 1", retentionDay2 < retentionDay1)
    }

    @Test
    fun higherStabilityRetainsMemoryLonger() {
        val oneDayMillis = 86_400_000L
        val retentionLowStability = engine.calculateRetention(elapsedMillis = oneDayMillis, stability = 1.0)
        val retentionHighStability = engine.calculateRetention(elapsedMillis = oneDayMillis, stability = 5.0)

        // e^(-1/5) ~ 0.8187
        assertTrue(retentionHighStability > retentionLowStability)
        assertEquals(0.8187, retentionHighStability, 0.01)
    }

    @Test
    fun isDueForRevisionFlagsDecayedMemories() {
        val item = MemoryItemEntity(
            content = "Android Jetpack Compose State",
            embedding = emptyList(),
            lastReviewedAt = 100_000L,
            stability = 1.0,
            nextReviewAt = 500_000L
        )

        // Far past review time
        val isDue = engine.isDueForRevision(
            item = item,
            currentTimeMillis = 100_000L + (86_400_000L * 2), // 2 days later
            retentionThreshold = 0.70
        )
        assertTrue(isDue)

        // Immediately after review
        val notDue = engine.isDueForRevision(
            item = item,
            currentTimeMillis = 100_000L + 1000L,
            retentionThreshold = 0.70
        )
        assertFalse(notDue)
    }

    @Test
    fun applyDecayUpdatesRetentionScore() {
        val oneDayMillis = 86_400_000L
        val item = MemoryItemEntity(
            content = "SM-2 Interval Scheduling",
            embedding = emptyList(),
            lastReviewedAt = 0L,
            stability = 1.0,
            retentionScore = 1.0
        )

        val updated = engine.applyDecay(item, currentTimeMillis = oneDayMillis)
        assertEquals(0.3678, updated.retentionScore, 0.01)
    }
}
