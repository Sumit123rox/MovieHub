package com.moviehub.core.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Platform-aware dispatchers for network operations.
 * Uses Dispatchers.Default as base (works on all KMP targets).
 * Android DI can override io with Dispatchers.IO for dedicated I/O threads.
 */
data class NetworkDispatchers(
    val io: CoroutineDispatcher = Dispatchers.Default,
)
