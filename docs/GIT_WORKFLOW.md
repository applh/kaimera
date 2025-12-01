# Git Workflow

The repository is initialized with Git and includes a comprehensive `.gitignore`.

## Initial Setup

```bash
# Already initialized with:
git init
git add -A
git commit -m "Initial commit: Kaimera camera app with CameraX"
```

## Adding Remote and Pushing

```bash
# Add your remote repository
git remote add origin <your-repo-url>

# Push to GitHub/GitLab/etc
git push -u origin main
```

## What's Tracked

- ✅ Source code (Kotlin, XML)
- ✅ Build configuration (Gradle files)
- ✅ Documentation (README, scripts)
- ✅ Resources (icons, strings, themes)
- ❌ Build artifacts (APKs, build folders)
- ❌ IDE files (.idea, *.iml)
- ❌ Local configuration (local.properties)

## Creating a Release

### 1. Update Version and Documentation

```bash
# Update CHANGELOG.md with release notes
# Update version in app/build.gradle.kts if needed
```

### 2. Commit and Tag

```bash
# Commit all changes
git add -A
git commit -m "vX.Y.Z: Release description"

# Create annotated tag
git tag -a vX.Y.Z -m "Release vX.Y.Z: Brief description

Detailed release notes:
- Feature 1
- Feature 2
- Bug fixes"
```

### 3. Build Debug APK

```bash
# Build debug APK
./gradlew assembleDebug

# APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### 4. Push to GitHub

```bash
# Push commits and tags
git push origin main
git push origin vX.Y.Z
```

### 5. Create GitHub Release

1. Go to your repository on GitHub
2. Click **Releases** → **Draft a new release**
3. Select the tag you just pushed (vX.Y.Z)
4. Fill in release details:
   - **Release title**: `vX.Y.Z - Release Name`
   - **Description**: Copy from CHANGELOG.md
5. Attach the debug APK:
   - Click **Attach binaries**
   - Upload `app/build/outputs/apk/debug/app-debug.apk`
   - Rename to `kaimera-vX.Y.Z-debug.apk` for clarity
6. Check **Set as the latest release** if applicable
7. Click **Publish release**

### Example Release Notes Template

```markdown
## What's New

- Feature 1 description
- Feature 2 description

## Bug Fixes

- Fix 1
- Fix 2

## Breaking Changes

- Breaking change description (if any)

## Installation

Download `kaimera-vX.Y.Z-debug.apk` and install on your Android device.

**Note**: You may need to enable "Install from unknown sources" in your device settings.
```

### Quick Release Script

```bash
#!/bin/bash
# Quick release script (save as release.sh)

VERSION=$1
if [ -z "$VERSION" ]; then
  echo "Usage: ./release.sh vX.Y.Z"
  exit 1
fi

# Build APK
./gradlew assembleDebug

# Commit, tag, and push
git add -A
git commit -m "$VERSION: Release"
git tag -a $VERSION -m "Release $VERSION"
git push origin main
git push origin $VERSION

echo "✅ Release $VERSION created!"
echo "📦 APK: app/build/outputs/apk/debug/app-debug.apk"
echo "🌐 Create GitHub release at: https://github.com/YOUR_USERNAME/kaimera/releases/new?tag=$VERSION"
```

