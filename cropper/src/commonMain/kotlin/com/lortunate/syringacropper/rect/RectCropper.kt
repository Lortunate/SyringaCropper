package com.lortunate.syringacropper.rect

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lortunate.syringacropper.CropperDrawMetrics
import com.lortunate.syringacropper.CropperEdgeHandleGeometry
import com.lortunate.syringacropper.CropperHandle
import com.lortunate.syringacropper.CropperStyle
import com.lortunate.syringacropper.RectCropInteractionConfig
import com.lortunate.syringacropper.calculateImageFitRect
import com.lortunate.syringacropper.drawCropperCornerHandleMarker
import com.lortunate.syringacropper.drawCropperEdgeHandleMarker
import com.lortunate.syringacropper.rememberCropperDrawMetrics
import kotlin.math.min

private val CORNERS = arrayOf(
    CropperHandle.TOP_LEFT,
    CropperHandle.TOP_RIGHT,
    CropperHandle.BOTTOM_RIGHT,
    CropperHandle.BOTTOM_LEFT
)

private val EDGES = arrayOf(
    CropperHandle.TOP_EDGE,
    CropperHandle.RIGHT_EDGE,
    CropperHandle.BOTTOM_EDGE,
    CropperHandle.LEFT_EDGE
)

@Composable
fun RectCropper(
    imageBitmap: ImageBitmap,
    state: RectCropState,
    modifier: Modifier = Modifier,
    style: CropperStyle = CropperStyle(),
    interaction: RectCropInteractionConfig = RectCropInteractionConfig(),
) {
    val (containerSize, updateContainerSize) = remember { mutableStateOf(Size.Zero) }
    val drawMetrics = rememberCropperDrawMetrics(style)

    val density = LocalDensity.current
    val gestureConfig = remember(drawMetrics, style, interaction, density) {
        with(density) {
            RectCropGestureConfig(
                cornerThresholdPx = style.handle.corner.touchRadius.toPx(),
                edgeThicknessPx = style.handle.edge.touchThickness.toPx(),
                enableCornerHandles = style.handle.corner.visible,
                enableEdgeHandles = style.handle.edge.visible,
                enableRectDrag = interaction.enableRectDrag,
            )
        }
    }

    val maskPath = remember { Path() }

    val imageBounds = remember(containerSize, imageBitmap.width, imageBitmap.height) {
        calculateImageFitRect(
            containerSize = containerSize,
            imageWidth = imageBitmap.width.toFloat(),
            imageHeight = imageBitmap.height.toFloat(),
        )
    }

    val minRectSizePx = with(density) { interaction.minRectSize.toPx() }

    LaunchedEffect(minRectSizePx) {
        state.updateConstraints(
            minRectSizePx = minRectSizePx,
        )
    }

    LaunchedEffect(imageBounds, interaction.defaultInsetFraction) {
        if (imageBounds.isEmpty) return@LaunchedEffect
        state.updateImageBounds(imageBounds)
        state.ensureDefaultRect(interaction.defaultInsetFraction)
    }

    Box(
        modifier = modifier.onSizeChanged {
            updateContainerSize(Size(it.width.toFloat(), it.height.toFloat()))
        },
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state, gestureConfig) {
                    detectDragGestures(
                        onDragStart = { state.beginDrag(it, gestureConfig) },
                        onDragEnd = state::endDrag,
                        onDragCancel = state::endDrag,
                        onDrag = { change, dragAmount ->
                            if (state.dragBy(dragAmount)) {
                                change.consume()
                            }
                        }
                    )
                }
                .pointerInput(state, interaction.enableDoubleTapReset) {
                    if (interaction.enableDoubleTapReset) {
                        detectTapGestures(
                            onDoubleTap = { state.resetSelection(interaction.defaultInsetFraction) }
                        )
                    }
                },
        ) {
            drawRectOverlay(
                state = state,
                imageBounds = imageBounds,
                style = style,
                drawMetrics = drawMetrics,
                maskPath = maskPath,
            )
        }
    }
}

