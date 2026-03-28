package com.lortunate.syringacropper

import com.lortunate.syringacropper.perspective.PerspectiveSourceSize
import org.jetbrains.compose.resources.decodeToImageBitmap

internal actual fun decodePreviewImage(bytes: ByteArray): PreviewImagePayload? {
    return runCatching {
        val previewBitmap = bytes.decodeToImageBitmap()
        PreviewImagePayload(
            previewBitmap = previewBitmap,
            sourceSize = PerspectiveSourceSize(width = previewBitmap.width, height = previewBitmap.height),
        )
    }.getOrNull()
}
