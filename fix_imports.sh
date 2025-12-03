#!/bin/bash

# Script to fix imports after code reorganization

echo "Fixing imports..."

# Fix PreferencesManager imports
find app/src/main/java/com/example/kaimera -name "*.kt" -type f -exec sed -i '' 's/import com\.example\.kaimera\.managers\.PreferencesManager/import com.example.kaimera.core.managers.PreferencesManager/g' {} \;

# Fix PermissionManager imports  
find app/src/main/java/com/example/kaimera -name "*.kt" -type f -exec sed -i '' 's/import com\.example\.kaimera\.managers\.PermissionManager/import com.example.kaimera.core.managers.PermissionManager/g' {} \;

# Fix ZoomableImageView imports
find app/src/main/java/com/example/kaimera -name "*.kt" -type f -exec sed -i '' 's/import com\.example\.kaimera\.ZoomableImageView/import com.example.kaimera.core.ui.components.ZoomableImageView/g' {} \;

# Fix ZoomableVideoLayout imports
find app/src/main/java/com/example/kaimera -name "*.kt" -type f -exec sed -i '' 's/import com\.example\.kaimera\.ZoomableVideoLayout/import com.example.kaimera.core.ui.components.ZoomableVideoLayout/g' {} \;

# Add imports for moved activities in LauncherApp.kt
echo "Adding activity imports to LauncherApp.kt..."

# Add imports for migrated activities to files that need them
grep -l "ColorLightActivity" app/src/main/java/com/example/kaimera/*.kt 2>/dev/null | while read file; do
    if ! grep -q "import com.example.kaimera.colorlight.ui.ColorLightActivity" "$file"; then
        sed -i '' '1a\
import com.example.kaimera.colorlight.ui.ColorLightActivity
' "$file"
    fi
done

grep -l "ChronometerActivity" app/src/main/java/com/example/kaimera/*.kt 2>/dev/null | while read file; do
    if ! grep -q "import com.example.kaimera.chronometer.ui.ChronometerActivity" "$file"; then
        sed -i '' '1a\
import com.example.kaimera.chronometer.ui.ChronometerActivity
' "$file"
    fi
done

grep -l "MmsActivity" app/src/main/java/com/example/kaimera/*.kt 2>/dev/null | while read file; do
    if ! grep -q "import com.example.kaimera.mms.ui.MmsActivity" "$file"; then
        sed -i '' '1a\
import com.example.kaimera.mms.ui.MmsActivity
' "$file"
    fi
done

grep -l "DebugLogActivity" app/src/main/java/com/example/kaimera/**/*.kt 2>/dev/null | while read file; do
    if ! grep -q "import com.example.kaimera.debug.ui.DebugLogActivity" "$file"; then
        sed -i '' '1a\
import com.example.kaimera.debug.ui.DebugLogActivity
' "$file"
    fi
done

echo "Import fixes complete!"
