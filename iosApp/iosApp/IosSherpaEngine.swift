//
// Created by Денис Хамидуллин on 09.04.2026.
//

import Foundation
import ComposeApp


@objc class IosSherpaEngine: NSObject, SherpaOnnxTtsEngine {

    private let bridge = SherpaOnnxTtsBridge()
    private let repository: IosVoiceModelRepositoryImpl
    private var isModelLoaded = false
    private var paragraphs: [ComposeApp.LectorParagraph] = []

    init(repository: IosVoiceModelRepositoryImpl) {
        self.repository = repository
    }

    func setPlaylist(paragraphs: [LectorParagraph]) {
        self.paragraphs = paragraphs as [ComposeApp.LectorParagraph]
    }

    func speak(index: Int32, speed: Float) async throws {
        guard isModelLoaded else { return }
        guard index >= 0 && Int(index) < paragraphs.count else { return }
        let text = paragraphs[Int(index)].text
        bridge.speak(text: text, speed: speed)
    }

    func loadVoice(model: VoiceModel) async throws {
        guard let path = repository.getModelPath(id: model.id) else {
            return
        }

        bridge.loadModel(
            onnxPath: path.onnxPath,
            tokensPath: path.tokensPath,
            espeakDataPath: path.espeakDataPath
        )
        isModelLoaded = true
    }

    func generate(text: String, speed: Float) async throws -> KotlinByteArray {
        let data = bridge.generateAudio(text: text, speed: speed)
        let bytes = [UInt8](data)
        let result = KotlinByteArray(size: Int32(bytes.count))
        for (i, byte) in bytes.enumerated() {
            result.set(index: Int32(i), value: Int8(bitPattern: byte))
        }
        return result
    }

    func playAudio(audio: KotlinByteArray) async throws {
        var data = Data(count: Int(audio.size))
        for i in 0..<Int(audio.size) {
            data[i] = UInt8(bitPattern: audio.get(index: Int32(i)))
        }
        bridge.playWav(data: data)
    }

    func stop() {
        bridge.stop()
    }

    func shutdown() {
        bridge.stop()
    }

}
