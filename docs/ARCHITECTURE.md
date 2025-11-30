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
│   │   │   ├── managers/           # Feature-specific managers (Camera, Location, etc.)
│   │   │   │   ├── CameraManager.kt
│   │   │   │   ├── PermissionManager.kt
│   │   │   │   ├── OrientationManager.kt
│   │   │   │   ├── VideoRecordingManager.kt
│   │   │   │   ├── BurstModeManager.kt
│   │   │   │   ├── IntervalometerManager.kt
│   │   │   │   ├── ChronometerManager.kt
│   │   │   │   ├── PreferencesManager.kt
│   │   │   │   └── StorageManager.kt
│   │   │   ├── utils/                   # Utility classes
│   │   │   │   └── ImageCaptureHelper.kt # Image format conversion (JPEG/WebP)
│   │   │   ├── MainActivity.kt          # Main camera UI coordinator
│   │   │   ├── GalleryActivity.kt       # Photo/video gallery
│   │   │   ├── GalleryAdapter.kt        # Gallery RecyclerView adapter
│   │   │   ├── PreviewActivity.kt       # Photo preview after capture
│   │   │   ├── SettingsActivity.kt      # Settings screen host
│   │   │   ├── SettingsFragment.kt      # Settings preferences
│   │   │   ├── StorageSettingsFragment.kt # Storage preferences
│   │   │   ├── FilterAdapter.kt         # Filter selection adapter
│   │   │   ├── GridOverlayView.kt       # Custom grid overlay view
│   │   │   ├── LevelIndicatorView.kt    # Custom level indicator view
│   │   │   ├── ZoomableImageView.kt     # Custom zoomable image view
│   │   │   ├── ZoomableVideoLayout.kt   # Custom zoomable video container
│   │   │   └── ExifUtils.kt             # EXIF metadata utilities (JPEG/WebP)
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
│   │   │   │   ├── storage_preferences.xml # Storage settings
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
├── docs/                                # Documentation
│   ├── ARCHITECTURE.md                  # This file
│   ├── MANAGERS.md                      # Manager pattern guide
│   ├── BUILDING.md
│   ├── CAMERAX_UPGRADE.md
│   └── ...
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

## Architecture Pattern

### Manager Pattern

Kaimera uses a **Manager Pattern** to organize complex functionality into focused, reusable components. This architecture provides:

- **Separation of Concerns** - Each manager handles a specific domain
- **Testability** - Managers can be unit tested independently
- **Maintainability** - Changes are isolated to relevant managers
- **Reusability** - Managers can be used across multiple activities

#### Manager Catalog

| Manager | Responsibility |
|---------|---------------|
| **CameraManager** | Camera initialization, mode switching, capture configuration |
| **PermissionManager** | Runtime permission requests and status checking |
| **OrientationManager** | Device orientation tracking and sensor management |
| **VideoRecordingManager** | Video recording lifecycle and timer |
| **BurstModeManager** | Rapid photo capture (long-press) |
| **IntervalometerManager** | Time-lapse photography scheduling |
| **ChronometerManager** | Stopwatch with optional audio recording |
| **PreferencesManager** | Centralized settings access |
| **StorageManager** | File I/O and storage location management |

For detailed information about each manager, see [Manager Architecture Guide](MANAGERS.md).

#### MainActivity Role

`MainActivity` acts as a **coordinator** that:
- Initializes and manages manager instances
- Implements manager callback interfaces
- Handles UI updates based on manager events
- Delegates business logic to appropriate managers

**Before Refactoring**: ~1,180 lines  
**After Refactoring**: ~875 lines (26% reduction)

## Permissions

The app requires the following permissions:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="28" />
```

All permissions are requested at runtime via `PermissionManager`.

## Development

### Code Overview

**MainActivity.kt** coordinates managers and handles UI:
```kotlin
class MainActivity : AppCompatActivity(),
    IntervalometerManager.Callback,
    BurstModeManager.Callback,
    ChronometerManager.Callback,
    VideoRecordingManager.Callback {
    
    // Manager instances
    private lateinit var cameraManager: CameraManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var orientationManager: OrientationManager
    // ... other managers
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize managers
        preferencesManager = PreferencesManager(this)
        cameraManager = CameraManager(this, this, previewView, preferencesManager)
        permissionManager = PermissionManager(this) { cameraManager.startCamera() }
        
        // Request permissions
        permissionManager.checkAndRequestPermissions()
    }
}
```

**CameraManager.kt** handles camera operations:
```kotlin
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val preferencesManager: PreferencesManager
) {
    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            // Bind preview and capture use cases
        }, ContextCompat.getMainExecutor(context))
    }
    
    fun toggleCamera() { /* Switch front/back */ }
    fun getImageCapture(): ImageCapture? { /* Return instance */ }
}
```

### Adding New Features

1. **Create a new manager** if the feature is complex enough
2. **Add callback interface** for communication with MainActivity
3. **Inject dependencies** through constructor
4. **Implement lifecycle methods** if needed (start/stop)
5. **Update MainActivity** to initialize and use the manager

Example:
```kotlin
// 1. Create FilterManager.kt
class FilterManager(
    private val context: Context,
    private val callback: Callback
) {
    interface Callback {
        fun onFilterApplied(filter: Filter)
    }
    
    fun applyFilter(filter: Filter) {
        // Apply filter logic
        callback.onFilterApplied(filter)
    }
}

// 2. Update MainActivity
class MainActivity : AppCompatActivity(), FilterManager.Callback {
    private lateinit var filterManager: FilterManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        filterManager = FilterManager(this, this)
    }
    
    override fun onFilterApplied(filter: Filter) {
        // Update UI
    }
}
```

