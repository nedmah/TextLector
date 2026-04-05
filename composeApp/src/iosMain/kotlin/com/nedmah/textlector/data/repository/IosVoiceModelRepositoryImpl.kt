package com.nedmah.textlector.data.repository

import com.nedmah.textlector.domain.model.ModelPath
import com.nedmah.textlector.domain.model.ModelState
import com.nedmah.textlector.domain.model.VoiceId
import com.nedmah.textlector.domain.model.VoiceModel
import com.nedmah.textlector.domain.repository.VoiceModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class IosVoiceModelRepositoryImpl : VoiceModelRepository {

    override fun getModelState(id: VoiceId): Flow<ModelState> =
        flowOf(ModelState.NotDownloaded)

    override fun downloadModel(model: VoiceModel): Flow<ModelState> =
        flowOf(ModelState.NotDownloaded) // Phase 3

    override fun getModelPath(id: VoiceId): ModelPath? = null

    override suspend fun deleteModel(id: VoiceId): Result<Unit> =
        Result.success(Unit)

    override fun isEspeakDataReady(): Boolean = false
}