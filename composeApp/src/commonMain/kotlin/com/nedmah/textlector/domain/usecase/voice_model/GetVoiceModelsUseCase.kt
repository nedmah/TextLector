package com.nedmah.textlector.domain.usecase.voice_model

import com.nedmah.textlector.domain.model.ModelState
import com.nedmah.textlector.domain.model.VoiceModel
import com.nedmah.textlector.domain.model.VoiceRegistry
import com.nedmah.textlector.domain.repository.VoiceModelRepository
import kotlinx.coroutines.flow.Flow

class GetVoiceModelsUseCase(
    private val repository: VoiceModelRepository
) {

    operator fun invoke(): List<VoiceModelWithState> =
        VoiceRegistry.all.map { model ->
            VoiceModelWithState(
                model = model,
                state = repository.getModelState(model.id)
            )
        }

}

data class VoiceModelWithState(
    val model: VoiceModel,
    val state: Flow<ModelState>
)