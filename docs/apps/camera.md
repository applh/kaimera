# Camera App Documentation

## Overview
The Camera App is a feature-rich photography and videography tool built using Android's CameraX library. It supports standard photo capture, video recording, burst mode, intervalometer (time-lapse), and real-time filters.

## Architecture
The app follows a modular architecture, separating UI logic from camera operations.

### Package Structure
- `com.example.kaimera.camera.ui`: Activities and UI components.
  - `MainActivity`: The primary entry point and viewfinder.
  - `PreviewActivity`: Displays captured media immediately after capture.
  - `SettingsActivity`: Manages app preferences.
- `com.example.kaimera.camera.managers`: Logic controllers for specific features.
  - `CameraManager`: Handles CameraX initialization, use case binding (Preview, ImageCapture, VideoCapture), and lifecycle management.
  - `BurstModeManager`: Manages rapid sequential photo capture.
  - `IntervalometerManager`: Handles time-lapse capture with configurable intervals and sleep modes.
  - `VideoRecordingManager`: Manages video recording sessions.
  - `StorageManager`: Handles file creation, naming, and cleanup.
- `com.example.kaimera.camera.utils`: Utility classes.
  - `ImageCaptureHelper`: Handles saving images in different formats (JPEG, WebP) and EXIF data manipulation.
  - `ExifUtils`: Utilities for reading and writing EXIF metadata.

## Key Features

### Photo Capture
- **Standard Capture**: High-quality JPEG or WebP capture.
- **Filters**: Real-time color matrix filters applied post-capture for file-based storage.
- **HDR**: Simulated HDR mode (currently a placeholder for future multi-exposure implementation).

### Video Recording
- **Resolutions**: Supports UHD (4K), FHD (1080p), and HD (720p).
- **High Speed**: Experimental support for 120fps recording on supported devices.

### Advanced Modes
- **Burst Mode**: Captures up to 20 photos in rapid succession.
- **Intervalometer**: Automated capture at set intervals. Supports a "Low Power" mode that unbinds the camera between shots to save battery during long sessions.

### Settings
- **Storage**: Configurable save location (App Storage vs. Public DCIM).
- **Formats**: JPEG vs. WebP.
- **Overlays**: Grid lines (3x3, 4x4, Golden Ratio) and Level Indicator.

## Technical Specificities

### CameraX Integration
The app uses `ProcessCameraProvider` to bind lifecycle-aware use cases.
- **Preview**: Connected to a `PreviewView`.
- **ImageCapture**: Configured for minimal latency or maximum quality based on user preference.
- **VideoCapture**: Uses `Recorder` with `QualitySelector` for resolution control.

### File Management
- **Scoped Storage**: Respects Android 10+ scoped storage rules.
- **Auto-Delete**: Optional feature to automatically clean up old files in App Storage to save space.

### EXIF Data
Custom `ExifUtils` ensure that metadata (location, orientation, description) is correctly preserved or added, especially when converting between formats or processing burst shots.
