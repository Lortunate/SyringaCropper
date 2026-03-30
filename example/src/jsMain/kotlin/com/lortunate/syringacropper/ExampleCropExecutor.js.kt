package com.lortunate.syringacropper

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import com.lortunate.syringacropper.perspective.PerspectiveQuad

internal actual object ExampleCropExecutor {
    actual val supportMessage: String? = "Native processing demo is currently supported on Android, JVM, and Wasm."

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
        throw UnsupportedOperationException(
            "ExampleCropExecutor.$operationName is unavailable on JavaScript fallback. Native processing demo is currently supported on Android, JVM, and Wasm.",
        )
    }
}
