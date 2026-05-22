package com.sumit.moviehub

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform