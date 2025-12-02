# Kaimera Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [v32.0.0] - 2025-12-02

### Added
- **Note App:**
  - Google Keep-like note-taking application
  - Create, edit, and delete notes
  - Color-code notes for organization
  - Pin important notes to the top
  - Archive completed notes
  - Search functionality
  - Local persistence using Room Database
- **Launcher Integration:**
  - Added "Notes" app to the launcher grid
  - **Quick Access Panel:** New bottom sheet panel in the launcher for quick note creation and viewing recent notes
  - Added "Quick Notes" Floating Action Button (FAB) to the launcher

## [v31.0.0] - 2025-12-02

### Added
- **Launcher Customization:**
  - New settings screen to manage launcher apps
  - Drag-and-drop reordering of apps
  - Toggle visibility for individual apps
  - Preferences persisted across restarts
  - Dynamic launcher grid that updates based on settings

## [v30.1.0] - 2025-12-02

### Added
- **MMS App Documentation:** Added `MMS_RESTRICTIONS.md` documenting Android SMS/MMS access restrictions
  - Explains restricted settings on Android 13+
  - Provides troubleshooting steps
  - Documents security considerations and carrier limitations

## [v30.0.0] - 2025-12-02

### Added
- **MMS Messaging App:** New app for sending MMS messages with media attachments.
  - Phone number input with validation
  - Optional message body
  - File picker for images, videos, and audio
  - File type and size validation (warns if >1MB)
  - Runtime permission handling (SMS, storage)
  - Material Design UI with file preview
  - Send button with validation
  - Progress indicator and status feedback

## [v29.2.0] - 2025-12-02

### Changed
- **Improved Chronometer UI:** Updated to use larger, more user-friendly interface from Settings panel.
  - Display size increased from 12sp to 48sp for better readability.
  - Buttons now show full labels ("▶️ Start", "🎤 Record") instead of just emojis.
  - Camera app chronometer panel now centered with proper margins.

## [v29.1.0] - 2025-12-02

### Changed
- **UI Refactor:** Extracted chronometer UI into a shared layout `view_chronometer_panel.xml`.
  - Ensures consistent look and feel between Camera app and Chronometer app.
  - Improves code maintainability.

## [v29.0.0] - 2025-12-02

### Added
- **Chronometer App:** Added a standalone Chronometer app to the Launcher.
  - Reuses existing chronometer logic and UI styles.
  - Accessible via a new "Timer" icon in the Launcher.
  - Includes Start, Stop, Reset, and Start with Audio functionality.

## [v28.0.0] - 2025-12-01

### Fixed
- **SharedPreferences Conflicts:** Fixed crashes caused by preference key conflicts between Camera and File Explorer apps
  - Renamed all Camera app preferences with `camera_` prefix
  - `save_location` → `camera_save_location`
  - `save_gps_location` → `camera_save_gps_location`
  - `file_naming_pattern` → `camera_file_naming_pattern`
  - `custom_file_prefix` → `camera_custom_file_prefix`
  - `auto_delete_enabled` → `camera_auto_delete_enabled`
  - `keep_files_for` → `camera_keep_files_for`
- **Photo Capture Crash:** Fixed crash when location permissions were denied
  - Made location permissions optional instead of required
  - Added defensive checks before attempting location retrieval
  - Shows informative message when GPS tagging is enabled but permissions denied
- **Settings Naming:** Standardized settings titles for consistency
  - Renamed "Overlays & Level Indicator" to "Overlay Settings"
  - Extracted hardcoded strings to `strings.xml`

### Changed
- **Breaking Change:** Users will need to reconfigure Camera settings after this update due to preference key changes

## [v27.0.0] - 2025-11-30

### Added
- **Launch Screen:** Configurable app launcher with multiple apps
  - Shows Camera and Browser icons on black background
  - Tap icons to launch respective apps
  - Toggle "Show Launch Screen" in Settings to skip directly to camera
  - "Return to Launch Screen" option in Settings
