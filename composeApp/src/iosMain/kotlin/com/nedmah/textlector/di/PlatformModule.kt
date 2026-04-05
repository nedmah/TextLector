package com.nedmah.textlector.di

import com.nedmah.textlector.common.platform.file.FileReader
import com.nedmah.textlector.common.platform.tts.IosTtsEngine
import com.nedmah.textlector.common.platform.tts.TtsEngine
import com.nedmah.textlector.data.db.DatabaseDriverFactory
import com.nedmah.textlector.data.repository.IosVoiceModelRepositoryImpl
import com.nedmah.textlector.domain.repository.VoiceModelRepository
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformModule = module {
    single { DatabaseDriverFactory() }
    single<ObservableSettings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    single { FileReader() }
    // iOS temporary ios engine directly
    // TODO: IosSherpaOnnxTtsEngine
    single<TtsEngine> { IosTtsEngine() }
    single<VoiceModelRepository> { IosVoiceModelRepositoryImpl() }
}