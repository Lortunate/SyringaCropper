package com.lortunate.syringacropper.rect

import androidx.compose.runtime.Immutable

/**
 * Aspect ratio configurations for the normal rectangle cropper.
 */
@Immutable
sealed interface RectCropAspectRatio {
    /**
     * The aspect ratio is not constrained.
     */
    data object Unconstrained : RectCropAspectRatio

    /**
     * A fixed aspect ratio constraint.
     */
    @Immutable
    @Suppress("unused", "ClassName")
    sealed class Fixed : RectCropAspectRatio {
        abstract val ratio: Float

        data object Square : Fixed() {
            override val ratio = 1f
        }

        data object Ratio4_3 : Fixed() {
            override val ratio = 4f / 3f
        }

        data object Ratio3_4 : Fixed() {
            override val ratio = 3f / 4f
        }

        data object Ratio16_9 : Fixed() {
            override val ratio = 16f / 9f
        }

        data object Ratio9_16 : Fixed() {
            override val ratio = 9f / 16f
        }

        data class Custom(val width: Float, val height: Float) : Fixed() {
            init {
                require(width > 0f) { "Width must be greater than 0" }
                require(height > 0f) { "Height must be greater than 0" }
            }

            override val ratio: Float get() = width / height
        }
    }
}
