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
) : TtsEngine {

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
            numThreads = 2,
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
}