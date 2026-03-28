package com.lortunate.syringacropper.perspective

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.lortunate.syringacropper.CropSourceSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class PerspectiveHandle {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    TOP_EDGE,
    RIGHT_EDGE,
    BOTTOM_EDGE,
    LEFT_EDGE,
}

/** UI-space quad used by the cropper and by exported crop selections. */
data class PerspectiveQuad(
    val topLeft: Offset,
    val topRight: Offset,
    val bottomRight: Offset,
    val bottomLeft: Offset,
)

/** Snapshot of the current selection in both normalized and source-image pixel coordinates. */
data class PerspectiveCropSelection(
    val imageQuad: PerspectiveQuad,
    val normalizedQuad: PerspectiveQuad,
    val sourceSize: CropSourceSize,
)

/** Creates and remembers a [PerspectiveCropState] for a single cropper session. */
@Composable
fun rememberPerspectiveCropState(): PerspectiveCropState = remember { PerspectiveCropState() }

@Stable
class PerspectiveCropState {
    private val cropState = PerspectiveQuadState()
    private val handleInteractor = PerspectiveHandleInteractor()

    private var dragState: PerspectiveDragState by mutableStateOf(PerspectiveDragState())
    private var minEdgeLengthPx: Float = 32f
    private var constraintSolveSteps: Int = 3

    val hasSelection: Boolean
        get() = cropState.quad != null

    internal val quad: PerspectiveQuad?
        get() = cropState.quad

    internal val imageRect: Rect
        get() = cropState.imageRect

    internal val activeHandle: PerspectiveHandle?
        get() = dragState.activeHandle

    /** Clears the current selection and any in-progress drag state. */
    fun clearSelection() {
        dragState = PerspectiveDragState()
        cropState.quad = null
        handleInteractor.updateQuad(null)
    }

    /** Recreates the default selection inside the current image rect. */
    fun resetSelection(insetFraction: Float = 0f) {
        clearSelection()
        PerspectiveQuadConstraints.defaultQuad(cropState.imageRect, insetFraction)
            ?.let(::commitQuad)
    }

    /** Returns the current selection normalized to the original image in the 0..1 range. */
    fun normalizedQuadOrNull(): PerspectiveQuad? {
        return cropState.quad?.let {
            PerspectiveQuadConstraints.toNormalized(
                quad = it,
                imageRect = cropState.imageRect,
            )
        }
    }

    /** Returns the current selection in original source-image pixel coordinates. */
    fun imageQuadOrNull(sourceSize: CropSourceSize): PerspectiveQuad? {
        return normalizedQuadOrNull()?.toImageSpace(sourceSize)
    }

    /** Returns the recommended selection snapshot using the original source-image pixel size. */
    fun selectionOrNull(sourceSize: CropSourceSize): PerspectiveCropSelection? {
        val normalizedQuad = normalizedQuadOrNull() ?: return null
        return PerspectiveCropSelection(
            imageQuad = normalizedQuad.toImageSpace(sourceSize),
            normalizedQuad = normalizedQuad,
            sourceSize = sourceSize,
        )
    }

    internal fun updateConstraints(minEdgeLengthPx: Float, constraintSolveSteps: Int) {
        this.minEdgeLengthPx = minEdgeLengthPx.coerceAtLeast(1f)
        this.constraintSolveSteps = constraintSolveSteps.coerceIn(1, 6)
        coerceQuadInsideImageRect()
    }

    internal fun updateImageRect(rect: Rect) {
        if (rect == cropState.imageRect || rect.isEmpty) return

        val previousRect = cropState.imageRect
        cropState.imageRect = rect

        cropState.quad
            ?.let { currentQuad ->
                if (previousRect.isEmpty) currentQuad
                else PerspectiveQuadConstraints.remap(currentQuad, previousRect, rect)
            }
            ?.let(::commitQuad)
        coerceQuadInsideImageRect()
    }

