package dev.yilliee.iotventure.screens.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import dev.yilliee.iotventure.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ClueMapScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedClue by remember { mutableStateOf<ClueMapPoint?>(null) }
    val cluePoints = remember { getMockCluePoints() }

    // Location state
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Create a reference to the MapView that can be accessed by the zoom buttons
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    // Check if we already have permission
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Request location permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    // Request permission if not granted
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
            // Map View
            AndroidView(
                factory = { ctx ->
                    // Initialize OSMDroid
                    Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

                    // Create MapView
                    MapView(ctx).apply {
                        tag = "mapView"
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true) // Keep multi-touch for pinch zoom
                        setBuiltInZoomControls(false)

                        // Disable built-in zoom controls
                        setBuiltInZoomControls(false)

                        // Set a higher zoom level (15.0 is city level, higher numbers = more zoomed in)
                        controller.setZoom(15.0)

                        // Set initial position to Pakistan (if no location available)
                        val initialPoint = GeoPoint(30.3753, 69.3451) // Center of Pakistan
                        controller.setCenter(initialPoint)

                        // Store reference to the MapView
                        mapViewRef.value = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    // Update map with clue points
                    mapView.overlays.clear()

                    // Add clue markers
                    cluePoints.forEach { clue ->
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(clue.latitude, clue.longitude)
                            title = clue.title
                            snippet = if (clue.isFound) "Found" else if (clue.isCurrent) "Current" else "Locked"
                            icon = ContextCompat.getDrawable(
                                context,
                                android.R.drawable.ic_menu_compass
                            )
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, _ ->
                                selectedClue = clue
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }

                    // Add paths between found clues
                    val completedClues = cluePoints.filter { it.isFound }
                    if (completedClues.size > 1) {
                        val path = Polyline().apply {
                            outlinePaint.color = android.graphics.Color.parseColor("#FFD700") // Gold color
                            outlinePaint.strokeWidth = 5f
                        }

                        completedClues.forEach { clue ->
                            path.addPoint(GeoPoint(clue.latitude, clue.longitude))
                        }

                        mapView.overlays.add(path)
                    }

                    // Add current path if there's a next clue
                    val lastCompletedClue = completedClues.lastOrNull()
                    val nextClue = cluePoints.find { it.isCurrent }
                    if (lastCompletedClue != null && nextClue != null) {
                        val currentPath = Polyline().apply {
                            outlinePaint.color = android.graphics.Color.parseColor("#FFD700") // Gold color
                            outlinePaint.strokeWidth = 5f
                            outlinePaint.alpha = 128 // Semi-transparent

                            // Add dashed effect
                            outlinePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 5f), 0f)
                        }

                        currentPath.addPoint(GeoPoint(lastCompletedClue.latitude, lastCompletedClue.longitude))
                        currentPath.addPoint(GeoPoint(nextClue.latitude, nextClue.longitude))

                        mapView.overlays.add(currentPath)
                    }

                    // Add user location marker if available
                    userLocation?.let { location ->
                        val userMarker = Marker(mapView).apply {
                            position = location
                            title = "Your Location"
                            icon = ContextCompat.getDrawable(
                                context,
                                android.R.drawable.ic_menu_mylocation
                            )
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        }
                        mapView.overlays.add(userMarker)
                    }

                    // Update location if permission granted
                    if (hasLocationPermission) {
                        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        try {
                            // Check if GPS provider is enabled
                            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                val locationListener = object : LocationListener {
                                    override fun onLocationChanged(location: Location) {
                                        userLocation = GeoPoint(location.latitude, location.longitude)
                                        mapView.controller.animateTo(userLocation)
                                        mapView.invalidate()
                                    }

                                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                                    override fun onProviderEnabled(provider: String) {}
                                    override fun onProviderDisabled(provider: String) {}
                                }

                                // Request location updates
                                locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    5000, // 5 seconds
                                    10f,  // 10 meters
                                    locationListener
                                )

                                // Get last known location
                                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { location ->
                                    userLocation = GeoPoint(location.latitude, location.longitude)
                                    mapView.controller.animateTo(userLocation)
                                }
                            }
                        } catch (e: SecurityException) {
                            // Handle permission exception
                        }
                    }

                    mapView.invalidate()
                }
            )

            // Only keep the location button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Zoom in button
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

                // Zoom out button
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

                // My location button
                FloatingActionButton(
                    onClick = {
                        userLocation?.let { location ->
                            mapViewRef.value?.controller?.animateTo(location)
                        }
                    },
                    containerColor = DarkSurface,
                    contentColor = Gold,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My Location",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Clue info card
            selectedClue?.let { clue ->
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
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (clue.isFound) SuccessGreen else Gold),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = clue.id.toString(),
                                    color = DarkBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = clue.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (clue.isFound) SuccessGreen else Gold,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = if (clue.isFound) "Found" else if (clue.isCurrent) "Current" else "Locked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                            }

                            IconButton(
                                onClick = { selectedClue = null }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextGray
                                )
                            }
                        }

                        if (clue.isFound || clue.isCurrent) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = clue.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextWhite
                            )

                            if (clue.isCurrent) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Gold.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Gold,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Hint: ${clue.hint}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gold
                                    )
                                }
                            }
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
                text = "Clue Map",
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

private fun getMockCluePoints(): List<ClueMapPoint> {
    return listOf(
        ClueMapPoint(
            id = 1,
            title = "Starting Point",
            description = "The adventure begins at the campus entrance.",
            hint = "Look for the large stone sign.",
            latitude = 33.6844,
            longitude = 73.0479,
            isFound = true,
            isCurrent = false
        ),
        ClueMapPoint(
            id = 2,
            title = "The Ancient Library",
            description = "Find the oldest book in the library's special collection.",
            hint = "Check the glass display case on the second floor.",
            latitude = 33.6900,
            longitude = 73.0550,
            isFound = true,
            isCurrent = false
        ),
        ClueMapPoint(
            id = 3,
            title = "The Clock Tower",
            description = "Discover the secret behind the clock tower's unusual chimes.",
            hint = "Count the number of chimes at noon.",
            latitude = 33.6950,
            longitude = 73.0500,
            isFound = true,
            isCurrent = false
        ),
        ClueMapPoint(
            id = 4,
            title = "The Hidden Garden",
            description = "Find the rare flower that blooms only at midnight.",
            hint = "Look for a garden entrance behind the science building.",
            latitude = 33.7000,
            longitude = 73.0600,
            isFound = false,
            isCurrent = true
        ),
        ClueMapPoint(
            id = 5,
            title = "The Professor's Office",
            description = "Locate the professor's secret research notes.",
            hint = "The office is on the top floor of the mathematics building.",
            latitude = 33.7050,
            longitude = 73.0450,
            isFound = false,
            isCurrent = false
        ),
        ClueMapPoint(
            id = 6,
            title = "The Underground Tunnel",
            description = "Navigate the forgotten tunnels beneath the campus.",
            hint = "The entrance is hidden in the oldest dormitory's basement.",
            latitude = 33.7100,
            longitude = 73.0650,
            isFound = false,
            isCurrent = false
        ),
        ClueMapPoint(
            id = 7,
            title = "The Final Secret",
            description = "Uncover the ultimate mystery of the treasure hunt.",
            hint = "Return to where it all began, but look up instead of forward.",
            latitude = 33.6844,
            longitude = 73.0550,
            isFound = false,
            isCurrent = false
        )
    )
}
