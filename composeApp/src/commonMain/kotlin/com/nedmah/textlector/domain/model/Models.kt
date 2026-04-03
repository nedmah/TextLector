package com.nedmah.textlector.domain.model

data class ProcessedDocument(
    val document: Document,
    val paragraphs: List<Paragraph>
)

data class Document(
    val id: String,           // UUID
    val title: String,
    val sourceType: SourceType,
    val createdAt: Long,
    val isFavorite: Boolean,
    val wordCount: Int,
    val estimatedReadingMinutes: Int,
    val lastOpenedAt: Long,
    val totalParagraphs: Int,
    val lastParagraphIndex: Int // for progress
)

data class Paragraph(
    val id : String,
    val documentId : String,
    val index : Int,
    val text : String,
)

sealed class SourceType{
    data object Manual : SourceType()
    data object Txt : SourceType()
    data object Pdf : SourceType()
    data class Url(val url : String) : SourceType()
    data object Docx : SourceType() // v2
    data object Camera : SourceType() // v2
}

enum class DocumentSortOrder { LAST_OPENED, CREATED_AT}

data class UserPreferences(
    val speechSpeed: Float,  // 0,5f - 2f
    val speechVoice: VoiceGender,
    val fontSize: Int,
    val isDarkMode: Boolean,
    val language: String
)

enum class VoiceGender { MALE, FEMALE}