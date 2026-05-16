package com.nedmah.textlector.data.repository

import com.nedmah.textlector.domain.model.ModelState
import com.nedmah.textlector.domain.repository.OcrDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class IosOcrDataRepository : OcrDataRepository {

    override fun getState(): Flow<ModelState> = flowOf(ModelState.Ready)

    override suspend fun download(): Flow<ModelState> = flowOf(ModelState.Ready)

    override suspend fun delete() = Unit

    override fun isReady(): Boolean = true
}