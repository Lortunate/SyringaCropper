package com.lortunate.syringacropper.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.lortunate.syringacropper.CropSourceSize
import com.lortunate.syringacropper.CropperRectSelection
import com.lortunate.syringacropper.avatar.AvatarCropSelection
import com.lortunate.syringacropper.avatar.AvatarCropShape
import com.lortunate.syringacropper.perspective.PerspectiveCropSelection
import com.lortunate.syringacropper.perspective.PerspectiveQuad
import kotlin.test.Test
import kotlin.test.assertEquals

class CropSelectionFactoriesTest {
    @Test
    fun toRectSelection_buildsNormalizedAndImageSpaceSnapshot() {
        val normalized = Rect(left = 0.1f, top = 0.2f, right = 0.8f, bottom = 0.9f)
        val sourceSize = CropSourceSize(width = 200, height = 100)

        assertEquals(
            CropperRectSelection(
                normalizedRect = normalized,
                imageRect = Rect(left = 20f, top = 20f, right = 160f, bottom = 90f),
                sourceSize = sourceSize,
            ),
            normalized.toRectSelection(sourceSize),
        )
    }

    @Test
    fun toAvatarSelection_buildsNormalizedAndImageSpaceSnapshot() {
        val normalized = Rect(left = 0.1f, top = 0.2f, right = 0.8f, bottom = 0.9f)
        val sourceSize = CropSourceSize(width = 200, height = 100)

        assertEquals(
            AvatarCropSelection(
                sourceSize = sourceSize,
                shape = AvatarCropShape.CIRCLE,
                normalizedRect = normalized,
                imageRect = Rect(left = 20f, top = 20f, right = 160f, bottom = 90f),
            ),
            normalized.toAvatarSelection(
                shape = AvatarCropShape.CIRCLE,
                sourceSize = sourceSize,
            ),
        )
    }

    @Test
    fun toPerspectiveSelection_buildsNormalizedAndImageSpaceSnapshot() {
        val normalized = PerspectiveQuad(
            topLeft = Offset(0.1f, 0.2f),
            topRight = Offset(0.9f, 0.2f),
            bottomRight = Offset(0.8f, 0.9f),
            bottomLeft = Offset(0.2f, 0.9f),
        )
        val sourceSize = CropSourceSize(width = 200, height = 100)

        assertEquals(
            PerspectiveCropSelection(
                normalizedQuad = normalized,
                imageQuad = PerspectiveQuad(
                    topLeft = Offset(20f, 20f),
                    topRight = Offset(180f, 20f),
                    bottomRight = Offset(160f, 90f),
                    bottomLeft = Offset(40f, 90f),
                ),
                sourceSize = sourceSize,
            ),
            normalized.toPerspectiveSelection(sourceSize),
        )
    }
}
