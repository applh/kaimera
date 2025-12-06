# Text3D App Documentation

## Overview
Text3D is a creative tool that generates 3D volumetric text. It allows users to input custom text, adjust its color, and extrusion depth, and view it in 3D space. It is built using the LibGDX framework integrated into the Kaimera Android app.

## Architecture
The app runs within a standard Android Activity (`Text3DActivity`) that hosts a LibGDX `Game` instance.

### Package Structure
- `com.example.kaimera.text3d`:
  - `Text3DActivity`: Android entry point. It manages the UI overlay (Settings dialog, FABs) and the LibGDX view.
  - `Text3DGame`: Main LibGDX `Game` class responsible for 3D rendering and camera control.

## Key Features
- **Customizable Text**: Update text content and HUD labels dynamically.
- **3D Extrusion**: Configurable depth for the 3D text effect.
- **Visual Customization**: HSV color picker for text personalization.
- **Interactive Camera**: Orbital camera controls (pan, rotate) to view text from any angle.
- **Snapshot**: (Planned) Output 3D text as images.

## Key Techniques

### Volumetric Text via Layer Stacking
To achieve the 3D "extrusion" effect without complex mesh generation, the app uses a **Layer Stacking** technique:

1.  **Multiple Passes**: The same text glyphs are rendered multiple times in a loop.
2.  **Z-Offset**: Each render pass is slightly offset in the Z-axis (depth).
3.  **Density**: The number of layers is dynamically calculated based on the requested `extrusionDepth` to ensure a dense, solid appearance without visible gaps.
4.  **Shading**: The "side" layers (internal layers) are tinted slightly darker (`0.6f` brightness) than the front face. This artificial shading creates a strong perception of solid volume and 3D form.

```kotlin
// Simplified Logic
val layers = (extrusionDepth * 2)
val layerDepth = totalDepth / layers
for (i in layers downTo 1) {
    matrix.translate(0f, 0f, -i * layerDepth)
    font.color = shadowColor // Darker
    font.draw(batch, text)
}
// Draw Front Face
matrix.translate(0f, 0f, 0f)
font.color = mainColor
font.draw(batch, text)
```


### Limitations
- **No True Geometry**: The side surfaces are an illusion created by the stacked edges. There is no continuous mesh connecting the front and back faces.
- **Visual Artifacts**: From steep angles or very close up, the individual layers may be visible ("ridges"). This is an expected trade-off for the performance flexibility of rendering arbitrary text dynamically.
