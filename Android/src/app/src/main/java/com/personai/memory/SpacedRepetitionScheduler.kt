package com.personai.memory

import kotlin.math.max

/**
 * Schedules memory items using an adapted SM-2 / Leitner spaced repetition interval engine.
 */
class SpacedRepetitionScheduler(
    private val baseIntervalMillis: Long = 86_400_000L // 1 day in ms
) {
    /**
     * Updates stability, repetition count, and schedules the next review timestamp.
     *
     * @param item Current memory entity.
     * @param success Boolean indicating whether recall was successful, or false if forgotten.
     * @param reviewTime Timestamp of this review occurrence.
     */
    fun scheduleNextReview(
        item: MemoryItemEntity,
        success: Boolean,
        reviewTime: Long = System.currentTimeMillis()
    ): MemoryItemEntity {
        return if (success) {
            val newRepetitionCount = item.repetitionCount + 1
            val growthFactor = when (newRepetitionCount) {
                1 -> 1.5
                2 -> 2.2
                else -> 2.2 + (0.15 * (newRepetitionCount - 2)).coerceAtMost(1.5)
            }
            val newStability = max(1.0, item.stability * growthFactor)
            val intervalMillis = (newStability * baseIntervalMillis).toLong()
            val nextReview = reviewTime + intervalMillis

            item.copy(
                lastReviewedAt = reviewTime,
                stability = newStability,
                repetitionCount = newRepetitionCount,
                retentionScore = 1.0,
                nextReviewAt = nextReview
            )
        } else {
            // Failure: reset repetition count, reduce stability
            val newStability = max(1.0, item.stability * 0.5)
            // Immediate / short review (e.g. 4 hours or immediate)
            val intervalMillis = (0.2 * baseIntervalMillis).toLong().coerceAtLeast(3_600_000L)
            val nextReview = reviewTime + intervalMillis

            item.copy(
                lastReviewedAt = reviewTime,
                stability = newStability,
                repetitionCount = 0,
                retentionScore = 0.5,
                nextReviewAt = nextReview
            )
        }
    }
}
