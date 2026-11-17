package com.nedmah.textlector.di

import com.nedmah.textlector.common.platform.ocr.OcrEngine
import com.nedmah.textlector.common.platform.tts.SherpaOnnxTtsEngine
import com.nedmah.textlector.domain.model.ModelState
import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.domain.model.PiperModelPath
import com.nedmah.textlector.domain.model.SupertonicModelPaths
import com.nedmah.textlector.domain.model.SupertonicModelState
import com.nedmah.textlector.domain.model.VoiceId
import com.nedmah.textlector.domain.model.VoiceModel
import com.nedmah.textlector.domain.repository.OcrDataRepository
import com.nedmah.textlector.domain.repository.SupertonicRepository
import com.nedmah.textlector.domain.repository.VoiceModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

// Temporary stubs so the desktop app starts. Replaced one by one in phase 2.

private const val NOT_YET = "Not available on desktop yet"

internal class StubVoiceModelRepository : VoiceModelRepository {
    override fun getModelState(id: VoiceId): Flow<ModelState> = flowOf(ModelState.NotDownloaded)
    override fun downloadModel(model: VoiceModel): Flow<ModelState> = flowOf(ModelState.Error(NOT_YET))
    override fun getModelPath(id: VoiceId): PiperModelPath? = null
    override suspend fun deleteModel(id: VoiceId): Result<Unit> = Result.success(Unit)
    override fun isEspeakDataReady(): Boolean = false
}

internal class StubSupertonicRepository : SupertonicRepository {
    override val downloadState: StateFlow<SupertonicModelState> =
        MutableStateFlow(SupertonicModelState.NotDownloaded)
    override fun download(): Flow<SupertonicModelState> = flowOf(SupertonicModelState.Error(NOT_YET))
    override fun deleteModel() = Unit
    override fun getModelPaths(): SupertonicModelPaths? = null
}

internal class StubOcrDataRepository : OcrDataRepository {
    override fun getState(): Flow<ModelState> = flowOf(ModelState.NotDownloaded)
    override suspend fun download(): Flow<ModelState> = flowOf(ModelState.Error(NOT_YET))
    override suspend fun delete() = Unit
    override fun isReady(): Boolean = false
}

internal class StubOcrEngine : OcrEngine {
    override suspend fun recognize(imageUri: String): String =
        throw UnsupportedOperationException("OCR is not available on desktop")
}

internal class SilentTtsEngine : SherpaOnnxTtsEngine {
    override suspend fun loadVoice(model: VoiceModel) = Unit
    override fun setPlaylist(paragraphs: List<Paragraph>) = Unit
    override suspend fun speak(index: Int, speed: Float) = Unit
    override suspend fun generate(text: String, speed: Float): ByteArray = ByteArray(0)
    override suspend fun playAudio(audio: ByteArray) = Unit
    override fun stop() = Unit
    override fun shutdown() = Unit
}