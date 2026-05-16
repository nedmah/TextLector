package com.nedmah.textlector.di

import com.nedmah.textlector.common.platform.file.FileReader
import com.nedmah.textlector.common.platform.ocr.OcrEngine
import com.nedmah.textlector.common.platform.tts.IosTtsEngine
import com.nedmah.textlector.common.platform.tts.SwitchableTtsEngine
import com.nedmah.textlector.common.platform.tts.TtsEngine
import com.nedmah.textlector.data.db.DatabaseDriverFactory
import com.nedmah.textlector.data.repository.IosOcrDataRepository
import com.nedmah.textlector.data.repository.IosVoiceModelRepositoryImpl
import com.nedmah.textlector.domain.repository.OcrDataRepository
import com.nedmah.textlector.domain.repository.VoiceModelRepository
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformModule = module {
    single { DatabaseDriverFactory() }
    single<ObservableSettings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    single { FileReader() }
    single<VoiceModelRepository> { IosVoiceModelRepositoryImpl() }
    single<TtsEngine> {
        SwitchableTtsEngine(
            nativeEngine = IosTtsEngine(),
            sherpaEngine = IosEngineHolder.sherpaEngine ?: error("SherpaEngine not initialized"),
            preferencesRepository = get()
        )
    }
    single<OcrEngine> {
        IosEngineHolder.ocrEngine ?: error("OcrEngine not initialized")
    }
    single<OcrDataRepository> { IosOcrDataRepository() }
}