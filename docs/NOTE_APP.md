# Note App Documentation

## Overview
The Note App is a new feature in Kaimera that allows users to take quick notes, organize them with colors, and pin important information. It is built using Jetpack Compose and Room Database.

## Features

### Note Management
- **Create**: Add new notes with a title and content.
- **Edit**: Modify existing notes.
- **Delete**: Remove unwanted notes.
- **Color Coding**: Assign one of 16 colors to each note for visual organization.

### Organization
- **Pinning**: Pin important notes to keep them at the top of the list.
- **Archiving**: Archive notes to hide them from the main view without deleting them.
- **Search**: Filter notes by title or content using the search bar.

### Launcher Integration
- **App Icon**: Launch the full Note App from the main launcher grid.
- **Quick Access Panel**: Access recent notes and create new ones instantly from the launcher using the "Quick Notes" FAB.

## Technical Details

### Architecture
- **MVVM**: Uses Model-View-ViewModel architecture.
- **Jetpack Compose**: UI is built entirely with Compose.
- **Room**: Data is persisted locally in a SQLite database using Room.
- **Coroutines/Flow**: Asynchronous operations and data observation.

### Database Schema
The `notes` table contains the following columns:
- `id`: Integer (Primary Key, Auto-increment)
- `title`: Text
- `content`: Text
- `color`: Integer (ARGB value)
- `isPinned`: Boolean
- `isArchived`: Boolean
- `timestamp`: Long
- `labels`: Text (Comma-separated)

## Usage
1.  **Open**: Tap the "Notes" icon in the launcher.
2.  **Create**: Tap the "+" FAB.
3.  **Edit**: Tap any note card.
4.  **Quick Access**: Tap the "Edit" icon FAB on the launcher screen to open the Quick Access Panel.
