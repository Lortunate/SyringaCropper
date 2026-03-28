package com.lortunate.syringacropper.perspective

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class PerspectiveInteractionConfig(
    val enableQuadDrag: Boolean = true,
    val defaultInsetFraction: Float = 0f,
    val minEdgeLength: Dp = 32.dp,
    val constraintSolveSteps: Int = 3,
)
