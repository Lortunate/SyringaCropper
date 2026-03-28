package com.lortunate.syringacropper.perspective

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

internal fun Path.buildQuad(
    topLeft: Offset,
    topRight: Offset,
    bottomRight: Offset,
    bottomLeft: Offset,
) {
    moveTo(topLeft.x, topLeft.y)
    lineTo(topRight.x, topRight.y)
    lineTo(bottomRight.x, bottomRight.y)
    lineTo(bottomLeft.x, bottomLeft.y)
    close()
}

internal fun DrawScope.drawPerspectiveGrid(
    topLeft: Offset,
    topRight: Offset,
    bottomRight: Offset,
    bottomLeft: Offset,
    lineCount: Int,
    color: Color,
    strokeWidth: Float,
) {
    val step = 1f / (lineCount + 1).toFloat()
    for (index in 1..lineCount) {
        val t = index * step

        val left = lerp(topLeft, bottomLeft, t)
        val right = lerp(topRight, bottomRight, t)
        drawLine(color = color, start = left, end = right, strokeWidth = strokeWidth)

        val top = lerp(topLeft, topRight, t)
        val bottom = lerp(bottomLeft, bottomRight, t)
        drawLine(color = color, start = top, end = bottom, strokeWidth = strokeWidth)
    }
}

internal fun DrawScope.drawCornerHandleMarker(
    center: Offset,
    shape: PerspectiveHandleShape,
    radius: Float,
    fillColor: Color,
    strokeColor: Color,
    strokeWidth: Float,
) {
    when (shape) {
        PerspectiveHandleShape.CIRCLE -> {
            drawCircle(color = fillColor, radius = radius, center = center, style = Fill)
            if (strokeWidth > 0f) {
                drawCircle(color = strokeColor, radius = radius, center = center, style = Stroke(width = strokeWidth))
            }
        }

        PerspectiveHandleShape.SQUARE -> {
            val markerSize = Size(radius * 2f, radius * 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            drawRect(color = fillColor, topLeft = topLeft, size = markerSize, style = Fill)
            if (strokeWidth > 0f) {
                drawRect(color = strokeColor, topLeft = topLeft, size = markerSize, style = Stroke(width = strokeWidth))
            }
        }
    }
}

internal fun DrawScope.drawEdgeHandleMarker(
    geometry: CenteredEdgeBarGeometry,
    cornerRadius: Float,
    fillColor: Color,
    strokeColor: Color,
    strokeWidth: Float,
) {
    val markerSize = Size(
        width = geometry.halfLength * 2f,
        height = geometry.halfThickness * 2f,
    )
    val markerTopLeft = Offset(
        x = geometry.center.x - geometry.halfLength,
        y = geometry.center.y - geometry.halfThickness,
    )
    val maxRadius = minOf(geometry.halfLength, geometry.halfThickness)
    val clampedRadius = cornerRadius.coerceIn(0f, maxRadius)
    val markerCornerRadius = CornerRadius(clampedRadius, clampedRadius)

    rotate(degrees = geometry.rotationDegrees, pivot = geometry.center) {
        drawRoundRect(
            color = fillColor,
            topLeft = markerTopLeft,
            size = markerSize,
            cornerRadius = markerCornerRadius,
            style = Fill,
        )
        if (strokeWidth > 0f) {
            drawRoundRect(
                color = strokeColor,
                topLeft = markerTopLeft,
                size = markerSize,
                cornerRadius = markerCornerRadius,
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

internal fun lerp(start: Offset, end: Offset, t: Float): Offset = Offset(
    x = start.x + (end.x - start.x) * t,
    y = start.y + (end.y - start.y) * t,
)
