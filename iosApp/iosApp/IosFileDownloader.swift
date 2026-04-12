//
// Created by Денис Хамидуллин on 12.04.2026.
//

import Foundation
import ComposeApp

class IosFileDownloader: NSObject, FileDownloader {
    func downloadFile(
        url: String,
        destPath: String,
        onProgress: @escaping (KotlinFloat) -> Void,
        onComplete: @escaping (KotlinBoolean) -> Void
    ) {
        SherpaOnnxTtsBridge().downloadFile(
            urlString: url,
            destPath: destPath,
            onProgress: { progress in onProgress(KotlinFloat(value: progress)) },
            onComplete: { success in onComplete(KotlinBoolean(value: success)) }
        )
    }
}