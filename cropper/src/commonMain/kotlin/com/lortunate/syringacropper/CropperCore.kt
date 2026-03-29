package com.lortunate.syringacropper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.min

/**
 * Defines the visual shape of cropper handles.
 */
enum class CropperHandleShape {
    CIRCLE,
    SQUARE,
}

/**
 * Defines the handles used for interaction.
 */
enum class CropperHandle {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    TOP_EDGE,
    RIGHT_EDGE,
    BOTTOM_EDGE,
    LEFT_EDGE,
}

/**
 * Geometry for a centered bar handle (typically used for edges).
 */
data class CropperEdgeHandleGeometry(
    val center: Offset,
    val width: Float,
    val height: Float,
    val rotationDegrees: Float = 0f,
)

/**
 * Result of a rectangular crop operation.
 */
data class CropperRectSelection(
    val imageRect: Rect,
    val normalizedRect: Rect,
    val sourceSize: CropSourceSize,
)

/**
 * Common pixel-space metrics used for drawing cropper overlays.
 */
internal data class CropperDrawMetrics(
    val frameStrokePx: Float,
    val gridStrokePx: Float,
    val imageBoundsStrokePx: Float,
    val cornerRadiusPx: Float,
    val cornerStrokePx: Float,
    val cornerTouchRadiusPx: Float,
    val edgeLengthPx: Float,
    val edgeThicknessPx: Float,
    val edgeCornerRadiusPx: Float,
    val edgeStrokePx: Float,
    val edgeTouchThicknessPx: Float,
)

/**
 * Utility to remember [CropperDrawMetrics] based on style and density.
 */
@Composable
internal fun rememberCropperDrawMetrics(
    style: CropperStyle,
): CropperDrawMetrics {
    val density = LocalDensity.current
    return remember(style, density) {
        with(density) {
            CropperDrawMetrics(
                frameStrokePx = style.frame.strokeWidth.toPx(),
                gridStrokePx = style.frame.gridStrokeWidth.toPx(),
                imageBoundsStrokePx = style.frame.imageBoundsStrokeWidth.toPx(),
                cornerRadiusPx = style.handle.corner.radius.toPx(),
                cornerStrokePx = style.handle.corner.strokeWidth.toPx(),
                cornerTouchRadiusPx = style.handle.corner.touchRadius.toPx(),
                edgeLengthPx = style.handle.edge.length.toPx(),
                edgeThicknessPx = style.handle.edge.thickness.toPx(),
                edgeCornerRadiusPx = style.handle.edge.cornerRadius.toPx(),
                edgeStrokePx = style.handle.edge.strokeWidth.toPx(),
                edgeTouchThicknessPx = style.handle.edge.touchThickness.toPx(),
            )
        }
    }
}

/**
 * Calculates the centered, aspect-fitted bounding rectangle for an image within a container size.
 */
fun calculateImageFitRect(
    containerSize: Size,
    imageWidth: Float,
    imageHeight: Float,
): Rect {
    if (containerSize.width <= 0f || containerSize.height <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
        return Rect.Zero
    }

    val scale = min(containerSize.width / imageWidth, containerSize.height / imageHeight)
    val drawWidth = imageWidth * scale
    val drawHeight = imageHeight * scale
    val left = (containerSize.width - drawWidth) * 0.5f
    val top = (containerSize.height - drawHeight) * 0.5f
    return Rect(left, top, left + drawWidth, top + drawHeight)
}

/**
 * Utility to draw a single cropper corner handle marker.
 */
