package com.lortunate.syringacropper

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Original image pixel size used to convert normalized selections into pixel-space coordinates.
 */
data class CropSourceSize(
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0) { "width must be > 0 but was $width" }
        require(height > 0) { "height must be > 0 but was $height" }
    }
}

/** Returns the original pixel size for this decoded [ImageBitmap]. */
fun ImageBitmap.toCropSourceSize(): CropSourceSize = CropSourceSize(width = width, height = height)
