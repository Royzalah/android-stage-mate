package com.roei.stagemate.utilities

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.roei.stagemate.data.models.IsraeliLocations
import kotlin.math.*

// Wrapper for GPS location services per course standards.
// Accessed globally via MyApp.locationManager.
class LocationManager(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getLastLocation(callback: (lat: Double, lon: Double) -> Unit, onFailure: () -> Unit) {
        try {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    callback(location.latitude, location.longitude)
                } else {
                    onFailure()
                }
            }.addOnFailureListener {
                onFailure()
            }
        } catch (_: Exception) {
            onFailure()
        }
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    fun findNearestCity(lat: Double, lon: Double): String {
        return IsraeliLocations.cities.minByOrNull { city ->
            calculateDistance(lat, lon, city.latitude, city.longitude)
        }?.name ?: "Tel Aviv"
    }
}
