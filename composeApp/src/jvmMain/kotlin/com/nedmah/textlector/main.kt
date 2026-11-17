package com.nedmah.textlector

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.nedmah.textlector.common.platform.tts.SherpaNativeLoader
import com.nedmah.textlector.di.dataModule
import com.nedmah.textlector.di.platformModule
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.GlobalContext.startKoin
import textlector.composeapp.generated.resources.Res
import textlector.composeapp.generated.resources.app_icon

fun main() = application {

    startKoin {
        modules(platformModule, dataModule)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "TextLector",
        icon = painterResource(Res.drawable.app_icon),
        state = rememberWindowState(size = DpSize(1000.dp, 750.dp)),
    ) {
        App()
    }
}