package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.domain.repository.DocumentRepository

class ToggleFavoriteUseCase(
    private val repository: DocumentRepository
) {

    suspend operator fun invoke(id: String, isFavorite: Boolean): Result<Unit> =
        repository.toggleFavorite(id, isFavorite)
}