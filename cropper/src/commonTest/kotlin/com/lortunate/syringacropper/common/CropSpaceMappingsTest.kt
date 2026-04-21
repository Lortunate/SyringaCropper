package com.lortunate.syringacropper.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.lortunate.syringacropper.CropSourceSize
import com.lortunate.syringacropper.perspective.PerspectiveQuad
import com.lortunate.syringacropper.perspective.toImageSpace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CropSpaceMappingsTest {
    @Test
    fun normalizeSelectionInsetFraction_clampsIntoSupportedRange() {
        assertEquals(0f, normalizeSelectionInsetFraction(-0.2f))
        assertEquals(0.2f, normalizeSelectionInsetFraction(0.2f))
        assertEquals(0.45f, normalizeSelectionInsetFraction(0.8f))
    }

    @Test
    fun denormalizeTo_scalesNormalizedRectIntoBoundsSpace() {
        val bounds = Rect(left = 10f, top = 20f, right = 210f, bottom = 120f)
        val normalized = Rect(left = 0.1f, top = 0.2f, right = 0.8f, bottom = 0.9f)

        assertEquals(
            Rect(left = 30f, top = 40f, right = 170f, bottom = 110f),
            normalized.denormalizeTo(bounds),
        )
    }

    @Test
    fun scaleToImageSpace_scalesRectUsingSourceSize() {
        val normalized = Rect(left = 0.1f, top = 0.2f, right = 0.8f, bottom = 0.9f)

        assertEquals(
            Rect(left = 20f, top = 20f, right = 160f, bottom = 90f),
            normalized.scaleToImageSpace(CropSourceSize(width = 200, height = 100)),
        )
    }

    @Test
    fun scaleToImageSpace_clampsRectCoordinatesWhenRequested() {
        val normalized = Rect(left = -0.1f, top = 0.25f, right = 1.2f, bottom = 1.5f)

        assertEquals(
            Rect(left = 0f, top = 25f, right = 200f, bottom = 100f),
            normalized.scaleToImageSpace(imageWidth = 200f, imageHeight = 100f, clampToBounds = true),
        )
    }

    @Test
    fun scaleToImageSpace_clampsPerspectiveQuadWhenRequested() {
        val normalized = PerspectiveQuad(
            topLeft = Offset(-0.1f, 0.2f),
            topRight = Offset(0.9f, -0.4f),
            bottomRight = Offset(1.2f, 1.4f),
            bottomLeft = Offset(0.3f, 0.8f),
        )

        val actual = normalized.scaleToImageSpace(
            imageWidth = 200f,
            imageHeight = 100f,
            clampToBounds = true,
        )

        assertOffsetEquals(Offset(0f, 20f), actual.topLeft)
        assertOffsetEquals(Offset(180f, 0f), actual.topRight)
        assertOffsetEquals(Offset(200f, 100f), actual.bottomRight)
        assertOffsetEquals(Offset(60f, 80f), actual.bottomLeft)
    }

    @Test
    fun perspectiveQuadToImageSpace_rejectsNonPositiveSourceSize() {
        val normalized = PerspectiveQuad(
            topLeft = Offset(0.1f, 0.2f),
            topRight = Offset(0.9f, 0.2f),
            bottomRight = Offset(0.8f, 0.9f),
            bottomLeft = Offset(0.2f, 0.9f),
        )

        assertFailsWith<IllegalArgumentException> {
            normalized.toImageSpace(CropSourceSize(width = 0, height = 100))
        }
        assertFailsWith<IllegalArgumentException> {
            normalized.toImageSpace(CropSourceSize(width = 100, height = 0))
        }
    }

    private fun assertOffsetEquals(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, absoluteTolerance = 0.001f)
        assertEquals(expected.y, actual.y, absoluteTolerance = 0.001f)
    }
}
