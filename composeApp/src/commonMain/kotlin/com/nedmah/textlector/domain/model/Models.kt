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

    data object Epub : SourceType()

    data object Fb2 : SourceType()
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
    val language: String,
    val useSherpaEngine: Boolean = false,
) {
    fun resolveVoiceId(): VoiceId =
        when {
            this.language == "ru" && this.speechVoice == VoiceGender.MALE -> VoiceId.RU_MALE
            this.language == "ru" && this.speechVoice == VoiceGender.FEMALE -> VoiceId.RU_FEMALE
            this.language == "en" && this.speechVoice == VoiceGender.MALE -> VoiceId.EN_MALE
            else -> VoiceId.EN_FEMALE
        }
}

enum class VoiceGender { MALE, FEMALE}