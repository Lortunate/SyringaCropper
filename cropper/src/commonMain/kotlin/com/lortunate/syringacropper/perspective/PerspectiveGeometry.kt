package com.lortunate.syringacropper.perspective

import com.lortunate.syringacropper.CropSourceSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

internal const val PERSPECTIVE_EPSILON = 1e-3f
private const val MIN_EDGE_LENGTH_FRACTION_LIMIT = 0.2f
private const val MAX_EDGE_LENGTH_FRACTION_LIMIT = 1f
private const val RADIANS_TO_DEGREES = 57.29578f

internal fun clampEdgeLengthFractionLimit(value: Float): Float = value.coerceIn(
    MIN_EDGE_LENGTH_FRACTION_LIMIT,
    MAX_EDGE_LENGTH_FRACTION_LIMIT,
)

internal data class CachedEdgeGeometry(
    val center: Offset,
    val tangent: Offset,
    val normal: Offset,
    val edgeLength: Float,
    val rotationDegrees: Float,
) {
    fun toCenteredEdgeBarGeometry(
        edgeLengthPx: Float,
        edgeThicknessPx: Float,
        edgeLengthFractionLimit: Float,
    ): CenteredEdgeBarGeometry? {
        if (edgeThicknessPx <= PERSPECTIVE_EPSILON) return null

        val clampedFraction = clampEdgeLengthFractionLimit(edgeLengthFractionLimit)
        val constrainedLength = minOf(edgeLengthPx.coerceAtLeast(0f), edgeLength * clampedFraction)
        if (constrainedLength <= PERSPECTIVE_EPSILON) return null

        return CenteredEdgeBarGeometry(
            center = center,
            tangent = tangent,
            normal = normal,
            halfLength = constrainedLength * 0.5f,
            halfThickness = edgeThicknessPx * 0.5f,
            rotationDegrees = rotationDegrees,
        )
    }
}

internal data class CenteredEdgeBarGeometry(
    val center: Offset,
    val tangent: Offset,
    val normal: Offset,
    val halfLength: Float,
    val halfThickness: Float,
    val rotationDegrees: Float,
) {
    fun contains(point: Offset, cornerRadiusPx: Float = 0f): Boolean {
        val toPoint = point - center
        val tangentDistance = toPoint.dot(tangent)
        val normalDistance = toPoint.dot(normal)
        val absTangentDistance = abs(tangentDistance)
        val absNormalDistance = abs(normalDistance)
        if (absTangentDistance > halfLength || absNormalDistance > halfThickness) return false

        val clampedCornerRadius = cornerRadiusPx.coerceIn(0f, minOf(halfLength, halfThickness))
        if (clampedCornerRadius <= PERSPECTIVE_EPSILON) return true

        val innerHalfLength = halfLength - clampedCornerRadius
        val innerHalfThickness = halfThickness - clampedCornerRadius
        if (absTangentDistance <= innerHalfLength || absNormalDistance <= innerHalfThickness) return true

        val cornerDx = absTangentDistance - innerHalfLength
        val cornerDy = absNormalDistance - innerHalfThickness
        return cornerDx * cornerDx + cornerDy * cornerDy <= clampedCornerRadius * clampedCornerRadius
    }
}

internal fun midpoint(start: Offset, end: Offset): Offset = Offset(
    x = (start.x + end.x) * 0.5f,
    y = (start.y + end.y) * 0.5f,
)

internal fun distanceSquared(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return dx * dx + dy * dy
}

internal fun Offset.scale(scale: Float): Offset = Offset(x * scale, y * scale)

internal fun Offset.dot(other: Offset): Float = x * other.x + y * other.y

internal fun Offset.coerceTo(rect: Rect): Offset = Offset(
    x = x.coerceIn(rect.left, rect.right),
    y = y.coerceIn(rect.top, rect.bottom),
)

internal fun projectToEdgeNormal(delta: Offset, edgeStart: Offset, edgeEnd: Offset): Offset {
    val edgeX = edgeEnd.x - edgeStart.x
    val edgeY = edgeEnd.y - edgeStart.y
    val edgeLength = sqrt(edgeX * edgeX + edgeY * edgeY)
    if (edgeLength <= PERSPECTIVE_EPSILON) return Offset.Zero
    val normal = Offset(-edgeY / edgeLength, edgeX / edgeLength)
    val distanceOnNormal = delta.dot(normal)
    return Offset(normal.x * distanceOnNormal, normal.y * distanceOnNormal)
}

internal fun cachedEdgeGeometry(
    edgeStart: Offset,
    edgeEnd: Offset,
): CachedEdgeGeometry? {
    val edgeX = edgeEnd.x - edgeStart.x
    val edgeY = edgeEnd.y - edgeStart.y
    val edgeMagnitude = sqrt(edgeX * edgeX + edgeY * edgeY)
    if (edgeMagnitude <= PERSPECTIVE_EPSILON) return null

    val tangent = Offset(edgeX / edgeMagnitude, edgeY / edgeMagnitude)
    return CachedEdgeGeometry(
        center = midpoint(edgeStart, edgeEnd),
        tangent = tangent,
        normal = Offset(-tangent.y, tangent.x),
        edgeLength = edgeMagnitude,
        rotationDegrees = atan2(tangent.y, tangent.x) * RADIANS_TO_DEGREES,
    )
}

fun PerspectiveQuad.toImageSpace(sourceSize: CropSourceSize): PerspectiveQuad =
    toImageSpace(sourceSize.width.toFloat(), sourceSize.height.toFloat())

private fun PerspectiveQuad.toImageSpace(imageWidth: Float, imageHeight: Float): PerspectiveQuad {
    require(imageWidth > 0f) { "imageWidth must be > 0 but was $imageWidth" }
    require(imageHeight > 0f) { "imageHeight must be > 0 but was $imageHeight" }

    return PerspectiveQuad(
        topLeft = topLeft.toImageSpace(imageWidth, imageHeight),
        topRight = topRight.toImageSpace(imageWidth, imageHeight),
        bottomRight = bottomRight.toImageSpace(imageWidth, imageHeight),
        bottomLeft = bottomLeft.toImageSpace(imageWidth, imageHeight),
    )
}

private fun Offset.toImageSpace(imageWidth: Float, imageHeight: Float): Offset = Offset(
    x = x.coerceIn(0f, 1f) * imageWidth,
    y = y.coerceIn(0f, 1f) * imageHeight,
)
