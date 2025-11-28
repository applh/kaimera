#!/bin/bash
echo "Building Kaimera APK..."

# Ensure executable
chmod +x gradlew

# Run build
./gradlew assembleDebug --info
code1=$?


./gradlew assembleRelease --info
code2=$?

if [ $code2 -eq 0 ]; then
    echo "Build Successful!"
    echo "APK location: app/build/outputs/apk/release/app-release.apk"
else
    echo "Build Failed. Please check the output above."
fi

if [ $code1 -eq 0 ]; then
    echo "Build Successful!"
    echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
else
    echo "Build Failed. Please check the output above."
fi
