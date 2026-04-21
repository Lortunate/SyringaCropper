package com.lortunate.syringacropper.avatar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.lortunate.syringacropper.center
import com.lortunate.syringacropper.common.normalizeSelectionInsetFraction
import com.lortunate.syringacropper.isEmptySize
import com.lortunate.syringacropper.squareRect
import kotlin.math.max
import kotlin.math.min

enum class AvatarCropShape {
    CIRCLE,
    SQUARE,
}

@Immutable
internal data class AvatarImageTransform(
    val scale: Float,
    val offset: Offset,
    val imageRect: Rect,
)

internal fun defaultSelectionRect(
    containerSize: Size,
    insetFraction: Float,
): Rect {
    if (containerSize.isEmptySize()) return Rect.Zero
    val safeInset = normalizeSelectionInsetFraction(insetFraction)
    val side = max(1f, min(containerSize.width, containerSize.height) * (1f - safeInset * 2f))
    return squareRect(center = containerSize.center, side = side)
}

internal fun transformedImageRect(
    containerSize: Size,
    baseImageRect: Rect,
    scale: Float,
    offset: Offset,
): Rect {
    if (containerSize.isEmptySize() || baseImageRect.isEmpty) return Rect.Zero
    val imageCenter = containerSize.center + offset
    val halfWidth = baseImageRect.width * scale * 0.5f
    val halfHeight = baseImageRect.height * scale * 0.5f
    return Rect(
        left = imageCenter.x - halfWidth,
        top = imageCenter.y - halfHeight,
        right = imageCenter.x + halfWidth,
        bottom = imageCenter.y + halfHeight,
    )
}

internal fun minScaleForSelection(selectionRect: Rect, baseImageRect: Rect): Float {
    if (selectionRect.isEmpty || baseImageRect.isEmpty) return 1f
    return max(
        1f,
        max(selectionRect.width / baseImageRect.width, selectionRect.height / baseImageRect.height),
    )
}

internal fun clampImageOffset(
    containerSize: Size,
    selectionRect: Rect,
    imageSize: Size,
    offset: Offset,
): Offset {
    if (containerSize.isEmptySize() || selectionRect.isEmpty || imageSize.isEmptySize()) return Offset.Zero

    val halfWidth = imageSize.width * 0.5f
    val halfHeight = imageSize.height * 0.5f
    val center = containerSize.center
    val currentCenter = center + offset

    val minCenterX = selectionRect.right - halfWidth
    val maxCenterX = selectionRect.left + halfWidth
    val minCenterY = selectionRect.bottom - halfHeight
    val maxCenterY = selectionRect.top + halfHeight

    return Offset(
        x = currentCenter.x.coerceIn(minCenterX, maxCenterX) - center.x,
        y = currentCenter.y.coerceIn(minCenterY, maxCenterY) - center.y,
    )
}

internal fun scaleImageOffset(
    previousOffset: Offset,
    previousBaseImageRect: Rect,
    nextBaseImageRect: Rect,
): Offset {
    if (previousBaseImageRect.isEmpty || nextBaseImageRect.isEmpty) return Offset.Zero
    return Offset(
        x = if (previousBaseImageRect.width > 0f) {
            previousOffset.x * (nextBaseImageRect.width / previousBaseImageRect.width)
        } else {
            0f
        },
        y = if (previousBaseImageRect.height > 0f) {
            previousOffset.y * (nextBaseImageRect.height / previousBaseImageRect.height)
        } else {
            0f
        },
    )
}

internal fun coerceImageTransform(
    containerSize: Size,
    selectionRect: Rect,
    baseImageRect: Rect,
    scale: Float,
    offset: Offset,
    maxScale: Float,
): AvatarImageTransform {
    if (containerSize.isEmptySize() || baseImageRect.isEmpty) {
        return AvatarImageTransform(
            scale = 1f,
            offset = Offset.Zero,
            imageRect = Rect.Zero,
        )
    }

    val safeScale = scale.coerceAtLeast(1f)
    if (selectionRect.isEmpty) {
        val resolvedScale = resolvedImageScale(
            requestedScale = safeScale,
            selectionRect = selectionRect,
            baseImageRect = baseImageRect,
            maxScale = maxScale,
        )
        return AvatarImageTransform(
            scale = resolvedScale,
            offset = offset,
            imageRect = transformedImageRect(
                containerSize = containerSize,
                baseImageRect = baseImageRect,
                scale = resolvedScale,
                offset = offset,
            ),
        )
    }

    val resolvedScale = resolvedImageScale(
        requestedScale = safeScale,
        selectionRect = selectionRect,
        baseImageRect = baseImageRect,
        maxScale = maxScale,
    )
    val resolvedOffset = clampImageOffset(
        containerSize = containerSize,
        selectionRect = selectionRect,
        imageSize = imageSizeForScale(baseImageRect, resolvedScale),
        offset = offset,
    )
    return AvatarImageTransform(
        scale = resolvedScale,
        offset = resolvedOffset,
        imageRect = transformedImageRect(
            containerSize = containerSize,
            baseImageRect = baseImageRect,
            scale = resolvedScale,
            offset = resolvedOffset,
        ),
    )
}

internal fun transformImageWithGesture(
    containerSize: Size,
    selectionRect: Rect,
    baseImageRect: Rect,
    currentScale: Float,
    currentOffset: Offset,
    maxScale: Float,
    centroid: Offset,
    pan: Offset,
    zoom: Float,
): AvatarImageTransform? {
    if (containerSize.isEmptySize() || selectionRect.isEmpty || baseImageRect.isEmpty) return null

    val currentTransform = coerceImageTransform(
        containerSize = containerSize,
        selectionRect = selectionRect,
        baseImageRect = baseImageRect,
        scale = currentScale,
        offset = currentOffset,
        maxScale = maxScale,
    )
    val nextScale = resolvedImageScale(
        requestedScale = currentTransform.scale * zoom,
        selectionRect = selectionRect,
        baseImageRect = baseImageRect,
        maxScale = maxScale,
    )
    val effectiveZoom = nextScale / currentTransform.scale
    val viewportCenter = containerSize.center
    val relativeCentroid = Offset(
        x = centroid.x - viewportCenter.x - currentTransform.offset.x,
        y = centroid.y - viewportCenter.y - currentTransform.offset.y,
    )
    val nextOffset = Offset(
        x = centroid.x + pan.x - viewportCenter.x - relativeCentroid.x * effectiveZoom,
        y = centroid.y + pan.y - viewportCenter.y - relativeCentroid.y * effectiveZoom,
    )
    return coerceImageTransform(
        containerSize = containerSize,
        selectionRect = selectionRect,
        baseImageRect = baseImageRect,
        scale = nextScale,
        offset = nextOffset,
        maxScale = maxScale,
    )
}

private fun imageSizeForScale(baseImageRect: Rect, scale: Float): Size {
    return Size(
        width = baseImageRect.width * scale,
        height = baseImageRect.height * scale,
    )
}

private fun resolvedImageScale(
    requestedScale: Float,
    selectionRect: Rect,
    baseImageRect: Rect,
    maxScale: Float,
): Float {
    val minimumScale = minScaleForSelection(selectionRect, baseImageRect)
    return requestedScale.coerceIn(
        minimumValue = minimumScale,
        maximumValue = maxOf(maxScale.coerceAtLeast(1f), minimumScale),
    )
}
