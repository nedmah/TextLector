package com.nedmah.textlector.common.platform.logging

actual object CrashReporter {

    actual fun log(message: String, tag: String) {
        // stub
    }

    actual fun recordException(e: Throwable, message: String) {
        // stub
    }

    actual fun setKey(key: String, value: String) {
        // stub
    }
}