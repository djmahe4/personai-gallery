package com.personai.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class QuadrantQueueManagerTest {

    private lateinit var queueManager: QuadrantQueueManager

    @Before
    fun setUp() {
        queueManager = QuadrantQueueManager()
    }

    @Test
    fun testEnqueueTask_routesToAppropriateQuadrant() {
        val t1 = EisenhowerTask(id = "1", title = "Bug", urgencyScore = 0.9f, importanceScore = 0.9f)
        val t2 = EisenhowerTask(id = "2", title = "Study", urgencyScore = 0.2f, importanceScore = 0.9f)
        val t3 = EisenhowerTask(id = "3", title = "Email", urgencyScore = 0.8f, importanceScore = 0.2f)
        val t4 = EisenhowerTask(id = "4", title = "Spam", urgencyScore = 0.1f, importanceScore = 0.1f)

        queueManager.enqueue(t1)
        queueManager.enqueue(t2)
        queueManager.enqueue(t3)
        queueManager.enqueue(t4)

        assertEquals(1, queueManager.getTasks(Quadrant.Q1_DO_NOW).size)
        assertEquals(1, queueManager.getTasks(Quadrant.Q2_SCHEDULE).size)
        assertEquals(1, queueManager.getTasks(Quadrant.Q3_DELEGATE).size)
        assertEquals(1, queueManager.getTasks(Quadrant.Q4_ELIMINATE).size)
    }

    @Test
    fun testFetchDueItems_filtersByDeadlineOrImminent() {
        val now = Instant.now()
        val dueTask = EisenhowerTask(
            id = "due",
            title = "Due now",
            urgencyScore = 0.9f,
            importanceScore = 0.9f,
            deadline = now.minus(5, ChronoUnit.MINUTES)
        )
        val futureTask = EisenhowerTask(
            id = "future",
            title = "Future",
            urgencyScore = 0.2f,
            importanceScore = 0.9f,
            deadline = now.plus(3, ChronoUnit.DAYS)
        )

        queueManager.enqueue(dueTask)
        queueManager.enqueue(futureTask)

        val dueItems = queueManager.getDueTasks(now)
        assertEquals(1, dueItems.size)
        assertEquals("due", dueItems[0].id)
    }

    @Test
    fun testIntegrateSpacedConceptRevision_addsAsQ2() {
        queueManager.integrateConceptRevision(
            conceptId = "c101",
            conceptName = "Transformers Architecture",
            retentionScore = 0.32f
        )

        val q2Tasks = queueManager.getTasks(Quadrant.Q2_SCHEDULE)
        assertEquals(1, q2Tasks.size)
        val task = q2Tasks[0]
        assertEquals("Review: Transformers Architecture", task.title)
        assertEquals(0.32f, task.conceptRetention ?: 1.0f, 0.001f)
        assertEquals(TaskSource.CONCEPT_REVISION, task.source)
    }
}
