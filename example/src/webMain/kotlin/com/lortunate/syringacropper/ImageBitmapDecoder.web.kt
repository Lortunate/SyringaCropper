package com.lortunate.syringacropper

import org.jetbrains.compose.resources.decodeToImageBitmap

internal actual fun decodePreviewImage(bytes: ByteArray): PreviewImagePayload? {
    return runCatching {
        val previewBitmap = bytes.decodeToImageBitmap()
        PreviewImagePayload(
            previewBitmap = previewBitmap,
            sourceSize = CropSourceSize(width = previewBitmap.width, height = previewBitmap.height),
        )
    }.getOrNull()
}
