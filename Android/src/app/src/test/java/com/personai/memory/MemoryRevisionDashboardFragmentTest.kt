package com.personai.memory

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryRevisionDashboardFragmentTest {

    @Test
    fun fragmentInitializesWithZeroDueCount() {
        val fragment = MemoryRevisionDashboardFragment()
        assertEquals(0, fragment.currentDueCount())
    }
}
