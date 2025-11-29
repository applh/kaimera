# Manager Architecture Guide

## Overview

Kaimera uses a **Manager Pattern** to organize complex functionality into focused, reusable components. Each manager encapsulates a specific domain of functionality, making the codebase more maintainable, testable, and modular.

## Design Principles

### Separation of Concerns
Each manager handles a single responsibility:
- **CameraManager** → Camera initialization and configuration
- **PermissionManager** → Runtime permissions
- **OrientationManager** → Device orientation and sensors
- **VideoRecordingManager** → Video recording lifecycle
- **BurstModeManager** → Rapid photo capture
- **IntervalometerManager** → Time-lapse photography
- **ChronometerManager** → Stopwatch functionality
- **PreferencesManager** → Centralized settings access
- **StorageManager** → File I/O operations

### Dependency Injection
Managers receive dependencies through constructor parameters, enabling:
- **Testability** - Easy to mock dependencies in unit tests
- **Flexibility** - Dependencies can be swapped or configured
- **Clarity** - Explicit dependencies are visible in the constructor

### Callback Pattern
Managers communicate with `MainActivity` through callbacks:
- **Decoupling** - Managers don't need direct references to activities
- **Lifecycle Safety** - Callbacks can be updated when activity recreates
- **Flexibility** - Multiple listeners can be registered if needed

## Manager Catalog

### CameraManager

**Location**: `app/src/main/java/com/example/kaimera/managers/CameraManager.kt`

**Purpose**: Manages all camera-related operations including initialization, mode switching, and capture configuration.

**Dependencies**:
```kotlin
CameraManager(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    preferencesManager: PreferencesManager
)
```

**Key Methods**:
- `startCamera()` - Initializes camera with current settings from preferences
- `toggleCamera()` - Switches between front/back cameras
- `setCaptureMode(mode: CaptureMode)` - Switches between photo/video modes
- `getImageCapture()` - Returns ImageCapture instance for photo operations
- `getVideoCapture()` - Returns VideoCapture instance for video operations
- `getCameraProvider()` - Returns ProcessCameraProvider for advanced use

**Features**:
- Automatic preference reading (quality, flash, resolution, frame rate)
- High-speed video support (120fps) with Camera2Interop
- Pinch-to-zoom gesture handling
- Target rotation updates for proper image orientation

**Usage Example**:
```kotlin
cameraManager = CameraManager(
    context = this,
    lifecycleOwner = this,
    previewView = previewView,
    preferencesManager = preferencesManager
)
cameraManager.startCamera()
```

---

### PermissionManager

**Location**: `app/src/main/java/com/example/kaimera/managers/PermissionManager.kt`

**Purpose**: Handles runtime permission requests and status checking.

**Dependencies**:
```kotlin
PermissionManager(
    activity: ComponentActivity,
    onCameraPermissionGranted: () -> Unit
)
```

**Required Permissions**:
- `CAMERA` - Required for camera access
- `RECORD_AUDIO` - Required for video recording with audio
- `WRITE_EXTERNAL_STORAGE` - Required on Android 9 and below

**Key Methods**:
- `checkAndRequestPermissions()` - Initiates permission request flow
- `allPermissionsGranted()` - Returns true if all required permissions are granted

**Usage Example**:
```kotlin
permissionManager = PermissionManager(this) {
    cameraManager.startCamera()
}
permissionManager.checkAndRequestPermissions()
```

---

### OrientationManager

**Location**: `app/src/main/java/com/example/kaimera/managers/OrientationManager.kt`

**Purpose**: Manages device orientation tracking and sensor data for camera rotation and level indicator.

**Dependencies**:
```kotlin
OrientationManager(
    context: Context,
    levelIndicator: LevelIndicatorView,
    preferencesManager: PreferencesManager,
    onRotationChanged: (Int) -> Unit
)
```

**Sensors Used**:
- `TYPE_ACCELEROMETER` - For tilt detection
- `TYPE_MAGNETIC_FIELD` - For compass orientation
- `OrientationEventListener` - For screen rotation (0°, 90°, 180°, 270°)

**Key Methods**:
- `start()` - Registers sensor listeners and enables orientation tracking
- `stop()` - Unregisters all listeners (call in `onPause()`)
- `refreshLevelIndicatorVisibility()` - Updates visibility based on preferences

**Usage Example**:
```kotlin
orientationManager = OrientationManager(
    context = this,
    levelIndicator = levelIndicator,
    preferencesManager = preferencesManager
) { rotation ->
    cameraManager.getImageCapture()?.targetRotation = rotation
    cameraManager.getVideoCapture()?.targetRotation = rotation
}
orientationManager.start()
```

---

### VideoRecordingManager

**Location**: `app/src/main/java/com/example/kaimera/managers/VideoRecordingManager.kt`