    internal fun ensureDefaultQuad(insetFraction: Float = 0f) {
        if (cropState.quad != null) return

        PerspectiveQuadConstraints.defaultQuad(cropState.imageRect, insetFraction)
            ?.let(::commitQuad)
    }

    internal fun dragHandleBy(handle: PerspectiveHandle, delta: Offset): Boolean {
        val candidate = handleInteractor.dragHandleBy(
            quad = cropState.quad,
            imageRect = cropState.imageRect,
            handle = handle,
            delta = delta,
            minEdgeLengthPx = minEdgeLengthPx,
            constraintSolveSteps = constraintSolveSteps,
        ) ?: return false
        commitQuad(candidate)
        return true
    }

    internal fun containsPointInQuad(point: Offset): Boolean {
        return cropState.quad?.let { PerspectiveQuadConstraints.contains(it, point) } == true
    }

    internal fun dragQuadBy(delta: Offset): Boolean {
        val currentQuad = cropState.quad ?: return false
        val candidate = PerspectiveQuadConstraints.translate(
            quad = currentQuad,
            delta = delta,
            imageRect = cropState.imageRect,
        ) ?: return false
        commitQuad(candidate)
        return true
    }

    internal fun beginDrag(point: Offset, config: PerspectiveGestureConfig) {
        val handle = handleInteractor.findHandle(
            quad = cropState.quad,
            point = point,
            config = config,
        )
        dragState = PerspectiveDragState(
            activeHandle = handle,
            draggingQuad = handle == null && config.enableQuadDrag && containsPointInQuad(point),
            retryHandleOnFirstDrag = handle == null,
        )
    }

    internal fun dragBy(
        point: Offset,
        delta: Offset,
        config: PerspectiveGestureConfig,
    ): Boolean {
        var nextDragState = dragState
        var handle = nextDragState.activeHandle
        if (handle == null && nextDragState.retryHandleOnFirstDrag) {
            handle = handleInteractor.findHandle(
                quad = cropState.quad,
                point = point,
                config = config,
            )
            nextDragState = nextDragState.copy(
                activeHandle = handle,
                retryHandleOnFirstDrag = false,
            )
        }

        val didDrag = when {
            handle != null -> dragHandleBy(handle, delta)
            nextDragState.draggingQuad -> dragQuadBy(delta)
            else -> false
        }
        dragState = nextDragState
        return didDrag
    }

    internal fun endDrag() {
        dragState = PerspectiveDragState()
    }

    internal fun edgeGeometry(
        handle: PerspectiveHandle,
        edgeLengthPx: Float,
        edgeThicknessPx: Float,
        edgeLengthFractionLimit: Float,
    ): CenteredEdgeBarGeometry? = handleInteractor.edgeGeometry(
        handle = handle,
        edgeLengthPx = edgeLengthPx,
        edgeThicknessPx = edgeThicknessPx,
        edgeLengthFractionLimit = edgeLengthFractionLimit,
    )

    private fun coerceQuadInsideImageRect() {
        cropState.quad
            ?.let {
                PerspectiveQuadConstraints.coerceInsideImageRect(
                    quad = it,
                    imageRect = cropState.imageRect,
                    minEdgeLengthPx = minEdgeLengthPx,
                )
            }
            ?.let(::commitQuad)
    }

    private fun commitQuad(quad: PerspectiveQuad) {
        cropState.quad = quad
        handleInteractor.updateQuad(quad)
    }
}

private data class PerspectiveDragState(
    val activeHandle: PerspectiveHandle? = null,
    val draggingQuad: Boolean = false,
    val retryHandleOnFirstDrag: Boolean = false,
)

internal data class PerspectiveGestureConfig(
    val cornerThresholdPx: Float,
    val edgeLengthPx: Float,
    val edgeThicknessPx: Float,
    val edgeCornerRadiusPx: Float,
    val edgeLengthFractionLimit: Float,
    val enableCornerHandles: Boolean,
    val enableEdgeHandles: Boolean,
    val enableQuadDrag: Boolean,
)

