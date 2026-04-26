package com.nedmah.textlector.common.platform.logging

actual object CrashReporter {
    actual fun log(message: String, tag: String) {
    }

    actual fun recordException(e: Throwable, message: String) {
    }

    actual fun setKey(key: String, value: String) {
    }
}