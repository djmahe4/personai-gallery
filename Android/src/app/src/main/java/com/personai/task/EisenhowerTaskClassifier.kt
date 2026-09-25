package com.personai.task

import java.time.Duration
import java.time.Instant

class EisenhowerTaskClassifier {

    fun classify(task: EisenhowerTask, now: Instant = Instant.now()): Quadrant {
        val isUrgent = computeUrgency(task, now)
        val isImportant = computeImportance(task)

        return when {
            isUrgent && isImportant -> Quadrant.Q1_DO_NOW
            !isUrgent && isImportant -> Quadrant.Q2_SCHEDULE
            isUrgent && !isImportant -> Quadrant.Q3_DELEGATE
            else -> Quadrant.Q4_ELIMINATE
        }
    }

    private fun computeUrgency(task: EisenhowerTask, now: Instant): Boolean {
        if (task.urgencyScore >= 0.5f) return true
        if (task.priority == TaskPriority.HIGH) return true
        if (task.deadline != null) {
            val hoursLeft = Duration.between(now, task.deadline).toHours()
            if (hoursLeft in 0..24) return true
        }
        return false
    }

    private fun computeImportance(task: EisenhowerTask): Boolean {
        if (task.importanceScore >= 0.5f) return true
        if (task.isCoreOrStarred) return true
        if (task.conceptRetention != null && task.conceptRetention < 0.4f) return true
        return false
    }
}
