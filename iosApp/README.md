# iOS app

## Dependencies

Native dependencies come from **Swift Package Manager** — no manual frameworks to restore.
Xcode resolves them automatically on first open (the sherpa-onnx repository is large, so the
first resolution can take a while).

| Package    | URL                                     | Rule                   | Product                |
|------------|-----------------------------------------|------------------------|------------------------|
| sherpa-onnx | `https://github.com/k2-fsa/sherpa-onnx` | Exact Version `1.13.8` | `sherpa-onnx` (static) |

The package ships its own ONNX Runtime (via `csukuangfj/onnxruntime-libs`) and the Swift wrapper
(`import SherpaOnnx`) built from the same release, so the Swift API always matches the binary.

Don't add another ONNX Runtime package (e.g. `microsoft/onnxruntime-swift-package-manager`) —
two ONNX Runtimes in one binary will clash at link time.

### Updating sherpa-onnx

Keep one version on both platforms:

1. Xcode → project `iosApp` → Package Dependencies → `sherpa-onnx` → set the new exact version.
2. Android: replace `composeApp/libs/sherpa-onnx-static-link-onnxruntime-<version>.aar`.
3. Update `sherpaOnnx` in `gradle/libs.versions.toml`.

## Building

The KMP framework is built by the **Compile Kotlin Framework** build phase
(`./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`), so just press Run.

- The target is built for `arm64` only — run on a real device.
- After changing dependencies or switching branches: Product → **Clean Build Folder** (⇧⌘K).
  If that's not enough: `rm -rf ~/Library/Developer/Xcode/DerivedData`.

The bridging header (`iosApp-Bridging-Header.h`) is only needed for `Bz2Wrapper`
(bzip2, used to unpack `espeak-ng-data.tar.bz2` for Piper).

## Native engines

Swift classes implement Kotlin interfaces and are registered in `IosEngineHolder` in `iOSApp.swift`
before Koin starts:

- `IosSherpaEngine` + `SherpaOnnxTtsBridge` — Piper (VITS) voices, WAV playback, downloads
- `IosSupertonicEngine` — Supertonic 3 via sherpa-onnx; all native calls go through one serial queue
- `IosOcrEngine`, `PdfTextExtractor`, `IosFileDownloader`, `IosTarExtractor`

`IosSupertonicRepository` is created in `iOSApp.swift` and shared with Koin through
`IosEngineHolder.supertonicRepository`, so the engine and the settings screen see the same download state.

## Model storage

All models live in the app's `Documents` directory:

```
Documents/tts_models/espeak-ng-data/     # shared by all Piper voices
Documents/tts_models/vits-piper-*/       # Piper voices (.onnx + tokens.txt)
Documents/supertonic3/                   # Supertonic 3 int8 model (7 files)
```

`Documents/supertonic/` (the old supertonic-kmp model, app ≤ 1.3.0) is deleted automatically on start.

> ⚠️ iOS changes the app's `Documents` path (container UUID) on every reinstall. Paths are always
> resolved at runtime — never hardcode them while debugging.