package com.nedmah.textlector.data.repository

import android.content.Context
import com.nedmah.textlector.domain.model.ModelPath
import com.nedmah.textlector.domain.model.ModelState
import com.nedmah.textlector.domain.model.VoiceId
import com.nedmah.textlector.domain.model.VoiceModel
import com.nedmah.textlector.domain.model.VoiceRegistry
import com.nedmah.textlector.domain.repository.VoiceModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AndroidVoiceModelRepositoryImpl(
    private val context: Context
) : VoiceModelRepository {

    private val modelsDir = File(context.filesDir, "tts_models").also { it.mkdirs() }
    private val espeakDir = File(modelsDir, "espeak-ng-data")  // keep this file separately bc it duplicates in models

    private val stateFlows = VoiceId.entries.associateWith {
        MutableStateFlow(resolveInitialState(it))
    }

    private fun resolveInitialState(id: VoiceId): ModelState {
        val model = VoiceRegistry.getById(id)
        val onnxFile = File(modelsDir, "${model.modelDirName}/${model.onnxFileName}")
        return if (onnxFile.exists() && espeakDir.exists()) ModelState.Ready
        else ModelState.NotDownloaded
    }

    override fun getModelState(id: VoiceId): Flow<ModelState> =
        stateFlows.getValue(id).asStateFlow()

    override fun downloadModel(model: VoiceModel): Flow<ModelState> = flow {
        val stateFlow = stateFlows.getValue(model.id)

        try {
            val modelDir = File(modelsDir, model.modelDirName).also { it.mkdirs() }

            // espeak-ng-data
            if (!espeakDir.exists()) {
                stateFlow.value = ModelState.Downloading(0f)
                emit(ModelState.Downloading(0f))
                downloadAndExtractEspeak()
            }

            // tokens.txt (~1KB)
            val tokensFile = File(modelDir, "tokens.txt")
            if (!tokensFile.exists()) {
                downloadFile(model.tokensUrl, tokensFile, onProgress = null)
            }

            // .onnx (~63MB)
            val onnxFile = File(modelDir, model.onnxFileName)
            if (!onnxFile.exists()) {
                downloadFile(model.onnxUrl, onnxFile) { progress ->
                    val state = ModelState.Downloading(0.1f + progress * 0.9f)
                    stateFlow.value = state
                }
            }

            stateFlow.value = ModelState.Ready
            emit(ModelState.Ready)

        } catch (e: Exception) {
            val error = ModelState.Error(e.message ?: "Download failed")
            stateFlow.value = error
            emit(error)
        }
    }.flowOn(Dispatchers.IO)

    override fun getModelPath(id: VoiceId): ModelPath? {
        val model = VoiceRegistry.getById(id)
        val modelDir = File(modelsDir, model.modelDirName)
        val onnxFile = File(modelDir, model.onnxFileName)
        val tokensFile = File(modelDir, "tokens.txt")
        if (!onnxFile.exists() || !tokensFile.exists() || !espeakDir.exists()) return null
        return ModelPath(
            onnxPath = onnxFile.absolutePath,
            tokensPath = tokensFile.absolutePath,
            espeakDataPath = espeakDir.absolutePath
        )
    }

    override suspend fun deleteModel(id: VoiceId): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val model = VoiceRegistry.getById(id)
                File(modelsDir, model.modelDirName).deleteRecursively()
                stateFlows.getValue(id).value = ModelState.NotDownloaded
            }
        }

    override fun isEspeakDataReady(): Boolean = espeakDir.exists()


    private fun downloadFile(
        url: String,
        dest: File,
        onProgress: ((Float) -> Unit)?
    ) {
        val tempFile = File(dest.parent, "${dest.name}.tmp")
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connect()
            val totalBytes = connection.contentLengthLong

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var downloaded = 0L
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        downloaded += bytes
                        if (totalBytes > 0) {
                            onProgress?.invoke(downloaded.toFloat() / totalBytes)
                        }
                        bytes = input.read(buffer)
                    }
                }
            }
            tempFile.renameTo(dest) // protect from partial download
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    private fun downloadAndExtractEspeak() {
        val tarFile = File(modelsDir, "espeak-ng-data.tar.bz2")
        try {
            downloadFile(VoiceRegistry.ESPEAK_DATA_URL, tarFile, onProgress = null)
            extractTarBz2(tarFile, modelsDir)
        } finally {
            tarFile.delete() // no need for that archive now
        }
    }

    private fun extractTarBz2(archive: File, destDir: File) {
        TarArchiveInputStream(
            BZip2CompressorInputStream(
                BufferedInputStream(
                    FileInputStream(archive)
                )
            )
        ).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { tar.copyTo(it) }
                }
                entry = tar.nextEntry
            }
        }
    }
}