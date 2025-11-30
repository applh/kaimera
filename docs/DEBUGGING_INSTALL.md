# Debugging APK Installation "Internal Error"

## Steps to Diagnose

### 1. Get Detailed Installation Error
Try installing via ADB to see the actual error:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Check Logcat for Crash Details
If the app installs but crashes immediately:
```bash
adb logcat -c  # Clear logs
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat | grep -i "androidruntime\|exception\|error"
```

### 3. Verify APK Integrity
Check if the APK is valid:
```bash
unzip -t app/build/outputs/apk/debug/app-debug.apk
```

### 4. Check Device Compatibility
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Ensure your device meets these requirements

## Common Causes of "Internal Error"

1. **Corrupted APK** - Rebuild with `./gradlew clean assembleDebug`
2. **Insufficient Storage** - Free up space on device
3. **App Crash on Launch** - Check logcat for stack trace
4. **Manifest Issues** - Duplicate activities or missing permissions
5. **Native Library Mismatch** - Architecture incompatibility

## Quick Fixes to Try

### Fix 1: Complete Uninstall
```bash
adb uninstall com.example.kaimera
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Fix 2: Rebuild from Scratch
```bash
./gradlew clean
rm -rf app/build
./gradlew assembleDebug
```

### Fix 3: Check for Multiple Users
```bash
adb shell pm list users
# If multiple users, uninstall for all:
adb uninstall --user 0 com.example.kaimera
```

## Next Steps

Please run the ADB install command and share the output so I can see the specific error message.
