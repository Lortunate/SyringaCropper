package com.lortunate.syringacropper.perspective

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
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
import com.lortunate.syringacropper.CropperCornerHandleStyle
import com.lortunate.syringacropper.CropperDrawMetrics
import com.lortunate.syringacropper.CropperEdgeHandleGeometry
import com.lortunate.syringacropper.CropperEdgeHandleStyle
import com.lortunate.syringacropper.CropperHandle
import com.lortunate.syringacropper.CropperStyle
import com.lortunate.syringacropper.PerspectiveInteractionConfig
import com.lortunate.syringacropper.calculateImageFitRect
import com.lortunate.syringacropper.drawCropperCornerHandleMarker
import com.lortunate.syringacropper.drawCropperEdgeHandleMarker
import com.lortunate.syringacropper.rememberCropperDrawMetrics

private val CORNER_DRAW_ORDER = listOf(
    CropperHandle.TOP_LEFT,
    CropperHandle.TOP_RIGHT,
    CropperHandle.BOTTOM_RIGHT,
    CropperHandle.BOTTOM_LEFT,
)

private val EDGE_DRAW_ORDER = listOf(
    CropperHandle.TOP_EDGE,
    CropperHandle.RIGHT_EDGE,
    CropperHandle.BOTTOM_EDGE,
    CropperHandle.LEFT_EDGE,
)

@Composable
fun PerspectiveCropper(
    imageBitmap: ImageBitmap,
    state: PerspectiveCropState,
    modifier: Modifier = Modifier,
    style: CropperStyle = CropperStyle(),
    interaction: PerspectiveInteractionConfig = PerspectiveInteractionConfig(),
) {
    val (containerSize, updateContainerSize) = remember { mutableStateOf(Size.Zero) }
    val drawMetrics = rememberCropperDrawMetrics(style)
    val edgeLengthFractionLimit =
        clampEdgeLengthFractionLimit(style.handle.edge.lengthFractionLimit)

    val density = LocalDensity.current
    val gestureConfig =
        remember(drawMetrics, edgeLengthFractionLimit, style, interaction, density) {
            with(density) {
                PerspectiveGestureConfig(
                    cornerThresholdPx = style.handle.corner.touchRadius.toPx(),
                    edgeLengthPx = style.handle.edge.touchLength.toPx(),
                    edgeThicknessPx = style.handle.edge.touchThickness.toPx(),
                    edgeCornerRadiusPx = style.handle.edge.cornerRadius.toPx(),
                    edgeLengthFractionLimit = edgeLengthFractionLimit,
                    enableCornerHandles = style.handle.corner.visible,
                    enableEdgeHandles = style.handle.edge.visible,
                    enableQuadDrag = interaction.enableQuadDrag,
                )
            }
        }

    val quadPath = remember { Path() }
    val maskPath = remember { Path() }

    val imageRect = remember(containerSize, imageBitmap.width, imageBitmap.height) {
        calculateImageFitRect(
            containerSize = containerSize,
            imageWidth = imageBitmap.width.toFloat(),
            imageHeight = imageBitmap.height.toFloat(),
        )
    }

    val minEdgeLengthPx = with(density) { interaction.minEdgeLength.toPx() }

    LaunchedEffect(minEdgeLengthPx, interaction.constraintSolveSteps) {
        state.updateConstraints(
            minEdgeLengthPx = minEdgeLengthPx,
            constraintSolveSteps = interaction.constraintSolveSteps,
        )
    }

    LaunchedEffect(imageRect, interaction.defaultInsetFraction) {
        if (imageRect.isEmpty) return@LaunchedEffect
        state.updateImageRect(imageRect)
        state.ensureDefaultQuad(interaction.defaultInsetFraction)
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
                .perspectiveDragGestures(
                    state = state,
                    config = gestureConfig,
                ),
        ) {
            drawPerspectiveOverlay(
                state = state,
                imageRect = imageRect,
                style = style,
                drawMetrics = drawMetrics,
                edgeLengthFractionLimit = edgeLengthFractionLimit,
                quadPath = quadPath,
                maskPath = maskPath,
            )
        }
    }
}

private fun Modifier.perspectiveDragGestures(
    state: PerspectiveCropState,
    config: PerspectiveGestureConfig,
): Modifier = pointerInput(state, config) {
    detectDragGestures(
        onDragStart = { state.beginDrag(it, config) },
        onDragEnd = state::endDrag,
        onDragCancel = state::endDrag,
        onDrag = { change, dragAmount ->
            if (state.dragBy(change.position, dragAmount, config)) {
                change.consume()
            }
        },
    )
}

