package com.personai.task

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class QuadrantQueueManager(
    private val classifier: EisenhowerTaskClassifier = EisenhowerTaskClassifier()
) {
    private val quadrantQueues = ConcurrentHashMap<Quadrant, CopyOnWriteArrayList<EisenhowerTask>>().apply {
        Quadrant.values().forEach { put(it, CopyOnWriteArrayList()) }
    }

    fun enqueue(task: EisenhowerTask) {
        val targetQuadrant = task.quadrant ?: classifier.classify(task)
        val categorizedTask = if (task.quadrant != targetQuadrant) task.copy(quadrant = targetQuadrant) else task
        quadrantQueues[targetQuadrant]?.add(categorizedTask)
    }

    fun getTasks(quadrant: Quadrant): List<EisenhowerTask> {
        return quadrantQueues[quadrant]?.toList() ?: emptyList()
    }

    fun getAllTasks(): List<EisenhowerTask> {
        return quadrantQueues.values.flatten()
    }

    fun getDueTasks(now: Instant = Instant.now()): List<EisenhowerTask> {
        return getAllTasks().filter { task ->
            task.deadline != null && !task.deadline.isAfter(now)
        }
    }

    fun integrateConceptRevision(
        conceptId: String,
        conceptName: String,
        retentionScore: Float
    ) {
        val task = EisenhowerTask(
            id = "concept_rev_$conceptId",
            title = "Review: $conceptName",
            description = "Spaced repetition revision for concept $conceptName",
            urgencyScore = 0.2f,
            importanceScore = 0.9f,
            conceptRetention = retentionScore,
            source = TaskSource.CONCEPT_REVISION,
            quadrant = Quadrant.Q2_SCHEDULE
        )
        enqueue(task)
    }

    fun clear() {
        quadrantQueues.values.forEach { it.clear() }
    }
}
