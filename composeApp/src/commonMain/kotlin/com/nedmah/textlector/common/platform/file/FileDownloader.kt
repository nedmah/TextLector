package com.nedmah.textlector.common.platform.file

interface FileDownloader {
    fun downloadFile(
        url : String,
        destPath : String,
        onProgress : (Float) -> Unit,
        onComplete : (Boolean) -> Unit
    )
}