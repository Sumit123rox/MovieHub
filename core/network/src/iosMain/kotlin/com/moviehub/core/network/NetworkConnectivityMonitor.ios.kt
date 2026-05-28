package com.moviehub.core.network

import com.moviehub.core.database.PlatformContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS NetworkConnectivityMonitor — defaults to online.
 * Future: wire NWPathMonitor via Network framework cinterop for real connectivity awareness.
 */
actual class NetworkConnectivityMonitor actual constructor(ctx: PlatformContext) {
    private val _isOnline = MutableStateFlow(true)
    actual val isOnline: StateFlow<Boolean> = _isOnline
    actual val isOnlineNow: Boolean get() = true
}
