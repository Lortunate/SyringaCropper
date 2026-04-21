package com.lortunate.syringacropper.processor

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import com.lortunate.syringacropper.perspective.PerspectiveQuad
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsNumber
import kotlin.js.toJsArray
import kotlin.js.toJsNumber
import kotlin.js.unsafeCast

@OptIn(ExperimentalWasmJsInterop::class)
actual object CropperProcessor {
    actual val supportMessage: String? = null

    actual fun perspectiveWarp(
        image: ImageBitmap,
        quad: PerspectiveQuad,
        tw: Int,
        th: Int,
    ): ImageBitmap {
        return process(image, preparePerspectiveWarpOperation(image, quad, tw, th))
    }

    actual fun rectCrop(image: ImageBitmap, rect: Rect): ImageBitmap {
        return process(image, prepareRectCropOperation(image, rect))
    }

    actual fun circleCrop(image: ImageBitmap, rect: Rect): ImageBitmap {
        return process(image, prepareCircleCropOperation(image, rect))
    }

    actual fun roundedRectCrop(image: ImageBitmap, rect: Rect, radius: Float): ImageBitmap {
        return process(image, prepareRoundedRectCropOperation(image, rect, radius))
    }

    actual fun resize(image: ImageBitmap, tw: Int, th: Int): ImageBitmap {
        return process(image, prepareResizeOperation(tw, th))
    }

    private fun process(image: ImageBitmap, operation: CropperOperation): ImageBitmap {
        return processNativeOperation(
            image = image,
            operation = operation,
            execute = { source, types, params ->
                processBytes(
                    source.readSkiaPixels().toJsByteArray(),
                    source.width,
                    source.height,
                    types.toJsIntArray(),
                    params.toJsFloatArray(),
                ).requireByteArray(operation.operationName)
            },
            toImageBitmap = { bytes, width, height, operationName ->
                bytes.toSkiaImageBitmap(width, height, operationName)
            },
        )
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun JsAny?.requireByteArray(operationName: String): ByteArray {
    val output = requireNotNull(this) {
        "CropperProcessor.$operationName returned no pixels."
    }.unsafeCast<JsArray<JsNumber>>()
    return ByteArray(output.length) { index -> output[index]!!.toInt().toByte() }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun ByteArray.toJsByteArray(): JsAny {
    return Array(size) { index -> (this[index].toInt() and 0xFF).toJsNumber() }.toJsArray().unsafeCast()
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun IntArray.toJsIntArray(): JsAny {
    return Array(size) { index -> this[index].toJsNumber() }.toJsArray().unsafeCast()
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun FloatArray.toJsFloatArray(): JsAny {
    return Array(size) { index -> this[index].toDouble().toJsNumber() }.toJsArray().unsafeCast()
}
