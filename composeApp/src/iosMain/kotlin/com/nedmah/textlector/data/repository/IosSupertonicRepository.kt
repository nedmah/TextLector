@file:OptIn(ExperimentalForeignApi::class)

package com.nedmah.textlector.data.repository

import com.nedmah.textlector.common.platform.logging.CrashReporter
import com.nedmah.textlector.di.IosEngineHolder
import com.nedmah.textlector.domain.model.SupertonicModel
import com.nedmah.textlector.domain.model.SupertonicModelPaths
import com.nedmah.textlector.domain.model.SupertonicModelState
import com.nedmah.textlector.domain.repository.SupertonicRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class IosSupertonicRepository : SupertonicRepository {

    private val fileManager = NSFileManager.defaultManager
    // Documents path changes on every reinstall, so it's resolved at runtime, never stored
    private val documentsDir = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, true
    ).first() as String
    private val modelDir = "$documentsDir/${SupertonicModel.DIR_NAME}"
    private val legacyDir = "$documentsDir/${SupertonicModel.LEGACY_DIR_NAME}"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _downloadState = MutableStateFlow(
        if (isComplete()) SupertonicModelState.Ready else SupertonicModelState.NotDownloaded
    )
    override val downloadState: StateFlow<SupertonicModelState> = _downloadState.asStateFlow()

    init {
        // supertonic-kmp model (app <= 1.3.0) is incompatible with sherpa-onnx
        scope.launch {
            if (fileManager.fileExistsAtPath(legacyDir)) fileManager.removeItemAtPath(legacyDir, null)
        }
    }

    override fun download(): Flow<SupertonicModelState> = flow {
        try {
            fileManager.createDirectoryAtPath(modelDir, true, null, null)
            var completedBytes = 0L
            for (file in SupertonicModel.files) {
                val dest = "$modelDir/${file.name}"
                if (!fileManager.fileExistsAtPath(dest)) {
                    val base = completedBytes
                    downloadFile(file.url, dest) { progress ->
                        _downloadState.value = SupertonicModelState.Downloading(
                            bytesDownloaded = (base + (progress * file.approxSizeBytes).toLong())
                                .coerceAtMost(SupertonicModel.totalBytes),
                            totalBytes = SupertonicModel.totalBytes,
                        )
                    }
                }
                completedBytes += file.approxSizeBytes
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
    }.flowOn(Dispatchers.Default)

    override fun deleteModel() {
        scope.launch {
            fileManager.removeItemAtPath(modelDir, null)
            _downloadState.value = SupertonicModelState.NotDownloaded
        }
    }

    override fun getModelPaths(): SupertonicModelPaths? =
        if (isComplete()) SupertonicModel.paths(modelDir) else null

    private fun isComplete(): Boolean =
        SupertonicModel.files.all { fileManager.fileExistsAtPath("$modelDir/${it.name}") }

    private suspend fun downloadFile(
        url: String,
        destPath: String,
        onProgress: (Float) -> Unit,
    ) = suspendCancellableCoroutine { cont ->
        val downloader = IosEngineHolder.fileDownloader
        if (downloader == null) {
            cont.resumeWithException(IllegalStateException("FileDownloader not initialized"))
            return@suspendCancellableCoroutine
        }
        downloader.downloadFile(
            url = url,
            destPath = destPath,
            onProgress = onProgress,
            onComplete = { success ->
                if (!cont.isActive) return@downloadFile
                if (success) cont.resume(Unit)
                else cont.resumeWithException(Exception("Download failed: $url"))
            },
        )
    }
}