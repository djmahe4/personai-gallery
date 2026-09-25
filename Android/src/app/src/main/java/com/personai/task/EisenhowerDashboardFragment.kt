package com.personai.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment

class EisenhowerDashboardFragment : Fragment() {

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
                            state = EisenhowerDashboardViewState(),
                            onActionClick = {}
                        )
                    }
                }
            }
        }
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