private fun DrawScope.drawRectOverlay(
    state: RectCropState,
    imageBounds: Rect,
    style: CropperStyle,
    drawMetrics: CropperDrawMetrics,
    maskPath: Path,
) {
    if (imageBounds.isEmpty) return
    val cropRect = state.cropRect ?: return
    val activeHandle = state.activeHandle

    if (style.mask.drawOutside) {
        maskPath.reset()
        maskPath.fillType = PathFillType.EvenOdd
        maskPath.addRect(Rect(Offset.Zero, size))
        maskPath.addRect(cropRect)
        drawPath(path = maskPath, color = style.mask.color, style = Fill)
    }

    if (style.frame.strokeWidth > 0.dp) {
        drawRect(
            color = style.frame.color,
            topLeft = cropRect.topLeft,
            size = cropRect.size,
            style = Stroke(width = drawMetrics.frameStrokePx)
        )
    }

    if (style.frame.showGrid && style.frame.gridLineCount > 0) {
        val stepX = cropRect.width / (style.frame.gridLineCount + 1)
        val stepY = cropRect.height / (style.frame.gridLineCount + 1)
        for (i in 1..style.frame.gridLineCount) {
            val x = cropRect.left + stepX * i
            val y = cropRect.top + stepY * i
            drawLine(
                color = style.frame.gridColor,
                start = Offset(x, cropRect.top),
                end = Offset(x, cropRect.bottom),
                strokeWidth = drawMetrics.gridStrokePx
            )
            drawLine(
                color = style.frame.gridColor,
                start = Offset(cropRect.left, y),
                end = Offset(cropRect.right, y),
                strokeWidth = drawMetrics.gridStrokePx
            )
        }
    }

    if (style.handle.corner.visible) {
        for (handle in CORNERS) {
            val center = when (handle) {
                CropperHandle.TOP_LEFT -> cropRect.topLeft
                CropperHandle.TOP_RIGHT -> cropRect.topRight
                CropperHandle.BOTTOM_RIGHT -> cropRect.bottomRight
                else -> cropRect.bottomLeft
            }

            val isActive = handle == activeHandle
            val cornerStyle = style.handle.corner
            val fill = if (isActive) cornerStyle.activeFillColor else cornerStyle.fillColor
            val stroke = if (isActive) cornerStyle.activeStrokeColor else cornerStyle.strokeColor
            val radius = drawMetrics.cornerRadiusPx

            drawCropperCornerHandleMarker(
                center = center,
                shape = cornerStyle.shape,
                radius = radius,
                fillColor = fill,
                strokeColor = stroke,
                strokeWidth = drawMetrics.cornerStrokePx,
            )
        }
    }

    if (style.handle.edge.visible) {
        for (handle in EDGES) {
            val isActive = handle == activeHandle
            val edgeStyle = style.handle.edge
            val fill = if (isActive) edgeStyle.activeFillColor else edgeStyle.fillColor
            val stroke = if (isActive) edgeStyle.activeStrokeColor else edgeStyle.strokeColor
            val len = drawMetrics.edgeLengthPx
            val thick = drawMetrics.edgeThicknessPx

            val isHorizontal =
                handle == CropperHandle.TOP_EDGE || handle == CropperHandle.BOTTOM_EDGE
            val actualLen = min(
                len,
                if (isHorizontal) cropRect.width * edgeStyle.lengthFractionLimit else cropRect.height * edgeStyle.lengthFractionLimit
            )

            val w = if (isHorizontal) actualLen else thick
            val h = if (isHorizontal) thick else actualLen

            val cx = when (handle) {
                CropperHandle.LEFT_EDGE -> cropRect.left
                CropperHandle.RIGHT_EDGE -> cropRect.right
                else -> cropRect.center.x
            }
            val cy = when (handle) {
                CropperHandle.TOP_EDGE -> cropRect.top
                CropperHandle.BOTTOM_EDGE -> cropRect.bottom
                else -> cropRect.center.y
            }

            drawCropperEdgeHandleMarker(
                geometry = CropperEdgeHandleGeometry(
                    center = Offset(cx, cy),
                    width = w,
                    height = h,
                ),
                cornerRadius = drawMetrics.edgeCornerRadiusPx,
                fillColor = fill,
                strokeColor = stroke,
                strokeWidth = drawMetrics.edgeStrokePx,
            )
        }
    }

    if (style.frame.showImageBounds) {
        drawRect(
            color = style.frame.imageBoundsColor,
            topLeft = imageBounds.topLeft,
            size = imageBounds.size,
            style = Stroke(width = drawMetrics.imageBoundsStrokePx),
        )
    }
}
