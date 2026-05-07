package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.domain.model.ModelState
import com.nedmah.textlector.domain.repository.OcrDataRepository
import kotlinx.coroutines.flow.Flow

class DownloadOcrDataUseCase(
    private val repository: OcrDataRepository
) {
    operator fun invoke(): Flow<ModelState> = repository.download()
}