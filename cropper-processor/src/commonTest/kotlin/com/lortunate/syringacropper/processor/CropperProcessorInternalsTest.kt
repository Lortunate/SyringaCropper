package com.lortunate.syringacropper.processor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.lortunate.syringacropper.perspective.PerspectiveQuad
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CropperProcessorInternalsTest {
    @Test
    fun toCropRectSpec_returnsRoundedSizeWithinBounds() {
        val spec = Rect(left = 10.2f, top = 5.8f, right = 30.6f, bottom = 26.1f)
            .toCropRectSpec(sourceWidth = 100, sourceHeight = 100, operationName = "rectCrop")

        assertEquals(
            CropRectSpec(left = 10.2f, top = 5.8f, width = 20, height = 20),
            spec,
        )
    }

    @Test
    fun toCropRectSpec_rejectsOutOfBoundsRects() {
        val error = assertFailsWith<IllegalArgumentException> {
            Rect(left = -1f, top = 0f, right = 10f, bottom = 10f)
                .toCropRectSpec(sourceWidth = 100, sourceHeight = 100, operationName = "rectCrop")
        }

        assertContains(error.message.orEmpty(), "inside the source image")
    }

    @Test
    fun normalizeRoundedRadius_clampsToHalfOfShortestEdge() {
        val radius = normalizeRoundedRadius(radius = 99f, cropWidth = 40, cropHeight = 20)

        assertEquals(10f, radius)
    }

    @Test
    fun requirePixelByteCount_rejectsUnexpectedBufferSize() {
        val error = assertFailsWith<IllegalArgumentException> {
            ByteArray(3).requirePixelByteCount(width = 1, height = 1, operationName = "resize")
        }

        assertContains(error.message.orEmpty(), "expected 4 bytes")
    }

    @Test
    fun requireProcessable_rejectsOutOfBoundsPerspectivePoints() {
        val quad = PerspectiveQuad(
            topLeft = Offset(-1f, 0f),
            topRight = Offset(30f, 0f),
            bottomRight = Offset(30f, 40f),
            bottomLeft = Offset(0f, 40f),
        )

        val error = assertFailsWith<IllegalArgumentException> {
            quad.requireProcessable(
                sourceWidth = 30,
                sourceHeight = 40,
                targetWidth = 20,
                targetHeight = 20,
            )
        }

        assertContains(error.message.orEmpty(), "perspectiveWarp topLeft")
    }
}
