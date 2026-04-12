package com.nedmah.textlector.common.platform.file

interface TarExtractor {
    fun extractTarBz2(archivePath: String, destPath: String): Boolean
}