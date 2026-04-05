package com.nedmah.textlector.di

import com.nedmah.textlector.common.platform.file.FileReader
import com.nedmah.textlector.data.db.DatabaseDriverFactory
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings
import org.koin.dsl.module
import java.util.prefs.Preferences

actual val platformModule = module {
    single { DatabaseDriverFactory() }
    single<ObservableSettings> { PreferencesSettings(Preferences.userRoot()) }
    single { FileReader() }
}