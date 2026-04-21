package com.lortunate.syringacropper

import androidx.compose.ui.graphics.ImageBitmap
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.lortunate.syringacropper.avatar.AvatarCropSelection
import com.lortunate.syringacropper.perspective.PerspectiveCropSelection
import com.lortunate.syringacropper.processor.crop

internal sealed interface CropResultRequest {
    val sourceBitmap: ImageBitmap

    fun crop(): ImageBitmap
}

internal data class PerspectiveCropResultRequest(
    override val sourceBitmap: ImageBitmap,
    val selection: PerspectiveCropSelection,
) : CropResultRequest {
    override fun crop(): ImageBitmap {
        return sourceBitmap.crop(selection)
    }
}

internal data class RectCropResultRequest(
    override val sourceBitmap: ImageBitmap,
    val selection: CropperRectSelection,
) : CropResultRequest {
    override fun crop(): ImageBitmap {
        return sourceBitmap.crop(selection)
    }
}

internal data class AvatarCropResultRequest(
    override val sourceBitmap: ImageBitmap,
    val selection: AvatarCropSelection,
) : CropResultRequest {
    override fun crop(): ImageBitmap {
        return sourceBitmap.crop(selection)
    }
}

internal object CropResultSessionStore {
    private var nextRequestId: Long = 0L
    private val pendingRequests = mutableMapOf<Long, CropResultRequest>()

    fun submit(request: CropResultRequest): Long {
        val requestId = ++nextRequestId
        pendingRequests.clear()
        pendingRequests[requestId] = request
        return requestId
    }

    fun consume(requestId: Long): CropResultRequest? {
        return pendingRequests.remove(requestId)
    }
}

internal fun NavBackStack<NavKey>.showCropResult(request: CropResultRequest) {
    removeAll { entry -> entry is NavRoute.CropResult }
    val requestId = CropResultSessionStore.submit(request)
    add(NavRoute.CropResult(requestId = requestId))
}

internal fun NavBackStack<NavKey>.showCropResult(
    sourceBitmap: ImageBitmap,
    selection: PerspectiveCropSelection,
) {
    showCropResult(PerspectiveCropResultRequest(sourceBitmap = sourceBitmap, selection = selection))
}

internal fun NavBackStack<NavKey>.showCropResult(
    sourceBitmap: ImageBitmap,
    selection: CropperRectSelection,
) {
    showCropResult(RectCropResultRequest(sourceBitmap = sourceBitmap, selection = selection))
}

internal fun NavBackStack<NavKey>.showCropResult(
    sourceBitmap: ImageBitmap,
    selection: AvatarCropSelection,
) {
    showCropResult(AvatarCropResultRequest(sourceBitmap = sourceBitmap, selection = selection))
}

internal fun ImageBitmap?.resolveSourceSize(sourceSize: CropSourceSize?): CropSourceSize? {
    return sourceSize ?: this?.toCropSourceSize()
}
