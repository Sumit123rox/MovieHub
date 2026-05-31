package com.moviehub.core.network

import com.moviehub.core.database.PlatformContext
import kotlinx.coroutines.flow.StateFlow

data class PowerState(
    val isPowerSaveMode: Boolean = false,
    val batteryLevel: Float = 1f,
    val isLowBattery: Boolean = batteryLevel < 0.2f || isPowerSaveMode,
)

expect class PowerStateManager(ctx: PlatformContext) {
    val state: StateFlow<PowerState>
}