**Purpose**: Manages video recording lifecycle, including start/stop and recording timer.

**Dependencies**:
```kotlin
VideoRecordingManager(
    context: Context,
    getVideoCapture: () -> VideoCapture<Recorder>?,
    handler: Handler
)
```

**Callback Interface**:
```kotlin
interface Callback {
    fun onRecordingStarted()
    fun onRecordingStopped(fileName: String, success: Boolean)
    fun onTimerUpdate(minutes: Int, seconds: Int)
    fun onError(message: String)
}
```

**Key Methods**:
- `startRecording(activity: ComponentActivity)` - Begins video recording
- `stopRecording()` - Stops recording and saves file
- `isRecording()` - Returns current recording state

**Features**:
- Automatic file naming with timestamps
- Recording duration timer with minute:second format
- MediaStore integration for gallery visibility
- Error handling with user-friendly messages

---

### BurstModeManager

**Location**: `app/src/main/java/com/example/kaimera/managers/BurstModeManager.kt`

**Purpose**: Handles rapid photo capture for burst mode (long-press).

**Dependencies**:
```kotlin
BurstModeManager(
    context: Context,
    getImageCapture: () -> ImageCapture?,
    handler: Handler,
    maxCount: Int = 20,
    interval: Long = 200
)
```

**Callback Interface**:
```kotlin
interface Callback {
    fun onBurstStarted()
    fun onBurstProgress(count: Int)
    fun onBurstCompleted(count: Int)
}
```

**Configuration**:
- `maxCount` - Maximum photos per burst (default: 20)
- `interval` - Delay between captures in milliseconds (default: 200ms)

---

### IntervalometerManager

**Location**: `app/src/main/java/com/example/kaimera/managers/IntervalometerManager.kt`

**Purpose**: Manages time-lapse photography with scheduled captures.

**Dependencies**:
```kotlin
IntervalometerManager(
    context: Context,
    getCameraProvider: () -> ProcessCameraProvider?,
    getImageCapture: () -> ImageCapture?,
    restartCamera: () -> Unit,
    handler: Handler
)
```

**Callback Interface**:
```kotlin
interface Callback {
    fun onCountdownUpdate(seconds: Int)
    fun onIntervalStarted()
    fun onIntervalProgress(count: Int, total: Int)
    fun onIntervalCompleted(count: Int)
    fun onSleepModeUpdate(isAsleep: Boolean, nextCaptureIn: Int)
}
```

**Features**:
- Start delay or alarm time scheduling
- Configurable interval (0.5s minimum)
- Photo count limit
- Low-power sleep mode (unbinds camera between shots)
- Countdown overlays for user feedback

---

### ChronometerManager

**Location**: `app/src/main/java/com/example/kaimera/managers/ChronometerManager.kt`

**Purpose**: Provides stopwatch functionality with optional audio recording.

**Dependencies**:
```kotlin
ChronometerManager(
    context: Context,
    handler: Handler
)
```

**Callback Interface**:
```kotlin
interface Callback {
    fun onTimeUpdate(hours: Int, minutes: Int, seconds: Int)
    fun onRecordingStarted()
    fun onRecordingStopped(fileName: String)
}
```

**Features**:
- Hour:minute:second display
- Optional audio recording integration
- Persistent timing across pause/resume

---

### PreferencesManager

**Location**: `app/src/main/java/com/example/kaimera/managers/PreferencesManager.kt`

**Purpose**: Centralized access to SharedPreferences with type-safe getters.

**Dependencies**:
```kotlin
PreferencesManager(context: Context)
```

**Available Settings**:
```kotlin
// Photo Settings
getPhotoQuality(): String          // "high", "medium", "low"
getFlashMode(): String              // "auto", "on", "off"
getCaptureMode(): String            // "quality", "latency"
getTargetResolution(): String       // "max", "12mp", "fhd", "hd"

// Video Settings
getVideoQuality(): String           // "uhd", "fhd", "hd"
getVideoFrameRate(): String         // "24", "30", "60"
is120fpsEnabled(): Boolean

// Storage Settings
getSaveLocation(): String           // "app_storage", "dcim", "sd_card"
getFileNamingPattern(): String      // "timestamp", "sequential"
getCustomFilePrefix(): String
getAutoDeleteDays(): Int

// Overlay Settings
isGridEnabled(): Boolean
isLevelIndicatorEnabled(): Boolean
getLevelIndicatorSensitivity(): Int
getLevelIndicatorCrosshairSize(): Int
getLevelIndicatorCircleSize(): Int

// Preview Settings
getAutoSaveDelay(): Int             // 0-5 seconds
```

**Grouped Getters**:
```kotlin
getPhotoSettings(): PhotoSettings
getVideoSettings(): VideoSettings
getStorageSettings(): StorageSettings
getOverlaySettings(): OverlaySettings
```

---

### StorageManager

