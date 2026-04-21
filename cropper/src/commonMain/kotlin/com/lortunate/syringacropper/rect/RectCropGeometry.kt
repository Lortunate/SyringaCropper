package com.lortunate.syringacropper.rect

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.lortunate.syringacropper.CropperHandle
import com.lortunate.syringacropper.common.normalizeSelectionInsetFraction
import kotlin.math.abs
import kotlin.math.max

internal object RectCropGeometry {
    fun defaultRect(
        imageBounds: Rect,
        insetFraction: Float,
        aspectRatio: RectCropAspectRatio
    ): Rect? {
        if (imageBounds.isEmpty) return null
        val safeInset = normalizeSelectionInsetFraction(insetFraction)
        val insetX = imageBounds.width * safeInset
        val insetY = imageBounds.height * safeInset
        val maxAvailable = Rect(
            imageBounds.left + insetX,
            imageBounds.top + insetY,
            imageBounds.right - insetX,
            imageBounds.bottom - insetY
        )
        return coerceAspectRatioFromCenter(maxAvailable, aspectRatio)
    }

    fun coerceAspectRatio(rect: Rect, aspectRatio: RectCropAspectRatio, imageBounds: Rect): Rect {
        if (aspectRatio !is RectCropAspectRatio.Fixed) return rect
        val targetRatio = aspectRatio.ratio
        val currentRatio = rect.width / rect.height

        val center = rect.center
        var newWidth = rect.width
        var newHeight = rect.height

        if (currentRatio > targetRatio) {
            newWidth = rect.height * targetRatio
        } else {
            newHeight = rect.width / targetRatio
        }

        val halfW = newWidth / 2f
        val halfH = newHeight / 2f

        // Scale down from center if exceeds image bounds
        var scale = 1f
        if (center.x - halfW < imageBounds.left) scale =
            minOf(scale, (center.x - imageBounds.left) / halfW)
        if (center.x + halfW > imageBounds.right) scale =
            minOf(scale, (imageBounds.right - center.x) / halfW)
        if (center.y - halfH < imageBounds.top) scale =
            minOf(scale, (center.y - imageBounds.top) / halfH)
        if (center.y + halfH > imageBounds.bottom) scale =
            minOf(scale, (imageBounds.bottom - center.y) / halfH)

        val finalHalfW = halfW * scale
        val finalHalfH = halfH * scale

        return Rect(
            center.x - finalHalfW,
            center.y - finalHalfH,
            center.x + finalHalfW,
            center.y + finalHalfH
        )
    }

    private fun coerceAspectRatioFromCenter(
        maxAvailable: Rect,
        aspectRatio: RectCropAspectRatio
    ): Rect {
        if (aspectRatio !is RectCropAspectRatio.Fixed) return maxAvailable
        val targetRatio = aspectRatio.ratio
        val currentRatio = maxAvailable.width / maxAvailable.height
        val center = maxAvailable.center

        val width: Float
        val height: Float
        if (currentRatio > targetRatio) {
            height = maxAvailable.height
            width = height * targetRatio
        } else {
            width = maxAvailable.width
            height = width / targetRatio
        }
        val halfW = width / 2f
        val halfH = height / 2f
        return Rect(center.x - halfW, center.y - halfH, center.x + halfW, center.y + halfH)
    }

    fun translate(rect: Rect, delta: Offset, imageBounds: Rect): Rect? {
        if (imageBounds.isEmpty) return null
        val minDx = imageBounds.left - rect.left
        val maxDx = imageBounds.right - rect.right
        val minDy = imageBounds.top - rect.top
        val maxDy = imageBounds.bottom - rect.bottom

        val clampedDx = delta.x.coerceIn(minDx, maxDx)
        val clampedDy = delta.y.coerceIn(minDy, maxDy)

        if (abs(clampedDx) < 1e-3f && abs(clampedDy) < 1e-3f) return null
        return rect.translate(clampedDx, clampedDy)
    }

