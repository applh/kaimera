#!/bin/bash

echo "Updating package declarations for Phase 4..."

# Gallery files
sed -i '' 's/^package com\.example\.kaimera$/package com.example.kaimera.gallery.ui/' app/src/main/java/com/example/kaimera/gallery/ui/GalleryActivity.kt
sed -i '' 's/^package com\.example\.kaimera$/package com.example.kaimera.gallery.ui/' app/src/main/java/com/example/kaimera/gallery/ui/MediaViewerActivity.kt
sed -i '' 's/^package com\.example\.kaimera$/package com.example.kaimera.gallery.adapters/' app/src/main/java/com/example/kaimera/gallery/adapters/GalleryAdapter.kt
sed -i '' 's/^package com\.example\.kaimera$/package com.example.kaimera.gallery.adapters/' app/src/main/java/com/example/kaimera/gallery/adapters/FilterAdapter.kt

# Camera UI files
sed -i '' 's/^package com\.example\.kaimera$/package com.example.kaimera.camera.ui/' app/src/main/java/com/example/kaimera/camera/ui/MainActivity.kt
sed -i '' 's/^package com\.example\.kaimera$/package com.example.kaimera.camera.ui/' app/src/main/java/com/example/kaimera/camera/ui/PreviewActivity.kt
sed -i '' 's/^package com\.example\.kaimera$/package com.example.kaimera.camera.ui/' app/src/main/java/com/example/kaimera/camera/ui/SettingsActivity.kt

# Camera components
sed -i '' 's/^package com\.example\.kaimera$/package com.example.kaimera.camera.ui.components/' app/src/main/java/com/example/kaimera/camera/ui/components/GridOverlayView.kt
sed -i '' 's/^package com\.example\.kaimera$/package com.example.kaimera.camera.ui.components/' app/src/main/java/com/example/kaimera/camera/ui/components/LevelIndicatorView.kt

# Camera fragments
for file in app/src/main/java/com/example/kaimera/camera/fragments/*.kt; do
  sed -i '' 's/^package com\.example\.kaimera\.fragments$/package com.example.kaimera.camera.fragments/' "$file"
done

# Camera managers
for file in app/src/main/java/com/example/kaimera/camera/managers/*.kt; do
  sed -i '' 's/^package com\.example\.kaimera\.managers$/package com.example.kaimera.camera.managers/' "$file"
done

# Camera utils
sed -i '' 's/^package com\.example\.kaimera$/package com.example.kaimera.camera.utils/' app/src/main/java/com/example/kaimera/camera/utils/ExifUtils.kt

echo "Package declarations updated!"
