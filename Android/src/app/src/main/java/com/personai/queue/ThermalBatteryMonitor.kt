package com.personai.queue

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThermalStatus {
    NOMINAL,
    MODERATE,
    SEVERE,
    CRITICAL
}

class ThermalBatteryMonitor {
    private val _thermalStatus = MutableStateFlow(ThermalStatus.NOMINAL)
    val thermalStatus: StateFlow<ThermalStatus> = _thermalStatus.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    fun updateThermalStatus(status: ThermalStatus) {
        _thermalStatus.value = status
    }

    fun updateBattery(level: Int, isCharging: Boolean) {
        _batteryLevel.value = level
        _isCharging.value = isCharging
    }

    fun isBatteryLowDischarging(): Boolean {
        return _batteryLevel.value < 20 && !_isCharging.value
    }
}