    fun findHandle(rect: Rect?, point: Offset, config: RectCropGestureConfig): CropperHandle? {
        if (rect == null) return null

        var selected: CropperHandle? = null
        var minDistanceSquared = Float.MAX_VALUE

        if (config.enableCornerHandles) {
            val cornerThresholdSq = config.cornerThresholdPx * config.cornerThresholdPx
            val corners = arrayOf(
                CropperHandle.TOP_LEFT,
                CropperHandle.TOP_RIGHT,
                CropperHandle.BOTTOM_RIGHT,
                CropperHandle.BOTTOM_LEFT
            )
            for (handle in corners) {
                val cornerPoint = when (handle) {
                    CropperHandle.TOP_LEFT -> rect.topLeft
                    CropperHandle.TOP_RIGHT -> rect.topRight
                    CropperHandle.BOTTOM_RIGHT -> rect.bottomRight
                    else -> rect.bottomLeft
                }
                val dx = cornerPoint.x - point.x
                val dy = cornerPoint.y - point.y
                val distSq = dx * dx + dy * dy
                if (distSq <= cornerThresholdSq && distSq < minDistanceSquared) {
                    selected = handle
                    minDistanceSquared = distSq
                }
            }
            if (selected != null) return selected
        }

        if (config.enableEdgeHandles) {
            val halfThick = config.edgeThicknessPx / 2f

            if (abs(point.y - rect.top) <= halfThick && point.x in rect.left..rect.right) {
                return CropperHandle.TOP_EDGE
            }
            if (abs(point.y - rect.bottom) <= halfThick && point.x in rect.left..rect.right) {
                return CropperHandle.BOTTOM_EDGE
            }
            if (abs(point.x - rect.left) <= halfThick && point.y in rect.top..rect.bottom) {
                return CropperHandle.LEFT_EDGE
            }
            if (abs(point.x - rect.right) <= halfThick && point.y in rect.top..rect.bottom) {
                return CropperHandle.RIGHT_EDGE
            }
        }
        return null
    }

