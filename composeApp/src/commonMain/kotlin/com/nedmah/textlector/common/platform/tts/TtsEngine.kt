package com.nedmah.textlector.common.platform.tts

import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.domain.model.VoiceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

interface TtsEngine {

    @OptIn(ExperimentalObjCRefinement::class)
    @HiddenFromObjC
    val engineChanged: Flow<Unit> get() = emptyFlow()

    suspend fun loadVoice(model: VoiceModel) {}
    fun setPlaylist(paragraphs: List<Paragraph>)
    suspend fun speak(index: Int, speed: Float)
    fun stop()
    fun shutdown()
}