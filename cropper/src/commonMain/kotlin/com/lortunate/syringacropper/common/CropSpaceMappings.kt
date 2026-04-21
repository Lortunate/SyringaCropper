package com.lortunate.syringacropper.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.lortunate.syringacropper.CropSourceSize
import com.lortunate.syringacropper.CropperRectSelection
import com.lortunate.syringacropper.DEFAULT_MAX_SELECTION_INSET
import com.lortunate.syringacropper.avatar.AvatarCropSelection
import com.lortunate.syringacropper.avatar.AvatarCropShape
import com.lortunate.syringacropper.perspective.PerspectiveCropSelection
import com.lortunate.syringacropper.perspective.PerspectiveQuad

internal fun normalizeSelectionInsetFraction(insetFraction: Float): Float {
    return insetFraction.coerceIn(0f, DEFAULT_MAX_SELECTION_INSET)
}

internal fun Rect.denormalizeTo(bounds: Rect, clampToBounds: Boolean = false): Rect {
    if (bounds.isEmpty) return Rect.Zero
    return Rect(
        left = denormalize(left, origin = bounds.left, size = bounds.width, clampToBounds = clampToBounds),
        top = denormalize(top, origin = bounds.top, size = bounds.height, clampToBounds = clampToBounds),
        right = denormalize(right, origin = bounds.left, size = bounds.width, clampToBounds = clampToBounds),
        bottom = denormalize(bottom, origin = bounds.top, size = bounds.height, clampToBounds = clampToBounds),
    )
}

fun Rect.scaleToImageSpace(
    sourceSize: CropSourceSize,
    clampToBounds: Boolean = false,
): Rect = scaleToImageSpace(
    imageWidth = sourceSize.width.toFloat(),
    imageHeight = sourceSize.height.toFloat(),
    clampToBounds = clampToBounds,
)

internal fun Rect.scaleToImageSpace(
    imageWidth: Float,
    imageHeight: Float,
    clampToBounds: Boolean = false,
): Rect {
    return Rect(
        left = scale(left, size = imageWidth, clampToBounds = clampToBounds),
        top = scale(top, size = imageHeight, clampToBounds = clampToBounds),
        right = scale(right, size = imageWidth, clampToBounds = clampToBounds),
        bottom = scale(bottom, size = imageHeight, clampToBounds = clampToBounds),
    )
}

internal fun Offset.scaleToImageSpace(
    imageWidth: Float,
    imageHeight: Float,
    clampToBounds: Boolean = false,
): Offset = Offset(
    x = scale(x, size = imageWidth, clampToBounds = clampToBounds),
    y = scale(y, size = imageHeight, clampToBounds = clampToBounds),
)

internal fun PerspectiveQuad.scaleToImageSpace(
    sourceSize: CropSourceSize,
    clampToBounds: Boolean = false,
): PerspectiveQuad = scaleToImageSpace(
    imageWidth = sourceSize.width.toFloat(),
    imageHeight = sourceSize.height.toFloat(),
    clampToBounds = clampToBounds,
)

internal fun PerspectiveQuad.scaleToImageSpace(
    imageWidth: Float,
    imageHeight: Float,
    clampToBounds: Boolean = false,
): PerspectiveQuad = PerspectiveQuad(
    topLeft = topLeft.scaleToImageSpace(imageWidth, imageHeight, clampToBounds),
    topRight = topRight.scaleToImageSpace(imageWidth, imageHeight, clampToBounds),
    bottomRight = bottomRight.scaleToImageSpace(imageWidth, imageHeight, clampToBounds),
    bottomLeft = bottomLeft.scaleToImageSpace(imageWidth, imageHeight, clampToBounds),
)

internal fun Rect.toRectSelection(sourceSize: CropSourceSize): CropperRectSelection = CropperRectSelection(
    normalizedRect = this,
    imageRect = scaleToImageSpace(sourceSize),
    sourceSize = sourceSize,
)

internal fun Rect.toAvatarSelection(
    shape: AvatarCropShape,
    sourceSize: CropSourceSize,
): AvatarCropSelection = AvatarCropSelection(
    sourceSize = sourceSize,
    shape = shape,
    normalizedRect = this,
    imageRect = scaleToImageSpace(sourceSize),
)

internal fun PerspectiveQuad.toPerspectiveSelection(sourceSize: CropSourceSize): PerspectiveCropSelection =
    PerspectiveCropSelection(
        normalizedQuad = this,
        imageQuad = scaleToImageSpace(sourceSize, clampToBounds = true),
        sourceSize = sourceSize,
    )

private fun denormalize(
    value: Float,
    origin: Float,
    size: Float,
    clampToBounds: Boolean,
): Float {
    val scaled = origin + value * size
    return if (clampToBounds) scaled.coerceIn(origin, origin + size) else scaled
}

private fun scale(
    value: Float,
    size: Float,
    clampToBounds: Boolean,
): Float {
    val scaled = value * size
    return if (clampToBounds) scaled.coerceIn(0f, size.coerceAtLeast(0f)) else scaled
}
