package com.nedmah.textlector.common.platform.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.nedmah.textlector.domain.model.ModelPath
import com.nedmah.textlector.domain.model.VoiceId
import com.nedmah.textlector.domain.model.VoiceModel
import com.nedmah.textlector.domain.repository.VoiceModelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AndroidSherpaOnnxTtsEngine(
    private val modelRepository: VoiceModelRepository
) : PiperTtsEngine {

    private var tts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null
    private var currentVoiceId: VoiceId? = null
    private var loadingJob: Job? = null

    override suspend fun loadVoice(model: VoiceModel) {
        if (model.id == currentVoiceId) return
        loadingJob = CoroutineScope(Dispatchers.IO).launch {
            val path = modelRepository.getModelPath(model.id) ?: return@launch
            tts?.release()
            tts = buildOfflineTts(path)
            currentVoiceId = model.id
            Log.d("SherpaEngine", "Model loaded: ${model.id}")
        }
    }

    override suspend fun speak(text: String, speed: Float) {
        loadingJob?.join()
        val engine = tts
        if (engine == null) {
            Log.e("SherpaEngine", "speak() called but tts is null")
            return
        }

        withContext(Dispatchers.IO) {
            val audio = engine.generate(text = text, sid = 0, speed = speed)

            if (coroutineContext.isActive) {
                playAudio(audio.samples, audio.sampleRate)
            }
        }
    }

    override fun stop() {
        audioTrack?.apply {
            pause()
            flush()
        }
    }

    override fun shutdown() {
        audioTrack?.release()
        audioTrack = null
        tts?.release()
        tts = null
        currentVoiceId = null
    }

    private fun buildOfflineTts(path: ModelPath): OfflineTts {
        val vitsConfig = OfflineTtsVitsModelConfig(
            model = path.onnxPath,
            lexicon = "",
            tokens = path.tokensPath,
            dataDir = path.espeakDataPath,
            noiseScale = 0.667f,
            noiseScaleW = 0.8f,
            lengthScale = 1.0f
        )
        val modelConfig = OfflineTtsModelConfig(
            vits = vitsConfig,
            numThreads = 4,
            debug = false,
            provider = "cpu"
        )
        val config = OfflineTtsConfig(
            model = modelConfig,
            maxNumSentences = 1
        )
        return OfflineTts(config = config)
    }

    private fun playAudio(samples: FloatArray, sampleRate: Int) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.release()
        audioTrack = track

        track.play()

        // write by chunks - if stop() is called, cycle ends
        val chunkSize = minBufferSize / 4  // Float = 4 bytes
        var offset = 0
        while (offset < samples.size && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            val end = minOf(offset + chunkSize, samples.size)
            track.write(samples, offset, end - offset, AudioTrack.WRITE_BLOCKING)
            offset = end
        }

        // wait until it finishes
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            track.stop()
        }
    }

    override suspend fun generate(text: String, speed: Float): ByteArray {
        loadingJob?.join()
        val engine = tts ?: return ByteArray(0)
        return withContext(Dispatchers.IO) {
            val audio = engine.generate(text = text, sid = 0, speed = speed)
            samplesToWav(audio.samples, audio.sampleRate)
        }
    }

    override suspend fun playAudio(audio: ByteArray) {
        withContext(Dispatchers.IO) {
            val samples = wavToSamples(audio)
            val sampleRate = wavSampleRate(audio)
            if (samples.isNotEmpty()) playAudio(samples, sampleRate)
        }
    }

    private fun samplesToWav(samples: FloatArray, sampleRate: Int): ByteArray {
        val pcm = ByteArray(samples.size * 2)
        samples.forEachIndexed { i, sample ->
            val s = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            pcm[i * 2] = (s.toInt() and 0xFF).toByte()
            pcm[i * 2 + 1] = (s.toInt() shr 8 and 0xFF).toByte()
        }  // multiply every sample by 32767 for pcm16 format
        return buildWavHeader(sampleRate, pcm.size) + pcm
    }

    private fun wavToSamples(wav: ByteArray): FloatArray {
        val dataOffset = 44 // we skip WAV header (first 44 bytes)
        if (wav.size <= dataOffset) return FloatArray(0)
        val pcm = wav.copyOfRange(dataOffset, wav.size)
        return FloatArray(pcm.size / 2) { i ->
            val s = (pcm[i * 2].toInt() and 0xFF) or (pcm[i * 2 + 1].toInt() shl 8)
            s.toShort() / Short.MAX_VALUE.toFloat()
        }
    }

    private fun wavSampleRate(wav: ByteArray): Int {
        if (wav.size < 28) return 22050
        return (wav[24].toInt() and 0xFF) or
                (wav[25].toInt() and 0xFF shl 8) or
                (wav[26].toInt() and 0xFF shl 16) or
                (wav[27].toInt() and 0xFF shl 24)
    }

    private fun buildWavHeader(sampleRate: Int, dataSize: Int): ByteArray {
        val totalSize = dataSize + 36
        return byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            (totalSize and 0xFF).toByte(), (totalSize shr 8 and 0xFF).toByte(),
            (totalSize shr 16 and 0xFF).toByte(), (totalSize shr 24 and 0xFF).toByte(),
            'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte(),
            'f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte(),
            16, 0, 0, 0, 1, 0, 1, 0,
            (sampleRate and 0xFF).toByte(), (sampleRate shr 8 and 0xFF).toByte(),
            (sampleRate shr 16 and 0xFF).toByte(), (sampleRate shr 24 and 0xFF).toByte(),
            (sampleRate * 2 and 0xFF).toByte(), (sampleRate * 2 shr 8 and 0xFF).toByte(),
            (sampleRate * 2 shr 16 and 0xFF).toByte(), (sampleRate * 2 shr 24 and 0xFF).toByte(),
            2, 0, 16, 0,
            'd'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte(),
            (dataSize and 0xFF).toByte(), (dataSize shr 8 and 0xFF).toByte(),
            (dataSize shr 16 and 0xFF).toByte(), (dataSize shr 24 and 0xFF).toByte()
        )
    }
}