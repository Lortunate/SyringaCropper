package com.lortunate.syringacropper.processor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import com.lortunate.syringacropper.CropSourceSize
import com.lortunate.syringacropper.CropperRectSelection
import com.lortunate.syringacropper.avatar.AvatarCropSelection
import com.lortunate.syringacropper.avatar.AvatarCropShape
import com.lortunate.syringacropper.perspective.PerspectiveCropSelection
import com.lortunate.syringacropper.perspective.PerspectiveQuad
import com.lortunate.syringacropper.perspective.toImageSpace
import com.lortunate.syringacropper.common.scaleToImageSpace
import kotlin.math.hypot
import kotlin.math.roundToInt

fun ImageBitmap.crop(selection: PerspectiveCropSelection): ImageBitmap {
    val quad = selection.normalizedQuad.toBitmapQuad(width, height)
    val (targetWidth, targetHeight) = quad.estimatedOutputSize()
    return CropperProcessor.perspectiveWarp(this, quad, targetWidth, targetHeight)
}

fun ImageBitmap.crop(selection: CropperRectSelection): ImageBitmap {
    return CropperProcessor.rectCrop(this, selection.normalizedRect.toBitmapRect(width, height))
}

fun ImageBitmap.crop(selection: AvatarCropSelection): ImageBitmap {
    val rect = selection.normalizedRect.toBitmapRect(width, height)
    return when (selection.shape) {
        AvatarCropShape.CIRCLE -> CropperProcessor.circleCrop(this, rect)
        AvatarCropShape.SQUARE -> CropperProcessor.rectCrop(this, rect)
    }
}

internal fun Rect.toBitmapRect(bitmapWidth: Int, bitmapHeight: Int): Rect {
    return scaleToImageSpace(
        sourceSize = CropSourceSize(width = bitmapWidth, height = bitmapHeight),
        clampToBounds = true,
    )
}

internal fun PerspectiveQuad.toBitmapQuad(bitmapWidth: Int, bitmapHeight: Int): PerspectiveQuad {
    return toImageSpace(
        sourceSize = CropSourceSize(width = bitmapWidth, height = bitmapHeight),
    )
}

internal fun PerspectiveQuad.estimatedOutputSize(): Pair<Int, Int> {
    val topWidth = topLeft.distanceTo(topRight)
    val bottomWidth = bottomLeft.distanceTo(bottomRight)
    val leftHeight = topLeft.distanceTo(bottomLeft)
    val rightHeight = topRight.distanceTo(bottomRight)

    val targetWidth = ((topWidth + bottomWidth) * 0.5f).roundToInt().coerceAtLeast(1)
    val targetHeight = ((leftHeight + rightHeight) * 0.5f).roundToInt().coerceAtLeast(1)
    return targetWidth to targetHeight
}

internal fun AvatarCropSelection.isCircleCrop(): Boolean = shape == AvatarCropShape.CIRCLE

private fun Offset.distanceTo(other: Offset): Float {
    return hypot(other.x - x, other.y - y)
}
