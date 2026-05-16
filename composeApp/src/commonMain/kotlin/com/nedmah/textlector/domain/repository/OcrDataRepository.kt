package com.nedmah.textlector.domain.repository

import com.nedmah.textlector.domain.model.ModelState
import kotlinx.coroutines.flow.Flow

interface OcrDataRepository {

    fun getState(): Flow<ModelState>
    suspend fun download() : Flow<ModelState>
    suspend fun delete()
    fun isReady(): Boolean
}