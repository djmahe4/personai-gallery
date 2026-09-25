package com.personai.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment

class EisenhowerDashboardFragment : Fragment() {

    private val dashboardState = mutableStateOf(EisenhowerDashboardViewState())
    var onActionClickListener: ((TaskSuggestion) -> Unit)? = null

    fun updateViewState(newState: EisenhowerDashboardViewState) {
        dashboardState.value = newState
    }

    fun currentViewState(): EisenhowerDashboardViewState = dashboardState.value

    fun saveState(outState: Bundle) {
        val current = dashboardState.value
        outState.putInt(KEY_Q1, current.q1Count)
        outState.putInt(KEY_Q2, current.q2Count)
        outState.putInt(KEY_Q3, current.q3Count)
        outState.putInt(KEY_Q4, current.q4Count)

        val suggestionBundles = ArrayList<Bundle>(current.topSuggestions.size)
        for (suggestion in current.topSuggestions) {
            val sBundle = Bundle().apply {
                putString(KEY_SUGGESTION_ID, suggestion.task.id)
                putString(KEY_SUGGESTION_TITLE, suggestion.task.title)
                putString(KEY_SUGGESTION_DESC, suggestion.task.description)
                putFloat(KEY_SUGGESTION_URGENCY, suggestion.task.urgencyScore)
                putFloat(KEY_SUGGESTION_IMPORTANCE, suggestion.task.importanceScore)
                putString(KEY_SUGGESTION_QUADRANT, suggestion.task.quadrant?.name)
                putString(KEY_SUGGESTION_PRIORITY, suggestion.task.priority.name)
                suggestion.task.conceptRetention?.let { putFloat(KEY_SUGGESTION_RETENTION, it) }
                putBoolean(KEY_SUGGESTION_STARRED, suggestion.task.isCoreOrStarred)
                putString(KEY_SUGGESTION_SOURCE, suggestion.task.source.name)
                putLong(KEY_SUGGESTION_CREATED_AT, suggestion.task.createdAt.toEpochMilli())
                suggestion.task.deadline?.let { putLong(KEY_SUGGESTION_DEADLINE, it.toEpochMilli()) }
                putString(KEY_SUGGESTION_RATIONALE, suggestion.rationale)
                putString(KEY_SUGGESTION_ACTION_LABEL, suggestion.actionLabel)
            }
            suggestionBundles.add(sBundle)
        }
        outState.putParcelableArrayList(KEY_TOP_SUGGESTIONS, suggestionBundles)
    }