private class PerspectiveQuadState {
    var imageRect: Rect by mutableStateOf(Rect.Zero)
    var quad: PerspectiveQuad? by mutableStateOf(null)
}

private object PerspectiveQuadConstraints {
    fun defaultQuad(imageRect: Rect, insetFraction: Float): PerspectiveQuad? {
        if (imageRect.isEmpty) return null

        val safeInset = insetFraction.coerceIn(0f, 0.45f)
        val insetX = imageRect.width * safeInset
        val insetY = imageRect.height * safeInset
        return PerspectiveQuad(
            topLeft = Offset(imageRect.left + insetX, imageRect.top + insetY),
            topRight = Offset(imageRect.right - insetX, imageRect.top + insetY),
            bottomRight = Offset(imageRect.right - insetX, imageRect.bottom - insetY),
            bottomLeft = Offset(imageRect.left + insetX, imageRect.bottom - insetY),
        )
    }

    fun remap(quad: PerspectiveQuad, source: Rect, target: Rect): PerspectiveQuad = PerspectiveQuad(
        topLeft = quad.topLeft.remap(source, target),
        topRight = quad.topRight.remap(source, target),
        bottomRight = quad.bottomRight.remap(source, target),
        bottomLeft = quad.bottomLeft.remap(source, target),
    )

    fun coerceInsideImageRect(
        quad: PerspectiveQuad,
        imageRect: Rect,
        minEdgeLengthPx: Float,
    ): PerspectiveQuad? {
        if (imageRect.isEmpty) return null

        val clamped = PerspectiveQuad(
            topLeft = quad.topLeft.coerceTo(imageRect),
            topRight = quad.topRight.coerceTo(imageRect),
            bottomRight = quad.bottomRight.coerceTo(imageRect),
            bottomLeft = quad.bottomLeft.coerceTo(imageRect),
        )
        return clamped.takeIf { isValid(it, minEdgeLengthPx) }
    }

    fun translate(
        quad: PerspectiveQuad,
        delta: Offset,
        imageRect: Rect,
    ): PerspectiveQuad? {
        if (imageRect.isEmpty) return null
        if (abs(delta.x) <= PERSPECTIVE_EPSILON && abs(delta.y) <= PERSPECTIVE_EPSILON) return null

        var minDx = Float.NEGATIVE_INFINITY
        var maxDx = Float.POSITIVE_INFINITY
        var minDy = Float.NEGATIVE_INFINITY
        var maxDy = Float.POSITIVE_INFINITY

        minDx = max(minDx, imageRect.left - quad.topLeft.x)
        maxDx = min(maxDx, imageRect.right - quad.topLeft.x)
        minDy = max(minDy, imageRect.top - quad.topLeft.y)
        maxDy = min(maxDy, imageRect.bottom - quad.topLeft.y)

        minDx = max(minDx, imageRect.left - quad.topRight.x)
        maxDx = min(maxDx, imageRect.right - quad.topRight.x)
        minDy = max(minDy, imageRect.top - quad.topRight.y)
        maxDy = min(maxDy, imageRect.bottom - quad.topRight.y)

        minDx = max(minDx, imageRect.left - quad.bottomRight.x)
        maxDx = min(maxDx, imageRect.right - quad.bottomRight.x)
        minDy = max(minDy, imageRect.top - quad.bottomRight.y)
        maxDy = min(maxDy, imageRect.bottom - quad.bottomRight.y)

        minDx = max(minDx, imageRect.left - quad.bottomLeft.x)
        maxDx = min(maxDx, imageRect.right - quad.bottomLeft.x)
        minDy = max(minDy, imageRect.top - quad.bottomLeft.y)
        maxDy = min(maxDy, imageRect.bottom - quad.bottomLeft.y)

        if (minDx > maxDx || minDy > maxDy) return null

        val clampedDelta = Offset(
            x = delta.x.coerceIn(minDx, maxDx),
            y = delta.y.coerceIn(minDy, maxDy),
        )
        if (abs(clampedDelta.x) <= PERSPECTIVE_EPSILON && abs(clampedDelta.y) <= PERSPECTIVE_EPSILON) {
            return null
        }

        return quad.translateBy(clampedDelta)
    }

