package com.nedmah.textlector.domain.usecase.voice_model

import com.nedmah.textlector.domain.model.ModelState
import com.nedmah.textlector.domain.model.VoiceId
import com.nedmah.textlector.domain.model.VoiceRegistry
import com.nedmah.textlector.domain.repository.VoiceModelRepository
import kotlinx.coroutines.flow.Flow

class DownloadVoiceModelUseCase(
    private val repository: VoiceModelRepository
) {
    operator fun invoke(id: VoiceId): Flow<ModelState> =
        repository.downloadModel(VoiceRegistry.getById(id))
}