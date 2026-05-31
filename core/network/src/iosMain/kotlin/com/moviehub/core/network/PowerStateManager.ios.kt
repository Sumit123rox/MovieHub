package com.moviehub.core.network

import com.moviehub.core.database.PlatformContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.UIKit.UIDevice
import kotlin.time.Duration.Companion.seconds

actual class PowerStateManager actual constructor(private val ctx: PlatformContext) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(refreshState())
    actual val state: StateFlow<PowerState> = _state.asStateFlow()

    init {
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(30.seconds)
                _state.value = refreshState()
            }
        }
    }

    private fun refreshState(): PowerState {
        val device = UIDevice.currentDevice
        val isPowerSave = false // NSProcessInfo.processInfo.isLowPowerModeEnabled not in Kotlin/Native bindings
        val batteryLevel = device.batteryLevel.toFloat()
        return PowerState(
            isPowerSaveMode = isPowerSave,
            batteryLevel = if (batteryLevel >= 0f) batteryLevel else 1f,
        )
    }
}
