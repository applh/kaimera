#!/bin/bash

echo "Adding imports for Phase 4 files..."

# Add R class imports to gallery files
for file in app/src/main/java/com/example/kaimera/gallery/ui/*.kt app/src/main/java/com/example/kaimera/gallery/adapters/*.kt; do
  if ! grep -q "import com.example.kaimera.R" "$file"; then
    sed -i '' '/^package/a\
\
import com.example.kaimera.R
' "$file"
  fi
done

# Add R class imports to camera files
for file in app/src/main/java/com/example/kaimera/camera/ui/*.kt app/src/main/java/com/example/kaimera/camera/ui/components/*.kt app/src/main/java/com/example/kaimera/camera/fragments/*.kt app/src/main/java/com/example/kaimera/camera/utils/*.kt; do
  if ! grep -q "import com.example.kaimera.R" "$file"; then
    sed -i '' '/^package/a\
\
import com.example.kaimera.R
' "$file"
  fi
done

# Update manager imports in camera managers
find app/src/main/java/com/example/kaimera/camera/managers -name "*.kt" -exec sed -i '' 's/import com\.example\.kaimera\.managers\.\([A-Z][a-zA-Z]*\)/import com.example.kaimera.camera.managers.\1/g' {} \;

# Update fragment imports
find app/src/main/java/com/example/kaimera/camera -name "*.kt" -exec sed -i '' 's/import com\.example\.kaimera\.fragments\.\([A-Z][a-zA-Z]*\)/import com.example.kaimera.camera.fragments.\1/g' {} \;

# Update component imports  
find app/src/main/java/com/example/kaimera -name "*.kt" -exec sed -i '' 's/import com\.example\.kaimera\.GridOverlayView/import com.example.kaimera.camera.ui.components.GridOverlayView/g' {} \;
find app/src/main/java/com/example/kaimera -name "*.kt" -exec sed -i '' 's/import com\.example\.kaimera\.LevelIndicatorView/import com.example.kaimera.camera.ui.components.LevelIndicatorView/g' {} \;

# Update ExifUtils imports
find app/src/main/java/com/example/kaimera -name "*.kt" -exec sed -i '' 's/import com\.example\.kaimera\.ExifUtils/import com.example.kaimera.camera.utils.ExifUtils/g' {} \;

# Update adapter imports
find app/src/main/java/com/example/kaimera -name "*.kt" -exec sed -i '' 's/import com\.example\.kaimera\.GalleryAdapter/import com.example.kaimera.gallery.adapters.GalleryAdapter/g' {} \;
find app/src/main/java/com/example/kaimera -name "*.kt" -exec sed -i '' 's/import com\.example\.kaimera\.FilterAdapter/import com.example.kaimera.gallery.adapters.FilterAdapter/g' {} \;

echo "Imports added!"
