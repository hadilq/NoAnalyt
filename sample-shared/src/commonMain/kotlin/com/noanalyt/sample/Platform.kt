package com.noanalyt.sample

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform