# Architecture & Development Guide

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
kaimera/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/kaimera/
│   │   │   ├── MainActivity.kt          # Main camera implementation
│   │   │   ├── GalleryActivity.kt       # Photo/video gallery
│   │   │   ├── GalleryAdapter.kt        # Gallery RecyclerView adapter
│   │   │   ├── PreviewActivity.kt       # Photo preview after capture
│   │   │   ├── SettingsActivity.kt      # Settings screen host
│   │   │   ├── SettingsFragment.kt      # Settings preferences
│   │   │   ├── FilterAdapter.kt         # Filter selection adapter
│   │   │   ├── GridOverlayView.kt       # Custom grid overlay view
│   │   │   ├── ZoomableImageView.kt     # Custom zoomable image view
│   │   │   └── ZoomableVideoLayout.kt   # Custom zoomable video container
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml    # Main camera UI
│   │   │   │   ├── activity_gallery.xml # Gallery layout
│   │   │   │   ├── activity_preview.xml # Preview layout
│   │   │   │   ├── activity_settings.xml # Settings layout
│   │   │   │   ├── item_gallery_image.xml # Gallery item
│   │   │   │   └── item_filter.xml      # Filter item
│   │   │   ├── xml/
│   │   │   │   ├── root_preferences.xml # Settings preferences
│   │   │   │   ├── backup_rules.xml
│   │   │   │   └── data_extraction_rules.xml
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

The app requires the following permissions:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

Both permissions are requested at runtime when the app launches. RECORD_AUDIO is required for video recording with audio.

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
