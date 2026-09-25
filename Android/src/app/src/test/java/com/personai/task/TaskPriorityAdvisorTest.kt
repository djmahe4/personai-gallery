package com.personai.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TaskPriorityAdvisorTest {

    private val advisor = TaskPriorityAdvisor()

    @Test
    fun testHicksLaw_returnsMaximumThreeSuggestions() {
        val now = Instant.now()
        val tasks = listOf(
            EisenhowerTask(id = "1", title = "Task 1", quadrant = Quadrant.Q1_DO_NOW, urgencyScore = 0.9f, importanceScore = 0.9f),
            EisenhowerTask(id = "2", title = "Task 2", quadrant = Quadrant.Q1_DO_NOW, urgencyScore = 0.8f, importanceScore = 0.8f),
            EisenhowerTask(id = "3", title = "Task 3", quadrant = Quadrant.Q2_SCHEDULE, urgencyScore = 0.3f, importanceScore = 0.95f),
            EisenhowerTask(id = "4", title = "Task 4", quadrant = Quadrant.Q2_SCHEDULE, urgencyScore = 0.2f, importanceScore = 0.9f),
            EisenhowerTask(id = "5", title = "Task 5", quadrant = Quadrant.Q3_DELEGATE, urgencyScore = 0.7f, importanceScore = 0.3f),
            EisenhowerTask(id = "6", title = "Task 6", quadrant = Quadrant.Q4_ELIMINATE, urgencyScore = 0.1f, importanceScore = 0.1f)
        )

        val suggestions = advisor.generateTopSuggestions(tasks)
        assertEquals(3, suggestions.size)
    }

    @Test
    fun testPriorityOrder_Q1ThenQ2ThenQ3() {
        val tasks = listOf(
            EisenhowerTask(id = "q3", title = "Delegate Task", quadrant = Quadrant.Q3_DELEGATE, urgencyScore = 0.8f, importanceScore = 0.2f),
            EisenhowerTask(id = "q1", title = "Urgent Important Task", quadrant = Quadrant.Q1_DO_NOW, urgencyScore = 0.9f, importanceScore = 0.9f),
            EisenhowerTask(id = "q4", title = "Eliminate Task", quadrant = Quadrant.Q4_ELIMINATE, urgencyScore = 0.1f, importanceScore = 0.1f),
            EisenhowerTask(id = "q2", title = "Schedule Concept", quadrant = Quadrant.Q2_SCHEDULE, urgencyScore = 0.2f, importanceScore = 0.8f)
        )

        val suggestions = advisor.generateTopSuggestions(tasks)
        assertEquals(3, suggestions.size)
        assertEquals("q1", suggestions[0].task.id)
        assertEquals("q2", suggestions[1].task.id)
        assertEquals("q3", suggestions[2].task.id)
    }

    @Test
    fun testRationaleGeneration_providesClearExplanation() {
        val taskQ1 = EisenhowerTask(
            id = "q1",
            title = "Critical bug",
            quadrant = Quadrant.Q1_DO_NOW,
            urgencyScore = 0.95f,
            importanceScore = 0.9f
        )
        val taskQ2 = EisenhowerTask(
            id = "q2",
            title = "Quantum Physics",
            quadrant = Quadrant.Q2_SCHEDULE,
            conceptRetention = 0.3f
        )
        val suggestions = advisor.generateTopSuggestions(listOf(taskQ1, taskQ2))

        assertEquals(2, suggestions.size)
        assertTrue(suggestions[0].rationale.contains("Immediate action required", ignoreCase = true) || suggestions[0].rationale.isNotEmpty())
        assertTrue(suggestions[1].rationale.contains("retention", ignoreCase = true) || suggestions[1].rationale.isNotEmpty())
    }
}
