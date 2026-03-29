package com.lortunate.syringacropper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Common interaction configuration for the rectangle cropper.
 */
@Immutable
data class RectCropInteractionConfig(
    val enableRectDrag: Boolean = true,
    val enableDoubleTapReset: Boolean = true,
    val defaultInsetFraction: Float = 0f,
    val minRectSize: Dp = 64.dp,
)

/**
 * Common interaction configuration for the perspective cropper.
 */
@Immutable
data class PerspectiveInteractionConfig(
    val enableQuadDrag: Boolean = true,
    val defaultInsetFraction: Float = 0f,
    val minEdgeLength: Dp = 32.dp,
    val constraintSolveSteps: Int = 3,
)

/**
 * Common interaction configuration for the avatar cropper.
 */
@Immutable
data class AvatarInteractionConfig(
    val defaultInsetFraction: Float = 0f,
    val maxScale: Float = 6f,
)
