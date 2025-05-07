package dev.yilliee.iotventure.screens.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import dev.yilliee.iotventure.R
import dev.yilliee.iotventure.data.model.Challenge
import dev.yilliee.iotventure.data.repository.GameRepository
import dev.yilliee.iotventure.di.ServiceLocator
import dev.yilliee.iotventure.ui.theme.DarkBackground
import dev.yilliee.iotventure.ui.theme.DarkSurface
import dev.yilliee.iotventure.ui.theme.Gold
import dev.yilliee.iotventure.ui.theme.TextGray
import dev.yilliee.iotventure.ui.theme.TextWhite
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ClueMapScreen(
    onBackClick: () -> Unit,
    onScanClick: (Challenge) -> Unit,
    initialChallengeId: Int = -1
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val challenges = remember { mutableStateOf<List<Challenge>>(emptyList()) }
    val userLocation = remember { mutableStateOf<GeoPoint?>(null) }
    val selectedChallenge = remember { mutableStateOf<Challenge?>(null) }
    val hasLocationPermission = remember { mutableStateOf(false) }
    val shouldCenterOnUser = remember { mutableStateOf(initialChallengeId == -1) }
    val initialChallenge = remember { mutableStateOf<Challenge?>(null) }

    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        try {
            val osmdroidDir = File(context.getExternalFilesDir(null), "osmdroid")
            if (!osmdroidDir.exists()) {
                osmdroidDir.mkdirs()
            }

            Configuration.getInstance().apply {
                osmdroidBasePath = osmdroidDir
                osmdroidTileCache = osmdroidDir
                tileFileSystemCacheMaxBytes = 1024L * 1024L * 50L // 50MB
                tileFileSystemCacheTrimBytes = 1024L * 1024L * 40L // 40MB
                tileDownloadThreads = 8
                tileFileSystemThreads = 16
                userAgentValue = "IOTVenture/1.0"
                gpsWaitTime = 1000L
            }
            Log.d("ClueMapScreen", "OSMDroid configuration initialized with path: ${osmdroidDir.absolutePath}")
        } catch (e: Exception) {
            Log.e("ClueMapScreen", "Error initializing OSMDroid configuration", e)
        }
    }

    // Request location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission.value = isGranted
        Log.d("ClueMapScreen", "Location permission: $isGranted")
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Load challenges
    LaunchedEffect(Unit) {
        try {
            val gameRepository = ServiceLocator.provideGameRepository(context)
            gameRepository.getChallenges().collectLatest { loadedChallenges ->
                challenges.value = loadedChallenges
                Log.d("ClueMapScreen", "Loaded ${loadedChallenges.size} challenges")

                // If we have an initial challenge ID, find and set it
                if (initialChallengeId != -1) {
                    val challenge = loadedChallenges.find { it.id == initialChallengeId }
                    if (challenge != null) {
                        initialChallenge.value = challenge
                        shouldCenterOnUser.value = false
                        Log.d("ClueMapScreen", "Set initial challenge: ${challenge.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ClueMapScreen", "Error loading challenges", e)
        }
    }

    // Set up location updates using FusedLocationProviderClient
    LaunchedEffect(hasLocationPermission.value) @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) {
        if (hasLocationPermission.value) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                    .setMinUpdateIntervalMillis(5000)
                    .build()

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        locationResult.lastLocation?.let { location ->
                            val geoPoint = GeoPoint(location.latitude, location.longitude)
                            userLocation.value = geoPoint
                            Log.d("ClueMapScreen", "Location updated: $geoPoint")

                            if (shouldCenterOnUser.value) {
                                mapViewRef.value?.controller?.animateTo(geoPoint)
                            } else  {
                                // Center on challenge location if one is selected
                                val challenge = initialChallenge.value!!
                                val challengeLocation = GeoPoint(
                                    challenge.location.topLeft.lat-0.0009, // Add slight padding to the top
                                    challenge.location.bottomRight.lng - 0.0014 // Add slight padding to the left
                                )
                                mapViewRef.value?.controller?.animateTo(challengeLocation)
                            }
                        }
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    context.mainLooper
                )

                // Get initial location
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        userLocation.value = geoPoint
                        Log.d("ClueMapScreen", "Initial location: $geoPoint")

                        if (shouldCenterOnUser.value) {
                            mapViewRef.value?.controller?.animateTo(geoPoint)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ClueMapScreen", "Error setting up location updates", e)
            }
        }
    }

    // Handle map overlays
    LaunchedEffect(challenges.value, userLocation.value, hasLocationPermission.value) {
        mapViewRef.value?.let { mapView ->
            try {
                mapView.overlays.clear()

                // Add user location marker
                userLocation.value?.let { location ->
                    val locationOverlay = Marker(mapView).apply {
                        position = location
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_my_location)
                        setOnMarkerClickListener { _, _ -> true }
                    }
                    mapView.overlays.add(locationOverlay)
                }

                // Add challenge markers
                challenges.value.forEach { challenge ->
                    try {
                        val geoPoint = GeoPoint(
                            challenge.location.topLeft.lat,
                            challenge.location.topLeft.lng
                        )

                        // Create polygon for challenge area
                        val polygon = Polygon(mapView).apply {
                            outlinePaint.color = dev.yilliee.iotventure.ui.theme.Gold.toArgb()
                            outlinePaint.strokeWidth = 3f
                            fillPaint.color = dev.yilliee.iotventure.ui.theme.Gold.copy(alpha = 0.2f).toArgb()

                            // Create rectangle points
                            val points = listOf(
                                GeoPoint(challenge.location.topLeft.lat, challenge.location.topLeft.lng),
                                GeoPoint(challenge.location.topLeft.lat, challenge.location.bottomRight.lng),
                                GeoPoint(challenge.location.bottomRight.lat, challenge.location.bottomRight.lng),
                                GeoPoint(challenge.location.bottomRight.lat, challenge.location.topLeft.lng)
                            )
                            setPoints(points)
                        }
                        mapView.overlays.add(polygon)

                        val marker = Marker(mapView).apply {
                            position = geoPoint
                            title = challenge.name
                            snippet = "${challenge.points} points"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_location_on)

                            setOnMarkerClickListener { _, _ ->
                                selectedChallenge.value = challenge
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    } catch (e: Exception) {
                        Log.e("ClueMapScreen", "Error creating marker for challenge ${challenge.id}", e)
                    }
                }

                mapView.invalidate()
            } catch (e: Exception) {
                Log.e("ClueMapScreen", "Error updating map overlays", e)
            }
        }
    }

    Scaffold(
        topBar = {
            MapTopBar(
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(18.0)
                        mapViewRef.value = this

                        // Disable default OSMDroid buttons
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                        try {
                            // Add location overlay
                            if (hasLocationPermission.value) {
                                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
                                locationOverlay.enableMyLocation()
                                locationOverlay.enableFollowLocation()
                                overlays.add(locationOverlay)
                            }

                            // If we have an initial challenge, center on it
                            initialChallenge.value?.let { challenge ->
                                try {
                                    val geoPoint = GeoPoint(
                                        challenge.location.topLeft.lat,
                                        challenge.location.topLeft.lng
                                    )
                                    controller.animateTo(geoPoint)
                                    Log.d("ClueMapScreen", "Centered on initial challenge: ${challenge.name}")
                                } catch (e: Exception) {
                                    Log.e("ClueMapScreen", "Error centering on initial challenge", e)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ClueMapScreen", "Error initializing MapView", e)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Map Controls Column
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                // Zoom In Button
                FloatingActionButton(
                    onClick = {
                        mapViewRef.value?.controller?.zoomIn()
                    },
                    containerColor = DarkSurface,
                    contentColor = Gold,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Zoom Out Button
                FloatingActionButton(
                    onClick = {
                        mapViewRef.value?.controller?.zoomOut()
                    },
                    containerColor = DarkSurface,
                    contentColor = Gold,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // My Location Button
            if (selectedChallenge.value == null) {
                FloatingActionButton(
                    onClick = {
                        userLocation.value?.let { location ->
                            mapViewRef.value?.controller?.animateTo(location)
                            shouldCenterOnUser.value = true
                        }
                    },
                    containerColor = DarkSurface,
                    contentColor = Gold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My Location",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Challenge popup
            selectedChallenge.value?.let { challenge ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = challenge.name,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${challenge.points} points",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onScanClick(challenge) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Scan NFC Token")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { selectedChallenge.value = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapTopBar(
    onBackClick: () -> Unit
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextWhite
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Challenge Map",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite
            )
        }
    }
}

data class ClueMapPoint(
    val id: Int,
    val title: String,
    val description: String,
    val hint: String,
    val latitude: Double,
    val longitude: Double,
    val isFound: Boolean,
    val isCurrent: Boolean
)




// Helper function to update user location
private fun updateUserLocation(context: Context, onLocationUpdate: (GeoPoint) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onLocationUpdate(GeoPoint(location.latitude, location.longitude))
                }
            }
    } catch (e: SecurityException) {
        Log.e("ClueMapScreen", "Error getting location: ${e.message}")
    }
}

// Helper function to check if a point is inside a polygon
private fun isPointInPolygon(point: GeoPoint, polygon: List<GeoPoint>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        if ((polygon[i].latitude > point.latitude) != (polygon[j].latitude > point.latitude) &&
            (point.longitude < (polygon[j].longitude - polygon[i].longitude) * (point.latitude - polygon[i].latitude) /
                    (polygon[j].latitude - polygon[i].latitude) + polygon[i].longitude)
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}