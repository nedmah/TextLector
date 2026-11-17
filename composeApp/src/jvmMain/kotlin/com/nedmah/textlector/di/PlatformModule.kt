package com.nedmah.textlector.di

import com.nedmah.textlector.common.platform.file.AppDirs
import com.nedmah.textlector.common.platform.file.FileReader
import com.nedmah.textlector.common.platform.ocr.OcrEngine
import com.nedmah.textlector.common.platform.review.DesktopReviewRequester
import com.nedmah.textlector.common.platform.review.ReviewRequester
import com.nedmah.textlector.common.platform.tts.DesktopPiperTtsEngine
import com.nedmah.textlector.common.platform.tts.SwitchableTtsEngine
import com.nedmah.textlector.common.platform.tts.TtsEngine
import com.nedmah.textlector.data.db.DatabaseDriverFactory
import com.nedmah.textlector.data.repository.FileSupertonicRepository
import com.nedmah.textlector.data.repository.FileVoiceModelRepository
import com.nedmah.textlector.domain.repository.OcrDataRepository
import com.nedmah.textlector.domain.repository.SupertonicRepository
import com.nedmah.textlector.domain.repository.VoiceModelRepository
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings
import org.koin.dsl.module
import java.util.prefs.Preferences

actual val platformModule = module {
    single { DatabaseDriverFactory() }
    single<ObservableSettings> {
        PreferencesSettings(Preferences.userRoot().node("com/nedmah/textlector"))
    }
    single { FileReader() }

    single<OcrEngine> { StubOcrEngine() }
    single<ReviewRequester> { DesktopReviewRequester() }

    // repository
    single<OcrDataRepository> { StubOcrDataRepository() }
    single<VoiceModelRepository> { FileVoiceModelRepository(AppDirs.dataDir) }
    single<SupertonicRepository> { FileSupertonicRepository(AppDirs.dataDir) }

    // TTS engines
    single { DesktopPiperTtsEngine(get()) }
    single<TtsEngine> {
        SwitchableTtsEngine(
            nativeEngine = SilentTtsEngine(),
            sherpaEngine = get<DesktopPiperTtsEngine>(),
            supertonicEngine = SilentTtsEngine(),
            preferencesRepository = get(),
        )
    }
}