//
// Created by Денис Хамидуллин on 12.04.2026.
//

import Foundation
import ComposeApp

class IosTarExtractor: NSObject, TarExtractor {
    func extractTarBz2(archivePath: String, destPath: String) -> Bool {
        return SherpaOnnxTtsBridge().extractTarBz2(
            archivePath: archivePath,
            destPath: destPath
        )
    }
}