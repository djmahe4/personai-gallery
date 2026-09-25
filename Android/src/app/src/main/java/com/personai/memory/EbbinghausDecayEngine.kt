package com.personai.memory

import kotlin.math.exp

/**
 * Computes memory retention decay following the Ebbinghaus forgetting curve:
 * R = e^(-delta_t / S)
 * where delta_t is the elapsed time (in hours or days) and S is the memory stability factor.
 */
class EbbinghausDecayEngine(
    private val timeUnitMillis: Double = 86_400_000.0 // Default 1 day in ms
) {
    /**
     * Calculates retention score R in range [0.0, 1.0].
     * @param elapsedMillis Time elapsed since the last review in milliseconds.
     * @param stability Stability factor S (> 0.0), representing half-life or persistence unit.
     */
    fun calculateRetention(elapsedMillis: Long, stability: Double): Double {
        if (elapsedMillis <= 0L) return 1.0
        val safeStability = if (stability <= 0.0) 0.001 else stability
        val deltaT = elapsedMillis.toDouble() / timeUnitMillis
        val exponent = -(deltaT / safeStability)
        return exp(exponent).coerceIn(0.0, 1.0)
    }

    /**
     * Determines whether an item needs review based on whether its calculated retention
     * falls below the threshold (default 0.70 = 70% retention).
     */
    fun isDueForRevision(
        item: MemoryItemEntity,
        currentTimeMillis: Long = System.currentTimeMillis(),
        retentionThreshold: Double = 0.70
    ): Boolean {
        val elapsed = currentTimeMillis - item.lastReviewedAt
        val currentRetention = calculateRetention(elapsed, item.stability)
        return currentRetention < retentionThreshold || currentTimeMillis >= item.nextReviewAt
    }

    /**
     * Returns an updated copy of the memory item entity with fresh retention score.
     */
    fun applyDecay(
        item: MemoryItemEntity,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): MemoryItemEntity {
        val elapsed = currentTimeMillis - item.lastReviewedAt
        val newRetention = calculateRetention(elapsed, item.stability)
        return item.copy(retentionScore = newRetention)
    }
}
