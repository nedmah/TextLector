package com.nedmah.textlector.common.platform.file

expect class FileReader {
    suspend fun readText(uri: String): Result<String>
    suspend fun readPdf(uri: String): Result<String>
}