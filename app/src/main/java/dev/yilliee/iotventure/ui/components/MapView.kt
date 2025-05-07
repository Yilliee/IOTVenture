package dev.yilliee.iotventure.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import dev.yilliee.iotventure.data.model.Challenge

@Composable
fun MapView(
    challenges: List<Challenge>,
    onChallengeSelected: (Challenge) -> Unit
) {
    val context = LocalContext.current
    val lahore = GeoPoint(31.5204, 74.3587)
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var isRefreshingLocation by remember { mutableStateOf(false) }
    
    // Initialize FusedLocationProviderClient
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            Configuration.getInstance().userAgentValue = context.packageName
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(lahore)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            
            // Add user location overlay if permission is granted
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
                myLocationOverlay.enableMyLocation()
                myLocationOverlay.enableFollowLocation()
                mapView.overlays.add(myLocationOverlay)
            }
            
            // Display actual challenges
            challenges.forEach { challenge ->
                try {
                    // Create a polygon for the challenge area
                    val polygon = Polygon().apply {
                        outlinePaint.color = 0x800000FF.toInt() // Semi-transparent blue
                        fillPaint.color = 0x400000FF.toInt() // More transparent blue
                        
                        // Create a box using the challenge coordinates
                        val points = listOf(
                            GeoPoint(challenge.location.topLeft.lat, challenge.location.topLeft.lng),
                            GeoPoint(challenge.location.topLeft.lat, challenge.location.bottomRight.lng),
                            GeoPoint(challenge.location.bottomRight.lat, challenge.location.bottomRight.lng),
                            GeoPoint(challenge.location.bottomRight.lat, challenge.location.topLeft.lng)
                        )
                        setPoints(points)
                        
                        // Add click listener
                        setOnClickListener { _, _, _ ->
                            try {
                                onChallengeSelected(challenge)
                            } catch (e: Exception) {
                                Log.e("MapView", "Error handling challenge selection", e)
                            }
                            true
                        }
                    }
                    
                    mapView.overlays.add(polygon)
                } catch (e: Exception) {
                    Log.e("MapView", "Error processing challenge ${challenge.id}", e)
                }
            }
            
            // Get user location if permission is granted
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED && !isRefreshingLocation
            ) {
                isRefreshingLocation = true
                val cancellationTokenSource = CancellationTokenSource()
                
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location: Location? ->
                    location?.let {
                        userLocation = GeoPoint(it.latitude, it.longitude)
                        mapView.controller.animateTo(userLocation)
                    }
                    isRefreshingLocation = false
                }.addOnFailureListener {
                    isRefreshingLocation = false
                }
            }
            
            mapView.invalidate()
        }
    )
} 