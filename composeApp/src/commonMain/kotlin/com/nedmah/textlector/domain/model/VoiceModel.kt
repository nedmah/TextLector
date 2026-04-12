package com.nedmah.textlector.domain.model

data class ModelPath(
    val onnxPath: String,
    val tokensPath: String,
    val espeakDataPath: String
)

sealed class ModelState {
    data object NotDownloaded : ModelState()
    data class Downloading(val progress: Float) : ModelState()  // 0f..1f
    data object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}

enum class VoiceId {
    RU_MALE, RU_FEMALE, EN_MALE, EN_FEMALE
}

data class VoiceModel(
    val id: VoiceId,
    val displayName: String,
    val language: String,
    val gender: VoiceGender,
    val onnxUrl: String,
    val tokensUrl: String,
    val onnxFileName: String,  // file's name on the disk
    val modelDirName: String,  // folder in filesDir
    val sizeBytes: Long
)

object VoiceRegistry {
    private const val HF_BASE = "https://huggingface.co/csukuangfj"
    const val ESPEAK_DATA_URL =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/espeak-ng-data.tar.bz2"

    val all: List<VoiceModel> = listOf(
        VoiceModel(
            id = VoiceId.RU_MALE,
            displayName = "Ruslan (RU)",
            language = "ru",
            gender = VoiceGender.MALE,
            onnxUrl = "$HF_BASE/vits-piper-ru_RU-ruslan-medium/resolve/main/ru_RU-ruslan-medium.onnx",
            tokensUrl = "$HF_BASE/vits-piper-ru_RU-ruslan-medium/resolve/main/tokens.txt",
            onnxFileName = "ru_RU-ruslan-medium.onnx",
            modelDirName = "vits-piper-ru_RU-ruslan-medium",
            sizeBytes = 63_000_000L
        ),
        VoiceModel(
            id = VoiceId.RU_FEMALE,
            displayName = "Irina (RU)",
            language = "ru",
            gender = VoiceGender.FEMALE,
            onnxUrl = "$HF_BASE/vits-piper-ru_RU-irina-medium/resolve/main/ru_RU-irina-medium.onnx",
            tokensUrl = "$HF_BASE/vits-piper-ru_RU-irina-medium/resolve/main/tokens.txt",
            onnxFileName = "ru_RU-irina-medium.onnx",
            modelDirName = "vits-piper-ru_RU-irina-medium",
            sizeBytes = 63_000_000L
        ),
        VoiceModel(
            id = VoiceId.EN_MALE,
            displayName = "Ryan (EN)",
            language = "en",
            gender = VoiceGender.MALE,
            onnxUrl = "$HF_BASE/vits-piper-en_US-ryan-medium/resolve/main/en_US-ryan-medium.onnx",
            tokensUrl = "$HF_BASE/vits-piper-en_US-ryan-medium/resolve/main/tokens.txt",
            onnxFileName = "en_US-ryan-medium.onnx",
            modelDirName = "vits-piper-en_US-ryan-medium",
            sizeBytes = 63_000_000L
        ),
        VoiceModel(
            id = VoiceId.EN_FEMALE,
            displayName = "Lessac (EN)",
            language = "en",
            gender = VoiceGender.FEMALE,
            onnxUrl = "$HF_BASE/vits-piper-en_US-lessac-medium/resolve/main/en_US-lessac-medium.onnx",
            tokensUrl = "$HF_BASE/vits-piper-en_US-lessac-medium/resolve/main/tokens.txt",
            onnxFileName = "en_US-lessac-medium.onnx",
            modelDirName = "vits-piper-en_US-lessac-medium",
            sizeBytes = 63_000_000L
        )
    )

    fun getDefault(systemLanguage: String): VoiceModel =
        if (systemLanguage.startsWith("ru")) all.first { it.id == VoiceId.RU_MALE }
        else all.first { it.id == VoiceId.EN_MALE }

    fun getById(id: VoiceId): VoiceModel = all.first { it.id == id }
}