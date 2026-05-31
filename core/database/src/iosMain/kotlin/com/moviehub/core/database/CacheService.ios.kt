package com.moviehub.core.database

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
internal actual fun cacheServiceTimeMillis(): Long =
    time(null) * 1000L
