package com.example.creation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object RecipeLocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    /**
     * Pedido único (one-shot) com PRIORITY_BALANCED_POWER_ACCURACY (Semana 8).
     * Devolve "Cidade, País", coordenadas formatadas ou null se não houver fix GPS.
     */
    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentRecipeLocationLabel(context: Context): String? {
        if (!hasLocationPermission(context)) return null

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()

        val location: Location? = suspendCancellableCoroutine { continuation ->
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { loc ->
                if (continuation.isActive) continuation.resume(loc)
            }.addOnFailureListener {
                if (continuation.isActive) continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }

        if (location == null) return null

        return reverseGeocodeOrCoordinates(context, location.latitude, location.longitude)
    }

    private suspend fun reverseGeocodeOrCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String = withContext(Dispatchers.IO) {
        val fallbackCoords = String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
        try {
            if (!Geocoder.isPresent()) return@withContext fallbackCoords
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                val addr = addresses.firstOrNull()
                                val label = listOfNotNull(addr?.locality, addr?.countryName)
                                    .joinToString(", ")
                                    .ifBlank { fallbackCoords }
                                if (cont.isActive) cont.resume(label)
                            }

                            override fun onError(errorMessage: String?) {
                                // Garante que a coroutine não fica pendurada em Modo Avião / offline
                                if (cont.isActive) cont.resume(fallbackCoords)
                            }
                        }
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                val addr = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                listOfNotNull(addr?.locality, addr?.countryName)
                    .joinToString(", ")
                    .ifBlank { fallbackCoords }
            }
        } catch (_: Exception) {
            fallbackCoords
        }
    }
}