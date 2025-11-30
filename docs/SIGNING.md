# Signing Keys Documentation

## Overview
This document explains how to create and manage signing keys for the Kaimera app.

## Debug Keystore

### Current Setup
- **File**: `app/debug.keystore`
- **Alias**: `androiddebugkey`
- **Store Password**: `android`
- **Key Password**: `android`
- **Validity**: 10,000 days

### Creating a New Debug Keystore
If you need to recreate the debug keystore:

```bash
keytool -genkey -v \
  -keystore app/debug.keystore \
  -alias androiddebugkey \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass android \
  -keypass android \
  -dname "CN=Android Debug,O=Android,C=US"
```

### When to Recreate Debug Keystore
- Installation fails with "App not installed" or signature mismatch
- You want to test on a new device that had a different debug build
- The keystore file is lost or corrupted

## Release Keystore

### Creating a Release Keystore
For production releases, create a secure keystore:

```bash
keytool -genkey -v \
  -keystore app/release.keystore \
  -alias kaimera-release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

You will be prompted for:
- **Keystore password** (choose a strong password)
- **Key password** (choose a strong password)
- **Name and organization details**

> [!CAUTION]
> **Keep your release keystore and passwords secure!** If you lose them, you cannot update your app on the Play Store.

### Configuring Release Keystore
Add these properties to `local.properties` (this file is gitignored):

```properties
store.password=YOUR_STORE_PASSWORD
key.alias=kaimera-release
key.password=YOUR_KEY_PASSWORD
```

## Building Signed APKs

### Debug Build
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

## Troubleshooting

### Installation Fails with "App not installed"
1. Uninstall the previous version:
   ```bash
   adb uninstall com.example.kaimera
   ```
2. Install the new APK:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Signature Mismatch
This happens when trying to install an APK signed with a different key. Solutions:
- Uninstall the old app first
- Use the same keystore as before
- Recreate the debug keystore (for debug builds only)

### Keystore Information
To view keystore details:
```bash
keytool -list -v -keystore app/debug.keystore -storepass android
```

## Security Best Practices

1. **Never commit `release.keystore` or `local.properties` to git**
2. **Backup your release keystore** in a secure location
3. **Use strong passwords** for release keystores
4. **Keep debug and release keystores separate**
5. **Rotate keys periodically** for enhanced security

## Files

- `app/debug.keystore` - Debug signing key (committed to git)
- `app/release.keystore` - Release signing key (NOT in git)
- `local.properties` - Release keystore passwords (NOT in git)
- `app/build.gradle.kts` - Signing configuration
