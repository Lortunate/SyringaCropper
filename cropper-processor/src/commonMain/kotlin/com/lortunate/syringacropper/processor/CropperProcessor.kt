package com.lortunate.syringacropper.processor

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import com.lortunate.syringacropper.perspective.PerspectiveQuad

expect object CropperProcessor {
    val supportMessage: String?
    fun perspectiveWarp(image: ImageBitmap, quad: PerspectiveQuad, tw: Int, th: Int): ImageBitmap
    fun rectCrop(image: ImageBitmap, rect: Rect): ImageBitmap
    fun circleCrop(image: ImageBitmap, rect: Rect): ImageBitmap
    fun roundedRectCrop(image: ImageBitmap, rect: Rect, radius: Float): ImageBitmap
    fun resize(image: ImageBitmap, tw: Int, th: Int): ImageBitmap
}

val CropperProcessor.isSupported: Boolean
    get() = supportMessage == null

fun ImageBitmap.perspectiveWarp(quad: PerspectiveQuad, tw: Int, th: Int): ImageBitmap {
    return CropperProcessor.perspectiveWarp(this, quad, tw, th)
}

fun ImageBitmap.rectCrop(rect: Rect): ImageBitmap {
    return CropperProcessor.rectCrop(this, rect)
}

fun ImageBitmap.circleCrop(rect: Rect): ImageBitmap {
    return CropperProcessor.circleCrop(this, rect)
}

fun ImageBitmap.roundedRectCrop(rect: Rect, radius: Float): ImageBitmap {
    return CropperProcessor.roundedRectCrop(this, rect, radius)
}

fun ImageBitmap.resize(tw: Int, th: Int): ImageBitmap {
    return CropperProcessor.resize(this, tw, th)
}
