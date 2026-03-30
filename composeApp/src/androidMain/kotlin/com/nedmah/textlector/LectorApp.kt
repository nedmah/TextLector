package com.nedmah.textlector

import android.app.Application
import com.nedmah.textlector.di.dataModule
import com.nedmah.textlector.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LectorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LectorApp)
            modules(platformModule, dataModule)
        }
    }
}