package com.example.kaimera.camera.managers
import com.example.kaimera.core.managers.PreferencesManager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import com.example.kaimera.camera.ui.components.LevelIndicatorView

/**
 * Manages device orientation and sensor events.
 * Handles screen rotation updates for the camera and accelerometer data for the level indicator.
 */
class OrientationManager(
    private val context: Context,
    private val levelIndicator: LevelIndicatorView,
    private val preferencesManager: PreferencesManager,
    private val onRotationChanged: (Int) -> Unit
) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            }

            updateOrientationAngles()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Do nothing
        }
    }

    private val orientationEventListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                return
            }

            val rotation = when (orientation) {
                in 45..134 -> Surface.ROTATION_270
                in 135..224 -> Surface.ROTATION_180
                in 225..314 -> Surface.ROTATION_90
                else -> Surface.ROTATION_0
            }

            onRotationChanged(rotation)
        }
    }

    fun start() {
        if (preferencesManager.isLevelIndicatorEnabled()) {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI)
            levelIndicator.visibility = View.VISIBLE
        } else {
            levelIndicator.visibility = android.view.View.GONE
        }

        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }
    }

    fun stop() {
        sensorManager.unregisterListener(sensorListener)
        orientationEventListener.disable()
    }
    
    fun refreshLevelIndicatorVisibility() {
        if (preferencesManager.isLevelIndicatorEnabled()) {
            if (levelIndicator.visibility != android.view.View.VISIBLE) {
                sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI)
                levelIndicator.visibility = View.VISIBLE
            }
        } else {
            sensorManager.unregisterListener(sensorListener)
            levelIndicator.visibility = android.view.View.GONE
        }
    }

    private fun updateOrientationAngles() {
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // orientationAngles[1] is pitch (x-axis tilt)
        // orientationAngles[2] is roll (y-axis tilt)
        // Values are in radians

        // orientationAngles[1] is pitch (x-axis tilt)
        // orientationAngles[2] is roll (y-axis tilt)
        // Values are in radians

        // Calculate angle for the level indicator (simplified)
        val angle = Math.toDegrees(Math.atan2(accelerometerReading[0].toDouble(), accelerometerReading[1].toDouble())).toFloat()
        levelIndicator.updateAngle(angle)
    }
}
