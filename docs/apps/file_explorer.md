# File Explorer App Documentation

## Overview
The File Explorer App allows users to navigate the device's file system, manage files, and open them with appropriate applications. It serves as a utility for managing local storage and accessing downloaded or captured content.

## Architecture
The app uses a simple list-based interface.

### Package Structure
- `com.example.kaimera.files.ui`:
  - `FileExplorerActivity`: Main activity handling navigation and file operations.
- `com.example.kaimera.files.adapters`:
  - `FileExplorerAdapter`: RecyclerView adapter for displaying file lists.

## Key Features

### Navigation
- **Directory Browsing**: Navigate through folders in the app's private storage and public directories (where permitted).
- **Breadcrumbs**: (Implicit) Back navigation moves up the directory tree.

### File Operations
- **Open**: Launches appropriate viewers for known file types (Images, Videos, Text).
- **Delete**: Long-press to delete files or directories recursively.
- **Details**: Displays file size and modification date.

## Technical Specificities

### File Handling
- **FileUtils**: Uses the centralized `FileUtils` class for:
  - Recursive directory deletion.
  - MIME type detection based on file extensions.
  - Human-readable file size formatting.

### Intents
- Uses `Intent.ACTION_VIEW` with appropriate MIME types to delegate file opening to other apps (e.g., Gallery for images, System Video Player for videos).

### Permissions
- Relies on `READ_EXTERNAL_STORAGE` (and `MANAGE_EXTERNAL_STORAGE` where applicable/requested) to access files outside the app-specific directories.