- **Simple Browser:** Full-featured web browser
  - URL bar with auto-HTTPS
  - Navigation controls (Back, Forward, Refresh, Home)
  - Configurable home URL (saved in preferences)
  - Settings panel with:
    - JavaScript toggle
    - DOM Storage toggle
    - Image loading toggle
    - Text size adjustment (Small/Normal/Large/Extra Large)
    - Granular data clearing (Cache, Cookies, History, or All)
  - Progress bar for page loading
  - Confirmation dialog for "Clear All Data"

### Fixed
- **Browser:** Added missing INTERNET permission
- **Browser:** Fixed white-on-white text color in URL bar

## [v26.0.0] - 2025-11-30

### Added
- **Intervalometer Feature** - Scheduled time-lapse photography with advanced controls
  - Configurable start delay (seconds) or specific alarm time (HH:MM)
  - Adjustable interval between shots (minimum 0.5 seconds)
  - Set total photo count or run indefinitely
  - Real-time counter display showing progress (e.g., "INT: 5 / 20")
  - Low Power Mode option for battery conservation
    - Camera unbinds between shots to save power
    - Black sleep overlay with countdown timer (HH:MM:SS format)
    - Progress bar for visual feedback
    - Auto wake-up 3 seconds before next scheduled shot
    - Manual "Wake Up" button to stop early
  - Automatic file naming with millisecond timestamps for uniqueness
  - Screen stays on during active capture phases
  - Graceful stop with final photo count notification

### Technical
- Added `IntervalometerConfig` data class with `lowPowerMode` boolean
- Implemented `enterSleepMode()` and `wakeUpCamera()` methods
- Added `cameraProvider` class variable for lifecycle management
- Created sleep overlay layout with countdown and controls
- Enhanced dialog with checkbox for Low Power Mode option

## [1.4.0] - 2025-11-28

### Added
- **120fps High-Speed Video Recording** - Slow-motion video capture (device-dependent)
  - Configurable via Settings → Video Quality → Enable 120fps
  - Automatic fallback to standard recording if unsupported
  - CameraX 1.5.1 upgrade for high-speed video support

### Changed
- Upgraded CameraX from 1.3.4 to 1.5.1
- Updated Kotlin from 1.9.0 to 2.1.0
- Updated Material Components to 1.12.0

## [1.3.0] - 2025-11-27

### Added
- **Settings Organization** - Reorganized settings into collapsible groups
  - Photo Settings sub-screen
  - Video Settings sub-screen
  - Overlays sub-screen (including Level Indicator)
  - Storage Options sub-screen

## [1.2.0] - 2025-11-24

### Added
- **Level Indicator** - Spirit level overlay with dual bubbles
  - Configurable sensitivity in Settings
  - Helps align horizontal and vertical shots
  - Static centered crosshair with dynamic level circle

## [1.1.0] - 2025-11-23

### Added
- **EXIF Editor** - View and edit image metadata
- **Media Controls** - Unified playback for video and audio
- **Debug Logs** - On-device log viewer in Settings

## [v26.0.0] - 2025-11-30

### Added
- **Video Frame Export:** Extract high-quality frames from paused videos.
  - Export button appears when video is paused.
  - Saves as JPEG or WebP based on preferences.
  - Respects quality settings (1-100).
  - Automatically updates gallery.

### Changed
- **Refactoring:** Moved settings fragments to `com.example.kaimera.fragments` package for better organization.
- **Documentation:** Updated deployment guide with SSH key persistence instructions.

### Fixed
- **Gallery Refresh:** Fixed issue where gallery didn't update after exporting a frame.
- **Build Warnings:** Removed unused variables and parameters across multiple files.

### Security
- **Keystore:** Secured release keystore by removing it from git and moving credentials to `local.properties`.

## [v25.0.0] - 2025-11-3022

### Added
- Initial release with core camera functionality
- Real-time camera preview
- Photo and video capture

## [1.0.0] - 2025-11-22

### Added
- Burst mode
- Filters and effects
- In-app gallery
- Material Design UI

[Unreleased]: https://github.com/yourusername/kaimera/compare/v1.5.0...HEAD
[1.5.0]: https://github.com/yourusername/kaimera/compare/v1.4.0...v1.5.0
[1.4.0]: https://github.com/yourusername/kaimera/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/yourusername/kaimera/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/yourusername/kaimera/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/yourusername/kaimera/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/yourusername/kaimera/releases/tag/v1.0.0
