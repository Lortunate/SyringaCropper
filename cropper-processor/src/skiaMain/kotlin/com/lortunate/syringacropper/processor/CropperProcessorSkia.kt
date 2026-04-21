package com.lortunate.syringacropper.processor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

internal fun ImageBitmap.readSkiaPixels(): ByteArray {
    val skia = asSkiaBitmap()
    val info = ImageInfo(skia.width, skia.height, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
    return requireNotNull(skia.readPixels(info, skia.width * BytesPerPixelArgb8888, 0, 0)) {
        "Failed to read source pixels for cropper processing."
    }
}

internal fun ByteArray.toSkiaImageBitmap(
    width: Int,
    height: Int,
    operationName: String,
): ImageBitmap {
    val bytes = requirePixelByteCount(width, height, operationName)
    val info = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
    return Image.makeRaster(info, bytes, width * BytesPerPixelArgb8888).toComposeImageBitmap()
}
