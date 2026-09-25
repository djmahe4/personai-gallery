package com.personai.queue

class CpuPacingGovernor(
    private val monitor: ThermalBatteryMonitor
) {

    fun computePacingDelayMs(): Long {
        return when (monitor.thermalStatus.value) {
            ThermalStatus.NOMINAL -> 0L
            ThermalStatus.MODERATE -> 250L
            ThermalStatus.SEVERE -> 1000L
            ThermalStatus.CRITICAL -> 5000L
        }
    }

    fun canExecuteBackgroundJob(isHighPriority: Boolean): Boolean {
        if (isHighPriority) {
            // High priority tasks bypass thermal / low battery restrictions
            return true
        }

        if (monitor.thermalStatus.value == ThermalStatus.SEVERE || monitor.thermalStatus.value == ThermalStatus.CRITICAL) {
            return false
        }

        if (monitor.isBatteryLowDischarging()) {
            return false
        }

        return true
    }
}
