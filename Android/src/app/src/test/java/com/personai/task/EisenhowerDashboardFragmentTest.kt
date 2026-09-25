package com.personai.task

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EisenhowerDashboardFragmentTest {

    @Test
    fun testDashboardFragmentViewContract() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fragment = EisenhowerDashboardFragment()

        val viewState = EisenhowerDashboardViewState(
            q1Count = 2,
            q2Count = 5,
            q3Count = 1,
            q4Count = 0,
            topSuggestions = listOf(
                TaskSuggestion(
                    task = EisenhowerTask(id = "1", title = "Fix Bug"),
                    rationale = "Immediate attention required",
                    actionLabel = "Do Now"
                )
            )
        )

        fragment.updateViewState(viewState)
        assertEquals(2, fragment.currentViewState().q1Count)
        assertEquals(5, fragment.currentViewState().q2Count)
        assertEquals(1, fragment.currentViewState().q3Count)
        assertEquals(0, fragment.currentViewState().q4Count)
        assertEquals(1, fragment.currentViewState().topSuggestions.size)
        assertEquals("Fix Bug", fragment.currentViewState().topSuggestions[0].task.title)

        var clickedSuggestion: TaskSuggestion? = null
        fragment.onActionClickListener = { clickedSuggestion = it }
        fragment.onActionClickListener?.invoke(viewState.topSuggestions[0])

        assertNotNull(clickedSuggestion)
        assertEquals("Fix Bug", clickedSuggestion?.task?.title)

        // Verify state preservation across bundle save/restore
        val outBundle = android.os.Bundle()
        fragment.saveState(outBundle)

        val restoredFragment = EisenhowerDashboardFragment()
        restoredFragment.restoreState(outBundle)
        assertEquals(2, restoredFragment.currentViewState().q1Count)
        assertEquals(5, restoredFragment.currentViewState().q2Count)
        assertEquals(1, restoredFragment.currentViewState().q3Count)
        assertEquals(0, restoredFragment.currentViewState().q4Count)
        assertNotNull(restoredFragment)
    }
}
