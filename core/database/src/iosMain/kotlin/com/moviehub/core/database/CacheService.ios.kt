package com.moviehub.core.database

import platform.Foundation.NSDate

internal actual fun cacheServiceTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()
