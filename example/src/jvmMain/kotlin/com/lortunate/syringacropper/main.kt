package com.lortunate.syringacropper

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.vinceglb.filekit.FileKit

fun main() = application {
    FileKit.init(appId = "SyringaCropper")
    Window(
        onCloseRequest = ::exitApplication,
        title = "SyringaCropper",
    ) {
        App()
    }
}
