package com.lortunate.syringacropper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.lortunate.syringacropper.avatar.AvatarCropSelection
import com.lortunate.syringacropper.avatar.AvatarCropShape
import com.lortunate.syringacropper.perspective.PerspectiveCropSelection
import com.lortunate.syringacropper.perspective.PerspectiveQuad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot
import kotlin.math.roundToInt

internal sealed interface CropResultRequest {
    val sourceBitmap: ImageBitmap

    fun crop(): ImageBitmap
}

internal data class PerspectiveCropResultRequest(
    override val sourceBitmap: ImageBitmap,
    val selection: PerspectiveCropSelection,
) : CropResultRequest {
    override fun crop(): ImageBitmap {
        val quad = selection.normalizedQuad.toBitmapQuad(sourceBitmap)
        val (targetWidth, targetHeight) = quad.estimatedOutputSize()
        return ExampleCropExecutor.perspectiveWarp(
            image = sourceBitmap,
            quad = quad,
            tw = targetWidth,
            th = targetHeight,
        )
    }
}

internal data class RectCropResultRequest(
    override val sourceBitmap: ImageBitmap,
    val selection: CropperRectSelection,
) : CropResultRequest {
    override fun crop(): ImageBitmap {
        return ExampleCropExecutor.rectCrop(
            image = sourceBitmap,
            rect = selection.normalizedRect.toBitmapRect(sourceBitmap),
        )
    }
}

internal data class AvatarCropResultRequest(
    override val sourceBitmap: ImageBitmap,
    val selection: AvatarCropSelection,
) : CropResultRequest {
    override fun crop(): ImageBitmap {
        val rect = selection.normalizedRect.toBitmapRect(sourceBitmap)
        return when (selection.shape) {
            AvatarCropShape.CIRCLE -> ExampleCropExecutor.circleCrop(sourceBitmap, rect)
            AvatarCropShape.SQUARE -> ExampleCropExecutor.rectCrop(sourceBitmap, rect)
        }
    }
}

private data class PendingCropRequest(
    val id: Long,
    val request: CropResultRequest,
)

internal object CropResultSessionStore {
    private var nextRequestId: Long = 0L
    private var pendingRequest: PendingCropRequest? = null
    private val _latestRequestId = MutableStateFlow<Long?>(null)
    val latestRequestId = _latestRequestId.asStateFlow()

    fun submit(request: CropResultRequest) {
        val requestId = ++nextRequestId
        pendingRequest = PendingCropRequest(
            id = requestId,
            request = request,
        )
        _latestRequestId.value = requestId
    }

    fun consume(requestId: Long): CropResultRequest? {
        val request = pendingRequest
        if (request?.id != requestId) return null
        pendingRequest = null
        return request.request
    }
}

internal fun NavBackStack<NavKey>.showCropResult(request: CropResultRequest) {
    removeAll { entry -> entry is NavRoute.CropResult }
    CropResultSessionStore.submit(request)
    add(NavRoute.CropResult)
}

private fun Rect.toBitmapRect(bitmap: ImageBitmap): Rect {
    val width = bitmap.width.toFloat()
    val height = bitmap.height.toFloat()
    return Rect(
        left = (left * width).coerceIn(0f, width),
        top = (top * height).coerceIn(0f, height),
        right = (right * width).coerceIn(0f, width),
        bottom = (bottom * height).coerceIn(0f, height),
    )
}

private fun PerspectiveQuad.toBitmapQuad(bitmap: ImageBitmap): PerspectiveQuad {
    val width = bitmap.width.toFloat()
    val height = bitmap.height.toFloat()

    fun Offset.scaleToBitmap() = Offset(
        x = (x * width).coerceIn(0f, width),
        y = (y * height).coerceIn(0f, height),
    )

    return PerspectiveQuad(
        topLeft = topLeft.scaleToBitmap(),
        topRight = topRight.scaleToBitmap(),
        bottomRight = bottomRight.scaleToBitmap(),
        bottomLeft = bottomLeft.scaleToBitmap(),
    )
}

private fun PerspectiveQuad.estimatedOutputSize(): Pair<Int, Int> {
    val topWidth = topLeft.distanceTo(topRight)
    val bottomWidth = bottomLeft.distanceTo(bottomRight)
    val leftHeight = topLeft.distanceTo(bottomLeft)
    val rightHeight = topRight.distanceTo(bottomRight)

    val targetWidth = ((topWidth + bottomWidth) * 0.5f).roundToInt().coerceAtLeast(1)
    val targetHeight = ((leftHeight + rightHeight) * 0.5f).roundToInt().coerceAtLeast(1)
    return targetWidth to targetHeight
}

private fun Offset.distanceTo(other: Offset): Float {
    return hypot(other.x - x, other.y - y)
}

internal expect object ExampleCropExecutor {
    val supportMessage: String?
    fun perspectiveWarp(image: ImageBitmap, quad: PerspectiveQuad, tw: Int, th: Int): ImageBitmap
    fun rectCrop(image: ImageBitmap, rect: Rect): ImageBitmap
    fun circleCrop(image: ImageBitmap, rect: Rect): ImageBitmap
    fun roundedRectCrop(image: ImageBitmap, rect: Rect, radius: Float): ImageBitmap
    fun resize(image: ImageBitmap, tw: Int, th: Int): ImageBitmap
}
