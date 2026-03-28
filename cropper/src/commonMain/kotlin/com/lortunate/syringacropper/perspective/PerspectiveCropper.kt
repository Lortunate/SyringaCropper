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

private val CORNER_DRAW_ORDER = listOf(
    PerspectiveHandle.TOP_LEFT,
    PerspectiveHandle.TOP_RIGHT,
    PerspectiveHandle.BOTTOM_RIGHT,
    PerspectiveHandle.BOTTOM_LEFT,
)

private val EDGE_DRAW_ORDER = listOf(
    PerspectiveHandle.TOP_EDGE,
    PerspectiveHandle.RIGHT_EDGE,
    PerspectiveHandle.BOTTOM_EDGE,
    PerspectiveHandle.LEFT_EDGE,
)

private data class PerspectiveDrawMetrics(
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
    val edgeTouchLengthPx: Float,
    val edgeTouchThicknessPx: Float,
    val minEdgeLengthPx: Float,
)

@Composable
fun PerspectiveCropper(
    imageBitmap: ImageBitmap,
    state: PerspectiveCropState,
    modifier: Modifier = Modifier,
    style: PerspectiveStyle = PerspectiveStyle(),
    interaction: PerspectiveInteractionConfig = PerspectiveInteractionConfig(),
) {
    val (containerSize, updateContainerSize) = remember { mutableStateOf(Size.Zero) }
    val drawMetrics = rememberDrawMetrics(style, interaction)
    val edgeLengthFractionLimit = clampEdgeLengthFractionLimit(style.handle.edge.lengthFractionLimit)
    val gestureConfig = remember(drawMetrics, edgeLengthFractionLimit, style, interaction) {
        PerspectiveGestureConfig(
            cornerThresholdPx = drawMetrics.cornerTouchRadiusPx,
            edgeLengthPx = drawMetrics.edgeTouchLengthPx,
            edgeThicknessPx = drawMetrics.edgeTouchThicknessPx,
            edgeCornerRadiusPx = drawMetrics.edgeCornerRadiusPx,
            edgeLengthFractionLimit = edgeLengthFractionLimit,
            enableCornerHandles = style.handle.corner.visible,
            enableEdgeHandles = style.handle.edge.visible,
            enableQuadDrag = interaction.enableQuadDrag,
        )
    }

    val quadPath = remember { Path() }
    val maskPath = remember { Path() }

    val imageRect = remember(containerSize, imageBitmap.width, imageBitmap.height) {
        calculateImageRect(
            containerSize = containerSize,
            imageWidth = imageBitmap.width.toFloat(),
            imageHeight = imageBitmap.height.toFloat(),
        )
    }

    LaunchedEffect(drawMetrics.minEdgeLengthPx, interaction.constraintSolveSteps) {
        state.updateConstraints(
            minEdgeLengthPx = drawMetrics.minEdgeLengthPx,
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

@Composable
private fun rememberDrawMetrics(
    style: PerspectiveStyle,
    interaction: PerspectiveInteractionConfig,
): PerspectiveDrawMetrics {
    val density = LocalDensity.current
    return remember(style, interaction, density) {
        with(density) {
            PerspectiveDrawMetrics(
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
                edgeTouchLengthPx = style.handle.edge.touchLength.toPx(),
                edgeTouchThicknessPx = style.handle.edge.touchThickness.toPx(),
                minEdgeLengthPx = interaction.minEdgeLength.toPx(),
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
    style: PerspectiveStyle,
    drawMetrics: PerspectiveDrawMetrics,
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

    if (style.mask.drawOutsideQuad) {
        maskPath.reset()
        maskPath.fillType = PathFillType.EvenOdd
        maskPath.addRect(Rect(Offset.Zero, size))
        maskPath.addPath(quadPath)
        drawPath(path = maskPath, color = style.mask.color, style = Fill)
    }

    drawPath(path = quadPath, color = style.frame.color, style = Stroke(width = drawMetrics.frameStrokePx))

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
    handle: PerspectiveHandle,
    center: Offset,
    activeHandle: PerspectiveHandle?,
    cornerStyle: PerspectiveCornerHandleStyle,
    drawMetrics: PerspectiveDrawMetrics,
) {
    val isActive = activeHandle == handle
    drawCornerHandleMarker(
        center = center,
        shape = cornerStyle.shape,
        radius = drawMetrics.cornerRadiusPx,
        fillColor = if (isActive) cornerStyle.activeFillColor else cornerStyle.fillColor,
        strokeColor = if (isActive) cornerStyle.activeStrokeColor else cornerStyle.strokeColor,
        strokeWidth = drawMetrics.cornerStrokePx,
    )
}

private fun DrawScope.drawEdgeHandle(
    handle: PerspectiveHandle,
    geometry: CenteredEdgeBarGeometry,
    activeHandle: PerspectiveHandle?,
    edgeStyle: PerspectiveEdgeHandleStyle,
    drawMetrics: PerspectiveDrawMetrics,
) {
    val isActive = activeHandle == handle
    drawEdgeHandleMarker(
        geometry = geometry,
        cornerRadius = drawMetrics.edgeCornerRadiusPx,
        fillColor = if (isActive) edgeStyle.activeFillColor else edgeStyle.fillColor,
        strokeColor = if (isActive) edgeStyle.activeStrokeColor else edgeStyle.strokeColor,
        strokeWidth = drawMetrics.edgeStrokePx,
    )
}

private fun calculateImageRect(
    containerSize: Size,
    imageWidth: Float,
    imageHeight: Float,
): Rect {
    if (containerSize.width <= 0f || containerSize.height <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
        return Rect.Zero
    }

    val scale = minOf(containerSize.width / imageWidth, containerSize.height / imageHeight)
    val drawWidth = imageWidth * scale
    val drawHeight = imageHeight * scale
    val left = (containerSize.width - drawWidth) * 0.5f
    val top = (containerSize.height - drawHeight) * 0.5f
    return Rect(left, top, left + drawWidth, top + drawHeight)
}
