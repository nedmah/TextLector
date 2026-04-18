//
// Created by Денис Хамидуллин on 18.04.2026.
//

import Foundation
import ComposeApp

@objc class IosPdfPageExtractor: NSObject, PdfPageExtractor {
    private let extractor = PdfTextExtractor()

    func pageCount(path: String) -> Int32 {
        return Int32(extractor.pageCount(pdfPath: path))
    }

    func extractPage(path: String, pageIndex: Int32) -> String {
        return extractor.extractPage(pdfPath: path, pageIndex: Int(pageIndex))
    }

    func ocrPage(path: String, pageIndex: Int32) -> String {
        return extractor.ocrPage(pdfPath: path, pageIndex: Int(pageIndex))
    }
}