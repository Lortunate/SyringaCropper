package com.lortunate.syringacropper.avatar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.lortunate.syringacropper.CropSourceSize

/** Snapshot of the current avatar selection in normalized and source-image pixel coordinates. */
data class AvatarCropSelection(
    val sourceSize: CropSourceSize,
    val shape: AvatarCropShape,
    val normalizedRect: Rect,
    val imageRect: Rect,
)

@Composable
fun rememberAvatarCropState(): AvatarCropState = remember { AvatarCropState() }

@Stable
class AvatarCropState {
    private var snapshot by mutableStateOf(AvatarCropSnapshot())

    val hasSelection: Boolean
        get() = !snapshot.selectionRect.isEmpty

    internal val selection: Rect
        get() = snapshot.selectionRect

    internal val currentImageRect: Rect
        get() = snapshot.currentImageRect

    internal val currentImageScale: Float
        get() = snapshot.imageScale

    internal val currentImageOffset: Offset
        get() = snapshot.imageOffset

    fun resetSelection(insetFraction: Float = DEFAULT_SELECTION_INSET_FRACTION) {
        val current = snapshot
        if (current.containerSize.isEmptySize()) return

        val safeInset = normalizedSelectionInsetFraction(insetFraction)
        applyReconciledSnapshot(
            current.copy(
                defaultInsetFraction = safeInset,
                selectionRect = defaultSelectionRect(current.containerSize, safeInset),
                imageScale = 1f,
                imageOffset = Offset.Zero,
            ),
        )
    }

    /** Returns the current selection using the original source-image pixel size. */
    fun selectionOrNull(
        shape: AvatarCropShape,
        sourceSize: CropSourceSize,
    ): AvatarCropSelection? {
        val current = snapshot
        val normalizedRect =
            normalizedRect(current.selectionRect, current.currentImageRect) ?: return null
        return AvatarCropSelection(
            sourceSize = sourceSize,
            shape = shape,
            normalizedRect = normalizedRect,
            imageRect = Rect(
                left = normalizedRect.left * sourceSize.width,
                top = normalizedRect.top * sourceSize.height,
                right = normalizedRect.right * sourceSize.width,
                bottom = normalizedRect.bottom * sourceSize.height,
            ),
        )
    }

    internal fun synchronize(
        containerSize: Size,
        imageWidth: Float,
        imageHeight: Float,
        defaultInsetFraction: Float,
        maxScale: Float,
    ) {
        val nextBaseImageRect = calculateBaseImageRect(containerSize, imageWidth, imageHeight)
        val safeInset = normalizedSelectionInsetFraction(defaultInsetFraction)
        val safeMaxScale = maxScale.coerceAtLeast(1f)
        val current = snapshot
        val viewportChanged = containerSize != current.containerSize ||
                nextBaseImageRect != current.baseImageRect ||
                safeInset != current.defaultInsetFraction ||
                current.selectionRect.isEmpty

        if (!viewportChanged && safeMaxScale == current.maxScale) return

        val nextSelectionRect = when {
            containerSize.isEmptySize() || nextBaseImageRect.isEmpty -> Rect.Zero
            viewportChanged -> defaultSelectionRect(containerSize, safeInset)
            else -> current.selectionRect
        }
        val nextOffset = if (viewportChanged) {
            scaleImageOffset(
                previousOffset = current.imageOffset,
                previousBaseImageRect = current.baseImageRect,
                nextBaseImageRect = nextBaseImageRect,
            )
        } else {
            current.imageOffset
        }
        applyReconciledSnapshot(
            current.copy(
                containerSize = containerSize,
                defaultInsetFraction = safeInset,
                baseImageRect = nextBaseImageRect,
                selectionRect = nextSelectionRect,
                imageScale = if (nextSelectionRect.isEmpty) 1f else current.imageScale,
                imageOffset = if (nextSelectionRect.isEmpty) Offset.Zero else nextOffset,
                maxScale = safeMaxScale,
            ),
        )
    }

    internal fun transformImage(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
    ): Boolean {
        val current = snapshot
        val nextTransform = transformImageWithGesture(
            containerSize = current.containerSize,
            selectionRect = current.selectionRect,
            baseImageRect = current.baseImageRect,
            currentScale = current.imageScale,
            currentOffset = current.imageOffset,
            maxScale = current.maxScale,
            centroid = centroid,
            pan = pan,
            zoom = zoom,
        ) ?: return false

        if (nextTransform.scale == current.imageScale &&
            nextTransform.offset == current.imageOffset &&
            nextTransform.imageRect == current.currentImageRect
        ) {
            return false
        }

        applySnapshot(
            current.copy(
                imageScale = nextTransform.scale,
                imageOffset = nextTransform.offset,
                currentImageRect = nextTransform.imageRect,
            ),
        )
        return true
    }

    private fun applySnapshot(nextSnapshot: AvatarCropSnapshot) {
        if (nextSnapshot != snapshot) {
            snapshot = nextSnapshot
        }
    }

    private fun applyReconciledSnapshot(nextSnapshot: AvatarCropSnapshot) {
        applySnapshot(nextSnapshot.reconcile())
    }

    private fun AvatarCropSnapshot.reconcile(): AvatarCropSnapshot {
        val nextTransform = coerceImageTransform(
            containerSize = containerSize,
            selectionRect = selectionRect,
            baseImageRect = baseImageRect,
            scale = imageScale,
            offset = imageOffset,
            maxScale = maxScale,
        )
        return copy(
            imageScale = nextTransform.scale,
            imageOffset = nextTransform.offset,
            currentImageRect = nextTransform.imageRect,
        )
    }

    private companion object {
        const val DEFAULT_SELECTION_INSET_FRACTION = 0f
    }
}

private data class AvatarCropSnapshot(
    val containerSize: Size = Size.Zero,
    val defaultInsetFraction: Float = 0f,
    val baseImageRect: Rect = Rect.Zero,
    val selectionRect: Rect = Rect.Zero,
    val imageScale: Float = 1f,
    val imageOffset: Offset = Offset.Zero,
    val currentImageRect: Rect = Rect.Zero,
    val maxScale: Float = 6f,
)
