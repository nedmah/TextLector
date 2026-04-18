package com.nedmah.textlector.domain.util


object TextSegmenter {
    private const val MIN_PARAGRAPHS = 3
    private const val MAX_WORDS_PER_PARAGRAPH = 100
    private const val MIN_WORDS_PER_PARAGRAPH = 20

    fun segment(text: String): List<String> {

        val cleaned = clean(text)

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

        val split = candidates.flatMap { paragraph ->
            if (paragraph.wordCount() > MAX_WORDS_PER_PARAGRAPH) {
                splitBySentences(paragraph)
            } else {
                listOf(paragraph)
            }
        }

        return mergeShortParagraphs(split)
    }

    private fun clean(text: String): String {
        return text
            .replace(Regex("\\r\\n"), "\n") // windows line endings
            .replace(Regex("[ \\t]+"), " ") // lots of spaces
            .replace(Regex("\\n{3,}"), "\n\n") // 3+ empty strings
            .replace(Regex("(?m)^\\s*\\d+\\s*$"), "") // when only nums
            .replace(Regex("(?m)^.{1,3}$\\n"), "") // garbage
            .trim()
    }

    private fun mergeShortParagraphs(paragraphs: List<String>): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()

        paragraphs.forEach { paragraph ->
            val isShort = paragraph.wordCount() < MIN_WORDS_PER_PARAGRAPH
            val isLikelyHeader = paragraph.wordCount() < 8 && !paragraph.endsWith(".")

            when {
                isLikelyHeader -> {
                    if (current.isNotEmpty()) {
                        result.add(current.trim().toString())
                        current.clear()
                    }
                    current.append(paragraph).append(" ")
                }
                isShort -> {
                    current.append(paragraph).append(" ")
                    if (current.wordCount() >= MIN_WORDS_PER_PARAGRAPH) {
                        result.add(current.trim().toString())
                        current.clear()
                    }
                }
                else -> {
                    if (current.isNotEmpty()) {
                        current.append(paragraph)
                        result.add(current.trim().toString())
                        current.clear()
                    } else {
                        result.add(paragraph)
                    }
                }
            }
        }

        if (current.isNotBlank()) result.add(current.trim().toString())
        return result.ifEmpty { paragraphs }
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