package com.nedmah.textlector.common.platform.file

expect class FileReader {
    suspend fun readPdf(uri: String, onProgress: (current: Int, total: Int) -> Unit): Result<String>
    suspend fun readBytes(uri: String): Result<ByteArray>

}