    fun contains(quad: PerspectiveQuad, point: Offset): Boolean {
        val cross1 = cross(quad.topLeft, quad.topRight, point)
        val cross2 = cross(quad.topRight, quad.bottomRight, point)
        val cross3 = cross(quad.bottomRight, quad.bottomLeft, point)
        val cross4 = cross(quad.bottomLeft, quad.topLeft, point)

        val allNonNegative = cross1 >= -PERSPECTIVE_EPSILON &&
                cross2 >= -PERSPECTIVE_EPSILON &&
                cross3 >= -PERSPECTIVE_EPSILON &&
                cross4 >= -PERSPECTIVE_EPSILON
        val allNonPositive = cross1 <= PERSPECTIVE_EPSILON &&
                cross2 <= PERSPECTIVE_EPSILON &&
                cross3 <= PERSPECTIVE_EPSILON &&
                cross4 <= PERSPECTIVE_EPSILON
        return allNonNegative || allNonPositive
    }

    fun toNormalized(
        quad: PerspectiveQuad,
        imageRect: Rect,
    ): PerspectiveQuad? {
        if (imageRect.isEmpty) return null

        return PerspectiveQuad(
            topLeft = quad.topLeft.toNormalized(imageRect),
            topRight = quad.topRight.toNormalized(imageRect),
            bottomRight = quad.bottomRight.toNormalized(imageRect),
            bottomLeft = quad.bottomLeft.toNormalized(imageRect),
        )
    }

    fun isValid(quad: PerspectiveQuad, minEdgeLengthPx: Float): Boolean {
        return isConvex(quad) && hasValidEdgeLength(quad, minEdgeLengthPx)
    }

    private fun hasValidEdgeLength(quad: PerspectiveQuad, minEdgeLengthPx: Float): Boolean {
        val minEdgeLengthSquared = minEdgeLengthPx * minEdgeLengthPx
        return distanceSquared(quad.topLeft, quad.topRight) >= minEdgeLengthSquared &&
                distanceSquared(quad.topRight, quad.bottomRight) >= minEdgeLengthSquared &&
                distanceSquared(quad.bottomRight, quad.bottomLeft) >= minEdgeLengthSquared &&
                distanceSquared(quad.bottomLeft, quad.topLeft) >= minEdgeLengthSquared
    }

    private fun isConvex(quad: PerspectiveQuad): Boolean {
        val cross1 = cross(quad.topLeft, quad.topRight, quad.bottomRight)
        val cross2 = cross(quad.topRight, quad.bottomRight, quad.bottomLeft)
        val cross3 = cross(quad.bottomRight, quad.bottomLeft, quad.topLeft)
        val cross4 = cross(quad.bottomLeft, quad.topLeft, quad.topRight)

        if (
            abs(cross1) < PERSPECTIVE_EPSILON ||
            abs(cross2) < PERSPECTIVE_EPSILON ||
            abs(cross3) < PERSPECTIVE_EPSILON ||
            abs(cross4) < PERSPECTIVE_EPSILON
        ) {
            return false
        }

        val allPositive = cross1 > 0f && cross2 > 0f && cross3 > 0f && cross4 > 0f
        val allNegative = cross1 < 0f && cross2 < 0f && cross3 < 0f && cross4 < 0f
        return allPositive || allNegative
    }

