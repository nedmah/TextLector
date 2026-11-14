package com.nedmah.textlector.ui.presentation.import_from

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTType
import platform.darwin.NSObject

@Composable
actual fun rememberFileLauncher(
    onResult: (uri: String?, mimeType: String, fileName: String?) -> Unit
): (mimeType: String) -> Unit {

    val delegateHolder = remember { mutableListOf<NSObject>() }

    return { mimeType ->
        val types = listOf(UTType.typeWithMIMEType(mimeType)).filterNotNull()

        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = types,
            asCopy = true
        )

        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>
            ) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                onResult(url?.path, mimeType, url?.lastPathComponent)
                delegateHolder.clear()
            }
            override fun documentPickerWasCancelled(
                controller: UIDocumentPickerViewController
            ) {
                onResult(null, mimeType, null)
                delegateHolder.clear()
            }
        }

        delegateHolder.clear()
        delegateHolder.add(delegate)
        picker.delegate = delegate

        UIApplication.sharedApplication.keyWindow
            ?.rootViewController
            ?.presentViewController(picker, animated = true, completion = null)
    }
}