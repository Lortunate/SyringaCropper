package com.lortunate.syringacropper.perspective

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lortunate.syringacropper.cropperGridColor
import com.lortunate.syringacropper.cropperHandleFillColor
import com.lortunate.syringacropper.cropperHandleStrokeColor
import com.lortunate.syringacropper.cropperMaskColor
import com.lortunate.syringacropper.primaryColor

@Immutable
enum class PerspectiveHandleShape {
    CIRCLE,
    SQUARE,
}

@Immutable
data class PerspectiveMaskStyle(
    val drawOutsideQuad: Boolean = true,
    val color: Color = cropperMaskColor,
)

@Immutable
data class PerspectiveFrameStyle(
    val showGrid: Boolean = true,
    val gridLineCount: Int = 2,
    val showImageBounds: Boolean = false,
    val strokeWidth: Dp = 2.dp,
    val gridStrokeWidth: Dp = 1.dp,
    val imageBoundsStrokeWidth: Dp = 1.dp,
    val color: Color = primaryColor,
    val gridColor: Color = cropperGridColor,
    val imageBoundsColor: Color = primaryColor,
)

@Immutable
data class PerspectiveCornerHandleStyle(
    val visible: Boolean = true,
    val shape: PerspectiveHandleShape = PerspectiveHandleShape.CIRCLE,
    val radius: Dp = 10.dp,
    val strokeWidth: Dp = 2.dp,
    val touchRadius: Dp = 36.dp,
    val fillColor: Color = cropperHandleFillColor,
    val strokeColor: Color = cropperHandleStrokeColor,
    val activeFillColor: Color = cropperHandleFillColor,
    val activeStrokeColor: Color = cropperHandleStrokeColor,
)

@Immutable
data class PerspectiveEdgeHandleStyle(
    val visible: Boolean = true,
    val length: Dp = 40.dp,
    val thickness: Dp = 12.dp,
    val cornerRadius: Dp = 6.dp,
    val strokeWidth: Dp = 2.dp,
    val touchLength: Dp = 56.dp,
    val touchThickness: Dp = 36.dp,
    val lengthFractionLimit: Float = 0.85f,
    val fillColor: Color = cropperHandleFillColor,
    val strokeColor: Color = cropperHandleStrokeColor,
    val activeFillColor: Color = cropperHandleFillColor,
    val activeStrokeColor: Color = cropperHandleStrokeColor,
)

@Immutable
data class PerspectiveHandleStyle(
    val corner: PerspectiveCornerHandleStyle = PerspectiveCornerHandleStyle(),
    val edge: PerspectiveEdgeHandleStyle = PerspectiveEdgeHandleStyle(),
)

@Immutable
data class PerspectiveStyle(
    val frame: PerspectiveFrameStyle = PerspectiveFrameStyle(),
    val handle: PerspectiveHandleStyle = PerspectiveHandleStyle(),
    val mask: PerspectiveMaskStyle = PerspectiveMaskStyle(),
)