    private fun cross(a: Offset, b: Offset, c: Offset): Float {
        val abx = b.x - a.x
        val aby = b.y - a.y
        val bcx = c.x - b.x
        val bcy = c.y - b.y
        return abx * bcy - aby * bcx
    }
}

private class PerspectiveHandleInteractor {
    private val cornerHandles = listOf(
        PerspectiveHandle.TOP_LEFT,
        PerspectiveHandle.TOP_RIGHT,
        PerspectiveHandle.BOTTOM_RIGHT,
        PerspectiveHandle.BOTTOM_LEFT,
    )
    private val edgeBindings = listOf(
        EdgeBinding(
            PerspectiveHandle.TOP_EDGE,
            PerspectiveHandle.TOP_LEFT,
            PerspectiveHandle.TOP_RIGHT
        ),
        EdgeBinding(
            PerspectiveHandle.RIGHT_EDGE,
            PerspectiveHandle.TOP_RIGHT,
            PerspectiveHandle.BOTTOM_RIGHT
        ),
        EdgeBinding(
            PerspectiveHandle.BOTTOM_EDGE,
            PerspectiveHandle.BOTTOM_LEFT,
            PerspectiveHandle.BOTTOM_RIGHT
        ),
        EdgeBinding(
            PerspectiveHandle.LEFT_EDGE,
            PerspectiveHandle.TOP_LEFT,
            PerspectiveHandle.BOTTOM_LEFT
        ),
    )
    private val edgeBindingByHandle = edgeBindings.associateBy(EdgeBinding::edgeHandle)

    private var edgeGeometryCache: Map<PerspectiveHandle, CachedEdgeGeometry> = emptyMap()

    fun updateQuad(quad: PerspectiveQuad?) {
        edgeGeometryCache = quad?.let { currentQuad ->
            edgeBindings.mapNotNull { binding ->
                cachedEdgeGeometry(
                    edgeStart = currentQuad.cornerOffset(binding.startCorner),
                    edgeEnd = currentQuad.cornerOffset(binding.endCorner),
                )?.let { binding.edgeHandle to it }
            }.toMap()
        } ?: emptyMap()
    }

    fun edgeGeometry(
        handle: PerspectiveHandle,
        edgeLengthPx: Float,
        edgeThicknessPx: Float,
        edgeLengthFractionLimit: Float,
    ): CenteredEdgeBarGeometry? = edgeGeometryCache[handle]?.toCenteredEdgeBarGeometry(
        edgeLengthPx = edgeLengthPx,
        edgeThicknessPx = edgeThicknessPx,
        edgeLengthFractionLimit = edgeLengthFractionLimit,
    )

    fun findHandle(
        quad: PerspectiveQuad?,
        point: Offset,
        config: PerspectiveGestureConfig,
    ): PerspectiveHandle? {
        if (quad == null) return null

        var selected: PerspectiveHandle? = null
        var minDistanceSquared = Float.MAX_VALUE
        val pointX = point.x
        val pointY = point.y

        if (config.enableCornerHandles) {
            val cornerThresholdSquared = config.cornerThresholdPx * config.cornerThresholdPx
            for (corner in cornerHandles) {
                val center = quad.cornerOffset(corner)
                val dx = center.x - pointX
                val dy = center.y - pointY
                val distanceSquared = dx * dx + dy * dy
                if (distanceSquared <= cornerThresholdSquared && distanceSquared < minDistanceSquared) {
                    selected = corner
                    minDistanceSquared = distanceSquared
                }
            }
            if (selected != null) return selected
        }

        if (!config.enableEdgeHandles) return null

        for (binding in edgeBindings) {
            val geometry = edgeGeometry(
                handle = binding.edgeHandle,
                edgeLengthPx = config.edgeLengthPx,
                edgeThicknessPx = config.edgeThicknessPx,
                edgeLengthFractionLimit = config.edgeLengthFractionLimit,
            ) ?: continue

            if (!geometry.contains(point, cornerRadiusPx = config.edgeCornerRadiusPx)) continue

            val dx = geometry.center.x - pointX
            val dy = geometry.center.y - pointY
            val distanceSquared = dx * dx + dy * dy
            if (distanceSquared < minDistanceSquared) {
                selected = binding.edgeHandle
                minDistanceSquared = distanceSquared
            }
        }
        return selected
    }

