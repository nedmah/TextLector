package com.nedmah.textlector.common.platform.file

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.dataWithBytes

@OptIn(ExperimentalForeignApi::class)
internal actual fun ByteArray.decodeWithCharset(charset: String): String {
    if (charset.lowercase() !in listOf("windows-1251", "cp1251")) {
        return decodeToString()
    }
    return usePinned { pinned ->
        val nsData = NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
        // NSWindowsCP1251StringEncoding = 0x0A01
        NSString.create(nsData, 0x0A01u)?.toString() ?: decodeToString()
    }
}