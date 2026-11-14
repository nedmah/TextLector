package com.nedmah.textlector.data.repository

import android.content.Context
import com.nedmah.supertonic_kmp.api.DownloadState
import com.nedmah.supertonic_kmp.api.SupertonicTts
import com.nedmah.textlector.common.platform.logging.CrashReporter
import com.nedmah.textlector.data.download.HttpFileDownloader
import com.nedmah.textlector.domain.model.SupertonicModel
import com.nedmah.textlector.domain.model.SupertonicModelPaths
import com.nedmah.textlector.domain.model.SupertonicModelState
import com.nedmah.textlector.domain.repository.SupertonicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class AndroidSupertonicRepositoryImpl(
    context: Context
) : SupertonicRepository {

    private val modelDir = File(context.filesDir, SupertonicModel.DIR_NAME)
    private val legacyDir = File(context.filesDir, SupertonicModel.LEGACY_DIR_NAME)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _downloadState = MutableStateFlow(
        if (isComplete()) SupertonicModelState.Ready else SupertonicModelState.NotDownloaded
    )

    override val downloadState: StateFlow<SupertonicModelState> = _downloadState.asStateFlow()

    init {
        // supertonic-kmp model (app <= 1.3.0) is incompatible with sherpa-onnx
        scope.launch { if (legacyDir.exists()) legacyDir.deleteRecursively() }
    }

    override fun download(): Flow<SupertonicModelState> = flow {
        try {
            modelDir.mkdirs()
            var completedBytes = 0L
            for (file in SupertonicModel.files) {
                val dest = File(modelDir, file.name)
                if (!dest.exists()) {
                    HttpFileDownloader.download(file.url, dest) { downloaded, _ ->
                        _downloadState.value = SupertonicModelState.Downloading(
                            bytesDownloaded = (completedBytes + downloaded)
                                .coerceAtMost(SupertonicModel.totalBytes),
                            totalBytes = SupertonicModel.totalBytes,
                        )
                    }
                }
                completedBytes += dest.length()
            }
            _downloadState.value = SupertonicModelState.Ready
        } catch (e: CancellationException) {
            _downloadState.value = SupertonicModelState.NotDownloaded
            throw e
        } catch (e: Exception) {
            CrashReporter.recordException(e, "Supertonic download failed")
            _downloadState.value = SupertonicModelState.Error(e.message ?: "Download failed")
        }
        emit(_downloadState.value)
    }.flowOn(Dispatchers.IO)

    override fun deleteModel() {
        scope.launch {
            modelDir.deleteRecursively()
            _downloadState.value = SupertonicModelState.NotDownloaded
        }
    }

    override fun getModelPaths(): SupertonicModelPaths? =
        if (isComplete()) SupertonicModel.paths(modelDir.absolutePath) else null

    private fun isComplete() : Boolean = SupertonicModel.files.all { File(modelDir, it.name).exists() }

}