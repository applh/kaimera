# Notes App Documentation

## Overview
The Notes App is a modern, material design-inspired note-taking application. It demonstrates the use of Jetpack Compose for building declarative UIs and Room for robust local data persistence.

## Architecture
The app follows the **MVVM (Model-View-ViewModel)** architecture pattern.

### Package Structure
- `com.example.kaimera.notes.data`: Data layer.
  - `Note`: Entity class representing a single note.
  - `NoteDao`: Data Access Object defining database operations.
  - `NoteDatabase`: Room database configuration.
  - `NoteRepository`: Single source of truth for data, abstracting the DAO.
- `com.example.kaimera.notes.ui`: UI layer.
  - `NoteActivity`: Host activity for the Compose content.
  - `NoteViewModel`: Manages UI state and communicates with the repository.
  - `NoteApp`: Main Composable entry point handling navigation.
  - `NoteListScreen`: Displays the staggered grid of notes.
  - `NoteEditScreen`: Interface for creating and editing notes.

## Key Features

### Note Management
- **CRUD**: Create, Read, Update, and Delete notes.
- **Organization**: Pin important notes to the top; Archive old notes to keep the main view clean.
- **Search**: Real-time search functionality filtering by title and content.

### Visual Customization
- **Color Coding**: Assign one of several preset colors to each note for visual categorization.
- **Staggered Grid**: Notes are displayed in a masonry-style grid that adapts to content length.

## Technical Specificities

### Jetpack Compose
The entire UI is built using **Jetpack Compose**, Android's modern toolkit for building native UI.
- **State Management**: Uses `StateFlow` and `collectAsState` to reactively update the UI.
- **Navigation**: Uses `androidx.navigation.compose` for screen transitions.

### Room Database
- **Persistence**: Data is stored locally in an SQLite database via Room.
- **Coroutines**: All database operations are performed asynchronously using Kotlin Coroutines to prevent blocking the main thread.
- **Flow**: The DAO exposes `Flow<List<Note>>` to provide real-time updates to the UI whenever the data changes.

### Dependency Injection
- **ViewModelFactory**: A custom factory is used to inject the `NoteRepository` into the `NoteViewModel`, ensuring clean separation of concerns and testability.
