package com.lortunate.syringacropper.avatar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lortunate.syringacropper.cropperGridColor
import com.lortunate.syringacropper.cropperMaskColor
import com.lortunate.syringacropper.primaryColor

@Immutable
data class AvatarMaskStyle(
    val drawOutsideSelection: Boolean = true,
    val color: Color = cropperMaskColor,
)

@Immutable
data class AvatarFrameStyle(
    val showFrame: Boolean = true,
    val showGrid: Boolean = true,
    val gridLineCount: Int = 2,
    val showImageBounds: Boolean = false,
    val strokeWidth: Dp = 2.dp,
    val gridStrokeWidth: Dp = 1.dp,
    val imageBoundsStrokeWidth: Dp = 1.dp,
    val color: Color = primaryColor,
    val gridColor: Color = cropperGridColor,
    val imageBoundsColor: Color = primaryColor,
)

@Immutable
data class AvatarCropStyle(
    val frame: AvatarFrameStyle = AvatarFrameStyle(),
    val mask: AvatarMaskStyle = AvatarMaskStyle(),
)
