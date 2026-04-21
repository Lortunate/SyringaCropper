package com.lortunate.syringacropper.processor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.lortunate.syringacropper.CropSourceSize
import com.lortunate.syringacropper.avatar.AvatarCropSelection
import com.lortunate.syringacropper.avatar.AvatarCropShape
import com.lortunate.syringacropper.perspective.PerspectiveQuad
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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

    @Test
    fun toBitmapRect_scalesNormalizedRectIntoBitmapSpace() {
        val rect = Rect(left = 0.1f, top = 0.2f, right = 0.8f, bottom = 0.9f)

        assertEquals(
            Rect(left = 20f, top = 20f, right = 160f, bottom = 90f),
            rect.toBitmapRect(bitmapWidth = 200, bitmapHeight = 100),
        )
    }

    @Test
    fun toBitmapQuad_scalesNormalizedQuadIntoBitmapSpace() {
        val quad = PerspectiveQuad(
            topLeft = Offset(0.1f, 0.2f),
            topRight = Offset(0.9f, 0.2f),
            bottomRight = Offset(0.8f, 0.9f),
            bottomLeft = Offset(0.2f, 0.9f),
        )

        assertEquals(
            PerspectiveQuad(
                topLeft = Offset(20f, 20f),
                topRight = Offset(180f, 20f),
                bottomRight = Offset(160f, 90f),
                bottomLeft = Offset(40f, 90f),
            ),
            quad.toBitmapQuad(bitmapWidth = 200, bitmapHeight = 100),
        )
    }

    @Test
    fun estimatedOutputSize_averagesOpposingEdges() {
        val quad = PerspectiveQuad(
            topLeft = Offset(0f, 0f),
            topRight = Offset(110f, 0f),
            bottomRight = Offset(90f, 60f),
            bottomLeft = Offset(0f, 60f),
        )

        assertEquals(100 to 62, quad.estimatedOutputSize())
    }

    @Test
    fun avatarSelection_identifiesCircleCrop() {
        val selection = AvatarCropSelection(
            sourceSize = CropSourceSize(width = 100, height = 100),
            shape = AvatarCropShape.CIRCLE,
            normalizedRect = Rect(0f, 0f, 1f, 1f),
            imageRect = Rect(0f, 0f, 100f, 100f),
        )

        assertTrue(selection.isCircleCrop())
    }

    @Test
    fun cropperProcessorSupportState_matchesSupportMessage() {
        assertEquals(CropperProcessor.supportMessage == null, CropperProcessor.isSupported)
    }
}
