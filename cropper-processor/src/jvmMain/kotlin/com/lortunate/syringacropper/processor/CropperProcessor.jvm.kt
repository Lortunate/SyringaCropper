package com.lortunate.syringacropper.processor

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import com.lortunate.syringacropper.perspective.PerspectiveQuad
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption

actual object CropperProcessor {
    actual val supportMessage: String? = null

    init { loadNativeLibrary() }

    actual fun perspectiveWarp(image: ImageBitmap, quad: PerspectiveQuad, tw: Int, th: Int): ImageBitmap {
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

    @JvmStatic private external fun process(b: ByteBuffer, w: Int, h: Int, types: IntArray, params: FloatArray): ByteArray

    private fun process(image: ImageBitmap, operation: CropperOperation): ImageBitmap {
        return processNativeOperation(
            image = image,
            operation = operation,
            execute = { source, types, params -> source.execute(types, params) },
            toImageBitmap = { bytes, width, height, operationName ->
                bytes.toSkiaImageBitmap(width, height, operationName)
            },
        )
    }

    private fun ImageBitmap.execute(types: IntArray, params: FloatArray): ByteArray {
        val pixels = readSkiaPixels()
        val w = width
        val h = height
        val buf = ByteBuffer.allocateDirect(w * h * BytesPerPixelArgb8888)
        buf.put(pixels)
        buf.rewind()
        return process(buf, w, h, types, params)
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
