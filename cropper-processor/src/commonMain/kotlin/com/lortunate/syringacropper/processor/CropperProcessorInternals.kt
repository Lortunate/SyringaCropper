package com.lortunate.syringacropper.processor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.lortunate.syringacropper.perspective.PerspectiveQuad
import kotlin.math.roundToInt

internal const val BytesPerPixelArgb8888 = 4

internal const val RectCropOp = 1
internal const val CircleCropOp = 2
internal const val RoundedRectCropOp = 3
internal const val PerspectiveWarpOp = 4
internal const val ResizeOp = 5

internal data class CropRectSpec(
    val left: Float,
    val top: Float,
    val width: Int,
    val height: Int,
)

internal fun requireTargetSize(width: Int, height: Int, operationName: String) {
    require(width > 0 && height > 0) {
        "$operationName requires a positive output size, but was ${width}x$height."
    }
}

internal fun Rect.toCropRectSpec(
    sourceWidth: Int,
    sourceHeight: Int,
    operationName: String,
): CropRectSpec {
    left.requireFinite("$operationName rect.left")
    top.requireFinite("$operationName rect.top")
    right.requireFinite("$operationName rect.right")
    bottom.requireFinite("$operationName rect.bottom")

    val cropWidth = width.roundToInt()
    val cropHeight = height.roundToInt()
    requireTargetSize(cropWidth, cropHeight, operationName)

    require(left >= 0f && top >= 0f) {
        "$operationName requires rect origin inside the source image, but was ($left, $top)."
    }
    require(right <= sourceWidth.toFloat() && bottom <= sourceHeight.toFloat()) {
        "$operationName rect must stay inside the source image ${sourceWidth}x$sourceHeight, " +
            "but was ($left, $top, $right, $bottom)."
    }

    return CropRectSpec(left = left, top = top, width = cropWidth, height = cropHeight)
}

internal fun CropRectSpec.toParams(): FloatArray =
    floatArrayOf(left, top, width.toFloat(), height.toFloat())

internal fun CropRectSpec.toRoundedRectParams(radius: Float): FloatArray =
    floatArrayOf(left, top, width.toFloat(), height.toFloat(), radius)

internal fun normalizeRoundedRadius(radius: Float, cropWidth: Int, cropHeight: Int): Float {
    radius.requireFinite("roundedRectCrop radius")
    require(radius >= 0f) { "roundedRectCrop radius must be >= 0, but was $radius." }
    return radius.coerceAtMost(minOf(cropWidth, cropHeight) / 2f)
}

internal fun PerspectiveQuad.requireProcessable(
    sourceWidth: Int,
    sourceHeight: Int,
    targetWidth: Int,
    targetHeight: Int,
) {
    requireTargetSize(targetWidth, targetHeight, "perspectiveWarp")
    topLeft.requireInBounds("perspectiveWarp topLeft", sourceWidth, sourceHeight)
    topRight.requireInBounds("perspectiveWarp topRight", sourceWidth, sourceHeight)
    bottomRight.requireInBounds("perspectiveWarp bottomRight", sourceWidth, sourceHeight)
    bottomLeft.requireInBounds("perspectiveWarp bottomLeft", sourceWidth, sourceHeight)
}

internal fun ByteArray.requirePixelByteCount(
    width: Int,
    height: Int,
    operationName: String,
): ByteArray {
    val expectedSize = width.toLong() * height.toLong() * BytesPerPixelArgb8888
    require(size.toLong() == expectedSize) {
        "$operationName returned $size bytes, expected $expectedSize bytes for ${width}x$height ARGB_8888 output."
    }
    return this
}

private fun Float.requireFinite(name: String) {
    require(isFinite()) { "$name must be finite, but was $this." }
}

private fun Offset.requireInBounds(name: String, sourceWidth: Int, sourceHeight: Int) {
    x.requireFinite("$name.x")
    y.requireFinite("$name.y")
    require(x in 0f..sourceWidth.toFloat() && y in 0f..sourceHeight.toFloat()) {
        "$name must stay inside the source image ${sourceWidth}x$sourceHeight, but was ($x, $y)."
    }
}
