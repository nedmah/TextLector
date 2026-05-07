package com.nedmah.textlector.di

import android.content.Context
import com.nedmah.textlector.common.platform.file.FileReader
import com.nedmah.textlector.common.platform.ocr.AndroidOcrEngine
import com.nedmah.textlector.common.platform.ocr.OcrEngine
import com.nedmah.textlector.common.platform.tts.AndroidSherpaOnnxTtsEngine
import com.nedmah.textlector.common.platform.tts.AndroidTtsEngine
import com.nedmah.textlector.common.platform.tts.SwitchableTtsEngine
import com.nedmah.textlector.common.platform.tts.TtsEngine
import com.nedmah.textlector.data.db.DatabaseDriverFactory
import com.nedmah.textlector.data.repository.AndroidOcrDataRepository
import com.nedmah.textlector.data.repository.AndroidVoiceModelRepositoryImpl
import com.nedmah.textlector.domain.repository.OcrDataRepository
import com.nedmah.textlector.domain.repository.VoiceModelRepository
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
    single<OcrEngine> { AndroidOcrEngine(get(), get(), get()) }

    // repository
    single<OcrDataRepository> { AndroidOcrDataRepository(get()) }
    single<VoiceModelRepository> { AndroidVoiceModelRepositoryImpl(androidContext()) }

    // TTS engines
    single { AndroidTtsEngine(androidContext()) }
    single { AndroidSherpaOnnxTtsEngine(get()) }
    single<TtsEngine> {
        SwitchableTtsEngine(
            nativeEngine = get<AndroidTtsEngine>(),
            sherpaEngine = get<AndroidSherpaOnnxTtsEngine>(),
            preferencesRepository = get()
        )
    }

}