package com.nedmah.textlector

interface Platform {
    val name: String
    val hasSystemTts: Boolean get() = true
    val hasCamera: Boolean get() = true
}

expect fun getPlatform(): Platform