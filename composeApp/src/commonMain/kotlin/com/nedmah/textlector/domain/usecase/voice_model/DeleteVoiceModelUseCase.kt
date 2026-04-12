package com.nedmah.textlector.domain.usecase.voice_model

import com.nedmah.textlector.domain.model.VoiceId
import com.nedmah.textlector.domain.repository.VoiceModelRepository

class DeleteVoiceModelUseCase(
    private val repository: VoiceModelRepository
) {
    suspend operator fun invoke(id: VoiceId): Result<Unit> =
        repository.deleteModel(id)
}