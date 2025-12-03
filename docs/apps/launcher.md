# Launcher App Documentation

## Overview
The Launcher App is the home screen of the Kaimera OS. It provides a customizable grid of applications and quick access to essential tools like Notes.

## Architecture
The launcher acts as the shell for the entire application suite.

### Package Structure
- `com.example.kaimera`: Root package.
  - `LauncherActivity`: The main home screen activity.
  - `LauncherApp`: Data model representing an installed/available app.
  - `LauncherAppsAdapter`: Adapter for the app grid.
  - `LauncherSettingsActivity`: Interface for customizing the launcher layout.

## Key Features

### App Grid
- **Customizable Layout**: Users can choose which apps to show/hide and reorder them via the Settings.
- **Dynamic Loading**: Apps are loaded dynamically based on the available activities in the package.

### Quick Access Panel
- **Quick Notes**: A bottom sheet panel that displays recent notes and allows quick creation of new ones without fully opening the Notes app.
- **Integration**: Directly queries the Notes database to display content.

### Settings
- **Visibility**: Toggle app visibility.
- **Ordering**: Drag-and-drop reordering of apps.

## Technical Specificities

### App Launching
- Uses explicit `Intents` to launch internal activities (Camera, Gallery, etc.).
- Can be extended to launch external packages using `PackageManager`.

### Persistence
- **SharedPreferences**: Stores the user's preferred app order and visibility settings.
- **Data Integrity**: Falls back to a default list if preferences are corrupted or missing.

### UI Components
- **BottomSheetDialog**: Used for the Quick Access Panel to provide a non-intrusive overlay.
- **RecyclerView**: Used for both the main app grid and the settings reordering list.
