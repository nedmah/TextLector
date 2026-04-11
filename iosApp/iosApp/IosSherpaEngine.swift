//
// Created by Денис Хамидуллин on 09.04.2026.
//

import Foundation
import ComposeApp


@objc class IosSherpaEngine: NSObject, TtsEngine {

    private let bridge = SherpaOnnxTtsBridge()
    private let repository: IosVoiceModelRepositoryImpl
    private var isModelLoaded = false

    init(repository: IosVoiceModelRepositoryImpl) {
        self.repository = repository
    }

    func speak(text: String, speed: Float) async throws {
        guard isModelLoaded else { return }
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

    func stop() {
        bridge.stop()
    }

    func shutdown() {
        bridge.stop()
    }
}
