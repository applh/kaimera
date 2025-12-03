package com.example.kaimera.camera.managers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val executor: Executor = Executors.newSingleThreadExecutor()

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onLocationResult: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            onLocationResult(null)
            return
        }

        // Try to get the last known location first as it's faster
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                onLocationResult(location)
            } else {
                // If last known location is null, request a fresh update
                requestFreshLocation(onLocationResult)
            }
        }.addOnFailureListener {
            // If failed, try fresh update
            requestFreshLocation(onLocationResult)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshLocation(onLocationResult: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            onLocationResult(null)
            return
        }

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            onLocationResult(location)
        }.addOnFailureListener {
            onLocationResult(null)
        }
    }
}
