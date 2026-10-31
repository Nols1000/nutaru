package com.github.nols1000.nutaru

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform