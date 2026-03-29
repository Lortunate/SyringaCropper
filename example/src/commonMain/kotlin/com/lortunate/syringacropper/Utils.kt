package com.lortunate.syringacropper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import com.lortunate.syringacropper.avatar.AvatarCropSelection
import com.lortunate.syringacropper.perspective.PerspectiveCropSelection
import com.lortunate.syringacropper.perspective.PerspectiveQuad
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlin.math.roundToInt

internal data class PreviewImagePayload(
    val previewBitmap: ImageBitmap,
    val sourceSize: CropSourceSize,
)

internal suspend fun PlatformFile.readPreviewImageOrNull() =
    runCatching { decodePreviewImage(readBytes()) }.getOrNull()

internal expect fun decodePreviewImage(bytes: ByteArray): PreviewImagePayload?

internal fun PerspectiveCropSelection.formatDebugSummary(): String {
    return buildString {
        appendLine("Image Quad")
        appendLine(imageQuad.formatDebugSummary())
        appendLine()
        appendLine("Normalized Quad")
        append(normalizedQuad.formatDebugSummary())
    }
}

internal fun AvatarCropSelection.formatDebugSummary(): String {
    return buildString {
        appendLine("Shape")
        appendLine(shape.name)
        appendLine()
        appendLine("Image Rect")
        appendLine(imageRect.formatRect())
        appendLine()
        appendLine("Normalized Rect")
        append(normalizedRect.formatRect())
    }
}

internal fun CropperRectSelection.formatDebugSummary(): String {
    return buildString {
        appendLine("Image Rect")
        appendLine(imageRect.formatRect())
        appendLine()
        appendLine("Normalized Rect")
        append(normalizedRect.formatRect())
    }
}

internal fun PerspectiveQuad.formatDebugSummary(): String {
    val left = minOf(topLeft.x, topRight.x, bottomRight.x, bottomLeft.x)
    val top = minOf(topLeft.y, topRight.y, bottomRight.y, bottomLeft.y)
    val right = maxOf(topLeft.x, topRight.x, bottomRight.x, bottomLeft.x)
    val bottom = maxOf(topLeft.y, topRight.y, bottomRight.y, bottomLeft.y)

    return buildString {
        appendLine("Points")
        appendLine("TL: ${topLeft.formatPair()}")
        appendLine("TR: ${topRight.formatPair()}")
        appendLine("BR: ${bottomRight.formatPair()}")
        appendLine("BL: ${bottomLeft.formatPair()}")
        append("Rect: (${left.formatValue()}, ${top.formatValue()}, ${right.formatValue()}, ${bottom.formatValue()})")
    }
}

private fun Offset.formatPair(): String {
    return "(${x.formatValue()}, ${y.formatValue()})"
}

private fun Rect.formatRect(): String {
    return "(${left.formatValue()}, ${top.formatValue()}, ${right.formatValue()}, ${bottom.formatValue()})"
}

private fun Float.formatValue(): String {
    return ((this * 10f).roundToInt() / 10f).toString()
}
