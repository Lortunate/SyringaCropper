## SyringaCropper

This repository contains the cropper libraries plus a multiplatform sample app.

### Modules

- `cropper`: Compose Multiplatform cropper UI library
- `cropper-processor`: image processing backend used by the croppers
- `example`: shared KMP sample app module
- `androidApp`: Android application entrypoint for the sample app

### Android sample app

Build debug APK:

```bash
./gradlew :androidApp:assembleDebug
```

Install to a connected device:

```bash
./gradlew :androidApp:installDebug
```

APK output:

```text
androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### Shared sample module

Build the desktop/JVM artifact for the shared sample module:

```bash
./gradlew :example:jvmJar
```
