# Gallery App Documentation

## Overview
The Gallery App provides a centralized interface for viewing, managing, and sharing media captured by the Camera App or stored on the device. It supports both images and videos with a smooth, modern UI.

## Architecture
The app is built around a standard Activity-Adapter pattern.

### Package Structure
- `com.example.kaimera.gallery.ui`:
  - `GalleryActivity`: The main grid view of all media files.
  - `MediaViewerActivity`: Full-screen viewer for individual items, supporting zoom and playback.
- `com.example.kaimera.gallery.adapters`:
  - `GalleryAdapter`: RecyclerView adapter for the media grid.
  - `FilterAdapter`: Adapter for applying filters (shared with Camera UI).

## Key Features

### Media Grid
- **Layout**: Responsive grid layout that adapts to screen size.
- **Loading**: Efficient asynchronous image loading using the **Coil** library.
- **Indicators**: Visual indicators for video files and file types.

### Media Viewer
- **Zoom**: Custom `ZoomableImageView` allows pinch-to-zoom and pan for high-resolution inspection.
- **Video Playback**: Integrated video player for reviewing captured footage.
- **Navigation**: Swipe between media items in full-screen mode.

### Management
- **Deletion**: Delete unwanted files directly from the viewer or grid.
- **Sharing**: Standard Android share intent integration to send media to other apps.
- **Details**: View file metadata (size, resolution, date, path).

## Technical Specificities

### Image Loading
The app leverages **Coil (Coroutine Image Loader)** for:
- Memory caching.
- Disk caching.
- Bitmap pooling.
- Automatic thumbnail generation for videos.

### Custom Components
- `ZoomableImageView`: A custom view handling multi-touch gestures for scaling and translation, ensuring smooth performance even with large bitmaps.
- `ZoomableVideoLayout`: A wrapper for video playback that supports similar zoom gestures.

### File Operations
Uses the centralized `FileUtils` and `StorageManager` (from the Camera module) to perform safe file operations, ensuring consistency across the application.