    fun dragHandleBy(
        rect: Rect,
        imageBounds: Rect,
        handle: CropperHandle,
        delta: Offset,
        minRectSizePx: Float,
        aspectRatio: RectCropAspectRatio
    ): Rect {
        var left = rect.left
        var top = rect.top
        var right = rect.right
        var bottom = rect.bottom

        when (handle) {
            CropperHandle.TOP_LEFT -> {
                left += delta.x; top += delta.y
            }

            CropperHandle.TOP_RIGHT -> {
                right += delta.x; top += delta.y
            }

            CropperHandle.BOTTOM_RIGHT -> {
                right += delta.x; bottom += delta.y
            }

            CropperHandle.BOTTOM_LEFT -> {
                left += delta.x; bottom += delta.y
            }

            CropperHandle.TOP_EDGE -> {
                top += delta.y
            }

            CropperHandle.RIGHT_EDGE -> {
                right += delta.x
            }

            CropperHandle.BOTTOM_EDGE -> {
                bottom += delta.y
            }

            CropperHandle.LEFT_EDGE -> {
                left += delta.x
            }
        }

        // Apply min bounds
        if (right - left < minRectSizePx) {
            if (handle == CropperHandle.LEFT_EDGE || handle == CropperHandle.TOP_LEFT || handle == CropperHandle.BOTTOM_LEFT) left =
                right - minRectSizePx
            else right = left + minRectSizePx
        }
        if (bottom - top < minRectSizePx) {
            if (handle == CropperHandle.TOP_EDGE || handle == CropperHandle.TOP_LEFT || handle == CropperHandle.TOP_RIGHT) top =
                bottom - minRectSizePx
            else bottom = top + minRectSizePx
        }

        // Maintain aspect ratio
        if (aspectRatio is RectCropAspectRatio.Fixed) {
            val targetRatio = aspectRatio.ratio
            val w = right - left
            val h = bottom - top

            val isHorizontal =
                handle == CropperHandle.LEFT_EDGE || handle == CropperHandle.RIGHT_EDGE
            val isVertical = handle == CropperHandle.TOP_EDGE || handle == CropperHandle.BOTTOM_EDGE

            if (isHorizontal) {
                val newH = w / targetRatio
                val cy = rect.center.y
                top = cy - newH / 2f
                bottom = cy + newH / 2f
            } else if (isVertical) {
                val newW = h * targetRatio
                val cx = rect.center.x
                left = cx - newW / 2f
                right = cx + newW / 2f
            } else {
                // Diagonal handles
                val dx = abs(right - left) - rect.width
                val dy = abs(bottom - top) - rect.height

                if (abs(dx) > abs(dy)) {
                    val newH = w / targetRatio
                    if (handle == CropperHandle.TOP_LEFT || handle == CropperHandle.TOP_RIGHT) top =
                        bottom - newH
                    else bottom = top + newH
                } else {
                    val newW = h * targetRatio
                    if (handle == CropperHandle.TOP_LEFT || handle == CropperHandle.BOTTOM_LEFT) left =
                        right - newW
                    else right = left + newW
                }
            }
        }

        // Clip to imageBounds with preserving aspect ratio (if Fixed)
        val clampedRect = Rect(left, top, right, bottom)

        if (clampedRect.left < imageBounds.left || clampedRect.right > imageBounds.right ||
            clampedRect.top < imageBounds.top || clampedRect.bottom > imageBounds.bottom
        ) {

            if (aspectRatio is RectCropAspectRatio.Fixed) {
                // Find safe width and height from the anchor point
                var safeW = clampedRect.width
                var safeH = clampedRect.height

                // The logic below ensures that if we exceed a boundary, we pull back the changing edge,
                // and proportionally scale the other edges from the fixed anchor point.
                if (clampedRect.left < imageBounds.left && (handle == CropperHandle.LEFT_EDGE || handle == CropperHandle.TOP_LEFT || handle == CropperHandle.BOTTOM_LEFT)) {
                    safeW = rect.right - imageBounds.left
                    safeH = safeW / aspectRatio.ratio
                }
                if (clampedRect.right > imageBounds.right && (handle == CropperHandle.RIGHT_EDGE || handle == CropperHandle.TOP_RIGHT || handle == CropperHandle.BOTTOM_RIGHT)) {
                    safeW = imageBounds.right - rect.left
                    safeH = safeW / aspectRatio.ratio
                }
                if (clampedRect.top < imageBounds.top && (handle == CropperHandle.TOP_EDGE || handle == CropperHandle.TOP_LEFT || handle == CropperHandle.TOP_RIGHT)) {
                    safeH = rect.bottom - imageBounds.top
                    safeW = safeH * aspectRatio.ratio
                }
                if (clampedRect.bottom > imageBounds.bottom && (handle == CropperHandle.BOTTOM_EDGE || handle == CropperHandle.BOTTOM_LEFT || handle == CropperHandle.BOTTOM_RIGHT)) {
                    safeH = imageBounds.bottom - rect.top
                    safeW = safeH * aspectRatio.ratio
                }

                // Ensure the safe dimensions aren't smaller than rect size or larger than possible space
                safeW = max(minRectSizePx, safeW)
                safeH = max(minRectSizePx, safeH)

                left = rect.left
                right = rect.right
                top = rect.top
                bottom = rect.bottom

                when (handle) {
                    CropperHandle.TOP_LEFT -> {
                        left = right - safeW; top = bottom - safeH
                    }

                    CropperHandle.TOP_RIGHT -> {
                        right = left + safeW; top = bottom - safeH
                    }

                    CropperHandle.BOTTOM_RIGHT -> {
                        right = left + safeW; bottom = top + safeH
                    }

                    CropperHandle.BOTTOM_LEFT -> {
                        left = right - safeW; bottom = top + safeH
                    }

                    CropperHandle.LEFT_EDGE -> {
                        left = right - safeW;
                        val cy = rect.center.y; top = cy - safeH / 2f; bottom = cy + safeH / 2f
                    }

                    CropperHandle.RIGHT_EDGE -> {
                        right = left + safeW;
                        val cy = rect.center.y; top = cy - safeH / 2f; bottom = cy + safeH / 2f
                    }

                    CropperHandle.TOP_EDGE -> {
                        top = bottom - safeH;
                        val cx = rect.center.x; left = cx - safeW / 2f; right = cx + safeW / 2f
                    }

                    CropperHandle.BOTTOM_EDGE -> {
                        bottom = top + safeH;
                        val cx = rect.center.x; left = cx - safeW / 2f; right = cx + safeW / 2f
                    }
                }
            }

            // Absolute clamp to bounds
            left = left.coerceAtLeast(imageBounds.left)
            top = top.coerceAtLeast(imageBounds.top)
            right = right.coerceAtMost(imageBounds.right)
            bottom = bottom.coerceAtMost(imageBounds.bottom)

            // Re-apply aspect ratio from center if edge handle violated ratio by clamping
            if (aspectRatio is RectCropAspectRatio.Fixed) {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val currentW = right - left
                val currentH = bottom - top
                if (abs(currentW / currentH - aspectRatio.ratio) > 1e-3f) {
                    val scale = minOf(currentW / rect.width, currentH / rect.height)
                    val nw = rect.width * scale
                    val nh = rect.height * scale
                    left = cx - nw / 2f
                    right = cx + nw / 2f
                    top = cy - nh / 2f
                    bottom = cy + nh / 2f
                }
            }
        }

        return Rect(left, top, right, bottom)
    }
}
