package com.lortunate.syringacropper.avatar

import androidx.compose.runtime.Immutable

@Immutable
data class AvatarInteractionConfig(
    val defaultInsetFraction: Float = 0f,
    val maxScale: Float = 6f,
)
