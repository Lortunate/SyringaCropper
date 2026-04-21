@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@file:JsModule("./cropper_processor_bridge.js")

package com.lortunate.syringacropper.processor

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsName("process")
internal external fun processBytes(
    input: JsAny,
    w: Int,
    h: Int,
    types: JsAny,
    params: JsAny,
): JsAny?
