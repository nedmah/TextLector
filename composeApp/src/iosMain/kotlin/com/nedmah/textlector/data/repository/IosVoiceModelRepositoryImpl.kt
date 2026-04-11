package com.nedmah.textlector.data.repository

import com.nedmah.textlector.di.IosEngineHolder
import com.nedmah.textlector.domain.model.ModelPath
import com.nedmah.textlector.domain.model.ModelState
import com.nedmah.textlector.domain.model.VoiceId
import com.nedmah.textlector.domain.model.VoiceModel
import com.nedmah.textlector.domain.model.VoiceRegistry
import com.nedmah.textlector.domain.repository.VoiceModelRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUserDomainMask
import platform.Foundation.downloadTaskWithURL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
class IosVoiceModelRepositoryImpl : VoiceModelRepository {

    private val fileManager = NSFileManager.defaultManager
    private val docsDir = fileManager
        .URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
        .first() as NSURL
    private val modelsDir = docsDir.URLByAppendingPathComponent("tts_models")!!

    private val stateFlows = VoiceId.entries.associateWith {
        MutableStateFlow(resolveInitialState(it))
    }

    init {
        fileManager.createDirectoryAtURL(modelsDir, true, null, null)
    }

    private fun resolveInitialState(id: VoiceId): ModelState {
        val model = VoiceRegistry.getById(id)
        val onnxFile = modelsDir
            .URLByAppendingPathComponent(model.modelDirName)!!
            .URLByAppendingPathComponent(model.onnxFileName)!!

        return if (fileManager.fileExistsAtPath(onnxFile.path!!)) ModelState.Ready
        else ModelState.NotDownloaded
    }

    override fun getModelState(id: VoiceId): Flow<ModelState> =
        stateFlows.getValue(id).asStateFlow()

    override fun downloadModel(model: VoiceModel): Flow<ModelState> = flow {
        val stateFlow = stateFlows.getValue(model.id)
        try {
            val modelDir = modelsDir.URLByAppendingPathComponent(model.modelDirName)!!
            fileManager.createDirectoryAtURL(modelDir, true, null, null)

            // espeak-ng-data
            val espeakDir = modelsDir.URLByAppendingPathComponent("espeak-ng-data")!!
            if (!fileManager.fileExistsAtPath(espeakDir.path!!)) {
                downloadFile(VoiceRegistry.ESPEAK_DATA_URL, modelsDir, "espeak-ng-data.tar.bz2")
                val tarPath = modelsDir.URLByAppendingPathComponent("espeak-ng-data.tar.bz2")!!.path!!
                val extracted = IosEngineHolder.tarExtractor?.extractTarBz2(
                    archivePath = tarPath,
                    destPath = modelsDir.path!!
                ) ?: false
                fileManager.removeItemAtPath(tarPath, null)
            }

            val tokensFile = modelDir.URLByAppendingPathComponent("tokens.txt")!!
            if (!fileManager.fileExistsAtPath(tokensFile.path!!)) {
                downloadFile(model.tokensUrl, modelDir, "tokens.txt")
            }

            val onnxFile = modelDir.URLByAppendingPathComponent(model.onnxFileName)!!
            if (!fileManager.fileExistsAtPath(onnxFile.path!!)) {
                downloadFile(model.onnxUrl, modelDir, model.onnxFileName)
            }

            stateFlow.value = ModelState.Ready
            emit(ModelState.Ready)

        } catch (e: Exception) {
            val error = ModelState.Error(e.message ?: "Download failed")
            stateFlow.value = error
            emit(error)
        }
    }

    override fun getModelPath(id: VoiceId): ModelPath? {
        val model = VoiceRegistry.getById(id)
        val modelDir = modelsDir.URLByAppendingPathComponent(model.modelDirName)!!
        val onnxFile = modelDir.URLByAppendingPathComponent(model.onnxFileName)!!
        val tokensFile = modelDir.URLByAppendingPathComponent("tokens.txt")!!
        if (!fileManager.fileExistsAtPath(onnxFile.path!!) ||
            !fileManager.fileExistsAtPath(tokensFile.path!!)) return null
        return ModelPath(
            onnxPath = onnxFile.path!!,
            tokensPath = tokensFile.path!!,
            espeakDataPath = espeakBundlePath()
        )
    }

    override suspend fun deleteModel(id: VoiceId): Result<Unit> = runCatching {
        val model = VoiceRegistry.getById(id)
        val modelDir = modelsDir.URLByAppendingPathComponent(model.modelDirName)!!
        fileManager.removeItemAtURL(modelDir, null)
        stateFlows.getValue(id).value = ModelState.NotDownloaded
    }

    override fun isEspeakDataReady(): Boolean = true

    private fun espeakBundlePath(): String =
        modelsDir.URLByAppendingPathComponent("espeak-ng-data")!!.path!!

    private suspend fun downloadFile(
        url: String,
        destDir: NSURL,
        fileName: String,
//        onProgress: ((Float) -> Unit)? = null
    ) = suspendCancellableCoroutine { continuation ->
        val nsUrl = NSURL.URLWithString(url)!!
        val session = NSURLSession.sharedSession

        val task = session.downloadTaskWithURL(nsUrl) { tempUrl, response, error ->
            if (error != null || tempUrl == null) {
                continuation.resumeWithException(
                    Exception(error?.localizedDescription ?: "Download failed")
                )
                return@downloadTaskWithURL
            }
            val destFile = destDir.URLByAppendingPathComponent(fileName)!!
            NSFileManager.defaultManager.moveItemAtURL(tempUrl, destFile, null)
            continuation.resume(Unit)
        }
        task.resume()
        continuation.invokeOnCancellation { task.cancel() }
    }
}