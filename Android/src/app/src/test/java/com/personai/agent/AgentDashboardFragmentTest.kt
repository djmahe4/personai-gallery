package com.personai.agent

import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgentDashboardFragmentTest {

    @Test
    fun testDashboardFragmentViewContractAndState() {
        val fragment = AgentDashboardFragment()

        val viewState = AgentDashboardViewState(
            agentStatus = AgentStatus.EXECUTING_TOOL,
            isOverlayPermissionGranted = true,
            isAccessibilityEnabled = true,
            executionHistory = listOf(
                AgentExecutionRecord(
                    id = "rec-1",
                    prompt = "find pictures of sunsets",
                    functionCall = FunctionCall("searchImages", mapOf("query" to "sunsets")),
                    result = ToolExecutionResult.Success("searchImages", "Found 3 images matching sunsets")
                )
            )
        )

        fragment.updateViewState(viewState)
        val current = fragment.currentViewState()
        assertEquals(AgentStatus.EXECUTING_TOOL, current.agentStatus)
        assertTrue(current.isOverlayPermissionGranted)
        assertTrue(current.isAccessibilityEnabled)
        assertEquals(1, current.executionHistory.size)
        assertEquals("rec-1", current.executionHistory[0].id)
        assertEquals("searchImages", current.executionHistory[0].functionCall.name)

        // Save & Restore state
        val outBundle = Bundle()
        fragment.saveState(outBundle)

        val restoredFragment = AgentDashboardFragment()
        restoredFragment.restoreState(outBundle)

        val restored = restoredFragment.currentViewState()
        assertEquals(AgentStatus.EXECUTING_TOOL, restored.agentStatus)
        assertTrue(restored.isOverlayPermissionGranted)
        assertTrue(restored.isAccessibilityEnabled)
        assertEquals(1, restored.executionHistory.size)
        assertEquals("rec-1", restored.executionHistory[0].id)
        assertEquals("find pictures of sunsets", restored.executionHistory[0].prompt)
        assertEquals("searchImages", restored.executionHistory[0].functionCall.name)
        assertTrue(restored.executionHistory[0].result is ToolExecutionResult.Success)
        assertNotNull(restored)
    }

    @Test
    fun testHistoryCapOnSaveState() {
        val fragment = AgentDashboardFragment()
        val records = (1..60).map { i ->
            AgentExecutionRecord(
                id = "rec-$i",
                prompt = "prompt $i",
                functionCall = FunctionCall("searchImages", mapOf("query" to "$i")),
                result = ToolExecutionResult.Success("searchImages", "Result $i")
            )
        }

        fragment.updateViewState(AgentDashboardViewState(executionHistory = records))
        val outBundle = Bundle()
        fragment.saveState(outBundle)

        val restoredFragment = AgentDashboardFragment()
        restoredFragment.restoreState(outBundle)
        val restored = restoredFragment.currentViewState()

        assertEquals(AgentDashboardFragment.MAX_SAVED_HISTORY, restored.executionHistory.size)
        assertEquals("rec-11", restored.executionHistory.first().id)
        assertEquals("rec-60", restored.executionHistory.last().id)
    }
}
