package com.personai.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class EisenhowerTaskClassifierTest {

    private val classifier = EisenhowerTaskClassifier()

    @Test
    fun testUrgentAndImportant_mapsToQ1_DoNow() {
        val now = Instant.now()
        val task = EisenhowerTask(
            id = "t1",
            title = "Critical bug fix",
            urgencyScore = 0.9f,
            importanceScore = 0.85f,
            deadline = now.plus(12, ChronoUnit.HOURS)
        )
        val quadrant = classifier.classify(task)
        assertEquals(Quadrant.Q1_DO_NOW, quadrant)
    }

    @Test
    fun testImportantNotUrgent_mapsToQ2_Schedule() {
        val now = Instant.now()
        val task = EisenhowerTask(
            id = "t2",
            title = "Concept Spaced Revision",
            urgencyScore = 0.2f,
            importanceScore = 0.9f,
            deadline = now.plus(5, ChronoUnit.DAYS),
            conceptRetention = 0.35f // retention < 0.4 marks high importance
        )
        val quadrant = classifier.classify(task)
        assertEquals(Quadrant.Q2_SCHEDULE, quadrant)
    }

    @Test
    fun testUrgentNotImportant_mapsToQ3_Delegate() {
        val now = Instant.now()
        val task = EisenhowerTask(
            id = "t3",
            title = "Routine batch sync",
            urgencyScore = 0.8f,
            importanceScore = 0.2f,
            deadline = now.plus(2, ChronoUnit.HOURS)
        )
        val quadrant = classifier.classify(task)
        assertEquals(Quadrant.Q3_DELEGATE, quadrant)
    }

    @Test
    fun testNeitherUrgentNorImportant_mapsToQ4_Eliminate() {
        val now = Instant.now()
        val task = EisenhowerTask(
            id = "t4",
            title = "Random promotional ping",
            urgencyScore = 0.1f,
            importanceScore = 0.1f,
            deadline = now.plus(30, ChronoUnit.DAYS)
        )
        val quadrant = classifier.classify(task)
        assertEquals(Quadrant.Q4_ELIMINATE, quadrant)
    }

    @Test
    fun testUrgencyInferredFromDeadlineUnder24HoursOrHighPriorityFlag() {
        val now = Instant.now()
        val taskWithImminentDeadline = EisenhowerTask(
            id = "t5",
            title = "Imminent deadline task",
            urgencyScore = 0.1f, // explicit score low, but deadline < 24h
            importanceScore = 0.8f,
            deadline = now.plus(10, ChronoUnit.HOURS)
        )
        val quadrant = classifier.classify(taskWithImminentDeadline)
        assertEquals(Quadrant.Q1_DO_NOW, quadrant)

        val taskWithPriorityFlag = EisenhowerTask(
            id = "t6",
            title = "Flagged urgent",
            urgencyScore = 0.1f,
            importanceScore = 0.2f,
            priority = TaskPriority.HIGH
        )
        val quadrant2 = classifier.classify(taskWithPriorityFlag)
        assertEquals(Quadrant.Q3_DELEGATE, quadrant2)
    }

    @Test
    fun testImportanceInferredFromLowRetentionOrCoreFlag() {
        val taskWithLowRetention = EisenhowerTask(
            id = "t7",
            title = "Low Retention Topic",
            urgencyScore = 0.1f,
            importanceScore = 0.1f, // explicit low
            conceptRetention = 0.38f // < 0.4 promotes importance
        )
        val quadrant = classifier.classify(taskWithLowRetention)
        assertEquals(Quadrant.Q2_SCHEDULE, quadrant)

        val taskWithCoreFlag = EisenhowerTask(
            id = "t8",
            title = "Core starred concept",
            urgencyScore = 0.1f,
            importanceScore = 0.1f,
            isCoreOrStarred = true
        )
        val quadrant2 = classifier.classify(taskWithCoreFlag)
        assertEquals(Quadrant.Q2_SCHEDULE, quadrant2)
    }
}
