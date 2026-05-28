package com.moviehub.core.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import com.moviehub.core.database.PlatformContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

actual class PowerStateManager actual constructor(private val ctx: PlatformContext) {
    private val appContext = ctx.applicationContext
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(refreshState())
    actual val state: StateFlow<PowerState> = _state.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            _state.value = refreshState()
        }
    }

    init {
        scope.launch {
            try {
                val filter = IntentFilter().apply {
                    addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                    addAction(Intent.ACTION_BATTERY_LOW)
                    addAction(Intent.ACTION_BATTERY_OKAY)
                }
                appContext.registerReceiver(receiver, filter)
            } catch (_: Exception) { }
        }
    }

    private fun refreshState(): PowerState {
        val isPowerSave = powerManager.isPowerSaveMode
        val batteryLevel = try {
            val batteryIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level > 0 && scale > 0) level.toFloat() / scale else 1f
        } catch (_: Exception) { 1f }
        return PowerState(isPowerSaveMode = isPowerSave, batteryLevel = batteryLevel)
    }
}
