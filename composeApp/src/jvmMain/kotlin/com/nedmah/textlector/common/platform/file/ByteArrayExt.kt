package com.nedmah.textlector.common.platform.file

internal actual fun ByteArray.decodeWithCharset(charset: String): String =
    toString(charset(charset))