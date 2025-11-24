# Kamerai 📸

A modern Android camera application built with Kotlin and CameraX, featuring real-time preview and one-tap photo capture.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![CameraX](https://img.shields.io/badge/CameraX-1.2.2-orange.svg)](https://developer.android.com/training/camerax)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-brightgreen.svg)](https://developer.android.com/about/versions/nougat)

## Features

- 📷 **Real-time Camera Preview** - Full-screen camera preview using CameraX
- 🎯 **One-Tap Capture** - Simple floating action button for photo capture
- 🔐 **Runtime Permissions** - Proper camera permission handling
- 💾 **Auto-Save** - Photos automatically saved with timestamps
- 📱 **Material Design** - Modern UI with Material Design components
- ⚡ **Lifecycle-Aware** - Automatic camera lifecycle management

## Screenshots

> **Note:** The app features a full-screen camera preview with a floating capture button at the bottom center.

## Quick Start

### For New Developers

```bash
# Clone the repository
git clone <your-repo-url>
cd kamerai

# Run the setup script (macOS only)
./setup.sh

# Build the app
./gradlew assembleRelease
```

The `setup.sh` script will automatically:
- ✅ Check and install Homebrew (if needed)
- ✅ Verify/install JDK 17+
- ✅ Check for Android SDK and required components
- ✅ Create `local.properties` with SDK path
- ✅ Verify Git installation

### Manual Setup

If you prefer manual setup or are on a different platform:

1. Install **JDK 17** or higher
2. Install **Android Studio** and **Android SDK**
3. Install **SDK Platform 33** and **Build Tools 33.0.0+**
4. Create `local.properties`:
   ```properties
   sdk.dir=/path/to/your/android/sdk
   ```

## Requirements

- **Android 7.0 (API 24)** or higher
- **Camera hardware** (rear camera)
- **Storage** for saving photos

## Installation

### Option 1: Download Pre-built APK

The signed APK is available at:
```
app/build/outputs/apk/release/app-release.apk
```

Install via ADB:
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### Option 2: Build from Source

```bash
# After running setup.sh (see Quick Start above)
./gradlew assembleRelease

# Or use the convenience script
sh build_app.sh
```

## Usage

1. **Launch** the Kamerai app
2. **Grant** camera permission when prompted
3. **Preview** appears automatically in full-screen
4. **Tap** the camera button (bottom center) to capture a photo
5. **Toast notification** confirms the save location

### Accessing Photos

Photos are saved to the app's private external storage:
```
/storage/emulated/0/Android/data/com.example.kamerai/files/
```

File naming format: `yyyy-MM-dd-HH-mm-ss-SSS.jpg`

## Technical Details

### Architecture

- **Language:** Kotlin
- **Min SDK:** 24 (Android 7.0 Nougat)
- **Target SDK:** 33 (Android 13)
- **Build System:** Gradle 7.5 with Kotlin DSL
- **Camera Library:** CameraX 1.2.2

### Key Dependencies

```kotlin
// CameraX
implementation("androidx.camera:camera-core:1.2.2")
implementation("androidx.camera:camera-camera2:1.2.2")
implementation("androidx.camera:camera-lifecycle:1.2.2")
implementation("androidx.camera:camera-view:1.2.2")

// UI
implementation("com.google.android.material:material:1.8.0")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")
```

### Project Structure

```
kamerai/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/kamerai/
│   │   │   └── MainActivity.kt          # Main camera implementation
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml    # UI layout
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml
│   │   │   └── mipmap-*/                # App icons
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts                 # App module config
│   └── release.keystore                 # Signing key
├── gradle/wrapper/                      # Gradle wrapper
├── build.gradle.kts                     # Root config
├── settings.gradle.kts
├── gradle.properties
├── local.properties
├── .gitignore
├── setup.sh                             # Development setup script
├── build_app.sh                         # Build convenience script
└── README.md
```

## Permissions

The app requires the following permission:

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

Camera permission is requested at runtime when the app launches.

## Git Workflow

The repository is initialized with Git and includes a comprehensive `.gitignore`.

### Initial Setup

```bash
# Already initialized with:
git init
git add -A
git commit -m "Initial commit: Kamerai camera app with CameraX"
```

### Adding Remote and Pushing

```bash
# Add your remote repository
git remote add origin <your-repo-url>

# Push to GitHub/GitLab/etc
git push -u origin main
```

### What's Tracked

- ✅ Source code (Kotlin, XML)
- ✅ Build configuration (Gradle files)
- ✅ Documentation (README, scripts)
- ✅ Resources (icons, strings, themes)
- ❌ Build artifacts (APKs, build folders)
- ❌ IDE files (.idea, *.iml)
- ❌ Local configuration (local.properties)

## Building

### Prerequisites

- **Android SDK** (API 33 installed)
- **JDK 17** (configured in `gradle.properties`)
- **Gradle 7.5** (via wrapper)

### Build Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK (signed)
./gradlew assembleRelease

# Install on connected device
./gradlew installRelease
```

### Signing Configuration

The release APK is automatically signed with:
- **Keystore:** `app/release.keystore`
- **Password:** `password`
- **Alias:** `key0`

> ⚠️ **Warning:** This is a demo keystore. For production, generate a new keystore with a strong password.

## Development

### Code Overview

**MainActivity.kt** implements:
- Runtime permission handling using `ActivityResultContracts`
- CameraX initialization with `ProcessCameraProvider`
- Camera preview binding to `PreviewView`
- Photo capture using `ImageCapture` use case
- File I/O for saving captured images

### Key Components

```kotlin
// Permission launcher
private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted -> ... }

