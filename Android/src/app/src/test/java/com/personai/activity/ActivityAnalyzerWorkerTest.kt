package com.personai.activity

import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], manifest = Config.NONE)
class ActivityAnalyzerWorkerTest {

    @Test
    fun testPeriodicWorkManagerConstraints() {
        val constraints = ActivityAnalyzerWorker.createWorkConstraints()

        assertTrue("Must require device idle", constraints.requiresDeviceIdle())
        assertTrue("Must require battery not low", constraints.requiresBatteryNotLow())
        assertFalse("Must not require charging", constraints.requiresCharging())
        assertEquals(NetworkType.NOT_REQUIRED, constraints.requiredNetworkType)
    }

    @Test
    fun testDozeModeAnalysisDeferral() {
        // When device is in doze mode, analysis must be deferred (shouldProcess returns false)
        assertFalse(
            "Analysis must be deferred when in doze mode",
            ActivityAnalyzerWorker.shouldAnalyze(isDozeMode = true, batteryPercent = 85)
        )

        // When battery is critically low (< 15%), analysis must be deferred
        assertFalse(
            "Analysis must be deferred when battery is below threshold",
            ActivityAnalyzerWorker.shouldAnalyze(isDozeMode = false, batteryPercent = 14)
        )

        // When device is not in doze mode and battery is sufficient, analysis proceeds
        assertTrue(
            "Analysis should proceed when active and battery is sufficient",
            ActivityAnalyzerWorker.shouldAnalyze(isDozeMode = false, batteryPercent = 50)
        )
    }
}
