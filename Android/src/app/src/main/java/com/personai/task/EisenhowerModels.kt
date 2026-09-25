package com.personai.task

import java.time.Instant

enum class Quadrant {
    Q1_DO_NOW,
    Q2_SCHEDULE,
    Q3_DELEGATE,
    Q4_ELIMINATE
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}

enum class TaskSource {
    USER_PROMPT,
    CONCEPT_REVISION,
    NOTIFICATION,
    SYSTEM
}

data class EisenhowerTask(
    val id: String,
    val title: String,
    val description: String = "",
    val urgencyScore: Float = 0.0f,
    val importanceScore: Float = 0.0f,
    val deadline: Instant? = null,
    val quadrant: Quadrant? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val conceptRetention: Float? = null,
    val isCoreOrStarred: Boolean = false,
    val source: TaskSource = TaskSource.USER_PROMPT,
    val createdAt: Instant = Instant.now()
)

data class TaskSuggestion(
    val task: EisenhowerTask,
    val rationale: String,
    val actionLabel: String = "Review"
)

data class EisenhowerDashboardViewState(
    val q1Count: Int = 0,
    val q2Count: Int = 0,
    val q3Count: Int = 0,
    val q4Count: Int = 0,
    val topSuggestions: List<TaskSuggestion> = emptyList()
)
