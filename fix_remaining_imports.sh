#!/bin/bash

echo "Fixing remaining imports..."

# Add StorageManager import to GalleryActivity
if ! grep -q "import com.example.kaimera.camera.managers.StorageManager" app/src/main/java/com/example/kaimera/gallery/ui/GalleryActivity.kt; then
  sed -i '' '/^import com.example.kaimera.R$/a\
import com.example.kaimera.camera.managers.StorageManager\
import com.example.kaimera.gallery.adapters.GalleryAdapter
' app/src/main/java/com/example/kaimera/gallery/ui/GalleryActivity.kt
fi

# Add imports to MediaViewerActivity
if ! grep -q "import com.example.kaimera.camera.managers.StorageManager" app/src/main/java/com/example/kaimera/gallery/ui/MediaViewerActivity.kt; then
  sed -i '' '/^import com.example.kaimera.R$/a\
import com.example.kaimera.camera.managers.StorageManager\
import com.example.kaimera.camera.utils.ExifUtils
' app/src/main/java/com/example/kaimera/gallery/ui/MediaViewerActivity.kt
fi

# Add imports to ImageCaptureHelper if it exists
if [ -f app/src/main/java/com/example/kaimera/utils/ImageCaptureHelper.kt ]; then
  if ! grep -q "import com.example.kaimera.camera.managers.StorageManager" app/src/main/java/com/example/kaimera/utils/ImageCaptureHelper.kt; then
    sed -i '' '/^package/a\
\
import com.example.kaimera.camera.managers.StorageManager\
import com.example.kaimera.camera.utils.ExifUtils
' app/src/main/java/com/example/kaimera/utils/ImageCaptureHelper.kt
  fi
fi

echo "Imports fixed!"
