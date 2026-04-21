package com.nedmah.textlector.common.platform.file

internal expect fun ByteArray.decodeWithCharset(charset: String): String