    fun dragHandleBy(
        quad: PerspectiveQuad?,
        imageRect: Rect,
        handle: PerspectiveHandle,
        delta: Offset,
        minEdgeLengthPx: Float,
        constraintSolveSteps: Int,
    ): PerspectiveQuad? {
        val sourceQuad = quad ?: return null
        if (imageRect.isEmpty) return null
        if (abs(delta.x) <= PERSPECTIVE_EPSILON && abs(delta.y) <= PERSPECTIVE_EPSILON) return null

        val fullCandidate = candidateQuadForHandleDelta(
            sourceQuad = sourceQuad,
            imageRect = imageRect,
            handle = handle,
            delta = delta,
            minEdgeLengthPx = minEdgeLengthPx,
        )
        if (fullCandidate != null) return fullCandidate

        var bestCandidate: PerspectiveQuad? = null
        var low = 0f
        var high = 1f
        repeat(constraintSolveSteps) {
            val mid = (low + high) * 0.5f
            val candidate = candidateQuadForHandleDelta(
                sourceQuad = sourceQuad,
                imageRect = imageRect,
                handle = handle,
                delta = delta.scale(mid),
                minEdgeLengthPx = minEdgeLengthPx,
            )
            if (candidate != null) {
                bestCandidate = candidate
                low = mid
            } else {
                high = mid
            }
        }
        return bestCandidate
    }

    private fun candidateQuadForHandleDelta(
        sourceQuad: PerspectiveQuad,
        imageRect: Rect,
        handle: PerspectiveHandle,
        delta: Offset,
        minEdgeLengthPx: Float,
    ): PerspectiveQuad? = when (handle) {
        PerspectiveHandle.TOP_LEFT,
        PerspectiveHandle.TOP_RIGHT,
        PerspectiveHandle.BOTTOM_RIGHT,
        PerspectiveHandle.BOTTOM_LEFT,
            -> candidateQuadWithCorner(
            sourceQuad = sourceQuad,
            imageRect = imageRect,
            handle = handle,
            target = sourceQuad.cornerOffset(handle) + delta,
            minEdgeLengthPx = minEdgeLengthPx,
        )

        PerspectiveHandle.TOP_EDGE,
        PerspectiveHandle.RIGHT_EDGE,
        PerspectiveHandle.BOTTOM_EDGE,
        PerspectiveHandle.LEFT_EDGE,
            -> candidateQuadForEdgeDelta(
            sourceQuad = sourceQuad,
            imageRect = imageRect,
            handle = handle,
            delta = delta,
            minEdgeLengthPx = minEdgeLengthPx,
        )
    }

    private fun candidateQuadWithCorner(
        sourceQuad: PerspectiveQuad,
        imageRect: Rect,
        handle: PerspectiveHandle,
        target: Offset,
        minEdgeLengthPx: Float,
    ): PerspectiveQuad? {
        val clampedTarget = target.coerceTo(imageRect)
        val candidate = when (handle) {
            PerspectiveHandle.TOP_LEFT -> sourceQuad.copy(topLeft = clampedTarget)
            PerspectiveHandle.TOP_RIGHT -> sourceQuad.copy(topRight = clampedTarget)
            PerspectiveHandle.BOTTOM_RIGHT -> sourceQuad.copy(bottomRight = clampedTarget)
            PerspectiveHandle.BOTTOM_LEFT -> sourceQuad.copy(bottomLeft = clampedTarget)
            PerspectiveHandle.TOP_EDGE,
            PerspectiveHandle.RIGHT_EDGE,
            PerspectiveHandle.BOTTOM_EDGE,
            PerspectiveHandle.LEFT_EDGE,
                -> return null
        }
        return candidate.takeIf { PerspectiveQuadConstraints.isValid(it, minEdgeLengthPx) }
    }

