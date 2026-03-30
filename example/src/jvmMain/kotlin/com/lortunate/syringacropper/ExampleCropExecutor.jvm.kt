package com.lortunate.syringacropper

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import com.lortunate.syringacropper.perspective.PerspectiveQuad
import com.lortunate.syringacropper.processor.CropperProcessor

internal actual object ExampleCropExecutor {
    actual val supportMessage: String? = null

    actual fun perspectiveWarp(image: ImageBitmap, quad: PerspectiveQuad, tw: Int, th: Int): ImageBitmap {
        return CropperProcessor.perspectiveWarp(image, quad, tw, th)
    }

    actual fun rectCrop(image: ImageBitmap, rect: Rect): ImageBitmap {
        return CropperProcessor.rectCrop(image, rect)
    }

    actual fun circleCrop(image: ImageBitmap, rect: Rect): ImageBitmap {
        return CropperProcessor.circleCrop(image, rect)
    }

    actual fun roundedRectCrop(image: ImageBitmap, rect: Rect, radius: Float): ImageBitmap {
        return CropperProcessor.roundedRectCrop(image, rect, radius)
    }

    actual fun resize(image: ImageBitmap, tw: Int, th: Int): ImageBitmap {
        return CropperProcessor.resize(image, tw, th)
    }
}
