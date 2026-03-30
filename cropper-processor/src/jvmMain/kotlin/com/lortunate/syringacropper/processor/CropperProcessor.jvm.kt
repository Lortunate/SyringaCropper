package com.lortunate.syringacropper.processor

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.lortunate.syringacropper.perspective.PerspectiveQuad
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption

actual object CropperProcessor {
    init { loadNativeLibrary() }

    actual fun perspectiveWarp(image: ImageBitmap, quad: PerspectiveQuad, tw: Int, th: Int): ImageBitmap {
        quad.requireProcessable(image.width, image.height, tw, th)
        val params = floatArrayOf(
            quad.topLeft.x, quad.topLeft.y, quad.topRight.x, quad.topRight.y,
            quad.bottomRight.x, quad.bottomRight.y, quad.bottomLeft.x, quad.bottomLeft.y,
            tw.toFloat(), th.toFloat()
        )
        return image.execute(intArrayOf(PerspectiveWarpOp), params).withSize(tw, th, "perspectiveWarp")
    }

    actual fun rectCrop(image: ImageBitmap, rect: Rect): ImageBitmap {
        val crop = rect.toCropRectSpec(image.width, image.height, "rectCrop")
        return image.execute(intArrayOf(RectCropOp), crop.toParams()).withSize(crop.width, crop.height, "rectCrop")
    }

    actual fun circleCrop(image: ImageBitmap, rect: Rect): ImageBitmap {
        val crop = rect.toCropRectSpec(image.width, image.height, "circleCrop")
        return image.execute(intArrayOf(CircleCropOp), crop.toParams()).withSize(crop.width, crop.height, "circleCrop")
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

    @JvmStatic private external fun process(b: ByteBuffer, w: Int, h: Int, types: IntArray, params: FloatArray): ByteArray

    private fun ImageBitmap.execute(types: IntArray, params: FloatArray): ByteArray {
        val skia = asSkiaBitmap()
        val w = skia.width
        val h = skia.height
        val buf = ByteBuffer.allocateDirect(w * h * BytesPerPixelArgb8888)
        val pixels = requireNotNull(skia.readPixels(skia.imageInfo, w * BytesPerPixelArgb8888, 0, 0)) {
            "Failed to read source pixels for cropper processing."
        }
        buf.put(pixels)
        buf.rewind()
        return process(buf, w, h, types, params)
    }

    private fun ByteArray.withSize(w: Int, h: Int, operationName: String): ImageBitmap {
        val bytes = requirePixelByteCount(w, h, operationName)
        val info = ImageInfo.makeN32(w, h, ColorAlphaType.PREMUL)
        return Image.makeRaster(info, bytes, w * BytesPerPixelArgb8888).toComposeImageBitmap()
    }

    private fun loadNativeLibrary() {
        val os = System.getProperty("os.name").lowercase()
        val (prefix, suffix) = when {
            os.contains("win") -> "" to ".dll"
            os.contains("mac") -> "lib" to ".dylib"
            else -> "lib" to ".so"
        }
        val resourceName = "${prefix}cropper_processor$suffix"

        var resourceError: Throwable? = null
        try {
            CropperProcessor::class.java.classLoader.getResourceAsStream(resourceName)?.use { input ->
                val tempPath = Files.createTempFile("cropper_processor", suffix)
                tempPath.toFile().deleteOnExit()
                Files.copy(input, tempPath, StandardCopyOption.REPLACE_EXISTING)
                System.load(tempPath.toAbsolutePath().toString())
                return
            }
        } catch (error: Throwable) {
            resourceError = error
        }

        try {
            System.loadLibrary("cropper_processor")
        } catch (error: UnsatisfiedLinkError) {
            throw IllegalStateException("Unable to load cropper_processor native library.").also { exception ->
                resourceError?.let(exception::addSuppressed)
                exception.addSuppressed(error)
            }
        }
    }
}
