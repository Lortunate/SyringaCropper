@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@file:JsModule("./cropper_processor_bridge.js")

package com.lortunate.syringacropper.processor

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsName("perspectiveWarp")
internal external fun perspectiveWarpBytes(
    input: JsAny,
    w: Int,
    h: Int,
    corners: JsAny,
    tw: Int,
    th: Int,
): JsAny?

@OptIn(ExperimentalWasmJsInterop::class)
@JsName("rectCrop")
internal external fun rectCropBytes(
    input: JsAny,
    sw: Int,
    sh: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
): JsAny?

@OptIn(ExperimentalWasmJsInterop::class)
@JsName("circleCrop")
internal external fun circleCropBytes(
    input: JsAny,
    sw: Int,
    sh: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
): JsAny?

@OptIn(ExperimentalWasmJsInterop::class)
@JsName("roundedRectCrop")
internal external fun roundedRectCropBytes(
    input: JsAny,
    sw: Int,
    sh: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    r: Float,
): JsAny?

@OptIn(ExperimentalWasmJsInterop::class)
@JsName("resizeImage")
internal external fun resizeBytes(
    input: JsAny,
    sw: Int,
    sh: Int,
    dw: Int,
    dh: Int,
): JsAny?
