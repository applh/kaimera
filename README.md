# Kaimera 📸

A modern Android camera application built with Kotlin and CameraX, featuring real-time preview and one-tap photo capture.

**Kaimera** is a play on words, blending "Camera" with *Kaizen* (the Japanese concept of continuous improvement). Like the mythical Chimera, this project strives to be a fantastic hybrid—a modern camera app that constantly evolves with progressive features.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![CameraX](https://img.shields.io/badge/CameraX-1.2.2-orange.svg)](https://developer.android.com/training/camerax)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-brightgreen.svg)](https://developer.android.com/about/versions/nougat)

## Features

- 📷 **Real-time Camera Preview** - Full-screen camera preview using CameraX
- 🎯 **One-Tap Capture** - Simple floating action button for photo capture
- 👁️ **Photo Preview** - Immediate preview with Keep/Retake options and auto-save timer (0-5s)
- 🔍 **Zoom Controls** - Pinch-to-zoom in camera preview and gallery
- 📏 **Grid Overlay** - Toggleable rule-of-thirds grid
- ⏱️ **Timer/Self-Timer** - 3s and 10s delay options
- ⚙️ **Photo Quality Settings** - High/Medium/Low JPEG quality options
- 🎥 **Video Recording** - Record videos with audio, configurable quality (4K/1080p/720p) and frame rate (24/30/60fps). Recording duration is displayed with a transparent overlay above the capture button. Supports pinch-to-zoom playback.
- 📸 **Burst Mode** - Rapid photo capture with long-press (up to 20 photos)
- 🔄 **Camera Switch** - Toggle between front and back cameras
- ⏱️ **Chronometer** - Compact stopwatch overlay integrated into the top bar with optional audio recording. Also available in Settings screen with large two-row layout.
- 💡 **Flash Control** - Toggle between Auto, On, and Off modes
- 🎨 **Filters and Effects** - Real-time color filters (Grayscale, Sepia, Vivid, Cool)
- 🖼️ **EXIF Editor** - View and edit image metadata (Description, User Comment) directly in the gallery or from thumbnails
- 🖼️ **In-App Gallery** - View and manage your captured photos, videos, and audio recordings. Tap to play videos and audio directly in the app. File sizes are displayed for each item.
- ⏯️ **Media Controls** - Unified playback controls with seek bar, play/pause, and time display for both video and audio.
- 📱 **Smart Orientation** - Photos are automatically rotated based on device orientation, even when UI is locked to portrait.
- 💾 **Auto-Save** - Photos automatically saved with timestamps
- 📱 **Material Design** - Modern UI with Material Design components
- ⚡ **Lifecycle-Aware** - Automatic camera lifecycle management
- 🎯 **Clean UI** - Streamlined interface with settings-based configuration
- 🐛 **Debug Logs** - On-device log viewer for troubleshooting (Settings → Debug Logs)

## Screenshots

> **Note:** The app features a full-screen camera preview with a floating capture button at the bottom center.

## Quick Start

```bash
# Clone the repository
git clone <your-repo-url>
cd kaimera

# Run the setup script (macOS only)
./setup.sh

# Build the app
./gradlew assembleRelease
```

For detailed build instructions, see [Building & Installation](docs/BUILDING.md).

## Documentation

We have detailed documentation available in the `docs/` directory:

- 🏗️ **[Architecture & Development](docs/ARCHITECTURE.md)** - Technical details, project structure, and code overview.
- 🛠️ **[Building & Installation](docs/BUILDING.md)** - How to build, install, and customize the app.
- 🚀 **[Deployment](docs/DEPLOYMENT.md)** - Deploying to GitHub and publishing to Google Play.
- 🗺️ **[Roadmap](docs/ROADMAP.md)** - Planned features and future ideas.
- 🔧 **[Troubleshooting](docs/TROUBLESHOOTING.md)** - Common issues and fixes.
- 🐛 **[Debugging](docs/DEBUGGING.md)** - Detailed debugging guide.
- 🐳 **[Docker Build](docs/DOCKER_BUILD.md)** - Using the containerized build system.
- 📊 **[Build Comparison](docs/BUILD_COMPARISON.md)** - Local vs. Docker builds.

## Usage

1. **Launch** the Kaimera app
2. **Grant** camera permission when prompted
3. **Preview** appears automatically in full-screen
4. **Tap** the camera button (bottom center) to capture a photo
5. **Toast notification** confirms the save location

### Accessing Photos

Photos are saved to the app's private external storage:
```
/storage/emulated/0/Android/data/com.example.kaimera/files/
```

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

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
