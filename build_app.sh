#!/bin/bash
echo "Building Kaimera APK..."

# Ensure executable
chmod +x gradlew

# Run build
./gradlew assembleRelease --info

if [ $? -eq 0 ]; then
    echo "Build Successful!"
    echo "APK location: app/build/outputs/apk/release/app-release.apk"
else
    echo "Build Failed. Please check the output above."
fi
