package com.nedmah.textlector.domain.repository

import com.nedmah.textlector.domain.model.ModelPath
import com.nedmah.textlector.domain.model.ModelState
import com.nedmah.textlector.domain.model.VoiceId
import com.nedmah.textlector.domain.model.VoiceModel
import kotlinx.coroutines.flow.Flow

interface VoiceModelRepository {

    fun getModelState(id: VoiceId): Flow<ModelState>

    fun downloadModel(model: VoiceModel): Flow<ModelState>

    // null if not downloaded
    fun getModelPath(id: VoiceId): ModelPath?

    suspend fun deleteModel(id: VoiceId): Result<Unit>

    fun isEspeakDataReady(): Boolean

}