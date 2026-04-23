package com.nedmah.textlector.common.platform.tts

import com.nedmah.textlector.domain.model.VoiceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

interface TtsEngine {

    @OptIn(ExperimentalObjCRefinement::class)
    @HiddenFromObjC
    val engineChanged: Flow<Unit> get() = emptyFlow()

    suspend fun speak(text : String, speed: Float)
    suspend fun loadVoice(model: VoiceModel)
    fun stop()
    fun shutdown()
    fun piperEngine(): PiperTtsEngine? = null
}