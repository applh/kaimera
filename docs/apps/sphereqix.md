# SphereQix App Documentation

## Overview
SphereQix is a 3D arcade game where the player controls a cursor on a spherical surface to capture territory while avoiding enemies. It is built using the LibGDX framework integrated into the Kaimera Android app.

## Architecture
The game runs within a standard Android Activity that hosts a LibGDX `AndroidApplication`.

### Package Structure
- `com.example.kaimera.sphereqix`:
  - `SphereQixActivity`: Android entry point.
  - `SphereQixGame`: Main LibGDX `Game` class.
  - `GameScreen`: Handles the main gameplay loop, rendering, and input.
  - `SphereMesh`: Manages the 3D Icosphere mesh, generation, and capture logic.
  - `PlayerController`: Handles player cursor movement and line drawing.
  - `EnemyManager`: Manages enemy entities (Orbiter, Chaser) and collision detection.
  - `LevelManager`: Controls game progression, scoring, and rules.
  - `InputController`: Handles camera orbital controls.

## Key Features

### 3D World
- **Icosphere**: The game world is a subdivided Icosahedron.
- **Camera**: Orbital camera with **pinch-to-zoom** and full 360-degree rotation (no pole clamping).
- **Settings**: In-game settings via dialog to adjust HUD color, Light color/intensity (HUD vs Camera Light), and grid density.
- **Home Navigation**: Standardized "Home" button in settings to return to Launcher.

### Gameplay
- **Capture**: Players draw lines to enclose areas. A graph-based flood fill algorithm determines captured regions.
- **Enemies**:
  - **Orbiter**: Moves in a fixed path around the sphere.
  - **Chaser**: Actively pursues the player or their drawing line.
- **Progression**: 3 levels with increasing difficulty (more enemies, higher capture requirements).

## Technical Specificities

### LibGDX Integration
- Uses `AndroidFragmentApplication` or `AndroidApplication` context.
- Custom Shaders (`ShaderProgram`) for rendering the sphere with vertex colors to indicate captured state.

### Algorithms
- **Mesh Generation**: Procedural generation of an Icosphere with subdivision.
- **Adjacency Graph**: Pre-calculated neighbor graph for faces to enable efficient flood fill.
- **Collision**: Ray-Sphere intersection for cursor placement; Distance-based checks for entity collisions.
