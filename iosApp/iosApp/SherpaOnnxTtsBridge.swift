//
//  SherpaOnnxTtsBridge.swift
//  iosApp
//
//  Created by Денис Хамидуллин on 06.04.2026.
//

import Foundation
import AVFAudio
import Darwin


@objc public class SherpaOnnxTtsBridge: NSObject, AVAudioPlayerDelegate {

    private var tts: SherpaOnnxOfflineTtsWrapper?
    private var audioPlayer: AVAudioPlayer?
    private var currentSemaphore: DispatchSemaphore?
    private var currentToken: Int = 0
    private let generateQueue = DispatchQueue(label: "com.lector.sherpa.generate")


    @objc public func extractTarBz2(archivePath: String, destPath: String) -> Bool {
        guard let decompressed = Bz2Wrapper.decompressBz2File(archivePath) else {
            return false
        }

        // Parse tar
        let fileManager = FileManager.default
        var offset = 0
        let blockSize = 512

        while offset + blockSize <= decompressed.count {
            let header = decompressed.subdata(in: offset..<offset + blockSize)
            offset += blockSize

            if header.allSatisfy({ $0 == 0 }) { break }

            let nameBytes = header.subdata(in: 0..<100)
            guard let name = String(bytes: nameBytes.prefix(while: { $0 != 0 }), encoding: .utf8),
                  !name.isEmpty else { continue }

            let sizeBytes = header.subdata(in: 124..<136)
            let sizeStr = String(bytes: sizeBytes.prefix(while: { $0 != 0 }), encoding: .utf8) ?? "0"
            let fileSize = Int(sizeStr.trimmingCharacters(in: .whitespaces), radix: 8) ?? 0

            let typeFlag = header[156]
            let destURL = URL(fileURLWithPath: destPath).appendingPathComponent(name)

            if typeFlag == UInt8(ascii: "5") || name.hasSuffix("/") {
                try? fileManager.createDirectory(at: destURL, withIntermediateDirectories: true)
            } else {
                try? fileManager.createDirectory(at: destURL.deletingLastPathComponent(), withIntermediateDirectories: true)
                if fileSize > 0 && offset + fileSize <= decompressed.count {
                    let fileData = decompressed.subdata(in: offset..<offset + fileSize)
                    try? fileData.write(to: destURL)
                }
            }

            let blocks = (fileSize + blockSize - 1) / blockSize
            offset += blocks * blockSize
        }

        return true
    }

    @objc public func loadModel(
        onnxPath: String,
        tokensPath: String,
        espeakDataPath: String
    ) {
        let vitsConfig = sherpaOnnxOfflineTtsVitsModelConfig(
            model: onnxPath,
            lexicon: "",
            tokens: tokensPath,
            dataDir: espeakDataPath
        )
        let modelConfig = sherpaOnnxOfflineTtsModelConfig(
            vits: vitsConfig,
            numThreads: 4,
            debug: 0,
            provider: "cpu"
        )
        var config = sherpaOnnxOfflineTtsConfig(model: modelConfig)
        withUnsafePointer(to: &config) { ptr in
            tts = SherpaOnnxOfflineTtsWrapper(config: ptr)
        }
    }

    @objc public func speak(text: String, speed: Float) {
        guard let tts = tts else { return }
        let audio = tts.generate(text: text, sid: 0, speed: speed)
        playAudioSync(samples: audio.samples, sampleRate: Int(audio.sampleRate))
    }

    @objc public func stop() {
        currentToken += 1
        audioPlayer?.stop()
        currentSemaphore?.signal()
        currentSemaphore = nil
    }

    @objc public func downloadFile(
        urlString: String,
        destPath: String,
        onProgress: @escaping (Float) -> Void,
        onComplete: @escaping (Bool) -> Void
    ) {
        guard let url = URL(string: urlString) else {
            onComplete(false)
            return
        }

        let delegate = DownloadDelegate(destPath: destPath, onProgress: onProgress, onComplete: onComplete)
        let session = URLSession(configuration: .default, delegate: delegate, delegateQueue: nil)
        let task = session.downloadTask(with: url)
        delegate.task = task
        task.resume()
    }

