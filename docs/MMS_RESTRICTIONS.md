# MMS App - SMS/MMS Access Restrictions

## Overview

The Kaimera MMS app requires SMS sending permissions to function. On Android 13 and later, apps that request SMS/MMS permissions may be subject to **restricted settings** that require additional user approval.

## What Are Restricted Settings?

Android's restricted settings are security features designed to protect users from malicious apps that request access to sensitive device capabilities. When an app requests SMS/MMS permissions, Android may flag it as requiring "restricted settings" approval.

**Reference:** [Google Support - Learn about restricted settings](https://support.google.com/android/answer/12623953)

## Why This Affects the MMS App

The MMS app requests the following sensitive permissions:
- `SEND_SMS` - Required to send MMS messages
- `READ_EXTERNAL_STORAGE` / `READ_MEDIA_*` - Required to access media files for attachments

These permissions may trigger Android's restricted settings protection, especially if:
- The app is sideloaded (not from Google Play Store)
- The app targets Android 13+ (API 33+)
- The device manufacturer has additional security policies

## How to Enable Restricted Settings

If you encounter a "restricted settings" warning when trying to use the MMS app:

### On Android 13+

1. Open **Settings** on your Android device
2. Tap **Apps**
3. Find and tap **Kaimera**
   - Tip: If you can't find it, tap "See all apps" or "App info" first
4. Tap **More** (⋮) → **Allow restricted settings**
5. Follow the on-screen instructions
6. Grant the requested permissions (SMS, Storage)

### Alternative Method

1. When the MMS app requests permissions, you may see a dialog about restricted settings
2. Tap **Settings** in the dialog
3. Enable "Allow restricted settings" for Kaimera
4. Return to the app and grant permissions

## Security Considerations

> [!WARNING]
> **Enable with Caution**: Restricted settings exist to protect your personal data. Only enable them for apps you trust.

The Kaimera MMS app is open-source and only uses SMS permissions for their intended purpose (sending MMS messages). However, you should:

- Review the source code if you have concerns
- Only install from trusted sources
- Be aware that enabling restricted settings gives the app significant access

## Limitations

Even with proper permissions, the MMS app may have limitations:

### Carrier Restrictions
- MMS delivery depends on carrier support
- File size limits vary by carrier (typically 300KB-600KB)
- Some carriers may block MMS from non-default messaging apps

### Android Version Compatibility
- **Android 13+**: Requires restricted settings approval
- **Android 10-12**: Standard runtime permissions
- **Android 9 and below**: Permissions granted at install time

## Troubleshooting

### MMS App Can't Request Permissions
**Symptom:** Permission dialogs don't appear  
**Solution:** Enable restricted settings as described above

### MMS Sending Fails
**Symptom:** "Failed to send MMS" error  
**Possible Causes:**
- Permissions not granted
- No active SIM card
- Carrier doesn't support MMS from third-party apps
- File too large for carrier limits
- No mobile data connection

**Solutions:**
1. Verify all permissions are granted
2. Check that you have an active SIM card
3. Try a smaller file (<500KB)
4. Ensure mobile data is enabled
5. Contact your carrier about third-party MMS support

### App Installed from Unknown Source
**Symptom:** Android blocks installation or flags as unsafe  
**Solution:**
1. Enable "Install unknown apps" for your file manager
2. Understand the security implications
3. Consider building from source for maximum trust

## For Developers

If you're building or modifying the MMS app:

### Declaring Permissions

The app declares SMS permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
```

### Runtime Permission Requests

The app requests permissions at runtime in `MmsActivity.kt`:

```kotlin
private fun requestPermissions() {
    val permissions = mutableListOf(Manifest.permission.SEND_SMS)
    
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    
    permissionLauncher.launch(permissions.toTypedArray())
}
```

### Testing Restricted Settings

To test the restricted settings flow:

1. Build and install the app via ADB (sideload)
2. Launch the MMS app
3. Observe the restricted settings warning
4. Follow the approval process
5. Verify permissions are granted correctly

## Additional Resources

- [Android Developers - Request App Permissions](https://developer.android.com/training/permissions/requesting)
- [Android Developers - SMS and MMS](https://developer.android.com/guide/topics/connectivity/sms)
- [Google Support - Restricted Settings](https://support.google.com/android/answer/12623953)