private fun DrawScope.drawPerspectiveOverlay(
    state: PerspectiveCropState,
    imageRect: Rect,
    style: CropperStyle,
    drawMetrics: CropperDrawMetrics,
    edgeLengthFractionLimit: Float,
    quadPath: Path,
    maskPath: Path,
) {
    if (imageRect.isEmpty) return

    val quad = state.quad ?: return
    val activeHandle = state.activeHandle

    quadPath.reset()
    quadPath.buildQuad(
        topLeft = quad.topLeft,
        topRight = quad.topRight,
        bottomRight = quad.bottomRight,
        bottomLeft = quad.bottomLeft,
    )

    if (style.mask.drawOutside) {
        maskPath.reset()
        maskPath.fillType = PathFillType.EvenOdd
        maskPath.addRect(Rect(Offset.Zero, size))
        maskPath.addPath(quadPath)
        drawPath(path = maskPath, color = style.mask.color, style = Fill)
    }

    drawPath(
        path = quadPath,
        color = style.frame.color,
        style = Stroke(width = drawMetrics.frameStrokePx)
    )

    if (style.frame.showGrid && style.frame.gridLineCount > 0) {
        drawPerspectiveGrid(
            topLeft = quad.topLeft,
            topRight = quad.topRight,
            bottomRight = quad.bottomRight,
            bottomLeft = quad.bottomLeft,
            lineCount = style.frame.gridLineCount,
            color = style.frame.gridColor,
            strokeWidth = drawMetrics.gridStrokePx,
        )
    }

    if (style.handle.corner.visible) {
        for (handle in CORNER_DRAW_ORDER) {
            drawCornerHandle(
                handle = handle,
                center = quad.cornerOffset(handle),
                activeHandle = activeHandle,
                cornerStyle = style.handle.corner,
                drawMetrics = drawMetrics,
            )
        }
    }

    if (style.handle.edge.visible) {
        for (handle in EDGE_DRAW_ORDER) {
            val geometry = state.edgeGeometry(
                handle = handle,
                edgeLengthPx = drawMetrics.edgeLengthPx,
                edgeThicknessPx = drawMetrics.edgeThicknessPx,
                edgeLengthFractionLimit = edgeLengthFractionLimit,
            ) ?: continue

            drawEdgeHandle(
                handle = handle,
                geometry = geometry,
                activeHandle = activeHandle,
                edgeStyle = style.handle.edge,
                drawMetrics = drawMetrics,
            )
        }
    }

    if (style.frame.showImageBounds) {
        drawRect(
            color = style.frame.imageBoundsColor,
            topLeft = imageRect.topLeft,
            size = imageRect.size,
            style = Stroke(width = drawMetrics.imageBoundsStrokePx),
        )
    }
}

private fun DrawScope.drawCornerHandle(
    handle: CropperHandle,
    center: Offset,
    activeHandle: CropperHandle?,
    cornerStyle: CropperCornerHandleStyle,
    drawMetrics: CropperDrawMetrics,
) {
    val isActive = activeHandle == handle
    drawCropperCornerHandleMarker(
        center = center,
        shape = cornerStyle.shape,
        radius = drawMetrics.cornerRadiusPx,
        fillColor = if (isActive) cornerStyle.activeFillColor else cornerStyle.fillColor,
        strokeColor = if (isActive) cornerStyle.activeStrokeColor else cornerStyle.strokeColor,
        strokeWidth = drawMetrics.cornerStrokePx,
    )
}

private fun DrawScope.drawEdgeHandle(
    handle: CropperHandle,
    geometry: CenteredEdgeBarGeometry,
    activeHandle: CropperHandle?,
    edgeStyle: CropperEdgeHandleStyle,
    drawMetrics: CropperDrawMetrics,
) {
    val isActive = activeHandle == handle
    drawCropperEdgeHandleMarker(
        geometry = CropperEdgeHandleGeometry(
            center = geometry.center,
            width = geometry.halfLength * 2f,
            height = geometry.halfThickness * 2f,
            rotationDegrees = geometry.rotationDegrees
        ),
        cornerRadius = drawMetrics.edgeCornerRadiusPx,
        fillColor = if (isActive) edgeStyle.activeFillColor else edgeStyle.fillColor,
        strokeColor = if (isActive) edgeStyle.activeStrokeColor else edgeStyle.strokeColor,
        strokeWidth = drawMetrics.edgeStrokePx,
    )
}
