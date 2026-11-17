package com.nedmah.textlector.di

import android.content.Context
import com.nedmah.textlector.common.platform.review.StoreReviewRequester
import com.nedmah.textlector.common.platform.file.FileReader
import com.nedmah.textlector.common.platform.ocr.AndroidOcrEngine
import com.nedmah.textlector.common.platform.ocr.OcrEngine
import com.nedmah.textlector.common.platform.review.ReviewRequester
import com.nedmah.textlector.common.platform.tts.AndroidSherpaOnnxTtsEngine
import com.nedmah.textlector.common.platform.tts.AndroidSupertonicTtsEngine
import com.nedmah.textlector.common.platform.tts.AndroidTtsEngine
import com.nedmah.textlector.common.platform.tts.SwitchableTtsEngine
import com.nedmah.textlector.common.platform.tts.TtsEngine
import com.nedmah.textlector.data.db.DatabaseDriverFactory
import com.nedmah.textlector.data.repository.AndroidOcrDataRepository
import com.nedmah.textlector.data.repository.FileSupertonicRepository
import com.nedmah.textlector.data.repository.FileVoiceModelRepository
import com.nedmah.textlector.domain.repository.OcrDataRepository
import com.nedmah.textlector.domain.repository.SupertonicRepository
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
    single<ReviewRequester> { StoreReviewRequester(androidContext()) }

    // repository
    single<OcrDataRepository> { AndroidOcrDataRepository(get()) }
    single<VoiceModelRepository> { FileVoiceModelRepository(androidContext().filesDir) }
    single<SupertonicRepository> { FileSupertonicRepository(androidContext().filesDir) }

    // TTS engines
    single { AndroidTtsEngine(androidContext()) }
    single { AndroidSherpaOnnxTtsEngine(get()) }
    single { AndroidSupertonicTtsEngine(get()) }
    single<TtsEngine> {
        SwitchableTtsEngine(
            nativeEngine = get<AndroidTtsEngine>(),
            sherpaEngine = get<AndroidSherpaOnnxTtsEngine>(),
            supertonicEngine = get<AndroidSupertonicTtsEngine>(),
            preferencesRepository = get()
        )
    }

}