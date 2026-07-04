package com.example.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.ParkingLot

@Composable
fun LeafletMapView(
    parkingLots: List<ParkingLot>,
    onLotSelected: (ParkingLot) -> Unit,
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier
) {
    var mapType by remember { mutableStateOf("streets") } // streets, satellite
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val baseMapUrl = if (mapType == "streets") {
        "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
    } else {
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var userLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isFarFromLots by remember { mutableStateOf(false) }

    // Fetch and monitor user's actual location via LocationManager when permission is granted
    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            onDispose {}
        } else {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            var listener: LocationListener? = null
            
            if (locationManager != null) {
                try {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        
                        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                        
                        if (gpsEnabled || networkEnabled) {
                            // Try to get best last known location immediately from enabled providers
                            val providers = locationManager.getProviders(true)
                            var bestLocation: Location? = null
                            for (provider in providers) {
                                try {
                                    val loc = locationManager.getLastKnownLocation(provider) ?: continue
                                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                                        bestLocation = loc
                                    }
                                } catch (se: SecurityException) {
                                    Log.e("LeafletMapView", "SecurityException checking provider $provider: ${se.message}")
                                }
                            }
                            
                            if (bestLocation != null) {
                                userLatLng = Pair(bestLocation.latitude, bestLocation.longitude)
                                Log.d("LeafletMapView", "Kotlin LocationManager fetched initial location: ${bestLocation.latitude}, ${bestLocation.longitude}")
                            }
                            
                            // Define the listener
                            val localListener = object : LocationListener {
                                override fun onLocationChanged(location: Location) {
                                    userLatLng = Pair(location.latitude, location.longitude)
                                    Log.d("LeafletMapView", "Kotlin LocationManager onLocationChanged: ${location.latitude}, ${location.longitude}")
                                }
                                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                                override fun onProviderEnabled(provider: String) {}
                                override fun onProviderDisabled(provider: String) {}
                            }
                            listener = localListener
                            
                            // Pick the best provider available (avoid PASSIVE_PROVIDER fallback unless necessary)
                            val provider = if (gpsEnabled) {
                                LocationManager.GPS_PROVIDER
                            } else {
                                LocationManager.NETWORK_PROVIDER
                            }
                            
                            try {
                                locationManager.requestLocationUpdates(
                                    provider,
                                    5000L, // check every 5 seconds
                                    10f,   // 10 meters change
                                    localListener,
                                    android.os.Looper.getMainLooper()
                                )
                                Log.d("LeafletMapView", "Kotlin LocationManager registered updates from $provider")
                            } catch (se: SecurityException) {
                                Log.e("LeafletMapView", "SecurityException requesting updates from $provider: ${se.message}")
                            }
                        } else {
                            Log.d("LeafletMapView", "Location providers are disabled. Bypassing active location queries.")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LeafletMapView", "Error fetching/monitoring location: ${e.message}")
                }
            }
            
            onDispose {
                if (locationManager != null && listener != null) {
                    try {
                        locationManager.removeUpdates(listener)
                        Log.d("LeafletMapView", "Kotlin LocationManager removed updates successfully")
                    } catch (e: Exception) {
                        Log.e("LeafletMapView", "Error removing location updates on dispose: ${e.message}")
                    }
                }
            }
        }
    }

    // Determine if user is far from the central parking lots (Coimbatore center is 11.0183, 76.9725)
    LaunchedEffect(userLatLng) {
        val latLng = userLatLng
        if (latLng != null) {
            val coimbatoreLat = 11.0183
            val coimbatoreLng = 76.9725
            val results = FloatArray(1)
            Location.distanceBetween(
                latLng.first, latLng.second,
                coimbatoreLat, coimbatoreLng,
                results
            )
            val distanceKm = results[0] / 1000f
            isFarFromLots = distanceKm > 100f
            Log.d("LeafletMapView", "User distance to Coimbatore: ${distanceKm} km. Far: $isFarFromLots")
        } else {
            isFarFromLots = false
        }
    }

    val parkingLotsJson = remember(parkingLots) {
        val jsonArray = JSONArray()
        parkingLots.forEach { lot ->
            val lat = when (lot.id) {
                1L -> 11.0183
                2L -> 11.0150
                3L -> 11.0250
                4L -> 11.0100
                else -> 11.0100 + ((lot.id * 7) % 11) * 0.0025
            }
            val lng = when (lot.id) {
                1L -> 76.9600
                2L -> 76.9725
                3L -> 76.9850
                4L -> 76.9550
                else -> 76.9550 + ((lot.id * 3) % 13) * 0.0020
            }
            val lotJson = JSONObject().apply {
                put("id", lot.id)
                put("name", lot.name)
                put("lat", lat)
                put("lng", lng)
                put("price", lot.pricePerHour)
                put("isFull", lot.availableSlots == 0)
            }
            jsonArray.put(lotJson)
        }
        jsonArray.toString()
    }

    val jsInterface = remember {
        LeafletMapInterface(
            currentBaseMapUrl = baseMapUrl,
            currentLotsJson = parkingLotsJson,
            userLat = userLatLng?.first,
            userLng = userLatLng?.second,
            onLotSelectedCallback = { id ->
                val lotId = id.toLongOrNull() ?: return@LeafletMapInterface
                val found = parkingLots.find { lot -> lot.id == lotId }
                if (found != null) {
                    onLotSelected(found)
                }
            }
        )
    }

    // Static HTML loaded once, baseMap and markers updated via JS
    val htmlContent = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <!-- Load Leaflet CSS with fallback -->
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" onerror="this.onerror=null;this.href='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css';" />
            
            <!-- Load Leaflet JS with sequential fallbacks -->
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" onerror="var s=document.createElement('script');s.src='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js';s.onerror=function(){var s2=document.createElement('script');s2.src='https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js';document.head.appendChild(s2);};document.head.appendChild(s);"></script>
            <style>
                html, body, #map {
                    height: 100vh;
                    width: 100vw;
                    margin: 0;
                    padding: 0;
                    overflow: hidden;
                    background-color: #F8FAFC;
                }
                .leaflet-bar { border: none !important; box-shadow: 0 4px 12px rgba(0,0,0,0.1) !important; border-radius: 12px !important; overflow: hidden; }
                .leaflet-bar a { background-color: #FFFFFF !important; color: #1C1B1F !important; border: none !important; }
                .leaflet-control-attribution { display: none !important; }
                .user-pulse-container {
                    background: transparent !important;
                    border: none !important;
                }
                .user-pulse {
                    width: 16px;
                    height: 16px;
                    background: #6750A4;
                    border: 2px solid #FFFFFF;
                    border-radius: 50%;
                    box-shadow: 0 0 0 4px rgba(103, 80, 164, 0.4);
                    animation: pulse 1.5s infinite;
                }
                @keyframes pulse {
                    0% { box-shadow: 0 0 0 0px rgba(103, 80, 164, 0.4); }
                    70% { box-shadow: 0 0 0 8px rgba(103, 80, 164, 0); }
                    100% { box-shadow: 0 0 0 0px rgba(103, 80, 164, 0); }
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var isMapLoaded = false;
                var map;
                var userMarker;
                var lotMarkers = [];
                var tileLayer;
                var currentMapTypeUrl = '';
                var lastUserLat = null;
                var lastUserLng = null;
                var rawLotsData = [];

                function initMap() {
                    if (isMapLoaded) return;
                    try {
                        var startLat = 11.0183;
                        var startLng = 76.9725;
                        try {
                            if (window.Android && window.Android.hasUserLocation()) {
                                startLat = window.Android.getUserLat();
                                startLng = window.Android.getUserLng();
                                lastUserLat = startLat;
                                lastUserLng = startLng;
                                console.log("Initializing map at user's location: " + startLat + ", " + startLng);
                            } else {
                                console.log("Initializing map at default Coimbatore center: 11.0183, 76.9725");
                            }
                        } catch (e) {
                            console.error("Failed to read user location on init: " + e.message);
                        }

                        map = L.map('map', { zoomControl: false, attributionControl: false }).setView([startLat, startLng], 14);
                        
                        var tileUrl = currentMapTypeUrl || 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
                        tileLayer = L.tileLayer(tileUrl, { maxZoom: 19 }).addTo(map);
                        
                        tileLayer.on('tileerror', function(error) {
                            console.warn("Tile load failed, falling back to openstreetmap...");
                            var osmUrl = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
                            if (tileLayer._url !== osmUrl) {
                                tileLayer.setUrl(osmUrl);
                            }
                        });

                        // Add User current location marker (pulsing blip)
                        var userIcon = L.divIcon({
                            html: '<div class="user-pulse"></div>',
                            className: 'user-pulse-container',
                            iconSize: [20, 20],
                            iconAnchor: [10, 10]
                        });
                        userMarker = L.marker([startLat, startLng], { icon: userIcon }).addTo(map);
                        userMarker.bindPopup("<b>You are here</b><br>Real-time Location").openPopup();

                        isMapLoaded = true;
                        console.log("Map successfully initialized!");
                        
                        // Force size recalculation to prevent rendering issues
                        setTimeout(function() {
                            map.invalidateSize();
                        }, 250);
                    } catch (e) {
                        console.error("Map initialization failed: " + e.message);
                    }
                }

                function setBaseMap(url) {
                    currentMapTypeUrl = url;
                    if (isMapLoaded && map) {
                        try {
                            if (tileLayer) {
                                map.removeLayer(tileLayer);
                            }
                            tileLayer = L.tileLayer(url, { maxZoom: 19 }).addTo(map);
                        } catch (e) {
                            console.error("Failed to set base map: " + e.message);
                        }
                    }
                }

                function addLotMarker(id, name, lat, lng, price, isFull) {
                    if (!isMapLoaded || !map) return;
                    var color = isFull ? "#F44336" : (price <= 30 ? "#4CAF50" : "#FFC107");
                    var markerHtml = `
                        <div style="background-color: ` + color + `; color: white; padding: 4px 8px; border-radius: 20px; font-weight: bold; font-size: 11px; border: 2px solid white; box-shadow: 0 4px 8px rgba(0,0,0,0.2); white-space: nowrap; display: inline-block;">
                            ₹` + price + `/hr
                        </div>
                    `;
                    
                    var customIcon = L.divIcon({
                        html: markerHtml,
                        className: 'custom-lot-marker',
                        iconSize: [60, 24],
                        iconAnchor: [30, 12]
                    });

                    var marker = L.marker([lat, lng], { icon: customIcon }).addTo(map);
                    marker.on('click', function() {
                        window.Android.selectLot(id.toString());
                    });
                    lotMarkers.push({id: id, marker: marker});
                }

                function centerOnMarkers() {
                    if (!isMapLoaded || !map || lotMarkers.length === 0) return;
                    try {
                        var group = L.featureGroup(lotMarkers.map(function(m) { return m.marker; }));
                        var bounds = group.getBounds();
                        if (lotMarkers.length === 1) {
                            var latLng = lotMarkers[0].marker.getLatLng();
                            map.setView(latLng, 15);
                        } else {
                            map.fitBounds(bounds, { padding: [50, 50] });
                        }
                    } catch (e) {
                        console.error("centerOnMarkers failed: " + e.message);
                    }
                }

                function renderMarkers() {
                    if (!isMapLoaded || !map) return;
                    try {
                        // Clear old markers
                        lotMarkers.forEach(function(m) { map.removeLayer(m.marker); });
                        lotMarkers = [];

                        // Add new markers at their actual, real coordinates (NO MORE SHIFTING!)
                        rawLotsData.forEach(function(lot) {
                            addLotMarker(lot.id, lot.name, lot.lat, lot.lng, lot.price, lot.isFull);
                        });

                        console.log("Rendered " + rawLotsData.length + " markers at actual coordinates successfully.");
                        map.invalidateSize();
                    } catch (e) {
                        console.error("renderMarkers failed: " + e.message);
                    }
                }

                function safeInjectMarkers(lots) {
                    if (!isMapLoaded || typeof map === 'undefined' || !map) {
                        console.log("Map not ready yet. Retrying marker injection in 100ms...");
                        setTimeout(function() { safeInjectMarkers(lots); }, 100);
                        return;
                    }
                    try {
                        rawLotsData = lots;
                        renderMarkers();
                    } catch (e) {
                        console.error("Failed to inject markers: " + e.message);
                    }
                }

                function updateUserMarker(lat, lng) {
                    if (!isMapLoaded || !map) return;
                    lastUserLat = lat;
                    lastUserLng = lng;
                    if (userMarker) {
                        userMarker.setLatLng([lat, lng]);
                        userMarker.bindPopup("<b>You are here</b><br>Real-time Location");
                    }
                }

                function centerOnUser() {
                    if (!isMapLoaded || !map) return;
                    try {
                        if (window.Android && window.Android.hasUserLocation()) {
                            var lat = window.Android.getUserLat();
                            var lng = window.Android.getUserLng();
                            lastUserLat = lat;
                            lastUserLng = lng;
                            
                            if (userMarker) {
                                userMarker.setLatLng([lat, lng]);
                                userMarker.bindPopup("<b>You are here</b><br>Real-time Location").openPopup();
                            }
                            map.setView([lat, lng], 14);
                            console.log("Centered map on Kotlin user location: " + lat + ", " + lng);
                        } else {
                            console.warn("User location not available from Android interface.");
                            if (lotMarkers.length > 0) {
                                centerOnMarkers();
                            } else {
                                map.setView([11.0183, 76.9725], 13);
                            }
                        }
                    } catch (e) {
                        console.error("centerOnUser failed: " + e.message);
                        if (map) map.setView([11.0183, 76.9725], 13);
                    }
                }

                function checkAndInit() {
                    try {
                        if (typeof L !== 'undefined' && typeof window.Android !== 'undefined') {
                            // 1. Fetch parameters from Android first
                            var url = "";
                            try {
                                url = window.Android.getBaseMapUrl();
                            } catch (e) {
                                console.error("Error calling getBaseMapUrl: " + e.message);
                            }
                            currentMapTypeUrl = url || 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';

                            // 2. Initialize Leaflet map
                            initMap();

                            // 3. Inject initial markers
                            var lotsJson = "";
                            try {
                                lotsJson = window.Android.getParkingLotsJson();
                            } catch (e) {
                                console.error("Error calling getParkingLotsJson: " + e.message);
                            }
                            if (lotsJson) {
                                try {
                                    var lots = JSON.parse(lotsJson);
                                    safeInjectMarkers(lots);
                                } catch(e) {
                                    console.error("Error parsing initial lots: " + e.message);
                                }
                            }

                            // 4. Center map to show parking lots if user is not available
                            try {
                                if (!window.Android.hasUserLocation() && lotMarkers.length > 0) {
                                    setTimeout(centerOnMarkers, 400);
                                }
                            } catch (e) {
                                console.error("Failed to fit initial bounds: " + e.message);
                            }
                        } else {
                            setTimeout(checkAndInit, 100);
                        }
                    } catch (globalErr) {
                        console.error("Global init error: " + globalErr.message);
                        initMap();
                    }
                }

                // Polling check for Leaflet readiness
                checkAndInit();
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: android.webkit.SslErrorHandler?,
                            error: android.net.http.SslError?
                        ) {
                            handler?.proceed()
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.evaluateJavascript("if (typeof setBaseMap === 'function') { setBaseMap('$baseMapUrl'); }", null)
                            view?.evaluateJavascript("if (typeof safeInjectMarkers === 'function' && typeof window.Android !== 'undefined') { safeInjectMarkers(JSON.parse(window.Android.getParkingLotsJson())); }", null)
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: android.webkit.GeolocationPermissions.Callback?
                        ) {
                            callback?.invoke(origin, true, false)
                        }

                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.d("LeafletMapJS", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                            return true
                        }
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.databaseEnabled = true
                    settings.setGeolocationEnabled(true)
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    
                    addJavascriptInterface(jsInterface, "Android")
                    
                    webViewInstance = this
                    loadDataWithBaseURL("https://appassets.androidplatform.net/", htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                jsInterface.currentBaseMapUrl = baseMapUrl
                jsInterface.currentLotsJson = parkingLotsJson
                jsInterface.userLat = userLatLng?.first
                jsInterface.userLng = userLatLng?.second

                webView.evaluateJavascript("if (typeof setBaseMap === 'function') { setBaseMap('$baseMapUrl'); }", null)
                webView.evaluateJavascript("if (typeof safeInjectMarkers === 'function' && typeof window.Android !== 'undefined') { safeInjectMarkers(JSON.parse(window.Android.getParkingLotsJson())); }", null)
                if (userLatLng != null) {
                    webView.evaluateJavascript("if (typeof updateUserMarker === 'function') { updateUserMarker(${userLatLng!!.first}, ${userLatLng!!.second}); }", null)
                }
            }
        )

        // Overlay Map Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Locate user button
            IconButton(
                onClick = {
                    webViewInstance?.evaluateJavascript("centerOnUser();", null)
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.GpsFixed,
                    contentDescription = "My Location",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Layer/Map style toggle button
            IconButton(
                onClick = {
                    mapType = if (mapType == "streets") "satellite" else "streets"
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            ) {
                Icon(
                    imageVector = if (mapType == "streets") Icons.Filled.Layers else Icons.Filled.Map,
                    contentDescription = "Toggle Map Layers",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Jump to Coimbatore Parking Lots floating button if the user is far away (like in a US-based emulator)
        if (isFarFromLots) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 84.dp) // below search card
            ) {
                Button(
                    onClick = {
                        webViewInstance?.evaluateJavascript("centerOnMarkers();", null)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalParking,
                        contentDescription = "Show Coimbatore Parking Lots",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Jump to Coimbatore Lots",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

class LeafletMapInterface(
    var currentBaseMapUrl: String,
    var currentLotsJson: String,
    var userLat: Double?,
    var userLng: Double?,
    private val onLotSelectedCallback: (String) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun getBaseMapUrl(): String = currentBaseMapUrl

    @android.webkit.JavascriptInterface
    fun getParkingLotsJson(): String = currentLotsJson

    @android.webkit.JavascriptInterface
    fun getUserLat(): Double = userLat ?: 0.0

    @android.webkit.JavascriptInterface
    fun getUserLng(): Double = userLng ?: 0.0

    @android.webkit.JavascriptInterface
    fun hasUserLocation(): Boolean = userLat != null && userLng != null

    @android.webkit.JavascriptInterface
    fun selectLot(id: String) {
        onLotSelectedCallback(id)
    }
}
