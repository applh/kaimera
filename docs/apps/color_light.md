# Color Light App Documentation

## Overview
The Color Light App turns the device's screen into a customizable light source. It is useful for photography lighting, reading in the dark, or mood lighting.

## Architecture
The app uses a single full-screen Activity.

### Package Structure
- `com.example.kaimera.colorlight.ui`:
  - `ColorLightActivity`: Handles touch inputs and screen rendering.

## Key Features

### Light Control
- **Hue**: Drag horizontally on the control pad to change the color hue.
- **Brightness**: Drag vertically to adjust the screen brightness.
- **Shortcuts**: Tap top/bottom areas for instant 100% or 0% brightness.

### UI Experience
- **Immersive Mode**: Hides system bars for a distraction-free full-screen light.
- **Auto-Hide Controls**: Labels and controls fade away after a few seconds of inactivity.

## Technical Specificities

### Screen Brightness
The app overrides the system brightness setting for the current window using `WindowManager.LayoutParams.screenBrightness`. This allows the app to be brighter or dimmer than the system setting without affecting global preferences.

### Color Model
Colors are calculated using the **HSV (Hue, Saturation, Value)** model, allowing for smooth transitions across the color spectrum while maintaining consistent brightness levels.
