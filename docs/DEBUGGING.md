# Android Crash Debugging Guide

This guide documents best practices for debugging crashes in the Kaimera app, based on lessons learned from the gallery crash issue.

## The Gallery Crash Case Study

### What Happened
The `GalleryActivity` was crashing immediately on launch, causing the app to minimize without showing any error message.

### Root Cause
The crash occurred during layout inflation (`setContentView()`) because the XML layout used Material 3 theme attributes (`?attr/colorPrimaryContainer`) that weren't available in the app's theme.

### Why It Was Hard to Debug
1. **Crash before try-catch**: The error happened during `setContentView()` before our try-catch in `loadGallery()` could catch it
2. **No error message**: The app just minimized without showing any toast or log
3. **No logcat access**: Without `adb logcat`, we couldn't see the stack trace

## Best Practices for Crash Prevention

### 1. Comprehensive Logging

Always add logging at critical points:

```kotlin
companion object {
    private const val TAG = "YourActivity"
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d(TAG, "onCreate: Starting")
    
    try {
        setContentView(R.layout.your_layout)
        Log.d(TAG, "onCreate: Layout inflated successfully")
    } catch (e: Exception) {
        Log.e(TAG, "onCreate: Failed to inflate layout", e)
        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        finish()
        return
    }
    
    // More initialization...
}
```

### 2. Wrap Critical Operations

Wrap operations that might fail:

```kotlin
// Layout inflation
try {
    setContentView(R.layout.activity_gallery)
} catch (e: Exception) {
    Log.e(TAG, "Layout inflation failed", e)
    showErrorAndFinish(e)
    return
}

// View initialization
try {
    recyclerView = findViewById(R.id.recyclerView)
    emptyView = findViewById(R.id.emptyView)
} catch (e: Exception) {
    Log.e(TAG, "View initialization failed", e)
    showErrorAndFinish(e)
    return
}
```

### 3. Use Safe Theme Attributes

Instead of using theme attributes that might not exist:

```xml
<!-- ❌ BAD: May not exist in all themes -->
<TextView
    android:textColor="?attr/colorOnPrimaryContainer"
    android:background="?attr/colorPrimaryContainer" />

<!-- ✅ GOOD: Use explicit colors or check theme first -->
<TextView
    android:textColor="#000000"
    android:background="#E0E0E0" />
```

### 4. Implement Global Exception Handler

Add a global uncaught exception handler in your Application class:

```kotlin
class KaimeraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CRASH", "Uncaught exception on thread ${thread.name}", throwable)
            
            // Show a user-friendly error
            val intent = Intent(this, CrashActivity::class.java)
            intent.putExtra("error", throwable.message)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            
            // Exit gracefully
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
```

### 5. Create a Debug Build Variant

Add debug-specific logging:

```kotlin
// In build.gradle.kts
buildTypes {
    debug {
        isDebuggable = true
        buildConfigField("boolean", "ENABLE_LOGGING", "true")
    }
    release {
        isDebuggable = false
        buildConfigField("boolean", "ENABLE_LOGGING", "false")
    }
}

// In code
if (BuildConfig.ENABLE_LOGGING) {
    Log.d(TAG, "Detailed debug info here")
}
```

## Debugging Tools

### 1. Logcat (Essential)

View logs in real-time:
```bash
# View all logs
adb logcat

# Filter by tag
adb logcat -s GalleryActivity

# Filter by priority (Error and above)
adb logcat *:E

# Clear and monitor
adb logcat -c && adb logcat | grep -i "exception\|error\|crash"
```

### 2. Android Studio Logcat

- **Logcat panel**: Bottom of Android Studio
- **Filters**: Create custom filters for your app package
- **Search**: Use regex to find specific errors

### 3. Crashlytics (Production)

For production apps, integrate Firebase Crashlytics:

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.google.firebase:firebase-crashlytics-ktx")
}

// In code
try {
    riskyOperation()
} catch (e: Exception) {
    FirebaseCrashlytics.getInstance().recordException(e)
    throw e
}
```

## Common Crash Patterns

### 1. Layout Inflation Crashes
- **Cause**: Missing resources, invalid theme attributes
- **Solution**: Wrap `setContentView()` in try-catch, use explicit values

### 2. Null Pointer Exceptions
- **Cause**: Accessing null objects
- **Solution**: Use null-safe operators (`?.`, `?:`, `!!`)

### 3. Resource Not Found
- **Cause**: Missing drawable, string, or layout
- **Solution**: Check resource files, use `@+id` correctly

### 4. ClassCastException
- **Cause**: Incorrect view type casting
- **Solution**: Use `findViewById<SpecificType>()` with correct type

## Debugging Checklist

When encountering a crash:

- [ ] Check logcat for stack trace
- [ ] Add logging before and after the suspected line
- [ ] Wrap suspicious code in try-catch
- [ ] Test on different Android versions
- [ ] Check for missing resources
- [ ] Verify theme attributes exist
- [ ] Test with different configurations (dark mode, locale)
- [ ] Use Android Studio's Layout Inspector
- [ ] Check for memory leaks with Profiler

## Prevention Strategies

1. **Code Review**: Have another developer review crash-prone code
2. **Unit Tests**: Test critical paths
3. **Integration Tests**: Test activity launches
4. **Lint Checks**: Enable all lint warnings
5. **Static Analysis**: Use tools like Detekt
6. **Beta Testing**: Test with real users before release

## Resources

- [Android Debugging Guide](https://developer.android.com/studio/debug)
- [Logcat Command-line Tool](https://developer.android.com/studio/command-line/logcat)
- [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics)
- [Android Profiler](https://developer.android.com/studio/profile)

---

**Remember**: The best crash is the one that never happens. Invest time in defensive programming and comprehensive error handling!
