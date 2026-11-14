package com.nedmah.textlector.domain.repository

import com.nedmah.textlector.domain.model.SupertonicModelPaths
import com.nedmah.textlector.domain.model.SupertonicModelState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SupertonicRepository {
    val downloadState: StateFlow<SupertonicModelState>
    fun download(): Flow<SupertonicModelState>
    fun deleteModel()
    fun getModelPaths(): SupertonicModelPaths? // null if the model isn't fully downloaded
}