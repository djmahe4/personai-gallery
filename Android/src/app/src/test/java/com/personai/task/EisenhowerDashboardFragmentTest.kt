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

        assertEquals(2, viewState.q1Count)
        assertEquals(5, viewState.q2Count)
        assertEquals(1, viewState.q3Count)
        assertEquals(0, viewState.q4Count)
        assertEquals(1, viewState.topSuggestions.size)
        assertEquals("Fix Bug", viewState.topSuggestions[0].task.title)
        assertNotNull(fragment)
    }
}
