package com.personai.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedRepetitionSchedulerTest {

    private val scheduler = SpacedRepetitionScheduler(baseIntervalMillis = 86_400_000L) // 1 day

    @Test
    fun successfulReviewIncrementsRepetitionAndStability() {
        val initialItem = MemoryItemEntity(
            content = "Prompt engineering techniques",
            embedding = emptyList(),
            lastReviewedAt = 1_000_000L,
            stability = 1.0,
            repetitionCount = 0,
            retentionScore = 1.0,
            nextReviewAt = 1_000_000L
        )

        val reviewTime = 2_000_000L
        val updated = scheduler.scheduleNextReview(initialItem, success = true, reviewTime = reviewTime)

        assertEquals(1, updated.repetitionCount)
        assertTrue("Stability should increase or remain >= 1.0", updated.stability >= 1.0)
        assertEquals(reviewTime, updated.lastReviewedAt)
        assertTrue("Next review should be in future", updated.nextReviewAt > reviewTime)
    }

    @Test
    fun consecutiveSuccessfulReviewsIncreaseIntervals() {
        var item = MemoryItemEntity(
            content = "LiteRT model acceleration",
            embedding = emptyList(),
            lastReviewedAt = 0L,
            stability = 1.0,
            repetitionCount = 0
        )

        val t1 = 1_000_000L
        item = scheduler.scheduleNextReview(item, success = true, reviewTime = t1)
        val interval1 = item.nextReviewAt - t1

        val t2 = item.nextReviewAt
        item = scheduler.scheduleNextReview(item, success = true, reviewTime = t2)
        val interval2 = item.nextReviewAt - t2

        assertTrue("Second interval should be greater than first: $interval2 > $interval1", interval2 > interval1)
        assertEquals(2, item.repetitionCount)
    }

    @Test
    fun failedReviewResetsRepetitionCountAndDecreasesStability() {
        val item = MemoryItemEntity(
            content = "Complex mathematical concept",
            embedding = emptyList(),
            lastReviewedAt = 1_000_000L,
            stability = 4.0,
            repetitionCount = 3,
            retentionScore = 0.9,
            nextReviewAt = 5_000_000L
        )

        val reviewTime = 2_000_000L
        val updated = scheduler.scheduleNextReview(item, success = false, reviewTime = reviewTime)

        assertEquals(0, updated.repetitionCount)
        assertEquals(2.0, updated.stability, 0.001)
        assertTrue(updated.nextReviewAt > reviewTime)
        // Interval should be short (< 1 day)
        assertTrue((updated.nextReviewAt - reviewTime) <= 86_400_000L)
    }
}
