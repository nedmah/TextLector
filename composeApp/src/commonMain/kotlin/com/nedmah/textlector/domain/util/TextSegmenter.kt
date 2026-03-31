package com.nedmah.textlector.domain.util


object TextSegmenter {
    private const val MIN_PARAGRAPHS = 3
    private const val MAX_WORDS_PER_PARAGRAPH = 150

    fun segment(text: String): List<String> {

        val cleaned = text.trim()

        val byDoubleNewline = cleaned.split("\n\n")
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        val candidates = if (byDoubleNewline.size >= MIN_PARAGRAPHS) {
            byDoubleNewline
        } else {
            val bySingleNewline = cleaned.split("\n")
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toList()

            if (bySingleNewline.size >= MIN_PARAGRAPHS) {
                bySingleNewline
            } else {
                splitBySentences(cleaned)
            }
        }

        return candidates.flatMap { paragraph ->
            if (paragraph.wordCount() > MAX_WORDS_PER_PARAGRAPH){
                splitBySentences(paragraph)
            } else{
                listOf(paragraph)
            }
        }
    }

    private fun splitBySentences(text: String): List<String> {
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        val chunks = mutableListOf<String>()
        val current = StringBuilder()

        sentences.forEach { sentence ->
            if (current.wordCount() + sentence.wordCount() > MAX_WORDS_PER_PARAGRAPH
                && current.isNotEmpty()
            ) {
                chunks.add(current.trim().toString())
                current.clear()
            }
            current.append(sentence).append(" ")
        }

        if (current.isNotBlank()) chunks.add(current.trim().toString())
        return chunks.ifEmpty { listOf(text) }
    }

    private fun String.wordCount(): Int =
        trim().split(Regex("\\s+")).count { it.isNotBlank() }

    private fun StringBuilder.wordCount(): Int = toString().wordCount()
}