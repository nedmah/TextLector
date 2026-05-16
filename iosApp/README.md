# iOS Frameworks

This folder contains native xcframework libraries for the iOS build.

## ⚠️ Important

The binaries are not stored in Git due to their large size (~80MB+).
After cloning the iOS repository, the build will not work until the frameworks are restored.

## How to restore

### Option 1 - from a local build (if there is a build-ios/ folder)

```bash
cd iosApp/Frameworks
cp -r build-ios/sherpa-onnx.xcframework .
cp -r build-ios/ios-onnxruntime/onnxruntime.xcframework .
```

### Option 2 - Download from GitHub releases

The current version of Sherpa ONNX is listed in `gradle/libs.versions.toml` → `sherpaOnnx`.

```bash
cd iosApp/Frameworks

curl -L -o sherpa-onnx-ios.tar.bz2 \
"https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.12.34/sherpa-onnx-v1.12.34-ios.tar.bz2"

tar -xjf sherpa-onnx-ios.tar.bz2
rm sherpa-onnx-ios.tar.bz2
```

After that, clean up DerivedData:
```bash
rm -rf ~/Library/Developer/Xcode/DerivedData
```

## SherpaOnnx.swift

`iosApp/iosApp/SherpaOnnx.swift` must match the library version.
If you encounter compilation errors after restoring the frameworks, download the correct version:

```bash
curl -L -o iosApp/iosApp/SherpaOnnx.swift \
"https://github.com/k2-fsa/sherpa-onnx/raw/v1.12.34/swift-api-examples/SherpaOnnx.swift"
```