# Troubleshooting

## Camera Not Working

- Ensure camera permission is granted: **Settings → Apps → Kaimera → Permissions**
- Verify device has a working rear camera
- Check Android version is 7.0 or higher

## Build Failures

```bash
# Stop Gradle daemon
./gradlew --stop

# Clean and rebuild
./gradlew clean assembleRelease
```

## Photos Not Saving

- Check device storage space
- Verify app has storage access (automatic for external files directory)
- Check logcat for error messages: `adb logcat | grep Kaimera`

## Further Debugging

For more detailed debugging information, including on-device logs and ADB usage, see [DEBUGGING.md](DEBUGGING.md) and [DEVICE_LOGGING.md](DEVICE_LOGGING.md).
