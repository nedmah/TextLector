package com.nedmah.textlector.data.repository

import android.content.Context
import com.nedmah.textlector.domain.model.ModelState
import com.nedmah.textlector.domain.repository.OcrDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AndroidOcrDataRepository(
    private val context: Context
) : OcrDataRepository {

    companion object {
        private const val ENG_URL =
            "https://github.com/tesseract-ocr/tessdata_best/raw/main/eng.traineddata"
        private const val RUS_URL =
            "https://github.com/tesseract-ocr/tessdata_best/raw/main/rus.traineddata"
        private const val ENG_FILE = "eng.traineddata"
        private const val RUS_FILE = "rus.traineddata"
    }

    private val tessDataDir: File
        get() = File(context.filesDir, "tessdata").also { it.mkdirs() }

    private val engFile get() = File(tessDataDir, ENG_FILE)
    private val rusFile get() = File(tessDataDir, RUS_FILE)

    private val _state = MutableStateFlow<ModelState>(computeState())

    override fun getState(): Flow<ModelState> = _state.asStateFlow()

    override fun isReady(): Boolean =
        engFile.exists() && rusFile.exists()

    override suspend fun download(): Flow<ModelState> = flow {
        emit(ModelState.Downloading(0f))
        _state.value = ModelState.Downloading(0f)

        try {
            downloadFile(ENG_URL, engFile).collect { progress ->
                val overall = progress * 0.5f
                _state.value = ModelState.Downloading(overall)
                emit(ModelState.Downloading(overall))
            }
            downloadFile(RUS_URL, rusFile).collect { progress ->
                val overall = 0.5f + progress * 0.5f
                _state.value = ModelState.Downloading(overall)
                emit(ModelState.Downloading(overall))
            }

            _state.value = ModelState.Ready
            emit(ModelState.Ready)
        } catch (e: Exception) {
            engFile.delete()
            rusFile.delete()
            val error = ModelState.Error(e.message ?: "Download failed")
            _state.value = error
            emit(error)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun delete() = withContext(Dispatchers.IO) {
        engFile.delete()
        rusFile.delete()
        _state.value = ModelState.NotDownloaded
    }

    private fun computeState(): ModelState =
        if (isReady()) ModelState.Ready else ModelState.NotDownloaded

    private fun downloadFile(url: String, dest: File): Flow<Float> = flow {
        var currentUrl = url
        var connection: HttpURLConnection? = null

        try {
            while (true) {
                connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == 307 || responseCode == 308
                ) {
                    val location = connection.getHeaderField("Location")
                    currentUrl = URL(URL(currentUrl), location).toExternalForm()
                    connection.disconnect()
                    connection = null
                    continue
                }
                break
            }

            val total = connection.contentLengthLong

            var downloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(8192)
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        downloaded += bytes
                        if (total > 0) emit(downloaded.toFloat() / total)
                        bytes = input.read(buffer)
                    }
                }
            }
        } finally {
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)


}