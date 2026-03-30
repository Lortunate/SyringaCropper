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

    init {
        System.loadLibrary("cropper_processor")
    }

    actual fun perspectiveWarp(
        image: ImageBitmap,
        quad: PerspectiveQuad,
        tw: Int,
        th: Int
    ): ImageBitmap {
        quad.requireProcessable(image.width, image.height, tw, th)
        val params = floatArrayOf(
            quad.topLeft.x, quad.topLeft.y, quad.topRight.x, quad.topRight.y,
            quad.bottomRight.x, quad.bottomRight.y, quad.bottomLeft.x, quad.bottomLeft.y,
            tw.toFloat(), th.toFloat()
        )
        return image.execute(intArrayOf(PerspectiveWarpOp), params)
            .withSize(tw, th, "perspectiveWarp")
    }

    actual fun rectCrop(image: ImageBitmap, rect: Rect): ImageBitmap {
        val crop = rect.toCropRectSpec(image.width, image.height, "rectCrop")
        return image.execute(intArrayOf(RectCropOp), crop.toParams())
            .withSize(crop.width, crop.height, "rectCrop")
    }

    actual fun circleCrop(image: ImageBitmap, rect: Rect): ImageBitmap {
        val crop = rect.toCropRectSpec(image.width, image.height, "circleCrop")
        return image.execute(intArrayOf(CircleCropOp), crop.toParams())
            .withSize(crop.width, crop.height, "circleCrop")
    }

    actual fun roundedRectCrop(image: ImageBitmap, rect: Rect, radius: Float): ImageBitmap {
        val crop = rect.toCropRectSpec(image.width, image.height, "roundedRectCrop")
        val normalizedRadius = normalizeRoundedRadius(radius, crop.width, crop.height)
        return image.execute(
            intArrayOf(RoundedRectCropOp),
            crop.toRoundedRectParams(normalizedRadius),
        ).withSize(crop.width, crop.height, "roundedRectCrop")
    }

    actual fun resize(image: ImageBitmap, tw: Int, th: Int): ImageBitmap {
        requireTargetSize(tw, th, "resize")
        val params = floatArrayOf(tw.toFloat(), th.toFloat())
        return image.execute(intArrayOf(ResizeOp), params).withSize(tw, th, "resize")
    }

    @JvmStatic
    private external fun process(
        b: ByteBuffer,
        w: Int,
        h: Int,
        types: IntArray,
        params: FloatArray
    ): ByteArray

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
