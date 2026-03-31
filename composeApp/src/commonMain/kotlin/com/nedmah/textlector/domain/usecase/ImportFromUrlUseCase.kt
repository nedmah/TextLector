package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.domain.model.SourceType

class ImportFromUrlUseCase {
    suspend operator fun invoke(uri: String, type: SourceType): Result<Unit> {
        TODO("when adding ktor")
    }
}