    private fun candidateQuadForEdgeDelta(
        sourceQuad: PerspectiveQuad,
        imageRect: Rect,
        handle: PerspectiveHandle,
        delta: Offset,
        minEdgeLengthPx: Float,
    ): PerspectiveQuad? {
        val binding = edgeBindingByHandle[handle] ?: return null
        val edgeStart = sourceQuad.cornerOffset(binding.startCorner)
        val edgeEnd = sourceQuad.cornerOffset(binding.endCorner)
        val edgeDelta = projectToEdgeNormal(delta, edgeStart, edgeEnd)

        val movedStart = (edgeStart + edgeDelta).coerceTo(imageRect)
        val movedEnd = (edgeEnd + edgeDelta).coerceTo(imageRect)

        val candidate = when (handle) {
            PerspectiveHandle.TOP_EDGE -> sourceQuad.copy(topLeft = movedStart, topRight = movedEnd)
            PerspectiveHandle.RIGHT_EDGE -> sourceQuad.copy(
                topRight = movedStart,
                bottomRight = movedEnd
            )

            PerspectiveHandle.BOTTOM_EDGE -> sourceQuad.copy(
                bottomRight = movedEnd,
                bottomLeft = movedStart
            )

            PerspectiveHandle.LEFT_EDGE -> sourceQuad.copy(
                topLeft = movedStart,
                bottomLeft = movedEnd
            )

            PerspectiveHandle.TOP_LEFT,
            PerspectiveHandle.TOP_RIGHT,
            PerspectiveHandle.BOTTOM_RIGHT,
            PerspectiveHandle.BOTTOM_LEFT,
                -> return null
        }
        return candidate.takeIf { PerspectiveQuadConstraints.isValid(it, minEdgeLengthPx) }
    }
}

private data class EdgeBinding(
    val edgeHandle: PerspectiveHandle,
    val startCorner: PerspectiveHandle,
    val endCorner: PerspectiveHandle,
)

internal fun PerspectiveQuad.cornerOffset(handle: PerspectiveHandle): Offset = when (handle) {
    PerspectiveHandle.TOP_LEFT -> topLeft
    PerspectiveHandle.TOP_RIGHT -> topRight
    PerspectiveHandle.BOTTOM_RIGHT -> bottomRight
    PerspectiveHandle.BOTTOM_LEFT -> bottomLeft
    PerspectiveHandle.TOP_EDGE,
    PerspectiveHandle.RIGHT_EDGE,
    PerspectiveHandle.BOTTOM_EDGE,
    PerspectiveHandle.LEFT_EDGE,
        -> error("Expected corner handle but got edge handle: $handle")
}

private fun PerspectiveQuad.translateBy(delta: Offset): PerspectiveQuad = PerspectiveQuad(
    topLeft = topLeft + delta,
    topRight = topRight + delta,
    bottomRight = bottomRight + delta,
    bottomLeft = bottomLeft + delta,
)

private fun Offset.remap(source: Rect, target: Rect): Offset {
    if (source.width <= 0f || source.height <= 0f) return this

    val normalizedX = ((x - source.left) / source.width).coerceIn(0f, 1f)
    val normalizedY = ((y - source.top) / source.height).coerceIn(0f, 1f)
    return Offset(
        x = target.left + normalizedX * target.width,
        y = target.top + normalizedY * target.height,
    )
}

private fun Offset.toNormalized(rect: Rect): Offset = Offset(
    x = ((x - rect.left) / rect.width).coerceIn(0f, 1f),
    y = ((y - rect.top) / rect.height).coerceIn(0f, 1f),
)
