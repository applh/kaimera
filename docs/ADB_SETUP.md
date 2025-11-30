# ADB Setup and Usage Guide

## What is ADB?

**ADB (Android Debug Bridge)** is a command-line tool that lets you communicate with Android devices. It's essential for:
- Installing APKs directly
- Debugging apps
- Viewing logs (logcat)
- Running shell commands on the device

## Installing ADB on macOS

### Method 1: Using Homebrew (Recommended)

1. **Install Homebrew** (if not already installed):
   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

2. **Install Android Platform Tools** (includes ADB):
   ```bash
   brew install --cask android-platform-tools
   ```

3. **Verify Installation**:
   ```bash
   adb version
   ```
   You should see output like: `Android Debug Bridge version 1.0.41`

### Method 2: Manual Installation

1. **Download Platform Tools**:
   - Visit: https://developer.android.com/studio/releases/platform-tools
   - Download the macOS ZIP file

2. **Extract and Add to PATH**:
   ```bash
   cd ~/Downloads
   unzip platform-tools-latest-darwin.zip
   sudo mv platform-tools /usr/local/
   echo 'export PATH="/usr/local/platform-tools:$PATH"' >> ~/.zshrc
   source ~/.zshrc
   ```

3. **Verify Installation**:
   ```bash
   adb version
   ```

## Connecting Your Smartphone

### Option 1: USB Connection (Recommended for First Time)

#### Step 1: Enable Developer Options on Your Phone

1. Open **Settings** on your Android device
2. Go to **About Phone**
3. Tap **Build Number** 7 times
4. You'll see "You are now a developer!"

#### Step 2: Enable USB Debugging

1. Go back to **Settings**
2. Find **Developer Options** (usually under System or Advanced)
3. Enable **USB Debugging**

#### Step 3: Connect via USB

1. **Connect your phone** to your Mac using a USB cable
2. **Unlock your phone**
3. You'll see a prompt: "Allow USB debugging?"
4. Check "Always allow from this computer"
5. Tap **OK**

#### Step 4: Verify Connection

```bash
adb devices
```

You should see:
```
List of devices attached
ABC123XYZ    device
```

If you see `unauthorized`, unlock your phone and accept the USB debugging prompt.

### Option 2: Wireless Connection (WiFi)

#### Prerequisites
- Phone and Mac must be on the **same WiFi network**
- USB debugging must be enabled
- Initial USB connection required to set up wireless

#### Setup Steps

1. **Connect via USB first** and verify with `adb devices`

2. **Enable TCP/IP mode**:
   ```bash
   adb tcpip 5555
   ```

3. **Find your phone's IP address**:
   - On phone: Settings → About Phone → Status → IP Address
   - Or run: `adb shell ip addr show wlan0`

4. **Disconnect USB cable**

5. **Connect wirelessly**:
   ```bash
   adb connect YOUR_PHONE_IP:5555
   ```
   Example: `adb connect 192.168.1.100:5555`

6. **Verify connection**:
   ```bash
   adb devices
   ```

#### Reconnecting Wirelessly

After initial setup, you can connect anytime with:
```bash
adb connect YOUR_PHONE_IP:5555
```

To switch back to USB:
```bash
adb usb
```

## Common ADB Commands

### Device Management
```bash
# List connected devices
adb devices

# Reboot device
adb reboot

# Reboot to recovery
adb reboot recovery

# Disconnect all devices
adb disconnect
```

### Installing APKs
```bash
# Install APK
adb install path/to/app.apk

# Install and replace existing app
adb install -r path/to/app.apk

# Uninstall app
adb uninstall com.example.package
```

### File Transfer
```bash
# Push file to device
adb push local/file.txt /sdcard/

# Pull file from device
adb pull /sdcard/file.txt ~/Desktop/
```

### Viewing Logs
```bash
# View all logs
adb logcat

# Clear logs
adb logcat -c

# Filter logs by tag
adb logcat -s TAG_NAME

# Save logs to file
adb logcat > logfile.txt

# View crash logs only
adb logcat | grep -E "AndroidRuntime|FATAL"
```

### Shell Commands
```bash
# Open device shell
adb shell

# Run single command
adb shell ls /sdcard/

# Check device info
adb shell getprop ro.build.version.release  # Android version
adb shell getprop ro.product.model          # Device model
```

## Troubleshooting

### "adb: command not found"
- ADB is not installed or not in PATH
- Solution: Follow installation steps above

### "no devices/emulators found"
- Phone not connected or USB debugging disabled
- Solution: Check USB cable, enable USB debugging, accept prompt on phone

### "device unauthorized"
- USB debugging prompt not accepted
- Solution: Unlock phone, revoke USB debugging authorizations in Developer Options, reconnect

### "offline" status
- Device connection lost
- Solution: 
  ```bash
  adb kill-server
  adb start-server
  adb devices
  ```

### Multiple devices connected
Specify device by serial number:
```bash
adb -s DEVICE_SERIAL install app.apk
```

## Installing Kaimera APK

Once ADB is set up, install the app:

```bash
# Navigate to project directory
cd /Users/lh/Downloads/antig/kaimera

# Uninstall old version (if exists)
adb uninstall com.example.kaimera

# Install new APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Security Notes

- **USB Debugging** gives full access to your device - only enable when needed
- **Always verify** the "Allow USB debugging" prompt shows your Mac's fingerprint
- **Revoke authorizations** in Developer Options if you lose your Mac or sell it
- **Disable USB debugging** when not actively developing

## Additional Resources

- [Official ADB Documentation](https://developer.android.com/studio/command-line/adb)
- [Platform Tools Release Notes](https://developer.android.com/studio/releases/platform-tools)