// Camera initialization
private fun startCamera() {
    val cameraProvider = ProcessCameraProvider.getInstance(this)
    // Bind preview and image capture use cases
}

// Photo capture
private fun takePhoto() {
    imageCapture.takePicture(outputOptions, executor, callback)
}
```

## Troubleshooting

### Camera Not Working

- Ensure camera permission is granted: **Settings → Apps → Kamerai → Permissions**
- Verify device has a working rear camera
- Check Android version is 7.0 or higher

### Build Failures

```bash
# Stop Gradle daemon
./gradlew --stop

# Clean and rebuild
./gradlew clean assembleRelease
```

### Photos Not Saving

- Check device storage space
- Verify app has storage access (automatic for external files directory)
- Check logcat for error messages: `adb logcat | grep Kamerai`

## Future Enhancements

- [ ] Front/back camera switching
- [ ] Flash toggle
- [ ] In-app photo gallery
- [ ] Real-time filters
- [ ] Video recording
- [ ] Pinch-to-zoom
- [ ] Grid overlay
- [ ] Timer/burst mode

## Publishing to Google Play

### Required Assets

1. **High-res icon** - 512x512 PNG (replace placeholder icons)
2. **Screenshots** - Phone and tablet screenshots
3. **Feature graphic** - 1024x500 promotional banner
4. **Privacy policy** - Required for camera permission

### Steps

1. Create app in [Google Play Console](https://play.google.com/console)
2. Upload `app-release.apk`
3. Complete store listing
4. Submit for review

## License

This project is provided as-is for educational and demonstration purposes.

## Contributing

This is a demonstration project. Feel free to fork and modify for your own use.

## Support

For issues or questions about the implementation, refer to:
- [CameraX Documentation](https://developer.android.com/training/camerax)
- [Android Developer Guide](https://developer.android.com/guide)
- [Material Design Components](https://material.io/develop/android)

## Acknowledgments

- Built with [CameraX](https://developer.android.com/training/camerax)
- UI components from [Material Design](https://material.io)
- Developed with [Android Studio](https://developer.android.com/studio)

---

**Made with ❤️ using Kotlin and CameraX**
