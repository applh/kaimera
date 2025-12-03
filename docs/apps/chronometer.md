# Chronometer App Documentation

## Overview
The Chronometer App provides precise time measurement capabilities. It includes a standard stopwatch with lap functionality and an innovative audio-triggered start/stop feature.

## Architecture
The app separates UI from timing logic.

### Package Structure
- `com.example.kaimera.chronometer.ui`:
  - `ChronometerActivity`: Displays the timer and lap list.
- `com.example.kaimera.camera.managers`: (Note: Currently shared/located in camera managers, planned for migration)
  - `ChronometerManager`: Handles the timing loop and audio detection logic.

## Key Features

### Stopwatch
- **Precision**: Millisecond-level accuracy using `SystemClock.elapsedRealtime()`.
- **Laps**: Record split times without stopping the main timer.
- **Persistence**: Timer state is maintained even if the activity is recreated (e.g., rotation).

### Audio Trigger
- **Hands-Free Control**: Start or stop the timer by clapping or making a loud noise.
- **Threshold**: Configurable decibel threshold to prevent accidental triggers.

## Technical Specificities

### Timing Loop
Uses a `Handler` and `Runnable` loop to update the UI efficiently without blocking the main thread, while the actual time calculation relies on system boot time deltas to ensure accuracy.

### Audio Processing
- **AudioRecord**: Captures raw audio input from the microphone.
- **Amplitude Analysis**: Calculates the root mean square (RMS) amplitude to determine the noise level in decibels.
- **Debounce**: Implements a cooldown period between triggers to prevent double-taps from a single sound.
