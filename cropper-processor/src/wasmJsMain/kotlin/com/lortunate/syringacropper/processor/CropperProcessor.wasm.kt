package com.lortunate.syringacropper.processor

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.lortunate.syringacropper.perspective.PerspectiveQuad
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsNumber
import kotlin.js.toJsArray
import kotlin.js.toJsNumber
import kotlin.js.unsafeCast
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

@OptIn(ExperimentalWasmJsInterop::class)
actual object CropperProcessor {
    actual fun perspectiveWarp(
        image: ImageBitmap,
        quad: PerspectiveQuad,
        tw: Int,
        th: Int,
    ): ImageBitmap {
        quad.requireProcessable(image.width, image.height, tw, th)
        val params = floatArrayOf(
            quad.topLeft.x, quad.topLeft.y, quad.topRight.x, quad.topRight.y,
            quad.bottomRight.x, quad.bottomRight.y, quad.bottomLeft.x, quad.bottomLeft.y,
        )
        return perspectiveWarpBytes(
            image.readPixels().toJsByteArray(),
            image.width,
            image.height,
            params.toJsFloatArray(),
            tw,
            th,
        )
            .requireByteArray("perspectiveWarp")
            .withSize(tw, th, "perspectiveWarp")
    }

    actual fun rectCrop(image: ImageBitmap, rect: Rect): ImageBitmap {
        val crop = rect.toCropRectSpec(image.width, image.height, "rectCrop")
        return rectCropBytes(
            image.readPixels().toJsByteArray(),
            image.width,
            image.height,
            crop.left.toInt(),
            crop.top.toInt(),
            crop.width,
            crop.height,
        ).requireByteArray("rectCrop")
            .withSize(crop.width, crop.height, "rectCrop")
    }

    actual fun circleCrop(image: ImageBitmap, rect: Rect): ImageBitmap {
        val crop = rect.toCropRectSpec(image.width, image.height, "circleCrop")
        return circleCropBytes(
            image.readPixels().toJsByteArray(),
            image.width,
            image.height,
            crop.left.toInt(),
            crop.top.toInt(),
            crop.width,
            crop.height,
        ).requireByteArray("circleCrop")
            .withSize(crop.width, crop.height, "circleCrop")
    }

    actual fun roundedRectCrop(image: ImageBitmap, rect: Rect, radius: Float): ImageBitmap {
        val crop = rect.toCropRectSpec(image.width, image.height, "roundedRectCrop")
        val normalizedRadius = normalizeRoundedRadius(radius, crop.width, crop.height)
        return roundedRectCropBytes(
            image.readPixels().toJsByteArray(),
            image.width,
            image.height,
            crop.left.toInt(),
            crop.top.toInt(),
            crop.width,
            crop.height,
            normalizedRadius,
        ).requireByteArray("roundedRectCrop")
            .withSize(crop.width, crop.height, "roundedRectCrop")
    }

    actual fun resize(image: ImageBitmap, tw: Int, th: Int): ImageBitmap {
        requireTargetSize(tw, th, "resize")
        return resizeBytes(
            image.readPixels().toJsByteArray(),
            image.width,
            image.height,
            tw,
            th,
        )
            .requireByteArray("resize")
            .withSize(tw, th, "resize")
    }

    private fun ImageBitmap.readPixels(): ByteArray {
        val skia = asSkiaBitmap()
        val info = ImageInfo(skia.width, skia.height, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
        return requireNotNull(skia.readPixels(info, skia.width * BytesPerPixelArgb8888, 0, 0)) {
            "Failed to read source pixels for cropper processing."
        }
    }

    private fun ByteArray.withSize(w: Int, h: Int, operationName: String): ImageBitmap {
        val bytes = requirePixelByteCount(w, h, operationName)
        val info = ImageInfo(w, h, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
        return Image.makeRaster(info, bytes, w * BytesPerPixelArgb8888).toComposeImageBitmap()
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun JsAny?.requireByteArray(operationName: String): ByteArray {
    val output = requireNotNull(this) {
        "CropperProcessor.$operationName returned no pixels."
    }.unsafeCast<JsArray<JsNumber>>()
    return ByteArray(output.length) { index -> output[index]!!.toInt().toByte() }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun ByteArray.toJsByteArray(): JsAny {
    return Array(size) { index -> (this[index].toInt() and 0xFF).toJsNumber() }.toJsArray().unsafeCast()
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun FloatArray.toJsFloatArray(): JsAny {
    return Array(size) { index -> this[index].toDouble().toJsNumber() }.toJsArray().unsafeCast()
}
