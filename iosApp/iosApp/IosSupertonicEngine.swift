//
// Created by Денис Хамидуллин on 28.05.2026.
//

import Foundation
import ComposeApp
import SherpaOnnx

@objc class IosSupertonicEngine: NSObject, SherpaOnnxTtsEngine {

    private let repository: SupertonicRepository
    private let player = SherpaOnnxTtsBridge() // used for WAV playback only

    // Serial queue owns all native state: the model is never used from two threads
    // and never released while generating (same role as the Mutex on Android)
    private let ttsQueue = DispatchQueue(label: "com.lector.supertonic.tts")
    private var tts: SherpaOnnxOfflineTtsWrapper?
    private var loadedPaths: SupertonicModelPaths?
    private var lang = "en"
    private var speakerId = Int(SupertonicModel.shared.speakerId(gender: VoiceGender.male))

    private var paragraphs: [LectorParagraph] = []

    init(repository: SupertonicRepository) {
        self.repository = repository
    }

    // VoiceModel describes a Piper voice; Supertonic only needs its language and gender
    func loadVoice(model: VoiceModel) async throws {
        let lang = model.language
        let speakerId = Int(SupertonicModel.shared.speakerId(gender: model.gender))
        ttsQueue.async {
            self.lang = lang
            self.speakerId = speakerId
            _ = self.ensureTts() // warm up, model load takes a while
        }
    }

    func setPlaylist(paragraphs: [LectorParagraph]) {
        self.paragraphs = paragraphs
    }

    func speak(index: Int32, speed: Float) async throws {
        guard index >= 0 && Int(index) < paragraphs.count else { return }
        let audio = try await generate(text: paragraphs[Int(index)].text, speed: speed)
        if audio.size > 0 { try await playAudio(audio: audio) }
    }

    func generate(text: String, speed: Float) async throws -> KotlinByteArray {
        let wav: Data = await withCheckedContinuation { continuation in
            ttsQueue.async {
                continuation.resume(returning: self.generateWav(text: text, speed: speed))
            }
        }
        return wav.toKotlinByteArray()
    }

    func playAudio(audio: KotlinByteArray) async throws {
        player.playWav(data: audio.toData())
    }

    func stop() {
        player.stop()
    }

    func shutdown() {
        player.stop()
        ttsQueue.async {
            self.tts = nil // deinit releases the native object
            self.loadedPaths = nil
        }
    }

    // MARK: - ttsQueue only

    private func generateWav(text: String, speed: Float) -> Data {
        guard let tts = ensureTts() else { return Data() }
        let config = SherpaOnnxGenerationConfigSwift(
            speed: speed,
            sid: speakerId,
            numSteps: Int(SupertonicModel.shared.NUM_STEPS),
            extra: ["lang": lang]
        )
        let audio = tts.generateWithConfig(text: text, config: config, callback: nil, arg: nil)
        guard audio.audio != nil else { return Data() }
        let samples = audio.samples
        guard !samples.isEmpty else { return Data() }
        return SherpaOnnxTtsBridge.makeWav(samples: samples, sampleRate: Int(audio.sampleRate))
    }

    // Builds the model lazily and drops it if the files were deleted
    private func ensureTts() -> SherpaOnnxOfflineTtsWrapper? {
        let paths = repository.getModelPaths()
        if paths != loadedPaths || tts == nil {
            tts = nil
            loadedPaths = nil
            guard let paths else { return nil }
            let wrapper = makeTts(paths)
            guard wrapper.tts != nil else {
                print("[IosSupertonicEngine] failed to create OfflineTts")
                return nil
            }
            tts = wrapper
            loadedPaths = paths
        }
        return tts
    }

    private func makeTts(_ paths: SupertonicModelPaths) -> SherpaOnnxOfflineTtsWrapper {
        let supertonic = sherpaOnnxOfflineTtsSupertonicModelConfig(
            durationPredictor: paths.durationPredictor,
            textEncoder: paths.textEncoder,
            vectorEstimator: paths.vectorEstimator,
            vocoder: paths.vocoder,
            ttsJson: paths.ttsJson,
            unicodeIndexer: paths.unicodeIndexer,
            voiceStyle: paths.voiceStyle
        )
        let modelConfig = sherpaOnnxOfflineTtsModelConfig(
            numThreads: 4,
            debug: 0,
            provider: "cpu",
            supertonic: supertonic
        )
        var config = sherpaOnnxOfflineTtsConfig(model: modelConfig)
        return withUnsafePointer(to: &config) { SherpaOnnxOfflineTtsWrapper(config: $0) }
    }
}

private extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let result = KotlinByteArray(size: Int32(count))
        for (i, byte) in enumerated() {
            result.set(index: Int32(i), value: Int8(bitPattern: byte))
        }
        return result
    }
}

private extension KotlinByteArray {
    func toData() -> Data {
        var data = Data(count: Int(size))
        for i in 0..<Int(size) {
            data[i] = UInt8(bitPattern: get(index: Int32(i)))
        }
        return data
    }
}
