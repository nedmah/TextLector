package com.nedmah.textlector.domain.util


object TextSegmenter {
    private const val MIN_PARAGRAPHS = 3
    private const val MAX_WORDS_PER_PARAGRAPH = 100
    private const val MIN_WORDS_PER_PARAGRAPH = 20
    private const val HARD_WRAP_MIN_LINE = 30
    private const val HARD_WRAP_MAX_LINE = 130
    private const val SHORT_LINE_RATIO = 0.75

    fun segment(text: String): List<String> {

        val cleaned = unwrapHardLines(clean(text))

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
            .trim()
    }

    private fun unwrapHardLines(text: String): String {
        val lines = text.lines().map { it.trim() }
        val lengths = lines.filter { it.isNotEmpty() }.map { it.length }.sorted()
        if (lengths.size < MIN_PARAGRAPHS) return text

        val typical = lengths[(lengths.size * 0.8).toInt().coerceAtMost(lengths.lastIndex)]
        if (typical !in HARD_WRAP_MIN_LINE..HARD_WRAP_MAX_LINE) return text

        val result = StringBuilder()
        lines.forEachIndexed { i, line ->
            result.append(line)
            if (i == lines.lastIndex) return@forEachIndexed
            val next = lines[i + 1]
            when {
                line.isEmpty() || next.isEmpty() -> result.append('\n')
                line.length < typical * SHORT_LINE_RATIO -> result.append('\n')
                line.endsWith('-') -> Unit
                else -> result.append(' ')
            }
        }
        return result.toString()
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
                    result.add(paragraph)
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