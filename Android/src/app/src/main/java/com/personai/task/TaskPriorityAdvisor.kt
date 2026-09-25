package com.personai.task

class TaskPriorityAdvisor(
    private val maxSuggestions: Int = 3
) {

    fun generateTopSuggestions(tasks: List<EisenhowerTask>): List<TaskSuggestion> {
        val sortedTasks = tasks
            .filter { it.quadrant != Quadrant.Q4_ELIMINATE }
            .sortedWith(
                compareBy<EisenhowerTask> { task ->
                    when (task.quadrant) {
                        Quadrant.Q1_DO_NOW -> 1
                        Quadrant.Q2_SCHEDULE -> 2
                        Quadrant.Q3_DELEGATE -> 3
                        Quadrant.Q4_ELIMINATE, null -> 4
                    }
                }.thenByDescending { it.urgencyScore + it.importanceScore }
            )

        return sortedTasks.take(maxSuggestions).map { task ->
            val rationale = generateRationale(task)
            val actionLabel = when (task.quadrant) {
                Quadrant.Q1_DO_NOW -> "Do Now"
                Quadrant.Q2_SCHEDULE -> "Schedule"
                Quadrant.Q3_DELEGATE -> "Delegate"
                else -> "Review"
            }
            TaskSuggestion(task = task, rationale = rationale, actionLabel = actionLabel)
        }
    }

    private fun generateRationale(task: EisenhowerTask): String {
        return when (task.quadrant) {
            Quadrant.Q1_DO_NOW -> "Immediate action required: High urgency and importance."
            Quadrant.Q2_SCHEDULE -> {
                if (task.conceptRetention != null && task.conceptRetention < 0.4f) {
                    "Critical concept retention decay detected (${(task.conceptRetention * 100).toInt()}%). Protect time to revise."
                } else {
                    "Important long-term task: Schedule focused time."
                }
            }
            Quadrant.Q3_DELEGATE -> "Urgent but routine: Recommended for AI agent delegation or quick batching."
            Quadrant.Q4_ELIMINATE -> "Low urgency and importance: Recommended to eliminate or archive."
            null -> "Pending classification."
        }
    }
}
