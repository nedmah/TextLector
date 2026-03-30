package com.nedmah.textlector

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.nedmah.textlector.di.dataModule
import com.nedmah.textlector.di.platformModule
import org.koin.core.context.GlobalContext.startKoin

fun main() = application {

    startKoin {
        modules(platformModule, dataModule)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "TextLector",
    ) {
        App()
    }
}