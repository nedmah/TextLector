//
// Created by Денис Хамидуллин on 16.05.2026.
//

import Foundation
import Vision
import UIKit
import ComposeApp

@objc class IosOcrEngine: NSObject, OcrEngine {

    func recognize(imageUri: String) async throws -> String {
        guard let url = URL(string: imageUri),
              let uiImage = UIImage(contentsOfFile: url.path),
              let cgImage = uiImage.cgImage else {
            throw NSError(domain: "OCR", code: -1,
                          userInfo: [NSLocalizedDescriptionKey: "Failed to load image"])
        }

        return try await withCheckedThrowingContinuation { continuation in
            let request = VNRecognizeTextRequest { request, error in
                if let error = error {
                    continuation.resume(throwing: error)
                    return
                }

                let text = request.results?
                .compactMap { $0 as? VNRecognizedTextObservation }
                .compactMap { $0.topCandidates(1).first?.string }
                .joined(separator: "\n") ?? ""

                if text.isEmpty {
                    continuation.resume(throwing: NSError(
                        domain: "OCR", code: -2,
                        userInfo: [NSLocalizedDescriptionKey: "No text detected"]
                    ))
                } else {
                    continuation.resume(returning: text)
                }
            }

            request.recognitionLevel = .accurate
            request.automaticallyDetectsLanguage = true

            let handler = VNImageRequestHandler(cgImage: cgImage)
            do {
                try handler.perform([request])
            } catch {
                continuation.resume(throwing: error)
            }
        }
    }
}