package com.flux.bank.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import com.flux.bank.domain.branch.GeoPoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Vị trí qua Fused Location Provider của Google Play services (trong nhà vẫn lấy được qua Wi-Fi/mạng). */
class FusedLocationSource(private val context: Context) : LocationSource {

    // Quyền đã được BranchMapFragment kiểm trước khi gọi (interface yêu cầu bên gọi kiểm)
    @SuppressLint("MissingPermission")
    override suspend fun current(): LocationResult {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(manager)) return LocationResult.Disabled
        val token = CancellationTokenSource()
        val location = try {
            withTimeoutOrNull(TIMEOUT_MS) {
                LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                    .await()
            }
        } finally {
            token.cancel()
        }
        return location?.let { LocationResult.Found(GeoPoint(it.latitude, it.longitude)) } ?: LocationResult.Timeout
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }
}
