package com.nedmah.textlector.di

import com.nedmah.textlector.common.platform.file.FileReader
import com.nedmah.textlector.common.platform.tts.TtsEngineFactory
import com.nedmah.textlector.data.db.DatabaseDriverFactory
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformModule = module {
    single { DatabaseDriverFactory() }
    single<ObservableSettings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    single { FileReader() }
    single { TtsEngineFactory().create() }
}