**Location**: `app/src/main/java/com/example/kaimera/managers/StorageManager.kt`

**Purpose**: Handles file I/O operations, storage location management, and file naming.

**Key Functions** (all static):
```kotlin
getStorageLocation(context: Context, location: String): File
createOutputFileOptions(context: Context, file: File, location: String, fileName: String): OutputFileOptions
generateFileName(pattern: String, prefix: String, extension: String): String
generateSequentialFileName(directory: File, prefix: String, extension: String): String
calculateStorageUsage(directory: File): Long
formatStorageSize(bytes: Long): String
deleteOldFiles(directory: File, daysToKeep: Int): Int
isSDCardAvailable(context: Context): Boolean
```

**Storage Locations**:
- `app_storage` - App's private external storage
- `dcim` - Public DCIM/Kaimera folder
- `sd_card` - External SD card (if available)

**File Naming Patterns**:
- `timestamp` - `PREFIX_yyyyMMdd_HHmmss.ext`
- `sequential` - `PREFIX_0001.ext`, `PREFIX_0002.ext`, etc.

## Integration Pattern

### MainActivity Structure

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
    private lateinit var videoRecordingManager: VideoRecordingManager
    private lateinit var burstModeManager: BurstModeManager
    private lateinit var intervalometerManager: IntervalometerManager
    private lateinit var chronometerManager: ChronometerManager
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize managers in dependency order
        preferencesManager = PreferencesManager(this)
        
        cameraManager = CameraManager(
            context = this,
            lifecycleOwner = this,
            previewView = previewView,
            preferencesManager = preferencesManager
        )
        
        permissionManager = PermissionManager(this) {
            cameraManager.startCamera()
        }
        
        orientationManager = OrientationManager(
            context = this,
            levelIndicator = levelIndicator,
            preferencesManager = preferencesManager
        ) { rotation ->
            cameraManager.getImageCapture()?.targetRotation = rotation
            cameraManager.getVideoCapture()?.targetRotation = rotation
        }
        
        // ... initialize other managers
        
        permissionManager.checkAndRequestPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        orientationManager.start()
        if (permissionManager.allPermissionsGranted()) {
            cameraManager.startCamera()
        }
    }
    
    override fun onPause() {
        super.onPause()
        orientationManager.stop()
    }
}
```

## Benefits

### Modularity
- Each manager is self-contained
- Easy to add new features by creating new managers
- Managers can be reused in other activities

### Testability
- Managers can be unit tested independently
- Dependencies can be mocked
- Business logic separated from Android framework

### Maintainability
- Changes to one feature don't affect others
- Clear ownership of functionality
- Easier to debug and trace issues

### Code Organization
- MainActivity reduced from ~1,180 to ~875 lines (26% reduction)
- Related functionality grouped together
- Consistent patterns across managers

## Best Practices

### 1. Keep Managers Focused
Each manager should handle one domain. If a manager grows too large, consider splitting it.

### 2. Use Callbacks for Communication
Managers should communicate with activities through callbacks, not direct method calls.

### 3. Inject Dependencies
Pass dependencies through constructors, not by accessing global state.

### 4. Handle Lifecycle Properly
Managers that use resources (sensors, listeners) should have `start()` and `stop()` methods.

### 5. Read Preferences Lazily
Managers should read preferences when needed (e.g., in `startCamera()`), not cache them.

### 6. Provide Getters for Shared Resources
When multiple components need access to the same resource (e.g., `ImageCapture`), expose it through a getter.

## Future Enhancements

### Potential New Managers
- **FilterManager** - Handle color filters and effects
- **GestureManager** - Centralize touch gesture handling
- **AnalyticsManager** - Track usage metrics
- **ExportManager** - Handle photo/video sharing and export

### Architecture Evolution
- Consider using **ViewModel** for state management
- Explore **Dependency Injection** frameworks (Hilt/Koin)
- Implement **Repository Pattern** for data access
- Add **Use Cases** layer for complex business logic

## Troubleshooting

### Manager Not Initialized
**Error**: `lateinit property has not been initialized`

**Solution**: Ensure managers are initialized in `onCreate()` before use.

### Callback Not Firing
**Issue**: Manager callbacks not being called

**Solution**: Verify the activity implements the callback interface and is set as the listener.

### Memory Leaks
**Issue**: Managers holding references to destroyed activities

**Solution**: Use weak references or clear callbacks in `onDestroy()`.

### Permission Issues
**Issue**: Camera not starting after permission granted

**Solution**: Ensure `PermissionManager.checkAndRequestPermissions()` is called and callback starts camera.

## See Also

- [Architecture Guide](ARCHITECTURE.md) - Overall app architecture
- [Building Guide](BUILDING.md) - How to build the app
- [CameraX Documentation](https://developer.android.com/training/camerax) - Official CameraX docs
