package com.nedmah.textlector

import androidx.compose.ui.window.ComposeUIViewController
import com.nedmah.textlector.di.dataModule
import com.nedmah.textlector.di.platformModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController { App() }

fun initKoin() {
    startKoin {
        modules(platformModule, dataModule)
    }
}