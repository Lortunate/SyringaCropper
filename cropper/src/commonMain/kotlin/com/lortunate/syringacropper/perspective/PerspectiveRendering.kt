package com.lortunate.syringacropper.perspective

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

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


internal fun lerp(start: Offset, end: Offset, t: Float): Offset = Offset(
    x = start.x + (end.x - start.x) * t,
    y = start.y + (end.y - start.y) * t,
)
