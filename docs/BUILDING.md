# Building & Installation Guide

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
# After running setup.sh (see Quick Start in README)
./gradlew assembleRelease

# Or use the convenience script
sh build_app.sh
```

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

## Customization

### Regenerating App Icons

The project includes scripts to generate launcher icons:


**Using Python/Pillow:**
```bash
pip3 install Pillow
python3 generate_icons.py
```

Both scripts create professional camera icons with:
- Purple gradient background (#6200EE to #3700B3)
- White camera design with lens and aperture
- All required densities (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)

After regenerating icons, rebuild the APK:
```bash
./gradlew assembleRelease
```