internal fun DrawScope.drawCropperCornerHandleMarker(
    center: Offset,
    shape: CropperHandleShape,
    radius: Float,
    fillColor: Color,
    strokeColor: Color,
    strokeWidth: Float,
) {
    when (shape) {
        CropperHandleShape.CIRCLE -> {
            drawCircle(color = fillColor, radius = radius, center = center, style = Fill)
            if (strokeWidth > 0f) {
                drawCircle(
                    color = strokeColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        CropperHandleShape.SQUARE -> {
            val markerSize = Size(radius * 2f, radius * 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            drawRect(color = fillColor, topLeft = topLeft, size = markerSize, style = Fill)
            if (strokeWidth > 0f) {
                drawRect(
                    color = strokeColor,
                    topLeft = topLeft,
                    size = markerSize,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}

/**
 * Utility to draw a single cropper edge handle marker.
 */
internal fun DrawScope.drawCropperEdgeHandleMarker(
    geometry: CropperEdgeHandleGeometry,
    cornerRadius: Float,
    fillColor: Color,
    strokeColor: Color,
    strokeWidth: Float,
) {
    val markerSize = Size(geometry.width, geometry.height)
    val topLeft = Offset(
        x = geometry.center.x - geometry.width / 2f,
        y = geometry.center.y - geometry.height / 2f,
    )
    val maxRadius = minOf(geometry.width, geometry.height) / 2f
    val clampedRadius = cornerRadius.coerceIn(0f, maxRadius)
    val markerCornerRadius = androidx.compose.ui.geometry.CornerRadius(clampedRadius, clampedRadius)

    rotate(degrees = geometry.rotationDegrees, pivot = geometry.center) {
        drawRoundRect(
            color = fillColor,
            topLeft = topLeft,
            size = markerSize,
            cornerRadius = markerCornerRadius,
            style = Fill,
        )
        if (strokeWidth > 0f) {
            drawRoundRect(
                color = strokeColor,
                topLeft = topLeft,
                size = markerSize,
                cornerRadius = markerCornerRadius,
                style = Stroke(width = strokeWidth),
            )
        }
    }
}


/**
 * Coerces this [Rect] to be completely inside the given [bounds], while respecting a [minSizePx].
 */
fun Rect.coerceInside(bounds: Rect, minSizePx: Float): Rect {
    if (bounds.isEmpty) return this
    val w = width.coerceIn(minSizePx.coerceAtMost(bounds.width), bounds.width)
    val h = height.coerceIn(minSizePx.coerceAtMost(bounds.height), bounds.height)

    val l = left.coerceIn(bounds.left, bounds.right - w)
    val t = top.coerceIn(bounds.top, bounds.bottom - h)
    return Rect(l, t, l + w, t + h)
}

/**
 * Normalizes a [Rect] to a 0..1 range within the given [bounds].
 */
fun Rect.toNormalizedRect(bounds: Rect): Rect {
    if (bounds.isEmpty) return Rect.Zero
    return Rect(
        left = ((left - bounds.left) / bounds.width).coerceIn(0f, 1f),
        top = ((top - bounds.top) / bounds.height).coerceIn(0f, 1f),
        right = ((right - bounds.left) / bounds.width).coerceIn(0f, 1f),
        bottom = ((bottom - bounds.top) / bounds.height).coerceIn(0f, 1f),
    )
}

/**
 * Remaps a [Rect] from a [source] rect to a [target] rect.
 */
fun Rect.remapRect(source: Rect, target: Rect): Rect {
    if (source.isEmpty) return this
    val left = target.left + (left - source.left) / source.width * target.width
    val top = target.top + (top - source.top) / source.height * target.height
    val right = target.left + (right - source.left) / source.width * target.width
    val bottom = target.top + (bottom - source.top) / source.height * target.height
    return Rect(left, top, right, bottom)
}

/**
 * Normalizes an [Offset] to a 0..1 range within the given [rect].
 */
fun Offset.toNormalized(rect: Rect): Offset {
    if (rect.width <= 0f || rect.height <= 0f) return this
    return Offset(
        x = ((x - rect.left) / rect.width).coerceIn(0f, 1f),
        y = ((y - rect.top) / rect.height).coerceIn(0f, 1f),
    )
}

/**
 * Remaps an [Offset] from a [source] rect to a [target] rect.
 */
fun Offset.remap(source: Rect, target: Rect): Offset {
    if (source.width <= 0f || source.height <= 0f) return this
    val normalizedX = (x - source.left) / source.width
    val normalizedY = (y - source.top) / source.height
    return Offset(
        x = target.left + normalizedX * target.width,
        y = target.top + normalizedY * target.height,
    )
}

/**
 * Linear interpolation between two [Offset]s.
 */
fun lerp(start: Offset, end: Offset, t: Float): Offset = Offset(
    x = start.x + (end.x - start.x) * t,
    y = start.y + (end.y - start.y) * t,
)

/**
 * Returns the midpoint between two [Offset]s.
 */
fun midpoint(start: Offset, end: Offset): Offset = Offset(
    x = (start.x + end.x) * 0.5f,
    y = (start.y + end.y) * 0.5f,
)

const val DEFAULT_MAX_SELECTION_INSET = 0.45f

/**
 * Returns the center [Offset] of this [Size].
 */
val Size.center: Offset
    get() = Offset(width * 0.5f, height * 0.5f)

/**
 * Returns true if the [Size] has zero or negative width or height.
 */
fun Size.isEmptySize(): Boolean = width <= 0f || height <= 0f

/**
 * Creates a square [Rect] with the given [center] and [side] length.
 */
fun squareRect(center: Offset, side: Float): Rect {
    val halfSide = side * 0.5f
    return Rect(
        left = center.x - halfSide,
        top = center.y - halfSide,
        right = center.x + halfSide,
        bottom = center.y + halfSide,
    )
}

/**
 * Scales an [Offset] by a constant factor.
 */
fun Offset.scale(scale: Float): Offset = Offset(x * scale, y * scale)

/**
 * Coerces an [Offset] to be within the bounds of a [rect].
 */
fun Offset.coerceTo(rect: Rect): Offset = Offset(
    x = x.coerceIn(rect.left, rect.right),
    y = y.coerceIn(rect.top, rect.bottom),
)

/**
 * Returns the squared distance between two [Offset]s.
 */
fun distanceSquared(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return dx * dx + dy * dy
}

/**
 * Returns the dot product of two [Offset]s.
 */
fun Offset.dot(other: Offset): Float = x * other.x + y * other.y
