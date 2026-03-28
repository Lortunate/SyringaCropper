package com.lortunate.syringacropper.avatar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity

private data class AvatarDrawMetrics(
    val frameStroke: Stroke,
    val gridStroke: Stroke,
    val imageBoundsStroke: Stroke,
)

private data class AvatarOverlayGeometry(
    val selectionPath: Path,
    val maskPath: Path?,
    val gridPath: Path?,
    val clipGrid: Boolean,
)

@Composable
fun AvatarCropper(
    imageBitmap: ImageBitmap,
    state: AvatarCropState,
    modifier: Modifier = Modifier,
    shape: AvatarCropShape = AvatarCropShape.CIRCLE,
    style: AvatarCropStyle = AvatarCropStyle(),
    interaction: AvatarInteractionConfig = AvatarInteractionConfig(),
) {
    var containerSize by remember { mutableStateOf(Size.Zero) }
    val selectionRect = state.selection
    val imageRect = state.currentImageRect
    val imageScale = state.currentImageScale
    val imageOffset = state.currentImageOffset
    val drawMetrics = rememberDrawMetrics(style)
    val overlayGeometry = rememberAvatarOverlayGeometry(
        containerSize = containerSize,
        selectionRect = selectionRect,
        shape = shape,
        frame = style.frame,
        drawMask = style.mask.drawOutsideSelection,
    )

    LaunchedEffect(
        state,
        containerSize,
        imageBitmap.width,
        imageBitmap.height,
        interaction.defaultInsetFraction,
        interaction.maxScale,
    ) {
        state.synchronize(
            containerSize = containerSize,
            imageWidth = imageBitmap.width.toFloat(),
            imageHeight = imageBitmap.height.toFloat(),
            defaultInsetFraction = interaction.defaultInsetFraction,
            maxScale = interaction.maxScale,
        )
    }

    Box(
        modifier = modifier.onSizeChanged {
            val nextSize = Size(it.width.toFloat(), it.height.toFloat())
            if (nextSize != containerSize) {
                containerSize = nextSize
            }
        },
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = imageOffset.x
                    translationY = imageOffset.y
                    scaleX = imageScale
                    scaleY = imageScale
                    transformOrigin = TransformOrigin.Center
                },
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        state.transformImage(
                            centroid = centroid,
                            pan = pan,
                            zoom = zoom,
                        )
                    }
                },
        ) {
            drawAvatarOverlay(
                imageRect = imageRect,
                style = style,
                drawMetrics = drawMetrics,
                overlayGeometry = overlayGeometry,
            )
        }
    }
}

@Composable
private fun rememberDrawMetrics(style: AvatarCropStyle): AvatarDrawMetrics {
    val density = LocalDensity.current
    return remember(
        style.frame.strokeWidth,
        style.frame.gridStrokeWidth,
        style.frame.imageBoundsStrokeWidth,
        density,
    ) {
        with(density) {
            AvatarDrawMetrics(
                frameStroke = Stroke(width = style.frame.strokeWidth.toPx()),
                gridStroke = Stroke(width = style.frame.gridStrokeWidth.toPx()),
                imageBoundsStroke = Stroke(width = style.frame.imageBoundsStrokeWidth.toPx()),
            )
        }
    }
}

@Composable
private fun rememberAvatarOverlayGeometry(
    containerSize: Size,
    selectionRect: Rect,
    shape: AvatarCropShape,
    frame: AvatarFrameStyle,
    drawMask: Boolean,
): AvatarOverlayGeometry? {
    val gridLineCount = frame.gridLineCount.coerceAtLeast(0)
    val showGrid = frame.showGrid && gridLineCount > 0
    return remember(containerSize, selectionRect, shape, drawMask, showGrid, gridLineCount) {
        if (containerSize.isEmptySize() || selectionRect.isEmpty) return@remember null

        val selectionPath = Path().apply {
            when (shape) {
                AvatarCropShape.CIRCLE -> addOval(selectionRect)
                AvatarCropShape.SQUARE -> addRect(selectionRect)
            }
        }
        val maskPath = if (drawMask) {
            Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, containerSize))
                addPath(selectionPath)
            }
        } else {
            null
        }
        val gridPath = if (showGrid) {
            Path().apply {
                val cellWidth = selectionRect.width / (gridLineCount + 1)
                val cellHeight = selectionRect.height / (gridLineCount + 1)
                repeat(gridLineCount) { index ->
                    val step = index + 1
                    val x = selectionRect.left + cellWidth * step
                    val y = selectionRect.top + cellHeight * step
                    moveTo(x, selectionRect.top)
                    lineTo(x, selectionRect.bottom)
                    moveTo(selectionRect.left, y)
                    lineTo(selectionRect.right, y)
                }
            }
        } else {
            null
        }

        AvatarOverlayGeometry(
            selectionPath = selectionPath,
            maskPath = maskPath,
            gridPath = gridPath,
            clipGrid = shape == AvatarCropShape.CIRCLE,
        )
    }
}

private fun DrawScope.drawAvatarOverlay(
    imageRect: Rect,
    style: AvatarCropStyle,
    drawMetrics: AvatarDrawMetrics,
    overlayGeometry: AvatarOverlayGeometry?,
) {
    if (overlayGeometry == null) return

    overlayGeometry.maskPath?.let {
        drawPath(it, color = style.mask.color, style = Fill)
    }

    if (style.frame.showFrame) {
        drawPath(
            path = overlayGeometry.selectionPath,
            color = style.frame.color,
            style = drawMetrics.frameStroke,
        )
    }

    overlayGeometry.gridPath?.let { gridPath ->
        if (overlayGeometry.clipGrid) {
            clipPath(overlayGeometry.selectionPath) {
                drawPath(
                    path = gridPath,
                    color = style.frame.gridColor,
                    style = drawMetrics.gridStroke,
                )
            }
        } else {
            drawPath(
                path = gridPath,
                color = style.frame.gridColor,
                style = drawMetrics.gridStroke,
            )
        }
    }

    if (style.frame.showImageBounds) {
        drawRect(
            color = style.frame.imageBoundsColor,
            topLeft = imageRect.topLeft,
            size = imageRect.size,
            style = drawMetrics.imageBoundsStroke,
        )
    }
}
