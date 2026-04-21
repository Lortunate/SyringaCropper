package com.lortunate.syringacropper.processor

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import com.lortunate.syringacropper.perspective.PerspectiveQuad

internal const val UnsupportedProcessorSupportMessage =
    "Native processing is currently supported on Android, JVM, and Wasm."

internal data class NativeProcessPayload(
    val types: IntArray,
    val params: FloatArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NativeProcessPayload

        if (!types.contentEquals(other.types)) return false
        if (!params.contentEquals(other.params)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = types.contentHashCode()
        result = 31 * result + params.contentHashCode()
        return result
    }
}

internal sealed interface CropperOperation {
    val operationName: String
    val outputWidth: Int
    val outputHeight: Int

    fun toNativePayload(): NativeProcessPayload
}

internal data class PerspectiveWarpOperation(
    val quad: PerspectiveQuad,
    override val outputWidth: Int,
    override val outputHeight: Int,
) : CropperOperation {
    override val operationName: String = "perspectiveWarp"

    override fun toNativePayload(): NativeProcessPayload {
        return NativeProcessPayload(
            types = intArrayOf(PerspectiveWarpOp),
            params = floatArrayOf(
                quad.topLeft.x,
                quad.topLeft.y,
                quad.topRight.x,
                quad.topRight.y,
                quad.bottomRight.x,
                quad.bottomRight.y,
                quad.bottomLeft.x,
                quad.bottomLeft.y,
                outputWidth.toFloat(),
                outputHeight.toFloat(),
            ),
        )
    }
}

internal data class RectCropOperation(
    val crop: CropRectSpec,
) : CropperOperation {
    override val operationName: String = "rectCrop"
    override val outputWidth: Int = crop.width
    override val outputHeight: Int = crop.height

    override fun toNativePayload(): NativeProcessPayload {
        return NativeProcessPayload(
            types = intArrayOf(RectCropOp),
            params = crop.toParams(),
        )
    }
}

internal data class CircleCropOperation(
    val crop: CropRectSpec,
) : CropperOperation {
    override val operationName: String = "circleCrop"
    override val outputWidth: Int = crop.width
    override val outputHeight: Int = crop.height

    override fun toNativePayload(): NativeProcessPayload {
        return NativeProcessPayload(
            types = intArrayOf(CircleCropOp),
            params = crop.toParams(),
        )
    }
}

internal data class RoundedRectCropOperation(
    val crop: CropRectSpec,
    val radius: Float,
) : CropperOperation {
    override val operationName: String = "roundedRectCrop"
    override val outputWidth: Int = crop.width
    override val outputHeight: Int = crop.height

    override fun toNativePayload(): NativeProcessPayload {
        return NativeProcessPayload(
            types = intArrayOf(RoundedRectCropOp),
            params = crop.toRoundedRectParams(radius),
        )
    }
}

internal data class ResizeOperation(
    override val outputWidth: Int,
    override val outputHeight: Int,
) : CropperOperation {
    override val operationName: String = "resize"

    override fun toNativePayload(): NativeProcessPayload {
        return NativeProcessPayload(
            types = intArrayOf(ResizeOp),
            params = floatArrayOf(outputWidth.toFloat(), outputHeight.toFloat()),
        )
    }
}

internal fun preparePerspectiveWarpOperation(
    image: ImageBitmap,
    quad: PerspectiveQuad,
    targetWidth: Int,
    targetHeight: Int,
): PerspectiveWarpOperation {
    quad.requireProcessable(image.width, image.height, targetWidth, targetHeight)
    return PerspectiveWarpOperation(
        quad = quad,
        outputWidth = targetWidth,
        outputHeight = targetHeight,
    )
}

internal fun prepareRectCropOperation(
    image: ImageBitmap,
    rect: Rect,
): RectCropOperation {
    return RectCropOperation(
        crop = rect.toCropRectSpec(image.width, image.height, "rectCrop"),
    )
}

internal fun prepareCircleCropOperation(
    image: ImageBitmap,
    rect: Rect,
): CircleCropOperation {
    return CircleCropOperation(
        crop = rect.toCropRectSpec(image.width, image.height, "circleCrop"),
    )
}

internal fun prepareRoundedRectCropOperation(
    image: ImageBitmap,
    rect: Rect,
    radius: Float,
): RoundedRectCropOperation {
    val crop = rect.toCropRectSpec(image.width, image.height, "roundedRectCrop")
    return RoundedRectCropOperation(
        crop = crop,
        radius = normalizeRoundedRadius(radius, crop.width, crop.height),
    )
}

internal fun prepareResizeOperation(
    targetWidth: Int,
    targetHeight: Int,
): ResizeOperation {
    requireTargetSize(targetWidth, targetHeight, "resize")
    return ResizeOperation(
        outputWidth = targetWidth,
        outputHeight = targetHeight,
    )
}

internal fun unsupportedProcessorOperation(
    operationName: String,
    message: String,
): Nothing {
    throw UnsupportedOperationException("CropperProcessor.$operationName $message")
}

internal inline fun processNativeOperation(
    image: ImageBitmap,
    operation: CropperOperation,
    execute: (image: ImageBitmap, types: IntArray, params: FloatArray) -> ByteArray,
    toImageBitmap: (bytes: ByteArray, width: Int, height: Int, operationName: String) -> ImageBitmap,
): ImageBitmap {
    val payload = operation.toNativePayload()
    return toImageBitmap(
        execute(image, payload.types, payload.params),
        operation.outputWidth,
        operation.outputHeight,
        operation.operationName,
    )
}
