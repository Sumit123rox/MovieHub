package com.moviehub.core.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class NetworkDispatchers(
    val io: CoroutineDispatcher = Dispatchers.Default
)
