# TextLector

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-4285F4?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/compose-multiplatform/)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20macOS%20%7C%20Windows-green.svg)]()
[![Offline](https://img.shields.io/badge/Offline-100%25-brightgreen)]()
[![Latest Release](https://img.shields.io/github/v/release/nedmah/TextLector)](https://github.com/nedmah/TextLector/releases)

A free, offline text-to-speech reader for Android, iOS, macOS and Windows, built with Kotlin Multiplatform and Compose Multiplatform.

> Open-source alternative to Speechify — no subscriptions, no internet required, no data leaves your device.

---

## Download

| Platform | Where                                                                                 |
|----------|---------------------------------------------------------------------------------------|
| Android | [RuStore](https://www.rustore.ru/catalog/app/com.nedmah.textlector) · APK on [GitHub Releases](https://github.com/nedmah/TextLector/releases) |
| macOS (Apple Silicon) | `.dmg` on [GitHub Releases](https://github.com/nedmah/TextLector/releases)            |
| Windows (x64) | `.msi` on [GitHub Releases](https://github.com/nedmah/TextLector/releases)            |
| iOS | build from source (see [Building](#ios))                                              |

The desktop builds are not signed, so the OS will warn you on first launch:

- **macOS** — open the app once, then go to *System Settings → Privacy & Security* and click **Open Anyway**.
- **Windows** — if SmartScreen appears, click **More info → Run anyway**.

---

## Demo

https://github.com/user-attachments/assets/0756bb22-88df-4ab7-adac-295487bf5d66

---

## Screenshots

<p float="left">
  <img src="screenshots/library.png" width="200"/>
  <img src="screenshots/reader.png" width="200"/>
  <img src="screenshots/settings.png" width="200"/>
</p>

---

## Features

- **Import** — PDF, TXT (UTF-8 / windows-1251), EPUB, FB2 (incl. `.fb2.zip`), URL, camera (OCR), or paste text manually
- **Offline TTS** — three engines: system voices, Piper (neural, ~63 MB per voice), Supertonic (highest quality, ~145 MB)
- **Paragraph highlighting** — current paragraph highlighted in sync with playback
- **Reading progress** — app remembers where you stopped in every document
- **Library** — manage documents, mark favorites, sort by date
- **Adjustable playback** — speed control (0.5x–2.0x), voice gender, language, font size
- **Switch engines at runtime** — no app restart required
- **Desktop app** — adaptive layout for wide windows with side navigation and a centered reader

On desktop, system TTS and camera import are not available — Piper is the default engine.

---

## TTS Architecture

TextLector uses a three-tier TTS system managed by a single `SwitchableTtsEngine`.

**Tier 1 — System TTS (Android / iOS)**
Android `TextToSpeech` and iOS `AVSpeechSynthesizer` — available immediately, no downloads.

**Tier 2 — Neural TTS via Piper / sherpa-onnx**
[sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) bundles ONNX Runtime and eSpeak-NG. Piper VITS models (~63 MB each) are downloaded on demand from HuggingFace and stored locally.

| Voice  | Language | Gender |
|--------|----------|--------|
| Ruslan | Russian  | Male   |
| Irina  | Russian  | Female |
| Ryan   | English  | Male   |
| Lessac | English  | Female |

**Tier 3 — Neural TTS via Supertonic / sherpa-onnx**
[Supertonic 3](https://github.com/supertone-inc/supertonic) by Supertone Inc., run through sherpa-onnx's built-in Supertonic support. Flow-matching synthesis with 10 voices (M1–M5, F1–F5); one multilingual model serves both Russian and English. The int8 model (~145 MB) is downloaded once from HuggingFace, file by file (interrupted downloads resume), then works fully offline.

Compared to Piper: higher naturalness, but slower generation on mobile CPU. `SupertonicModel.NUM_STEPS` trades quality for speed.

Both ONNX-based engines use a shared `TtsQueue` that prefetches the next paragraph in the background while the current one plays.

On desktop, the same models run through the sherpa-onnx JVM library and audio is played with `javax.sound`.

---

## Tech Stack

| Layer         | Technology                                                 |
|---------------|------------------------------------------------------------|
| UI            | Compose Multiplatform (Android, iOS, Desktop)              |
| Architecture  | MVI + ViewModel (commonMain)                               |
| DI            | Koin Multiplatform                                         |
| Navigation    | Navigation Compose CMP (typesafe routes)                   |
| Database      | SQLDelight 2.x                                             |
| Preferences   | multiplatform-settings                                     |
| File I/O      | okio, kmp-zip                                              |
| HTTP          | Ktor Client 3.x                                            |
| HTML parsing  | Ksoup (Jsoup KMP port)                                     |
| Neural TTS    | sherpa-onnx 1.13.8 (Piper VITS + Supertonic 3)             |
| PDF (Android) | PdfBox-Android                                             |
| PDF (iOS)     | PDFKit + Vision OCR fallback                               |
| PDF (Desktop) | Apache PDFBox                                              |
| OCR           | Tesseract4Android / Vision (iOS)                           |

---

## Project Structure

```
composeApp/src/
├── commonMain/          # shared UI, ViewModels, domain, data
│   ├── domain/          # models, repository interfaces, use cases, parsers
│   ├── data/            # SQLDelight, repository implementations
│   ├── ui/              # Compose screens and components
│   └── common/platform  # TTS engines interfaces, TtsQueue, FileReader, OCR, review
├── jvmAndAndroidMain/   # java.io code shared by Android and Desktop: downloads, model repositories, WAV
├── androidMain/         # Android actuals: sherpa-onnx engines, OCR, PDF
├── androidRustore/      # RuStore flavor: in-app review via RuStore SDK
├── androidPlay/         # Google Play flavor: no-op review
├── iosMain/             # iOS actuals
└── jvmMain/             # Desktop actuals: sherpa-onnx JVM engines, audio, file dialogs, app data dir

iosApp/iosApp/           # Swift: IosSherpaEngine, IosSupertonicEngine, SherpaOnnxTtsBridge,
                         # PdfTextExtractor, IosOcrEngine, IosFileDownloader, IosTarExtractor
```

---

## Building

### Prerequisites

- Android Studio (recent stable)
- JDK 21 — for desktop packaging use a vendor build (Temurin, Corretto); Homebrew's OpenJDK is rejected by the packaging check
- Xcode 16+ (iOS only)

### Local setup

These files are not in the repository:

- `local.properties` in the project root (optional for debug and desktop builds):
  ```properties
  sdk.dir=/path/to/Android/sdk
  KEYSTORE_PASSWORD=...
  KEY_PASSWORD=...
  ```
  The signing keys are only needed for signed Android release builds.
- `composeApp/google-services.json` — Firebase config (Crashlytics), Android only.

### Android

The app has two product flavors: `rustore` (with RuStore in-app review) and `play`.

```bash
./gradlew :composeApp:assembleRustoreDebug
./gradlew :composeApp:assembleRustoreRelease   # signed, needs textlector.jks
./gradlew :composeApp:assemblePlayRelease
./gradlew :composeApp:bundlePlayRelease        # AAB for Google Play
```

The sherpa-onnx Android library is a local AAR in `composeApp/libs/`
(`sherpa-onnx-static-link-onnxruntime-<version>.aar` from [sherpa-onnx releases](https://github.com/k2-fsa/sherpa-onnx/releases)).

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run on a device. sherpa-onnx is pulled in via Swift Package Manager — see [iosApp/README.md](iosApp/README.md).

### Desktop

Supported: macOS on Apple Silicon and Windows x64.

```bash
./gradlew :composeApp:run           # run from sources
./gradlew :composeApp:packageDmg    # macOS → composeApp/build/compose/binaries/main/dmg/
./gradlew :composeApp:packageMsi    # Windows → composeApp/build/compose/binaries/main/msi/
```

Each installer must be built on its own OS. The sherpa-onnx JVM library and its native libraries are in `composeApp/libs/desktop/`
(`sherpa-onnx-jvm-<version>.jar` and `sherpa-onnx-native-lib-<os>-<version>.jar`); the right native jar is picked for the build machine.

The [Desktop build](.github/workflows/desktop.yml) workflow builds both installers on GitHub Actions.

App data (database, voice models, log) is stored in `~/Library/Application Support/TextLector` on macOS and `%APPDATA%\TextLector` on Windows.

### Shared KMP module

```bash
./gradlew :composeApp:compileKotlinIosArm64
./gradlew :composeApp:jvmTest
```

---

## Roadmap

- [x] PDF, TXT, EPUB, FB2, manual text import
- [x] URL import
- [x] Native TTS (Android + iOS)
- [x] Neural TTS via Piper / sherpa-onnx (Android + iOS)
- [x] Camera / OCR import
- [x] Neural TTS via Supertonic — flow-matching, RU + EN
- [x] Desktop app (macOS + Windows)
- [ ] Background playback (MediaSession / AVAudioSession)
- [ ] Desktop: drag & drop import, keyboard shortcuts, OCR for scanned PDFs

---

## License

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)