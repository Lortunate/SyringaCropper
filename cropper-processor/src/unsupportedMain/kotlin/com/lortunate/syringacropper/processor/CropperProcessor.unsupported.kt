package com.lortunate.syringacropper.processor

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import com.lortunate.syringacropper.perspective.PerspectiveQuad

actual object CropperProcessor {
    actual val supportMessage: String? = UnsupportedProcessorSupportMessage

    actual fun perspectiveWarp(image: ImageBitmap, quad: PerspectiveQuad, tw: Int, th: Int): ImageBitmap {
        unsupported("perspectiveWarp")
    }

    actual fun rectCrop(image: ImageBitmap, rect: Rect): ImageBitmap {
        unsupported("rectCrop")
    }

    actual fun circleCrop(image: ImageBitmap, rect: Rect): ImageBitmap {
        unsupported("circleCrop")
    }

    actual fun roundedRectCrop(image: ImageBitmap, rect: Rect, radius: Float): ImageBitmap {
        unsupported("roundedRectCrop")
    }

    actual fun resize(image: ImageBitmap, tw: Int, th: Int): ImageBitmap {
        unsupported("resize")
    }

    private fun unsupported(operationName: String): Nothing {
        return unsupportedProcessorOperation(
            operationName,
            "is unavailable on this target. $supportMessage",
        )
    }
}
