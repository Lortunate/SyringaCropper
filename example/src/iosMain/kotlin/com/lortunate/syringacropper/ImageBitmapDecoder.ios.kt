package com.lortunate.syringacropper

import org.jetbrains.compose.resources.decodeToImageBitmap

internal actual fun decodePreviewImage(bytes: ByteArray): PreviewImagePayload? {
    return runCatching {
        val previewBitmap = bytes.decodeToImageBitmap()
        PreviewImagePayload(
            previewBitmap = previewBitmap,
            sourceSize = previewBitmap.toCropSourceSize(),
        )
    }.getOrNull()
}
