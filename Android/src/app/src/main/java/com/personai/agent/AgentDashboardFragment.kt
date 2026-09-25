package com.personai.agent

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

data class AgentDashboardViewState(
    val agentStatus: AgentStatus = AgentStatus.IDLE,
    val isOverlayPermissionGranted: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val executionHistory: List<AgentExecutionRecord> = emptyList()
)

class AgentDashboardFragment : Fragment() {

    private val dashboardState = mutableStateOf(AgentDashboardViewState())

    fun updateViewState(newState: AgentDashboardViewState) {
        dashboardState.value = newState
    }

    fun currentViewState(): AgentDashboardViewState = dashboardState.value

    fun saveState(outState: Bundle) {
        val current = dashboardState.value
        outState.putString(KEY_STATUS, current.agentStatus.name)
        outState.putBoolean(KEY_OVERLAY_PERM, current.isOverlayPermissionGranted)
        outState.putBoolean(KEY_ACCESSIBILITY_PERM, current.isAccessibilityEnabled)

        val savedHistory = current.executionHistory.takeLast(MAX_SAVED_HISTORY)
        val historyBundles = ArrayList<Bundle>(savedHistory.size)
        for (item in savedHistory) {
            val b = Bundle().apply {
                putString(KEY_ITEM_ID, item.id)
                putString(KEY_ITEM_PROMPT, item.prompt)
                putString(KEY_ITEM_FUNCTION_NAME, item.functionCall.name)
                val paramsBundle = Bundle()
                item.functionCall.parameters.forEach { (k, v) -> paramsBundle.putString(k, v) }
                putBundle(KEY_ITEM_FUNCTION_PARAMS, paramsBundle)

                when (val res = item.result) {
                    is ToolExecutionResult.Success -> {
                        putBoolean(KEY_ITEM_RESULT_SUCCESS, true)
                        putString(KEY_ITEM_RESULT_OUTPUT, res.output)
                    }
                    is ToolExecutionResult.Failure -> {
                        putBoolean(KEY_ITEM_RESULT_SUCCESS, false)
                        putString(KEY_ITEM_RESULT_OUTPUT, res.error)
                    }
                }
                putLong(KEY_ITEM_TIMESTAMP, item.timestamp)
            }
            historyBundles.add(b)
        }
        outState.putParcelableArrayList(KEY_HISTORY, historyBundles)
    }

    fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return

        val statusName = savedInstanceState.getString(KEY_STATUS) ?: AgentStatus.IDLE.name
        val status = try {
            AgentStatus.valueOf(statusName)
        } catch (e: Exception) {
            AgentStatus.IDLE
        }
        val overlayPerm = savedInstanceState.getBoolean(KEY_OVERLAY_PERM, false)
        val accessibilityPerm = savedInstanceState.getBoolean(KEY_ACCESSIBILITY_PERM, false)

        val historyBundles = savedInstanceState.getParcelableArrayList<Bundle>(KEY_HISTORY)
        val historyList = mutableListOf<AgentExecutionRecord>()
        if (historyBundles != null) {
            for (b in historyBundles) {
                val id = b.getString(KEY_ITEM_ID) ?: ""
                val prompt = b.getString(KEY_ITEM_PROMPT) ?: ""
                val fnName = b.getString(KEY_ITEM_FUNCTION_NAME) ?: ""
                val paramsBundle = b.getBundle(KEY_ITEM_FUNCTION_PARAMS)
                val params = mutableMapOf<String, String>()
                paramsBundle?.keySet()?.forEach { k ->
                    paramsBundle.getString(k)?.let { v -> params[k] = v }
                }
                val isSuccess = b.getBoolean(KEY_ITEM_RESULT_SUCCESS, true)
                val output = b.getString(KEY_ITEM_RESULT_OUTPUT) ?: ""
                val result = if (isSuccess) {
                    ToolExecutionResult.Success(fnName, output)
                } else {
                    ToolExecutionResult.Failure(fnName, output)
                }
                val timestamp = b.getLong(KEY_ITEM_TIMESTAMP, 0L)
                historyList.add(
                    AgentExecutionRecord(
                        id = id,
                        prompt = prompt,
                        functionCall = FunctionCall(fnName, params),
                        result = result,
                        timestamp = timestamp
                    )
                )
            }
        }

        dashboardState.value = AgentDashboardViewState(
            agentStatus = status,
            isOverlayPermissionGranted = overlayPerm,
            isAccessibilityEnabled = accessibilityPerm,
            executionHistory = historyList
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        restoreState(savedInstanceState)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AgentDashboardContent(viewState = dashboardState.value)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveState(outState)
    }

    companion object {
        private const val KEY_STATUS = "agent_status"
        private const val KEY_OVERLAY_PERM = "overlay_permission"
        private const val KEY_ACCESSIBILITY_PERM = "accessibility_permission"
        private const val KEY_HISTORY = "execution_history"

        private const val KEY_ITEM_ID = "item_id"
        private const val KEY_ITEM_PROMPT = "item_prompt"
        private const val KEY_ITEM_FUNCTION_NAME = "item_function_name"
        private const val KEY_ITEM_FUNCTION_PARAMS = "item_function_params"
        private const val KEY_ITEM_RESULT_SUCCESS = "item_result_success"
        private const val KEY_ITEM_RESULT_OUTPUT = "item_result_output"
        private const val KEY_ITEM_TIMESTAMP = "item_timestamp"

        const val MAX_SAVED_HISTORY = 50
    }
}

@Composable
fun AgentDashboardContent(
    viewState: AgentDashboardViewState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "PersonAI Agent Dashboard",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Agent Status: ${viewState.agentStatus.name}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Overlay Permission: " + if (viewState.isOverlayPermissionGranted) "Granted" else "Missing",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Accessibility: " + if (viewState.isAccessibilityEnabled) "Active" else "Disabled",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Execution History",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewState.executionHistory, key = { it.id }) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = record.prompt,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Function: ${record.functionCall.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        when (val res = record.result) {
                            is ToolExecutionResult.Success -> {
                                Text(
                                    text = "Result: ${res.output}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            is ToolExecutionResult.Failure -> {
                                Text(
                                    text = "Error: ${res.error}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
