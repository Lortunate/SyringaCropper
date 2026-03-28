package com.lortunate.syringacropper

/** Original image size used to convert normalized selections into pixel-space coordinates. */
data class CropSourceSize(
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0) { "width must be > 0 but was $width" }
        require(height > 0) { "height must be > 0 but was $height" }
    }
}