    @objc public func generateAudio(text: String, speed: Float) -> Data {
        var result = Data()
        let myToken = self.currentToken
        generateQueue.sync {
            guard let tts = self.tts else { return }

            final class CallbackContext {
                weak var owner: SherpaOnnxTtsBridge?
                let token: Int
                init(_ owner: SherpaOnnxTtsBridge, _ token: Int) {
                    self.owner = owner
                    self.token = token
                }
            }

            let ctx = CallbackContext(self, myToken)
            let rawCtx = Unmanaged.passRetained(ctx).toOpaque()
            defer { Unmanaged<CallbackContext>.fromOpaque(rawCtx).release() }

            let audioResult = tts.generateWithCallbackWithArg(
                text: text,
                callback: { _, _, rawArg -> Int32 in
                    guard let rawArg else { return 0 }
                    let ctx = Unmanaged<CallbackContext>.fromOpaque(rawArg).takeUnretainedValue()
                    return ctx.owner?.currentToken == ctx.token ? 1 : 0
                },
                arg: rawCtx,
                sid: 0,
                speed: speed
            )

            guard self.currentToken == myToken else { return }

            let samples = audioResult.samples
            guard !samples.isEmpty else { return }

            var data = Data(capacity: samples.count * 2)
            for sample in samples {
                let clamped = max(-1.0, min(1.0, sample))
                let intSample = Int16(clamped * Float(Int16.max))
                withUnsafeBytes(of: intSample) { data.append(contentsOf: $0) }
            }
            var wav = self.makeWavHeader(dataSize: data.count, sampleRate: Int(audioResult.sampleRate))
            wav.append(data)
            result = wav
        }
        return result
    }

    @objc public func playWav(data: Data) {
        playWavData(data)
    }

    public func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        currentSemaphore?.signal()
        currentSemaphore = nil
    }

    public func audioPlayerDecodeErrorDidOccur(_ player: AVAudioPlayer, error: Error?) {
        print("[SherpaOnnxBridge] decode error: \(error?.localizedDescription ?? "unknown")")
        currentSemaphore?.signal()
        currentSemaphore = nil
    }

    private func playAudioSync(samples: [Float], sampleRate: Int) {
        var data = Data(capacity: samples.count * 2)
        for sample in samples {
            let clamped = max(-1.0, min(1.0, sample))
            let intSample = Int16(clamped * Float(Int16.max))
            withUnsafeBytes(of: intSample) { data.append(contentsOf: $0) }
        }
        var wav = makeWavHeader(dataSize: data.count, sampleRate: sampleRate)
        wav.append(data)
        playWavData(wav)
    }

    private func playWavData(_ data: Data) {
        guard let player = try? AVAudioPlayer(data: data) else { return }
        audioPlayer = player
        player.delegate = self
        let sem = DispatchSemaphore(value: 0)
        currentSemaphore = sem
        player.play()
        sem.wait()
        currentSemaphore = nil
    }

    private func playPCM(data: Data, sampleRate: Int) {
        // WAV header + data
        var wav = makeWavHeader(dataSize: data.count, sampleRate: sampleRate)
        wav.append(data)
        audioPlayer = try? AVAudioPlayer(data: wav)
        audioPlayer?.play()
    }

    private func makeWavHeader(dataSize: Int, sampleRate: Int) -> Data {
        var header = Data()
        let totalSize = dataSize + 36
        header.append(contentsOf: "RIFF".utf8)
        header.append(littleEndian: UInt32(totalSize))
        header.append(contentsOf: "WAVE".utf8)
        header.append(contentsOf: "fmt ".utf8)
        header.append(littleEndian: UInt32(16))
        header.append(littleEndian: UInt16(1))  // PCM
        header.append(littleEndian: UInt16(1))  // mono
        header.append(littleEndian: UInt32(sampleRate))
        header.append(littleEndian: UInt32(sampleRate * 2))
        header.append(littleEndian: UInt16(2))
        header.append(littleEndian: UInt16(16))
        header.append(contentsOf: "data".utf8)
        header.append(littleEndian: UInt32(dataSize))
        return header
    }
}

private class DownloadDelegate: NSObject, URLSessionDownloadDelegate {
    let destPath: String
    let onProgress: (Float) -> Void
    let onComplete: (Bool) -> Void
    var task: URLSessionDownloadTask?

    init(destPath: String, onProgress: @escaping (Float) -> Void, onComplete: @escaping (Bool) -> Void) {
        self.destPath = destPath
        self.onProgress = onProgress
        self.onComplete = onComplete
    }

    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didFinishDownloadingTo location: URL) {
        let destURL = URL(fileURLWithPath: destPath)
        do {
            try FileManager.default.moveItem(at: location, to: destURL)
            onComplete(true)
        } catch {
            print("DownloadDelegate: move error \(error)")
            onComplete(false)
        }
    }

    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask,
                    didWriteData bytesWritten: Int64,
                    totalBytesWritten: Int64,
                    totalBytesExpectedToWrite: Int64) {
        guard totalBytesExpectedToWrite > 0 else { return }
        let progress = Float(totalBytesWritten) / Float(totalBytesExpectedToWrite)
        onProgress(progress)
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        if let error = error {
            print("DownloadDelegate: error \(error)")
            onComplete(false)
        }
    }
}

private extension Data {
    mutating func append<T: FixedWidthInteger>(littleEndian value: T) {
        var v = value.littleEndian
        Swift.withUnsafeBytes(of: &v) { self.append(contentsOf: $0) }
    }
}
