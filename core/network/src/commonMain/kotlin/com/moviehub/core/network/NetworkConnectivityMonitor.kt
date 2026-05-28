package com.moviehub.core.network

import com.moviehub.core.database.PlatformContext
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-aware network connectivity monitor.
 * Exposes reactive [isOnline] StateFlow and synchronous [isOnlineNow] check.
 */
expect class NetworkConnectivityMonitor(ctx: PlatformContext) {
    val isOnline: StateFlow<Boolean>
    val isOnlineNow: Boolean
}
