package com.lortunate.syringacropper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Common style for the cropper mask (the semi-transparent overlay).
 */
@Immutable
data class CropperMaskStyle(
    val drawOutside: Boolean = true,
    val color: Color = cropperMaskColor,
)

/**
 * Common style for the cropper frame and grid lines.
 */
@Immutable
data class CropperFrameStyle(
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

/**
 * Style for corner handles.
 */
@Immutable
data class CropperCornerHandleStyle(
    val visible: Boolean = true,
    val shape: CropperHandleShape = CropperHandleShape.CIRCLE,
    val radius: Dp = 10.dp,
    val strokeWidth: Dp = 2.dp,
    val touchRadius: Dp = 36.dp,
    val fillColor: Color = cropperHandleFillColor,
    val strokeColor: Color = cropperHandleStrokeColor,
    val activeFillColor: Color = cropperHandleFillColor,
    val activeStrokeColor: Color = cropperHandleStrokeColor,
)

/**
 * Style for edge handles.
 */
@Immutable
data class CropperEdgeHandleStyle(
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

/**
 * Combined handle style.
 */
@Immutable
data class CropperHandleStyle(
    val corner: CropperCornerHandleStyle = CropperCornerHandleStyle(),
    val edge: CropperEdgeHandleStyle = CropperEdgeHandleStyle(),
)

@Immutable
data class CropperStyle(
    val frame: CropperFrameStyle = CropperFrameStyle(),
    val handle: CropperHandleStyle = CropperHandleStyle(),
    val mask: CropperMaskStyle = CropperMaskStyle(),
)

/**
 * Specialized style for avatar cropper (no handles).
 */
@Immutable
data class AvatarCropStyle(
    val frame: CropperFrameStyle = CropperFrameStyle(),
    val mask: CropperMaskStyle = CropperMaskStyle(),
)
