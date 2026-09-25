package com.personai.queue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ThermalBatteryMonitorTest {

    private lateinit var monitor: ThermalBatteryMonitor

    @Before
    fun setUp() {
        monitor = ThermalBatteryMonitor()
    }

    @Test
    fun testThermalStatusTracking() {
        assertEquals(ThermalStatus.NOMINAL, monitor.thermalStatus.value)

        monitor.updateThermalStatus(ThermalStatus.MODERATE)
        assertEquals(ThermalStatus.MODERATE, monitor.thermalStatus.value)

        monitor.updateThermalStatus(ThermalStatus.SEVERE)
        assertEquals(ThermalStatus.SEVERE, monitor.thermalStatus.value)

        monitor.updateThermalStatus(ThermalStatus.CRITICAL)
        assertEquals(ThermalStatus.CRITICAL, monitor.thermalStatus.value)
    }

    @Test
    fun testBatteryLevelAndChargingStateTransitions() {
        assertEquals(100, monitor.batteryLevel.value)
        assertFalse(monitor.isCharging.value)

        monitor.updateBattery(level = 15, isCharging = false)
        assertEquals(15, monitor.batteryLevel.value)
        assertFalse(monitor.isCharging.value)
        assertTrue(monitor.isBatteryLowDischarging())

        monitor.updateBattery(level = 15, isCharging = true)
        assertTrue(monitor.isCharging.value)
        assertFalse(monitor.isBatteryLowDischarging())
    }
}
