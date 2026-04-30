package com.nedmah.textlector.common.platform.logging

expect object CrashReporter {
    fun log(message: String, tag: String)
    fun recordException(e: Throwable, message: String)
    fun setKey(key: String, value: String)
}