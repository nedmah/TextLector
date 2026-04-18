package com.nedmah.textlector.common.platform.file

interface PdfPageExtractor {
    fun extractPage(path: String, pageIndex: Int): String
    fun ocrPage(path: String, pageIndex: Int): String
    fun pageCount(path: String): Int
}