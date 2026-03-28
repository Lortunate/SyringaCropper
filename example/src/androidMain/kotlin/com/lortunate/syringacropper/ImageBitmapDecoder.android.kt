package com.lortunate.syringacropper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.ceil
import kotlin.math.log2

private const val MAX_PREVIEW_BITMAP_BYTES = 100L * 1024L * 1024L
private const val BYTES_PER_ARGB_8888_PIXEL = 4L

internal actual fun decodePreviewImage(bytes: ByteArray): PreviewImagePayload? {
    if (bytes.isEmpty()) return null

    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    val width = bounds.outWidth
    val height = bounds.outHeight
    if (width <= 0 || height <= 0) return null

    val inSampleSize = calculateInSampleSize(width = width, height = height)
    val decodeOptions = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        this.inSampleSize = inSampleSize
    }

    val previewBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)?.asImageBitmap()
        ?: return null
    return PreviewImagePayload(
        previewBitmap = previewBitmap,
        sourceSize = CropSourceSize(width = width, height = height),
    )
}

private fun calculateInSampleSize(width: Int, height: Int): Int {
    val totalBytes = width.toLong() * height.toLong() * BYTES_PER_ARGB_8888_PIXEL
    if (totalBytes <= MAX_PREVIEW_BITMAP_BYTES) return 1

    val scale = ceil(kotlin.math.sqrt(totalBytes.toDouble() / MAX_PREVIEW_BITMAP_BYTES.toDouble()))
    val exponent = ceil(log2(scale)).toInt().coerceAtLeast(0)
    return 1 shl exponent
}
