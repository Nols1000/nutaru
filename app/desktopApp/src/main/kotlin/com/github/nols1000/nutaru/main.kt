package com.github.nols1000.nutaru

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Nutaru",
    ) {
        App()
    }
}