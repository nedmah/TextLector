package com.nedmah.textlector

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform