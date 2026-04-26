package com.nedmah.textlector.common.platform.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics

actual object CrashReporter {

    actual fun log(message: String, tag: String) {
        val prefix = if (tag.isNotEmpty()) "[$tag] " else ""
        FirebaseCrashlytics.getInstance().log("$prefix$message")
    }

    actual fun recordException(e: Throwable, message: String) {
        if (message.isNotEmpty()) {
            FirebaseCrashlytics.getInstance().log("[ERROR] $message")
        }
        FirebaseCrashlytics.getInstance().recordException(e)
    }

    actual fun setKey(key: String, value: String) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }
}