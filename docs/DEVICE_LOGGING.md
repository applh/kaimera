# Viewing Logs on Physical Device

When testing the Kaimera app on a smartphone, you have several options to view logs:

## Quick Reference

| Method | Requires Computer | Difficulty | Real-time |
|--------|------------------|------------|-----------|
| ADB Logcat | Yes | Easy | ✅ Yes |
| Log Viewer App | No | Very Easy | ✅ Yes |
| Wireless ADB | Yes (initial setup) | Medium | ✅ Yes |
| In-App Debug Screen | No | Very Easy | ❌ No |

---

## Option 1: ADB Logcat (Recommended) 🖥️

**Best for:** Developers with a computer available

### Setup:
```bash
# 1. Enable USB Debugging on phone
# Settings → About Phone → Tap "Build Number" 7 times
# Settings → Developer Options → Enable "USB Debugging"

# 2. Connect phone via USB
adb devices

# 3. View logs
adb logcat
```

### Useful Commands:
```bash
# Filter for Kaimera app only
adb logcat | grep "com.example.kaimera"

# Filter by specific activity
adb logcat -s GalleryActivity MainActivity

# Show only errors
adb logcat *:E

# Clear and start fresh
adb logcat -c && adb logcat

# Save to file
adb logcat > kaimera_logs.txt
```

---

## Option 2: Log Viewer Apps (No Computer Needed) 📱

**Best for:** Quick debugging without a computer

### Recommended Apps:

1. **Logcat Reader** ⭐ Recommended
   - [Download from Play Store](https://play.google.com/store/apps/details?id=com.dp.logcatapp)
   - Clean interface, no ads
   - Filter by package name

2. **MatLog**
   - Advanced filtering
   - Save logs to file
   - Color-coded levels

3. **aLogcat**
   - Real-time viewing
   - Search functionality

### How to Use:
1. Install app from Play Store
2. Grant "Read Logs" permission
3. Filter by: `com.example.kaimera`
4. Watch logs in real-time as you use the app

---

## Option 3: Wireless ADB (No Cable) 📡

**Best for:** Developers who want wireless debugging

### One-Time Setup:
```bash
# 1. Connect phone via USB first
adb tcpip 5555

# 2. Find phone's IP address
# Settings → About Phone → Status → IP Address
# Example: 192.168.1.100

# 3. Disconnect USB cable

# 4. Connect wirelessly
adb connect 192.168.1.100:5555

# 5. Verify connection
adb devices

# 6. Use logcat as normal
adb logcat
```

### Reconnecting:
```bash
# Phone and computer must be on same WiFi
adb connect YOUR_PHONE_IP:5555
```

---

## Option 4: In-App Debug Screen 🔍

**Best for:** Quick on-device log viewing (no computer/apps needed)

### Access:
The app now includes a hidden debug screen:

1. Open Kaimera app
2. Go to **Settings**
3. Scroll to bottom
4. Tap **"Version"** 5 times quickly
5. Debug Logs screen will open

### Features:
- View recent app logs
- Filter for Kaimera-specific logs
- Scroll through log history
- No external tools needed

**Note:** On Android 4.1+, apps can only read their own logs. If you see an error, use ADB or a log viewer app instead.

---

## Troubleshooting

### "adb: command not found"
Install Android SDK Platform Tools:
- **Mac:** `brew install android-platform-tools`
- **Windows:** Download from [developer.android.com](https://developer.android.com/studio/releases/platform-tools)
- **Linux:** `sudo apt install adb`

### "device unauthorized"
1. Disconnect USB
2. On phone: Settings → Developer Options → Revoke USB Debugging authorizations
3. Reconnect USB
4. Tap "Allow" on phone prompt

### "no devices/emulators found"
1. Check USB cable (try a different one)
2. Enable USB Debugging
3. Change USB mode to "File Transfer" or "PTP"
4. Try different USB port

### Wireless ADB not connecting
1. Ensure phone and computer on same WiFi
2. Check firewall settings
3. Restart ADB: `adb kill-server && adb start-server`

---

## Best Practices

1. **Clear logs before testing:**
   ```bash
   adb logcat -c
   ```

2. **Filter for your app:**
   ```bash
   adb logcat | grep "Kaimera\|GalleryActivity\|MainActivity"
   ```

3. **Save logs for later analysis:**
   ```bash
   adb logcat > debug_session_$(date +%Y%m%d_%H%M%S).txt
   ```

4. **Use log levels appropriately:**
   - `Log.d()` - Debug info
   - `Log.i()` - Informational
   - `Log.w()` - Warnings
   - `Log.e()` - Errors

---

## Quick Start Guide

**For first-time users:**

1. **Easiest:** Install "Logcat Reader" app from Play Store
2. **Most powerful:** Enable USB Debugging + use `adb logcat`
3. **No tools:** Use in-app Debug Screen (tap Version 5 times in Settings)

**Recommended workflow:**
1. Clear logs: `adb logcat -c`
2. Reproduce the issue
3. View logs: `adb logcat | grep "Kaimera"`
4. Look for lines with `ERROR` or `Exception`

---

**Remember:** Logs are your best friend when debugging! Always check them when something goes wrong.
