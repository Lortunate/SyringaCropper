package com.lortunate.syringacropper.rect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.lortunate.syringacropper.CropSourceSize
import com.lortunate.syringacropper.CropperHandle
import com.lortunate.syringacropper.CropperRectSelection
import com.lortunate.syringacropper.common.denormalizeTo
import com.lortunate.syringacropper.common.toRectSelection
import com.lortunate.syringacropper.coerceInside
import com.lortunate.syringacropper.remapRect
import com.lortunate.syringacropper.toNormalizedRect

@Composable
fun rememberRectCropState(
    initialAspectRatio: RectCropAspectRatio = RectCropAspectRatio.Unconstrained
): RectCropState = remember {
    RectCropState().apply {
        aspectRatio = initialAspectRatio
    }
}

@Stable
class RectCropState {
    var cropRect: Rect? by mutableStateOf(null)
        internal set

    var imageBounds: Rect by mutableStateOf(Rect.Zero)
        internal set

    var aspectRatio: RectCropAspectRatio by mutableStateOf(RectCropAspectRatio.Unconstrained)
        internal set

    private var minRectSizePx: Float = 64f

    internal var activeHandle: CropperHandle? by mutableStateOf(null)
    internal var isDraggingRect: Boolean by mutableStateOf(false)

    val hasSelection: Boolean
        get() = cropRect != null

    fun clearSelection() {
        activeHandle = null
        isDraggingRect = false
        cropRect = null
    }

    fun resetSelection(insetFraction: Float = 0f) {
        clearSelection()
        val rect = RectCropGeometry.defaultRect(imageBounds, insetFraction, aspectRatio)
        if (rect != null) {
            cropRect = rect
        }
    }

    /**
     * Programmatically set the crop rect using normalized coordinates (0..1).
     * Useful for restoring a previous crop session.
     */
    fun restoreSelection(normalizedRect: Rect) {
        clearSelection()
        if (imageBounds.isEmpty) return

        val actualRect = normalizedRect.denormalizeTo(imageBounds)
        cropRect = actualRect.coerceInside(imageBounds, minRectSizePx)
    }

    fun setCropAspectRatio(ratio: RectCropAspectRatio) {
        if (this.aspectRatio == ratio) return
        this.aspectRatio = ratio
        cropRect = cropRect?.let { currentRect ->
            RectCropGeometry.coerceAspectRatio(currentRect, ratio, imageBounds)
        } ?: RectCropGeometry.defaultRect(imageBounds, 0.1f, ratio)
    }

    fun normalizedRectOrNull(): Rect? {
        return cropRect?.toNormalizedRect(imageBounds)
    }

    fun selectionOrNull(sourceSize: CropSourceSize): CropperRectSelection? {
        val norm = normalizedRectOrNull() ?: return null
        return norm.toRectSelection(sourceSize)
    }

    internal fun updateConstraints(minRectSizePx: Float) {
        this.minRectSizePx = minRectSizePx.coerceAtLeast(1f)
        coerceInsideImageBounds()
    }

    internal fun updateImageBounds(bounds: Rect) {
        if (bounds == imageBounds || bounds.isEmpty) return

        val previousBounds = imageBounds
        imageBounds = bounds

        cropRect = cropRect?.let { currentRect ->
            if (previousBounds.isEmpty) currentRect
            else currentRect.remapRect(previousBounds, bounds)
        }
        coerceInsideImageBounds()
    }

    internal fun ensureDefaultRect(insetFraction: Float = 0f) {
        if (cropRect != null) return
        cropRect = RectCropGeometry.defaultRect(imageBounds, insetFraction, aspectRatio)
    }

    internal fun beginDrag(point: Offset, config: RectCropGestureConfig) {
        activeHandle = RectCropGeometry.findHandle(cropRect, point, config)
        isDraggingRect = activeHandle == null && config.enableRectDrag && cropRect?.let {
            point.x >= it.left && point.x <= it.right && point.y >= it.top && point.y <= it.bottom
        } == true
    }

    internal fun dragBy(delta: Offset): Boolean {
        val currentRect = cropRect ?: return false
        val handle = activeHandle

        if (handle != null) {
            val candidate = RectCropGeometry.dragHandleBy(
                rect = currentRect,
                imageBounds = imageBounds,
                handle = handle,
                delta = delta,
                minRectSizePx = minRectSizePx,
                aspectRatio = aspectRatio
            )
            if (candidate != currentRect) {
                cropRect = candidate
                return true
            }
        } else if (isDraggingRect) {
            val candidate = RectCropGeometry.translate(
                rect = currentRect,
                delta = delta,
                imageBounds = imageBounds
            )
            if (candidate != null) {
                cropRect = candidate
                return true
            }
        }
        return false
    }

    internal fun endDrag() {
        activeHandle = null
        isDraggingRect = false
    }

    private fun coerceInsideImageBounds() {
        cropRect = cropRect?.coerceInside(imageBounds, minRectSizePx)
    }
}

internal data class RectCropGestureConfig(
    val cornerThresholdPx: Float,
    val edgeThicknessPx: Float,
    val enableCornerHandles: Boolean,
    val enableEdgeHandles: Boolean,
    val enableRectDrag: Boolean,
)
