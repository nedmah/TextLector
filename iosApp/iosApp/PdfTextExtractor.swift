//
// Created by Денис Хамидуллин on 18.04.2026.
//

import Foundation
import PDFKit
import Vision

@objc public class PdfTextExtractor: NSObject {

    /** Extracts text from one page with PDFKit (fast) */
    @objc public func extractPage(pdfPath: String, pageIndex: Int) -> String {
        guard let url = URL(string: "file://\(pdfPath)"),
            let doc = PDFDocument(url: url),
            let page = doc.page(at: pageIndex)
        else { return "" }
        return page.string ?? ""
    }

    /** Extracts text from one page with Vision OCR (slow)
     *  call when PDFKit returns bad text
     */
    @objc public func ocrPage(pdfPath: String, pageIndex: Int) -> String {
        guard let url = URL(string: "file://\(pdfPath)"),
            let doc = PDFDocument(url: url),
            let page = doc.page(at: pageIndex)
        else { return "" }

        // Render to bitmap — 2x for better OCR quality
        let pageRect = page.bounds(for: .mediaBox)
        let scale: CGFloat = 2.0
        let width = Int(pageRect.width * scale)
        let height = Int(pageRect.height * scale)

        guard
            let context = CGContext(
                data: nil,
                width: width,
                height: height,
                bitsPerComponent: 8,
                bytesPerRow: 0,
                space: CGColorSpaceCreateDeviceRGB(),
                bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
            )
        else { return "" }

        // white bg
        context.setFillColor(CGColor(red: 1, green: 1, blue: 1, alpha: 1))
        context.fill(CGRect(x: 0, y: 0, width: width, height: height))

        context.scaleBy(x: scale, y: scale)
        page.draw(with: .mediaBox, to: context)

        guard let cgImage = context.makeImage() else { return "" }

        // Vision OCR
        var result = ""
        let semaphore = DispatchSemaphore(value: 0)

        let request = VNRecognizeTextRequest { req, error in
            defer { semaphore.signal() }
            guard error == nil,
                let observations = req.results as? [VNRecognizedTextObservation]
            else { return }
            result =
                observations
                .compactMap { $0.topCandidates(1).first?.string }
                .joined(separator: "\n")
        }

        request.recognitionLevel = .accurate
        request.usesLanguageCorrection = true

        request.recognitionLanguages = ["ru-RU", "en-US"]

        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        try? handler.perform([request])
        semaphore.wait()

        return result
    }

    /** returns page count in PDF */
    @objc public func pageCount(pdfPath: String) -> Int {
        guard let url = URL(string: "file://\(pdfPath)"),
            let doc = PDFDocument(url: url)
        else { return 0 }
        return doc.pageCount
    }
}
