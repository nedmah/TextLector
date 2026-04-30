package com.nedmah.textlector.common.platform.util

actual val isDebug: Boolean = System.getProperty("debug")?.toBoolean() ?: false