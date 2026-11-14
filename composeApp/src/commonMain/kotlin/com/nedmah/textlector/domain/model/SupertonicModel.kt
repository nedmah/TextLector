package com.nedmah.textlector.domain.model

data class SupertonicModelPaths(
    val durationPredictor: String,
    val textEncoder: String,
    val vectorEstimator: String,
    val vocoder: String,
    val ttsJson: String,
    val unicodeIndexer: String,
    val voiceStyle: String,
)

object SupertonicModel {
    const val DIR_NAME = "supertonic3"
    const val LEGACY_DIR_NAME = "supertonic" // supertonic-kmp storage, app <= 1.3.0
    const val NUM_STEPS = 6

    private const val HF_BASE =
        "https://huggingface.co/csukuangfj2/sherpa-onnx-supertonic-3-tts-int8-2026-05-11/resolve/main"

    data class ModelFile(
        val name: String,
        val approxSizeBytes: Long
    ) {
        val url: String get() = "$HF_BASE/$name"
    }

    private val durationPredictor = ModelFile("duration_predictor.int8.onnx", 3_700_000)
    private val textEncoder = ModelFile("text_encoder.int8.onnx", 36_400_000)
    private val vectorEstimator = ModelFile("vector_estimator.int8.onnx", 78_400_000)
    private val vocoder = ModelFile("vocoder.int8.onnx", 26_000_000)
    private val ttsJson = ModelFile("tts.json", 8_250)
    private val unicodeIndexer = ModelFile("unicode_indexer.bin", 262_000)
    private val voiceStyle = ModelFile("voice.bin", 517_000)

    val files = listOf(
        durationPredictor, textEncoder, vectorEstimator, vocoder,
        ttsJson, unicodeIndexer, voiceStyle,
    )

    val totalBytes: Long = files.sumOf { it.approxSizeBytes }

    fun paths(dir: String) = SupertonicModelPaths(
        durationPredictor = "$dir/${durationPredictor.name}",
        textEncoder = "$dir/${textEncoder.name}",
        vectorEstimator = "$dir/${vectorEstimator.name}",
        vocoder = "$dir/${vocoder.name}",
        ttsJson = "$dir/${ttsJson.name}",
        unicodeIndexer = "$dir/${unicodeIndexer.name}",
        voiceStyle = "$dir/${voiceStyle.name}",
    )

    // voice.bin speakers are sorted by style name: F1..F5 -> 0..4, M1..M5 -> 5..9
    fun speakerId(gender: VoiceGender): Int = when (gender) {
        VoiceGender.FEMALE -> 0
        VoiceGender.MALE -> 5
    }
}