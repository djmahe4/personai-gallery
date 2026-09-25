package com.personai.queue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CpuPacingGovernorTest {

    private lateinit var monitor: ThermalBatteryMonitor
    private lateinit var governor: CpuPacingGovernor

    @Before
    fun setUp() {
        monitor = ThermalBatteryMonitor()
        governor = CpuPacingGovernor(monitor)
    }

    @Test
    fun testNominalThermalAndHealthyBattery_zeroPacingDelay() {
        monitor.updateThermalStatus(ThermalStatus.NOMINAL)
        monitor.updateBattery(level = 80, isCharging = false)

        assertEquals(0L, governor.computePacingDelayMs())
        assertTrue(governor.canExecuteBackgroundJob(isHighPriority = false))
    }

    @Test
    fun testModerateThermal_injectsThrottleDelay() {
        monitor.updateThermalStatus(ThermalStatus.MODERATE)
        monitor.updateBattery(level = 80, isCharging = false)

        assertEquals(250L, governor.computePacingDelayMs())
        assertTrue(governor.canExecuteBackgroundJob(isHighPriority = false))
    }

    @Test
    fun testSevereThermal_injectsHeavyBackoff() {
        monitor.updateThermalStatus(ThermalStatus.SEVERE)
        monitor.updateBattery(level = 80, isCharging = false)

        assertEquals(1000L, governor.computePacingDelayMs())
        assertFalse(governor.canExecuteBackgroundJob(isHighPriority = false))
        assertTrue(governor.canExecuteBackgroundJob(isHighPriority = true)) // High priority bypass
    }

    @Test
    fun testCriticalThermalOrLowBatteryDischarging_pausesNonCritical() {
        // Critical thermal
        monitor.updateThermalStatus(ThermalStatus.CRITICAL)
        monitor.updateBattery(level = 80, isCharging = false)
        assertFalse(governor.canExecuteBackgroundJob(isHighPriority = false))
        assertEquals(5000L, governor.computePacingDelayMs())

        // Low battery < 20% discharging
        monitor.updateThermalStatus(ThermalStatus.NOMINAL)
        monitor.updateBattery(level = 18, isCharging = false)
        assertFalse(governor.canExecuteBackgroundJob(isHighPriority = false))
        assertTrue(governor.canExecuteBackgroundJob(isHighPriority = true)) // High priority bypass
    }
}
