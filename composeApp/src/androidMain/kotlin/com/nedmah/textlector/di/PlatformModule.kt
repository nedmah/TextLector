package com.nedmah.textlector.di

import android.content.Context
import com.nedmah.textlector.common.platform.FileReader
import com.nedmah.textlector.data.db.DatabaseDriverFactory
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseDriverFactory(androidContext()) }

    single<ObservableSettings>{
        val prefs = androidContext().getSharedPreferences("lector_prefs", Context.MODE_PRIVATE)
        SharedPreferencesSettings(prefs)
    }

    single { FileReader(androidContext()) }
}