package com.lortunate.syringacropper.processor

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.lortunate.syringacropper.perspective.PerspectiveQuad
import java.nio.ByteBuffer

actual object CropperProcessor {
    actual val supportMessage: String? = null

    init {
        System.loadLibrary("cropper_processor")
    }

    actual fun perspectiveWarp(
        image: ImageBitmap,
        quad: PerspectiveQuad,
        tw: Int,
        th: Int
    ): ImageBitmap {
        return process(image, preparePerspectiveWarpOperation(image, quad, tw, th))
    }

    actual fun rectCrop(image: ImageBitmap, rect: Rect): ImageBitmap {
        return process(image, prepareRectCropOperation(image, rect))
    }

    actual fun circleCrop(image: ImageBitmap, rect: Rect): ImageBitmap {
        return process(image, prepareCircleCropOperation(image, rect))
    }

    actual fun roundedRectCrop(image: ImageBitmap, rect: Rect, radius: Float): ImageBitmap {
        return process(image, prepareRoundedRectCropOperation(image, rect, radius))
    }

    actual fun resize(image: ImageBitmap, tw: Int, th: Int): ImageBitmap {
        return process(image, prepareResizeOperation(tw, th))
    }

    @JvmStatic
    private external fun process(
        b: ByteBuffer,
        w: Int,
        h: Int,
        types: IntArray,
        params: FloatArray
    ): ByteArray

    private fun process(image: ImageBitmap, operation: CropperOperation): ImageBitmap {
        return processNativeOperation(
            image = image,
            operation = operation,
            execute = { source, types, params -> source.execute(types, params) },
            toImageBitmap = { bytes, width, height, operationName ->
                bytes.withSize(width, height, operationName)
            },
        )
    }

    private fun ImageBitmap.execute(types: IntArray, params: FloatArray): ByteArray {
        val bm = asAndroidBitmap()
        val buf = ByteBuffer.allocateDirect(bm.byteCount)
        bm.copyPixelsToBuffer(buf)
        buf.rewind()
        return process(buf, bm.width, bm.height, types, params)
    }

    private fun ByteArray.withSize(w: Int, h: Int, operationName: String): ImageBitmap {
        requirePixelByteCount(w, h, operationName)
        return createBitmap(w, h, Bitmap.Config.ARGB_8888)
            .apply {
                copyPixelsFromBuffer(ByteBuffer.wrap(this@withSize))
            }
            .asImageBitmap()
    }
}