    fun restoreState(savedState: Bundle) {
        val rawSuggestions = savedState.getParcelableArrayList<Bundle>(KEY_TOP_SUGGESTIONS)
        val restoredSuggestions = rawSuggestions?.mapNotNull { sBundle ->
            val id = sBundle.getString(KEY_SUGGESTION_ID) ?: return@mapNotNull null
            val title = sBundle.getString(KEY_SUGGESTION_TITLE) ?: return@mapNotNull null
            val desc = sBundle.getString(KEY_SUGGESTION_DESC, "")
            val urgency = sBundle.getFloat(KEY_SUGGESTION_URGENCY, 0.0f)
            val importance = sBundle.getFloat(KEY_SUGGESTION_IMPORTANCE, 0.0f)
            val quadName = sBundle.getString(KEY_SUGGESTION_QUADRANT)
            val quadrant = quadName?.let { runCatching { Quadrant.valueOf(it) }.getOrNull() }
            val prioName = sBundle.getString(KEY_SUGGESTION_PRIORITY)
            val priority = prioName?.let { runCatching { TaskPriority.valueOf(it) }.getOrNull() } ?: TaskPriority.MEDIUM
            val retention = if (sBundle.containsKey(KEY_SUGGESTION_RETENTION)) sBundle.getFloat(KEY_SUGGESTION_RETENTION) else null
            val starred = sBundle.getBoolean(KEY_SUGGESTION_STARRED, false)
            val srcName = sBundle.getString(KEY_SUGGESTION_SOURCE)
            val source = srcName?.let { runCatching { TaskSource.valueOf(it) }.getOrNull() } ?: TaskSource.USER_PROMPT
            val createdAtMillis = sBundle.getLong(KEY_SUGGESTION_CREATED_AT, System.currentTimeMillis())
            val deadlineMillis = if (sBundle.containsKey(KEY_SUGGESTION_DEADLINE)) sBundle.getLong(KEY_SUGGESTION_DEADLINE) else null

            val task = EisenhowerTask(
                id = id,
                title = title,
                description = desc,
                urgencyScore = urgency,
                importanceScore = importance,
                quadrant = quadrant,
                priority = priority,
                conceptRetention = retention,
                isCoreOrStarred = starred,
                source = source,
                createdAt = java.time.Instant.ofEpochMilli(createdAtMillis),
                deadline = deadlineMillis?.let { java.time.Instant.ofEpochMilli(it) }
            )
            val rationale = sBundle.getString(KEY_SUGGESTION_RATIONALE, "")
            val actionLabel = sBundle.getString(KEY_SUGGESTION_ACTION_LABEL, "Review")
            TaskSuggestion(task = task, rationale = rationale, actionLabel = actionLabel)
        } ?: emptyList()

        dashboardState.value = EisenhowerDashboardViewState(
            q1Count = savedState.getInt(KEY_Q1, 0),
            q2Count = savedState.getInt(KEY_Q2, 0),
            q3Count = savedState.getInt(KEY_Q3, 0),
            q4Count = savedState.getInt(KEY_Q4, 0),
            topSuggestions = restoredSuggestions
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { restoreState(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        EisenhowerDashboardScreen(
                            state = dashboardState.value,
                            onActionClick = { suggestion ->
                                onActionClickListener?.invoke(suggestion)
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_Q1 = "key_q1_count"
        private const val KEY_Q2 = "key_q2_count"
        private const val KEY_Q3 = "key_q3_count"
        private const val KEY_Q4 = "key_q4_count"
        private const val KEY_TOP_SUGGESTIONS = "key_top_suggestions"
        private const val KEY_SUGGESTION_ID = "s_id"
        private const val KEY_SUGGESTION_TITLE = "s_title"
        private const val KEY_SUGGESTION_DESC = "s_desc"
        private const val KEY_SUGGESTION_URGENCY = "s_urgency"
        private const val KEY_SUGGESTION_IMPORTANCE = "s_importance"
        private const val KEY_SUGGESTION_QUADRANT = "s_quadrant"
        private const val KEY_SUGGESTION_PRIORITY = "s_priority"
        private const val KEY_SUGGESTION_RETENTION = "s_retention"
        private const val KEY_SUGGESTION_STARRED = "s_starred"
        private const val KEY_SUGGESTION_SOURCE = "s_source"
        private const val KEY_SUGGESTION_CREATED_AT = "s_created_at"
        private const val KEY_SUGGESTION_DEADLINE = "s_deadline"
        private const val KEY_SUGGESTION_RATIONALE = "s_rationale"
        private const val KEY_SUGGESTION_ACTION_LABEL = "s_action_label"
    }
}

@Composable
fun EisenhowerDashboardScreen(
    state: EisenhowerDashboardViewState,
    onActionClick: (TaskSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Eisenhower Priority Matrix",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Quadrant 2x2 summary cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuadrantMetricCard(title = "Q1: Do Now", count = state.q1Count, modifier = Modifier.weight(1f))
            QuadrantMetricCard(title = "Q2: Schedule", count = state.q2Count, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuadrantMetricCard(title = "Q3: Delegate", count = state.q3Count, modifier = Modifier.weight(1f))
            QuadrantMetricCard(title = "Q4: Eliminate", count = state.q4Count, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Top Actionable Suggestions (Hick's Law)",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(state.topSuggestions) { suggestion ->
                SuggestionCard(suggestion = suggestion, onActionClick = onActionClick)
            }
        }
    }
}

@Composable
private fun QuadrantMetricCard(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "$count tasks", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: TaskSuggestion,
    onActionClick: (TaskSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = suggestion.task.title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = suggestion.rationale, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onActionClick(suggestion) }
            ) {
                Text(text = suggestion.actionLabel)
            }
        }
    }
}
