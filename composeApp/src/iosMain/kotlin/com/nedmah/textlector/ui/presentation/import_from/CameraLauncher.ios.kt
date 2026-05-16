package com.nedmah.textlector.ui.presentation.import_from

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.temporaryDirectory
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

@Composable
actual fun rememberCameraLauncher(onImageCaptured: (uri: String) -> Unit): () -> Unit {
    val delegateHolder = remember { mutableListOf<NSObject>() }

    return {
        val picker = UIImagePickerController()
        picker.sourceType =
            UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera

        val delegate = object : NSObject(),
            UIImagePickerControllerDelegateProtocol,
            UINavigationControllerDelegateProtocol {

            override fun imagePickerController(
                picker: UIImagePickerController,
                didFinishPickingMediaWithInfo: Map<Any?, *>
            ) {
                picker.dismissViewControllerAnimated(true, completion = null)

                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage

                if (image != null) {
                    val data = UIImageJPEGRepresentation(image, 0.9)
                    val tempDir = NSFileManager.defaultManager.temporaryDirectory
                    val fileName = "camera_ocr_${NSDate().timeIntervalSince1970}.jpg"
                    val fileURL = tempDir.URLByAppendingPathComponent(fileName)

                    if (data != null && fileURL != null) {
                        data.writeToURL(fileURL, atomically = true)
                        onImageCaptured(fileURL.absoluteString ?: "")
                    }
                }
                delegateHolder.clear()
            }

            override fun imagePickerControllerDidCancel(
                picker: UIImagePickerController
            ) {
                picker.dismissViewControllerAnimated(true, completion = null)
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