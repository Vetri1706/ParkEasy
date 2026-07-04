package com.example.ui

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
import com.example.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import com.example.data.Booking
import com.example.data.Partner
import com.example.data.ParkingLot
import com.example.data.Vehicle
import com.example.data.WalletTransaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ParkEasyAppContent(viewModel: ParkEasyViewModel) {
    val role by viewModel.userRole.collectAsState()

    Scaffold(
        topBar = { RoleHeader(viewModel) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (role) {
                "Customer" -> CustomerAppContent(viewModel)
                "Partner" -> PartnerAppContent(viewModel)
                "Admin" -> AdminPanelContent(viewModel)
            }
        }
    }
}

@Composable
fun RoleHeader(viewModel: ParkEasyViewModel) {
    val role by viewModel.userRole.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .border(BorderStroke(1.dp, GeoBorder.copy(alpha = 0.3f)), RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Park Easy Logo",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, GeoPurplePrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Park Easy",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = GeoDarkText,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Geometric Balance",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoPurplePrimary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Box {
                Button(
                    onClick = { expanded = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (role) {
                            "Admin" -> GeoError
                            "Partner" -> GeoPurplePrimary
                            else -> GeoPurplePrimary
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("role_selector_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = when (role) {
                            "Admin" -> Icons.Filled.AdminPanelSettings
                            "Partner" -> Icons.Filled.Storefront
                            else -> Icons.Filled.DirectionsCar
                        },
                        contentDescription = "Role Mode",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = role, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "Dropdown", modifier = Modifier.size(16.dp))
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Customer (Driver)") },
                        leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = "Driver") },
                        onClick = {
                            viewModel.userRole.value = "Customer"
                            expanded = false
                        },
                        modifier = Modifier.testTag("role_toggle_customer")
                    )
                    DropdownMenuItem(
                        text = { Text("Parking Partner (Shop Owner)") },
                        leadingIcon = { Icon(Icons.Filled.Storefront, contentDescription = "Shop Owner") },
                        onClick = {
                            viewModel.userRole.value = "Partner"
                            expanded = false
                        },
                        modifier = Modifier.testTag("role_toggle_partner")
                    )
                    DropdownMenuItem(
                        text = { Text("Admin (Park Easy Control)") },
                        leadingIcon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Admin") },
                        onClick = {
                            viewModel.userRole.value = "Admin"
                            expanded = false
                        },
                        modifier = Modifier.testTag("role_toggle_admin")
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. CUSTOMER MODE
// ==========================================

@Composable
fun CustomerBottomNavBar(viewModel: ParkEasyViewModel) {
    val currentScreen by viewModel.customerScreen.collectAsState()
    
    val activeTab = when (currentScreen) {
        "Home" -> "Home"
        "ExploreMap" -> "Explore"
        "History" -> "Bookings"
        "Wallet" -> "Wallet"
        "Settings" -> "Settings"
        else -> "Home"
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GeoBorder.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = "Home",
                icon = Icons.Outlined.Home,
                activeIcon = Icons.Filled.Home,
                isActive = activeTab == "Home",
                onClick = { viewModel.customerScreen.value = "Home" }
            )
            
            BottomNavItem(
                label = "Explore",
                icon = Icons.Outlined.Explore,
                activeIcon = Icons.Filled.Explore,
                isActive = activeTab == "Explore",
                onClick = { viewModel.customerScreen.value = "ExploreMap" }
            )
            
            BottomNavItem(
                label = "Bookings",
                icon = Icons.Outlined.Assignment,
                activeIcon = Icons.Filled.Assignment,
                isActive = activeTab == "Bookings",
                onClick = { viewModel.customerScreen.value = "History" }
            )
            
            BottomNavItem(
                label = "Wallet",
                icon = Icons.Outlined.Wallet,
                activeIcon = Icons.Filled.Wallet,
                isActive = activeTab == "Wallet",
                onClick = { viewModel.customerScreen.value = "Wallet" }
            )
            
            BottomNavItem(
                label = "Settings",
                icon = Icons.Outlined.Settings,
                activeIcon = Icons.Filled.Settings,
                isActive = activeTab == "Settings",
                onClick = { viewModel.customerScreen.value = "Settings" }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GeoPurpleLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = activeIcon,
                    contentDescription = label,
                    tint = Color(0xFF1D192B),
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = GeoGrayLabel,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) GeoPurplePrimary else GeoGrayLabel
        )
    }
}

@Composable
fun CustomerAppContent(viewModel: ParkEasyViewModel) {
    val screen by viewModel.customerScreen.collectAsState()

    Scaffold(
        bottomBar = {
            if (screen in listOf("Home", "ExploreMap", "History", "Wallet", "Timer", "Settings")) {
                CustomerBottomNavBar(viewModel)
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "customer_navigation"
            ) { currentScreen ->
                when (currentScreen) {
                    "Home" -> CustomerHomeScreen(viewModel)
                    "ExploreMap" -> ExploreMapScreen(viewModel)
                    "Details" -> ParkingLotDetailsScreen(viewModel)
                    "BookingFlow" -> BookingFlowScreen(viewModel)
                    "Confirmation" -> BookingConfirmationScreen(viewModel)
                    "Wallet" -> WalletScreen(viewModel)
                    "History" -> BookingHistoryScreen(viewModel)
                    "Timer" -> LiveParkingTimerScreen(viewModel)
                    "Review" -> SubmitReviewScreen(viewModel)
                    "Settings" -> SettingsScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun CustomerHomeScreen(viewModel: ParkEasyViewModel) {
    val lots by viewModel.parkingLots.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val activeBk by viewModel.activeBooking.collectAsState()
    val walletState by viewModel.wallet.collectAsState()
    val filters by viewModel.searchFilters.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    var showFiltersDialog by remember { mutableStateOf(false) }

    // Client-side filtering logic based on text and dynamic filter combinations
    val filteredLots = lots.filter { lot ->
        val matchesQuery = lot.name.contains(query, ignoreCase = true) || lot.location.contains(query, ignoreCase = true)
        val matchesCheapest = !filters.cheapest || lot.pricePerHour <= 30.0
        val matchesNearest = !filters.nearest || lot.distanceMeters <= 500
        val matchesCovered = !filters.covered || lot.isCovered
        val matchesCctv = !filters.cctv || lot.hasCCTV
        val matchesEv = !filters.evCharging || lot.hasEVCharging
        val matchesSecurity = !filters.security || lot.hasSecurity
        val matchesWashroom = !filters.washroom || lot.hasWashroom

        matchesQuery && matchesCheapest && matchesNearest && matchesCovered && matchesCctv && matchesEv && matchesSecurity && matchesWashroom
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
    ) {
        // Hello Greeting Banner
        item {
            val name by viewModel.profileName.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "WELCOME BACK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoPurplePrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hello, $name 👋",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = GeoDarkText
                    )
                    Text(
                        text = "Find and reserve your premium spot in seconds",
                        fontSize = 12.sp,
                        color = GeoGrayLabel
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GeoPurpleContainer)
                        .border(2.dp, Color.White, CircleShape)
                        .shadow(2.dp, CircleShape)
                        .clickable { viewModel.navigateToCustomer("Wallet") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (name.isNotEmpty()) name.take(2).uppercase() else "BR",
                        color = GeoPurpleOnContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Active Booking alert (Floating style if checked in)
        if (activeBk != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, GeoPurpleLight, RoundedCornerShape(20.dp))
                        .clickable { viewModel.navigateToCustomer("Timer") },
                    color = GeoPurpleLight.copy(alpha = 0.4f),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GeoPurplePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "Active Timer",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = "Active Parking Booking",
                                fontWeight = FontWeight.Bold,
                                color = GeoPurpleOnContainer
                            )
                            Text(
                                text = "Parked at ${activeBk?.parkingLotName}. Tap to view timer.",
                                fontSize = 12.sp,
                                color = GeoPurpleOnContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "View Timer",
                            tint = GeoPurpleOnContainer
                        )
                    }
                }
            }
        }

        // Search Input & Filters Trigger
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = query,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search for parking nearby...", fontSize = 14.sp, color = GeoGrayLabel) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = GeoGrayLabel) },
                    modifier = Modifier
                        .weight(1.0f)
                        .testTag("search_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GeoSurfaceVariant,
                        unfocusedContainerColor = GeoSurfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = GeoDarkText,
                        unfocusedTextColor = GeoDarkText
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = { showFiltersDialog = true },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(GeoSurface)
                        .border(1.dp, GeoBorder.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Filters",
                        tint = GeoPurplePrimary
                    )
                }
            }
        }

        // Quick shortcut buttons row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("Wallet", Icons.Filled.Wallet, "Wallet"),
                    Triple("My Passes", Icons.Filled.AssignmentTurnedIn, "History"),
                    Triple("My Vehicles", Icons.Filled.DirectionsCar, "BookingFlow"),
                    Triple("Bookings", Icons.Filled.History, "History")
                ).forEach { (label, icon, target) ->
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(GeoSurfaceVariant)
                            .clickable { viewModel.navigateToCustomer(target) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = icon, contentDescription = label, tint = GeoPurplePrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GeoDarkText, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        // Dynamic Promo Card leading to dedicated Map Tab
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { viewModel.customerScreen.value = "ExploreMap" }
                    .border(1.dp, GeoPurpleLight.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color(0xFF1E1B2C) else GeoPurpleLight.copy(alpha = 0.25f)
                ),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(GeoPurplePrimary)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LIVE EXPLORER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Interactive Smart Map",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) Color.White else GeoDarkText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Find, filter, and book parking spaces in real-time with Google Maps styling.",
                            fontSize = 12.sp,
                            color = GeoGrayLabel
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Explore,
                        contentDescription = "Explore Map",
                        tint = GeoPurplePrimary,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }
        }

        // Nearby Parking Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Nearby Parking Lots",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${filteredLots.size} lots found",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (filteredLots.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.SentimentDissatisfied,
                            contentDescription = "Empty list",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No parking lots match your criteria.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = "Try clearing active search filters.", fontSize = 12.sp)
                    }
                }
            }
        }

        // List of filtered lots
        items(filteredLots) { lot ->
            ParkingLotCard(lot = lot, onSelect = {
                viewModel.selectedLot.value = lot
                viewModel.navigateToCustomer("Details")
            }, onFavoriteToggle = { viewModel.toggleFavorite(lot) })
        }
    }

    // Dynamic Search Filter dialog
    if (showFiltersDialog) {
        AlertDialog(
            onDismissRequest = { showFiltersDialog = false },
            title = { Text("Smart Search Filters") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterToggleRow("Cheapest First (<= ₹30/hr)", filters.cheapest) {
                        viewModel.searchFilters.value = filters.copy(cheapest = it)
                    }
                    FilterToggleRow("Nearest First (<= 500m)", filters.nearest) {
                        viewModel.searchFilters.value = filters.copy(nearest = it)
                    }
                    FilterToggleRow("Covered Only", filters.covered) {
                        viewModel.searchFilters.value = filters.copy(covered = it)
                    }
                    FilterToggleRow("Has CCTV Monitoring", filters.cctv) {
                        viewModel.searchFilters.value = filters.copy(cctv = it)
                    }
                    FilterToggleRow("Has EV Charging Port", filters.evCharging) {
                        viewModel.searchFilters.value = filters.copy(evCharging = it)
                    }
                    FilterToggleRow("Has 24/7 Guard Security", filters.security) {
                        viewModel.searchFilters.value = filters.copy(security = it)
                    }
                    FilterToggleRow("Has Onsite Restrooms", filters.washroom) {
                        viewModel.searchFilters.value = filters.copy(washroom = it)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFiltersDialog = false }) {
                    Text("Apply Filters")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.searchFilters.value = MapFilters()
                    showFiltersDialog = false
                }) {
                    Text("Clear All")
                }
            }
        )
    }
}

@Composable
fun FilterToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ParkingLotCard(lot: ParkingLot, onSelect: () -> Unit, onFavoriteToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(1.dp, GeoBorder.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .shadow(1.dp, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon representative of lot status - styled with a clean geometric border/shape
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (lot.availableSlots > 0) GeoPurpleContainer
                        else Color(0xFFFEE2E2)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalParking,
                    contentDescription = lot.name,
                    tint = if (lot.availableSlots > 0) GeoPurpleOnContainer else GeoError,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1.0f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(
                            text = lot.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (lot.hasCCTV) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(GeoPurpleLight)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CCTV",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GeoPurpleOnContainer
                                )
                            }
                        }
                    }
                    IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (lot.isSavedAsFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (lot.isSavedAsFavorite) Color.Red else Color.Gray
                        )
                    }
                }

                Text(
                    text = lot.location,
                    fontSize = 12.sp,
                    color = GeoGrayLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Star, contentDescription = "Rating", tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = lot.rating.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GeoDarkText)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "• ${lot.distanceMeters}m", fontSize = 11.sp, color = GeoGrayLabel)
                    }

                    Text(
                        text = "₹${lot.pricePerHour.toInt()}/hr",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = GeoPurplePrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lot.availableSlots > 0) "${lot.availableSlots} / ${lot.totalSlots} Slots Free" else "Fully Booked",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lot.availableSlots > 0) GeoSuccess else GeoError
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (lot.hasEVCharging) Icon(Icons.Filled.EvStation, contentDescription = "EV Station", modifier = Modifier.size(16.dp), tint = GeoPurplePrimary)
                        if (lot.isCovered) Icon(Icons.Filled.Roofing, contentDescription = "Covered", modifier = Modifier.size(16.dp), tint = GeoGrayLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun ParkingLotDetailsScreen(viewModel: ParkEasyViewModel) {
    val lot by viewModel.selectedLot.collectAsState()
    val isPredicting by viewModel.isPredicting.collectAsState()
    val prediction by viewModel.aiPrediction.collectAsState()

    var selectedDay by remember { mutableStateOf("Today") }
    var selectedTime by remember { mutableStateOf("6:00 PM") }

    if (lot == null) return

    val currentLot = lot!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Lot Header back button & title
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.shadow(2.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBackCustomer() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Parking Lot Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Main Card Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentLot.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Available",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    Text(
                        text = currentLot.location,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailMetric("Rating", "⭐ ${currentLot.rating}")
                        DetailMetric("Available Slots", "${currentLot.availableSlots} Slots")
                        DetailMetric("Base Price", "₹${currentLot.pricePerHour.toInt()}/hr")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Working Hours", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = currentLot.openingHours, fontSize = 13.sp, color = Color.DarkGray)
                }
            }

            // AI Predicting Demand Card (Gemini UI Component)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI Forecasting",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Parking Prediction",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = "Forecasting occupancies, surge prices & congestion tips using real-time local Gemini modeling.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Selection fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var dayExpanded by remember { mutableStateOf(false) }
                        var timeExpanded by remember { mutableStateOf(false) }

                        // Day Select Box
                        Box(modifier = Modifier.weight(1.0f)) {
                            OutlinedButton(
                                onClick = { dayExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedDay)
                            }
                            DropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                                listOf("Today", "Tomorrow", "Saturday", "Sunday").forEach { d ->
                                    DropdownMenuItem(text = { Text(d) }, onClick = {
                                        selectedDay = d
                                        dayExpanded = false
                                    })
                                }
                            }
                        }

                        // Time Select Box
                        Box(modifier = Modifier.weight(1.0f)) {
                            OutlinedButton(
                                onClick = { timeExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedTime)
                            }
                            DropdownMenu(expanded = timeExpanded, onDismissRequest = { timeExpanded = false }) {
                                listOf("9:00 AM", "1:00 PM", "6:00 PM", "9:00 PM").forEach { t ->
                                    DropdownMenuItem(text = { Text(t) }, onClick = {
                                        selectedTime = t
                                        timeExpanded = false
                                    })
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.triggerAiPrediction(currentLot, selectedDay, selectedTime) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_predict_button"),
                        enabled = !isPredicting
                    ) {
                        if (isPredicting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text("Generate AI Forecast")
                        }
                    }

                    if (prediction != null && !isPredicting) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Projected Occupancy", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    text = "${prediction!!.occupancyPercent}% Occupied",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (prediction!!.demandLevel) {
                                        "High" -> Color(0xFFEF4444)
                                        "Moderate" -> Color(0xFFF59E0B)
                                        else -> Color(0xFF10B981)
                                    }
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Demand Status", fontSize = 12.sp, color = Color.Gray)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (prediction!!.demandLevel) {
                                                "High" -> Color(0xFFFEE2E2)
                                                "Moderate" -> Color(0xFFFEF3C7)
                                                else -> Color(0xFFD1FAE5)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${prediction!!.demandLevel} Demand",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = when (prediction!!.demandLevel) {
                                            "High" -> Color(0xFF991B1B)
                                            "Moderate" -> Color(0xFF92400E)
                                            else -> Color(0xFF065F46)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = prediction!!.pricingSurgeText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = prediction!!.advice,
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Onsite Facilities
            Text(text = "Onsite Facilities", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FacilityBadge("CCTV", Icons.Filled.PhotoCamera, currentLot.hasCCTV)
                FacilityBadge("Covered", Icons.Filled.Roofing, currentLot.isCovered)
                FacilityBadge("EV Charging", Icons.Filled.EvStation, currentLot.hasEVCharging)
                FacilityBadge("Security", Icons.Filled.Security, currentLot.hasSecurity)
                FacilityBadge("Washroom", Icons.Filled.Bathtub, currentLot.hasWashroom)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CTA Booking Button
            Button(
                onClick = { viewModel.navigateToCustomer("BookingFlow") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("book_button")
            ) {
                Text(text = "Book Parking Spot", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DetailMetric(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RowScope.FacilityBadge(label: String, icon: Any, active: Boolean) {
    Box(
        modifier = Modifier
            .weight(1.0f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else Color.White
            )
            .border(
                1.dp,
                if (active) MaterialTheme.colorScheme.primary else Color.LightGray,
                RoundedCornerShape(12.dp)
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
                contentDescription = label,
                tint = if (active) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = if (active) MaterialTheme.colorScheme.primary else Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BookingFlowScreen(viewModel: ParkEasyViewModel) {
    val lot by viewModel.selectedLot.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()

    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var durationHours by remember { mutableStateOf(2) }
    var isEvChargingReserved by remember { mutableStateOf(false) }

    var registerVehicleMode by remember { mutableStateOf(false) }
    var vNumber by remember { mutableStateOf("") }
    var vBrand by remember { mutableStateOf("") }
    var vModel by remember { mutableStateOf("") }
    var vColor by remember { mutableStateOf("") }
    var vType by remember { mutableStateOf("Car") }

    if (lot == null) return

    val currentLot = lot!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Back Header
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.shadow(2.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBackCustomer() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configure Booking",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Step 1: Select/Add Vehicle
            Text(text = "1. Select Vehicle", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            if (!registerVehicleMode) {
                if (vehicles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "No vehicles registered yet", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { registerVehicleMode = true }) {
                                Text("+ Add Vehicle")
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        vehicles.forEach { vehicle ->
                            val isSel = selectedVehicle?.id == vehicle.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        2.dp,
                                        if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedVehicle = vehicle },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "${vehicle.brand} ${vehicle.model}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(text = vehicle.number, color = Color.Gray, fontSize = 14.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = vehicle.type,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        RadioButton(selected = isSel, onClick = { selectedVehicle = vehicle })
                                    }
                                }
                            }
                        }

                        TextButton(onClick = { registerVehicleMode = true }) {
                            Text("+ Register Another Vehicle")
                        }
                    }
                }
            } else {
                // Register Vehicle Inline Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "Add New Vehicle", fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = vNumber,
                            onValueChange = { vNumber = it },
                            placeholder = { Text("Vehicle Plate No. (e.g. TN38AB1234)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = vBrand,
                                onValueChange = { vBrand = it },
                                placeholder = { Text("Brand (e.g., Hyundai)") },
                                modifier = Modifier.weight(1.0f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = vModel,
                                onValueChange = { vModel = it },
                                placeholder = { Text("Model (e.g., i20)") },
                                modifier = Modifier.weight(1.0f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = vColor,
                            onValueChange = { vColor = it },
                            placeholder = { Text("Color (e.g., Polar White)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Vehicle type tabs
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Bike", "Car", "SUV", "EV").forEach { t ->
                                val isSel = vType == t
                                Button(
                                    onClick = { vType = t },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSel) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        contentColor = if (isSel) Color.White else Color.Black
                                    ),
                                    modifier = Modifier.weight(1.0f)
                                ) {
                                    Text(text = t, fontSize = 11.sp)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { registerVehicleMode = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                if (vNumber.isNotBlank()) {
                                    viewModel.registerVehicle(vNumber, vType, vBrand, vModel, vColor)
                                    vNumber = ""
                                    vBrand = ""
                                    vModel = ""
                                    vColor = ""
                                    registerVehicleMode = false
                                }
                            }) {
                                Text("Save Vehicle")
                            }
                        }
                    }
                }
            }

            // Step 2: Duration Slider
            Text(text = "2. Select Duration", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Booking duration")
                        Text(text = "$durationHours hours", fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = durationHours.toFloat(),
                        onValueChange = { durationHours = it.toInt() },
                        valueRange = 1f..12f,
                        steps = 10
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "1 hour", fontSize = 11.sp)
                        Text(text = "12 hours", fontSize = 11.sp)
                    }
                }
            }

            // Step 3: Optional Facilities Addition
            Text(text = "3. Add Ons", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (currentLot.hasEVCharging) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            Icon(Icons.Filled.EvStation, contentDescription = "EV Charging Station", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Reserve EV Charging Port", fontWeight = FontWeight.Bold)
                                Text(text = "+₹20 standard flat fee", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Checkbox(checked = isEvChargingReserved, onCheckedChange = { isEvChargingReserved = it })
                    }
                }
            } else {
                Text(text = "No add-ons available for this lot currently.", fontSize = 12.sp, color = Color.Gray)
            }

            // Step 4: Summary Bill and Book CTA
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Bill Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Parking (₹${currentLot.pricePerHour.toInt()}/hr × $durationHours hrs)", fontSize = 13.sp)
                        Text(text = "₹${(currentLot.pricePerHour * durationHours).toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    if (isEvChargingReserved) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "EV Charging Port Reservation", fontSize = 13.sp)
                            Text(text = "₹20", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Total Payable Amount", fontWeight = FontWeight.Bold)
                        val total = (currentLot.pricePerHour * durationHours) + (if (isEvChargingReserved) 20.0 else 0.0)
                        Text(text = "₹${total.toInt()}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // CTA
            Button(
                onClick = {
                    if (selectedVehicle != null) {
                        viewModel.createBooking(
                            currentLot,
                            selectedVehicle!!,
                            "Today, July 4",
                            "6:00 PM",
                            durationHours,
                            isEvChargingReserved
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = selectedVehicle != null
            ) {
                Text(text = "Confirm Booking", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BookingConfirmationScreen(viewModel: ParkEasyViewModel) {
    val booking by viewModel.confirmedBooking.collectAsState()

    if (booking == null) return

    val currentBooking = booking!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFD1FAE5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Success",
                tint = Color(0xFF065F46),
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "Booking Confirmed!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Your secure parking slot has been held at ${currentBooking.parkingLotName}.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        // QR Code Container (Drawn manually via Canvas)
        Card(
            modifier = Modifier
                .size(220.dp)
                .shadow(4.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Manual drawing of neat QR Block matrices for presentation
                    val sizeX = size.width
                    val sizeY = size.height

                    // Outline Frame
                    drawRect(Color.Black, style = Stroke(width = 6f))

                    // QR Corners
                    drawRect(Color.Black, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(sizeX * 0.3f, sizeY * 0.3f))
                    drawRect(Color.White, topLeft = Offset(10f, 10f), size = androidx.compose.ui.geometry.Size(sizeX * 0.3f - 20f, sizeY * 0.3f - 20f))
                    drawRect(Color.Black, topLeft = Offset(20f, 20f), size = androidx.compose.ui.geometry.Size(sizeX * 0.3f - 40f, sizeY * 0.3f - 40f))

                    drawRect(Color.Black, topLeft = Offset(sizeX * 0.7f, 0f), size = androidx.compose.ui.geometry.Size(sizeX * 0.3f, sizeY * 0.3f))
                    drawRect(Color.White, topLeft = Offset(sizeX * 0.7f + 10f, 10f), size = androidx.compose.ui.geometry.Size(sizeX * 0.3f - 20f, sizeY * 0.3f - 20f))
                    drawRect(Color.Black, topLeft = Offset(sizeX * 0.7f + 20f, 20f), size = androidx.compose.ui.geometry.Size(sizeX * 0.3f - 40f, sizeY * 0.3f - 40f))

                    drawRect(Color.Black, topLeft = Offset(0f, sizeY * 0.7f), size = androidx.compose.ui.geometry.Size(sizeX * 0.3f, sizeY * 0.3f))
                    drawRect(Color.White, topLeft = Offset(10f, sizeY * 0.7f + 10f), size = androidx.compose.ui.geometry.Size(sizeX * 0.3f - 20f, sizeY * 0.3f - 20f))
                    drawRect(Color.Black, topLeft = Offset(20f, sizeY * 0.7f + 20f), size = androidx.compose.ui.geometry.Size(sizeX * 0.3f - 40f, sizeY * 0.3f - 40f))

                    // Inner matrix noise lines
                    drawLine(Color.Black, Offset(sizeX * 0.4f, sizeY * 0.2f), Offset(sizeX * 0.6f, sizeY * 0.2f), strokeWidth = 14f)
                    drawLine(Color.Black, Offset(sizeX * 0.2f, sizeY * 0.5f), Offset(sizeX * 0.8f, sizeY * 0.5f), strokeWidth = 14f)
                    drawLine(Color.Black, Offset(sizeX * 0.5f, sizeY * 0.4f), Offset(sizeX * 0.5f, sizeY * 0.8f), strokeWidth = 14f)
                }
            }
        }

        Text(
            text = "Booking ID: ${currentBooking.bookingId}",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        // Info breakdown card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Summary", fontWeight = FontWeight.Bold)
                InfoRow("Vehicle Number", currentBooking.vehicleNumber)
                InfoRow("Arrival Slot Time", "${currentBooking.date} • ${currentBooking.time}")
                InfoRow("Duration Held", "${currentBooking.durationHours} Hours")
                InfoRow("Total Paid", "₹${currentBooking.totalPrice.toInt()}")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var locationSimulated by remember { mutableStateOf(false) }

            Button(
                onClick = { locationSimulated = true },
                modifier = Modifier
                    .weight(1.0f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Filled.Navigation, contentDescription = "Navigate")
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (locationSimulated) "Route: 3 mins away" else "Navigate")
            }

            Button(
                onClick = { viewModel.checkInBooking(currentBooking) },
                modifier = Modifier
                    .weight(1.0f)
                    .height(50.dp)
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan to CheckIn")
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Scan QR CheckIn")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun LiveParkingTimerScreen(viewModel: ParkEasyViewModel) {
    val booking by viewModel.activeBooking.collectAsState()

    if (booking == null) return

    val currentBooking = booking!!

    // Simulated Countdown timer state
    var minutesRemaining by remember { mutableStateOf(currentBooking.durationHours * 60) }
    var tickTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(tickTrigger) {
        delay(1000)
        if (minutesRemaining > 0) {
            minutesRemaining -= 1
            tickTrigger += 1
        }
    }

    val hrs = minutesRemaining / 60
    val mins = minutesRemaining % 60
    val timerText = String.format("%02d:%02d", hrs, mins)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Warning notification banner
        if (minutesRemaining <= 10) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = "Expiring", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Parking session expiring in less than 10 mins! Extend time now to prevent penalties.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = "Active", tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your vehicle is safely checked in. The system is currently tracking your parking time.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        Text(
            text = "Currently Parked At",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Text(
            text = currentBooking.parkingLotName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = "Vehicle: ${currentBooking.vehicleNumber}",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Countdown Dial (Drawn with custom Arc Canvas)
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 12.dp.toPx()
                // Track arc
                drawCircle(Color(0xFFE5E7EB), radius = size.minDimension / 2 - stroke / 2, style = Stroke(width = stroke))
                // Progress arc
                val sweep = (minutesRemaining.toFloat() / (currentBooking.durationHours * 60).toFloat()) * 360f
                drawArc(
                    color = if (minutesRemaining <= 10) Color(0xFFEF4444) else SlateIndigo,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = timerText, fontSize = 38.sp, fontWeight = FontWeight.Black)
                Text(text = "Remaining", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.extendParking(currentBooking, 1) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("extend_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Extend")
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Extend Time (+1 Hour • ₹30)")
        }

        Button(
            onClick = { viewModel.checkOutBooking(currentBooking) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("end_parking_button")
        ) {
            Icon(Icons.Filled.ExitToApp, contentDescription = "CheckOut")
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "End Parking & Check-Out")
        }
    }
}

@Composable
fun SubmitReviewScreen(viewModel: ParkEasyViewModel) {
    val bookings by viewModel.bookings.collectAsState()
    val scope = rememberCoroutineScope()

    // Grab the latest completed booking to review
    val latestCompleted = bookings.firstOrNull { it.status == "Completed" && it.cleanlinessRating == 0 }

    if (latestCompleted == null) {
        // Fallback or navigate away
        LaunchedEffect(Unit) { viewModel.navigateToCustomer("Home") }
        return
    }

    var cleanliness by remember { mutableStateOf(5) }
    var security by remember { mutableStateOf(5) }
    var entry by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Review Your Parking",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = "How was your experience at ${latestCompleted.parkingLotName}?",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        HorizontalDivider()

        // Cleanliness Rating
        RatingPickerRow("Cleanliness", cleanliness) { cleanliness = it }

        // Security Rating
        RatingPickerRow("Security Protection", security) { security = it }

        // Ease of entry
        RatingPickerRow("Ease of Entry", entry) { entry = it }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            placeholder = { Text("Write comments or suggestions (optional)...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.submitReview(latestCompleted.id, cleanliness, security, entry, comment) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_review_button")
        ) {
            Text(text = "Submit Feedback", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RatingPickerRow(label: String, rating: Int, onRatingSelected: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..5).forEach { star ->
                val active = star <= rating
                IconButton(onClick = { onRatingSelected(star) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (active) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "$star Stars",
                        tint = if (active) Color(0xFFFFB300) else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WalletScreen(viewModel: ParkEasyViewModel) {
    val walletState by viewModel.wallet.collectAsState()
    val txs by viewModel.walletTransactions.collectAsState()
    val hasPass by viewModel.hasMonthlyPass.collectAsState()
    val passExpiry by viewModel.monthlyPassExpiry.collectAsState()

    var addAmountStr by remember { mutableStateOf("") }
    var couponStr by remember { mutableStateOf("") }
    var referralStr by remember { mutableStateOf("") }
    var referralCodeApplied by remember { mutableStateOf(false) }
    var showAddMoneyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Back Header
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.shadow(2.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBackCustomer() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Digital Wallet & Passes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Balance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GeoBorder.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = GeoPurplePrimary)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Total Wallet Balance", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "₹${walletState?.balance?.toInt() ?: 520}",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Button(
                            onClick = { showAddMoneyDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = GeoPurplePrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = "+ Add Money", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Loyalty Points", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text(text = "${walletState?.loyaltyPoints ?: 120} Points", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Cashback Tier", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text(text = "Silver Member", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Monthly Pass Feature for Office workers
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CardMembership, contentDescription = "Pass", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Office Worker Monthly Pass", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Text(
                        text = "Unlimited daily parking across any Coimbatore partner lots for just ₹2500/month. Perfect for daily office commuters.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )

                    if (hasPass) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFD1FAE5))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✓ Pass Active (Expires: $passExpiry)",
                                color = Color(0xFF065F46),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.buyMonthlyPass() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = (walletState?.balance ?: 0.0) >= 2500
                        ) {
                            Text(text = "Purchase Pass (₹2500/mo)")
                        }
                    }
                }
            }

            // Coupon Codes and referral system
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Coupons & Referrals", fontWeight = FontWeight.Bold)

                    // Coupon claim
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = couponStr,
                            onValueChange = { couponStr = it },
                            placeholder = { Text("Coupon (e.g. PARKEASY20)") },
                            modifier = Modifier.weight(1.0f),
                            singleLine = true
                        )
                        Button(onClick = {
                            if (viewModel.applyCoupon(couponStr)) {
                                couponStr = "CLAIMED"
                            }
                        }) {
                            Text("Apply")
                        }
                    }

                    // Referral claim
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = referralStr,
                            onValueChange = { referralStr = it },
                            placeholder = { Text("Enter friend's email") },
                            modifier = Modifier.weight(1.0f),
                            singleLine = true
                        )
                        Button(onClick = {
                            viewModel.claimReferralReward()
                            referralCodeApplied = true
                            referralStr = ""
                        }) {
                            Text("Invite")
                        }
                    }
                    if (referralCodeApplied) {
                        Text(text = "Invite sent! Friendly bonus ₹50 added to your wallet.", color = Color(0xFF065F46), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Transaction History
            Text(text = "Wallet Transactions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                txs.forEach { tx ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = Color.White,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (tx.type == "Add" || tx.type == "Refund" || tx.type == "Referral" || tx.type == "Cashback") Color(0xFFD1FAE5)
                                            else Color(0xFFFEE2E2)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (tx.type == "Add") Icons.Filled.AddCircle else Icons.Filled.RemoveCircle,
                                        contentDescription = tx.type,
                                        tint = if (tx.type == "Add" || tx.type == "Refund" || tx.type == "Referral" || tx.type == "Cashback") Color(0xFF065F46) else Color(0xFF991B1B)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = tx.description, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = tx.type, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Text(
                                text = "${if (tx.type == "Add" || tx.type == "Refund" || tx.type == "Referral" || tx.type == "Cashback") "+" else "-"}₹${tx.amount.toInt()}",
                                fontWeight = FontWeight.Black,
                                color = if (tx.type == "Add" || tx.type == "Refund" || tx.type == "Referral" || tx.type == "Cashback") Color(0xFF065F46) else Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddMoneyDialog) {
        AlertDialog(
            onDismissRequest = { showAddMoneyDialog = false },
            title = { Text("Add Money to Wallet") },
            text = {
                OutlinedTextField(
                    value = addAmountStr,
                    onValueChange = { addAmountStr = it },
                    placeholder = { Text("Enter amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val amt = addAmountStr.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        viewModel.addMoneyToWallet(amt)
                        addAmountStr = ""
                        showAddMoneyDialog = false
                    }
                }) {
                    Text("Pay via UPI")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMoneyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BookingHistoryScreen(viewModel: ParkEasyViewModel) {
    val bookings by viewModel.bookings.collectAsState()
    val filterState by viewModel.bookingHistoryFilter.collectAsState()

    val filteredList = bookings.filter {
        when (filterState) {
            "Upcoming" -> it.status == "Upcoming"
            "Completed" -> it.status == "Completed"
            "Cancelled" -> it.status == "Cancelled"
            else -> true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.shadow(2.dp)) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateBackCustomer() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Booking History & Passes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // Filter tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Upcoming", "Completed", "Cancelled").forEach { f ->
                        val isSel = filterState == f
                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { viewModel.bookingHistoryFilter.value = f }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = f,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else Color.Black
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("No bookings matching this category.", color = Color.Gray)
                    }
                }
            }

            items(filteredList) { booking ->
                var invoiceDownloaded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = booking.parkingLotName, fontWeight = FontWeight.Black)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (booking.status) {
                                            "Completed" -> Color(0xFFD1FAE5)
                                            "Upcoming" -> Color(0xFFFEF3C7)
                                            else -> Color(0xFFFEE2E2)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = booking.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (booking.status) {
                                        "Completed" -> Color(0xFF065F46)
                                        "Upcoming" -> Color(0xFF92400E)
                                        else -> Color(0xFF991B1B)
                                    }
                                )
                            }
                        }

                        Text(text = "Booking ID: ${booking.bookingId}", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "Date/Time: ${booking.date} • ${booking.time}", fontSize = 13.sp)
                        Text(text = "Vehicle Plate: ${booking.vehicleNumber}", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Amount Paid: ₹${booking.totalPrice.toInt()}", fontWeight = FontWeight.Bold)
                            
                            if (booking.status == "Completed") {
                                Button(
                                    onClick = { invoiceDownloaded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = "Invoice", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = if (invoiceDownloaded) "Invoice Saved" else "Download Bill", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. PARKING PARTNER MODE
// ==========================================

@Composable
fun PartnerAppContent(viewModel: ParkEasyViewModel) {
    val screen by viewModel.partnerScreen.collectAsState()

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "partner_navigation"
    ) { currentScreen ->
        when (currentScreen) {
            "Dashboard" -> PartnerDashboardScreen(viewModel)
            "LotForm" -> PartnerLotFormScreen(viewModel)
        }
    }
}

@Composable
fun PartnerDashboardScreen(viewModel: ParkEasyViewModel) {
    val lots by viewModel.parkingLots.collectAsState()
    val config by viewModel.systemConfig.collectAsState()

    var showDocDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Welcome Partner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Partner Dashboard",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(text = "Manage your spots, rates and trace earnings", fontSize = 13.sp, color = Color.Gray)
                }
                Button(onClick = { showDocDialog = true }, shape = RoundedCornerShape(12.dp)) {
                    Text("Upload KYC")
                }
            }
        }

        // Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox(label = "Today's Earnings", value = "₹3,420", modifier = Modifier.weight(1.0f))
                StatBox(label = "Today's Bookings", value = "18 Bookings", modifier = Modifier.weight(1.0f))
                StatBox(label = "Platform Commission", value = "${config?.commissionPercentage?.toInt() ?: 30}%", modifier = Modifier.weight(1.0f))
            }
        }

        // Manage Lots title & add lot button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "My Parking Lots", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Button(onClick = { viewModel.partnerScreen.value = "LotForm" }) {
                    Text("+ Add Lot")
                }
            }
        }

        items(lots) { lot ->
            var expandedSettings by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = lot.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "₹${lot.pricePerHour.toInt()}/hr",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { expandedSettings = !expandedSettings }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Settings, contentDescription = "Manage Slot")
                            }
                        }
                    }

                    Text(text = lot.location, fontSize = 12.sp, color = Color.Gray)

                    // Slots breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SlotStatusItem("Bike", lot.bikeSlotsAvailable, lot.bikeSlotsTotal)
                        SlotStatusItem("Car", lot.carSlotsAvailable, lot.carSlotsTotal)
                        SlotStatusItem("SUV", lot.suvSlotsAvailable, lot.suvSlotsTotal)
                        SlotStatusItem("EV", lot.evSlotsAvailable, lot.evSlotsTotal)
                    }

                    if (expandedSettings) {
                        HorizontalDivider()
                        Text(text = "Interactive Slot Configuration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.addOrUpdatePartnerLot(lot.copy(availableSlots = (lot.availableSlots - 1).coerceAtLeast(0)))
                                },
                                modifier = Modifier.weight(1.0f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Block Slot", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    viewModel.addOrUpdatePartnerLot(lot.copy(availableSlots = (lot.availableSlots + 1).coerceAtMost(lot.totalSlots)))
                                },
                                modifier = Modifier.weight(1.0f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("Release Slot", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { viewModel.deletePartnerLot(lot) },
                                modifier = Modifier.weight(1.0f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) {
                                Text("Delete", fontSize = 11.sp)
                            }
                        }

                        // Dynamic rates configuration
                        Text(text = "Configure Weekend Dynamic Rates (₹)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(onClick = { viewModel.addOrUpdatePartnerLot(lot.copy(weekendPrice = lot.pricePerHour + 15)) }, modifier = Modifier.weight(1.0f)) {
                                Text("+15 Weekend", fontSize = 9.sp)
                            }
                            OutlinedButton(onClick = { viewModel.addOrUpdatePartnerLot(lot.copy(festivalPrice = lot.pricePerHour + 30)) }, modifier = Modifier.weight(1.0f)) {
                                Text("+30 Festival", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDocDialog) {
        var aadhaar by remember { mutableStateOf("") }
        var pan by remember { mutableStateOf("") }
        var license by remember { mutableStateOf("") }
        var bank by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDocDialog = false },
            title = { Text("Partner Registration (KYC)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Submit documents for admin verification.", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(value = aadhaar, onValueChange = { aadhaar = it }, placeholder = { Text("Aadhaar Number") })
                    OutlinedTextField(value = pan, onValueChange = { pan = it }, placeholder = { Text("PAN Number") })
                    OutlinedTextField(value = license, onValueChange = { license = it }, placeholder = { Text("Shop License No.") })
                    OutlinedTextField(value = bank, onValueChange = { bank = it }, placeholder = { Text("Bank Account (IFSC)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (aadhaar.isNotBlank() && pan.isNotBlank()) {
                        viewModel.registerPartnerProfile(
                            "Karthik Raja", "karthik@gmail.com", "9876543210", aadhaar, pan, license, bank
                        )
                        showDocDialog = false
                    }
                }) {
                    Text("Submit KYC")
                }
            }
        )
    }
}

@Composable
fun SlotStatusItem(type: String, available: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = type, fontSize = 12.sp, color = Color.Gray)
        Text(text = "$available/$total", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun PartnerLotFormScreen(viewModel: ParkEasyViewModel) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var totalSlots by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Register New Parking Lot", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("Parking Lot Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = location, onValueChange = { location = it }, placeholder = { Text("Full Location / Address") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = price, onValueChange = { price = it }, placeholder = { Text("Price Per Hour (₹)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = totalSlots, onValueChange = { totalSlots = it }, placeholder = { Text("Total Slots Available") }, modifier = Modifier.fillMaxWidth())

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { viewModel.partnerScreen.value = "Dashboard" }) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val p = price.toDoubleOrNull() ?: 30.0
                val ts = totalSlots.toIntOrNull() ?: 20
                if (name.isNotBlank() && location.isNotBlank()) {
                    viewModel.addOrUpdatePartnerLot(
                        ParkingLot(
                            name = name,
                            location = location,
                            pricePerHour = p,
                            totalSlots = ts,
                            availableSlots = ts,
                            distanceMeters = (100..900).random(),
                            hasCCTV = true,
                            isCovered = true,
                            hasEVCharging = false,
                            hasSecurity = true,
                            hasWashroom = false,
                            openingHours = "9AM-10PM",
                            rating = 5.0,
                            weekdayPrice = p,
                            weekendPrice = p + 10,
                            festivalPrice = p + 20,
                            peakHoursPrice = p + 15
                        )
                    )
                }
            }) {
                Text("Register Lot")
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.shadow(1.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
            Text(text = value, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
    }
}

// ==========================================
// 3. ADMIN PANEL MODE
// ==========================================

@Composable
fun AdminPanelContent(viewModel: ParkEasyViewModel) {
    val partners by viewModel.partners.collectAsState()
    val lots by viewModel.parkingLots.collectAsState()
    val config by viewModel.systemConfig.collectAsState()

    var commissionValue by remember { mutableStateOf(config?.commissionPercentage ?: 30.0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Stats Title
        item {
            Column {
                Text(text = "Global Admin Center", fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(text = "Verify partner licenses, audit lots and trace fees", fontSize = 13.sp, color = Color.Gray)
            }
        }

        // Comm stats cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox("Total Partners", "${partners.size} Profiles", modifier = Modifier.weight(1.0f))
                StatBox("Live Lots", "${lots.size} Hubs", modifier = Modifier.weight(1.0f))
                StatBox("Admin Earnings", "₹12,480", modifier = Modifier.weight(1.0f))
            }
        }

        // Commission Adjustment slider
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Configure Platform Commission (%)", fontWeight = FontWeight.Bold)
                    Text(text = "Adjust commission deducted from Parking Partners.", fontSize = 12.sp, color = Color.Gray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = commissionValue.toFloat(),
                            onValueChange = {
                                commissionValue = it.toDouble()
                                viewModel.setPlatformCommission(it.toDouble())
                            },
                            valueRange = 5f..50f,
                            modifier = Modifier.weight(1.0f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "${commissionValue.toInt()}%", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Customer Pays: ₹100", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "Partner Gets: ₹${100 - commissionValue.toInt()}", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "Park Easy Commission: ₹${commissionValue.toInt()}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // KYC Verification queue
        item {
            Text(text = "Pending KYC Partner Approvals", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        val pendingPartners = partners.filter { it.status == "Pending" }

        if (pendingPartners.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "All KYC applications reviewed. Verification queue clear.", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }

        items(pendingPartners) { partner ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = partner.name, fontWeight = FontWeight.Bold)
                            Text(text = partner.email, fontSize = 12.sp, color = Color.Gray)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "KYC Pending", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                        }
                    }

                    HorizontalDivider()

                    Text(text = "Aadhaar: ${partner.aadhaar}", fontSize = 13.sp)
                    Text(text = "PAN Card: ${partner.pan}", fontSize = 13.sp)
                    Text(text = "Shop License: ${partner.shopLicense}", fontSize = 13.sp)
                    Text(text = "Bank Account: ${partner.bankAccount}", fontSize = 13.sp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.approvePartner(partner) },
                            modifier = Modifier.weight(1.0f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Verify & Approve", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.rejectPartner(partner) },
                            modifier = Modifier.weight(1.0f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Reject Documents", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Global Lots list management
        item {
            Text(text = "Audit Active Parking Hubs", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        items(lots) { lot ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = Color.White,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = lot.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Rate: ₹${lot.pricePerHour.toInt()}/hr • ${lot.availableSlots}/${lot.totalSlots} slots free", fontSize = 12.sp, color = Color.Gray)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.deletePartnerLot(lot) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Delete Lot", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ParkEasyViewModel) {
    val name by viewModel.profileName.collectAsState()
    val mobile by viewModel.profileMobile.collectAsState()
    val email by viewModel.profileEmail.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val walletState by viewModel.wallet.collectAsState()
    val hasPass by viewModel.hasMonthlyPass.collectAsState()

    var editName by remember { mutableStateOf(name) }
    var editMobile by remember { mutableStateOf(mobile) }
    var editEmail by remember { mutableStateOf(email) }
    var showSavedMessage by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF121212) else Color(0xFFF8FAFC))
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 40.dp)
    ) {
        // App Header styled with huge typography like the image reference
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "SETTINGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeoPurplePrimary,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your Profile",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDarkTheme) Color.White else GeoDarkText,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Manage your digital workspace & parking details",
                    fontSize = 13.sp,
                    color = GeoGrayLabel
                )
            }
        }

        // Profile Card with high-fidelity aesthetics matching "Holidays in Norway"
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GeoPurpleLight.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color(0xFF1E1B2C) else GeoPurpleLight.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(GeoPurplePrimary)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (name.isNotEmpty()) name.take(2).uppercase() else "PE",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = if (isDarkTheme) Color.White else GeoDarkText
                        )
                        Text(
                            text = email,
                            fontSize = 12.sp,
                            color = GeoGrayLabel
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDarkTheme) Color(0xFF2D2544) else GeoPurpleLight)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Points: ${walletState?.loyaltyPoints ?: 120} • Active Member",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeoPurpleOnContainer
                            )
                        }
                    }
                }
            }
        }

        // Color theme switch - customized pill selector like the reference image
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GeoBorder.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dark Color Mode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDarkTheme) Color.White else GeoDarkText
                        )
                        Text(
                            text = "Switch system visual theme on the fly",
                            fontSize = 11.sp,
                            color = GeoGrayLabel
                        )
                    }
                    
                    // Styled Pill Switch resembling the task tracker
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDarkTheme) GeoPurplePrimary else Color(0xFFE2E8F0))
                            .clickable { viewModel.isDarkTheme.value = !isDarkTheme }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .align(if (isDarkTheme) Alignment.CenterEnd else Alignment.CenterStart)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = "Mode Icon",
                                tint = if (isDarkTheme) GeoPurplePrimary else Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Profile Form Card styled like the image's text inputs
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GeoBorder.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Edit Profile Info",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = if (isDarkTheme) Color.White else GeoDarkText
                    )

                    // Text fields with beautiful high-contrast styling
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPurplePrimary,
                            unfocusedBorderColor = GeoBorder.copy(alpha = 0.4f),
                            focusedLabelColor = GeoPurplePrimary,
                            unfocusedLabelColor = GeoGrayLabel,
                            focusedTextColor = if (isDarkTheme) Color.White else GeoDarkText,
                            unfocusedTextColor = if (isDarkTheme) Color.White else GeoDarkText
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPurplePrimary,
                            unfocusedBorderColor = GeoBorder.copy(alpha = 0.4f),
                            focusedLabelColor = GeoPurplePrimary,
                            unfocusedLabelColor = GeoGrayLabel,
                            focusedTextColor = if (isDarkTheme) Color.White else GeoDarkText,
                            unfocusedTextColor = if (isDarkTheme) Color.White else GeoDarkText
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editMobile,
                        onValueChange = { editMobile = it },
                        label = { Text("Mobile Number", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPurplePrimary,
                            unfocusedBorderColor = GeoBorder.copy(alpha = 0.4f),
                            focusedLabelColor = GeoPurplePrimary,
                            unfocusedLabelColor = GeoGrayLabel,
                            focusedTextColor = if (isDarkTheme) Color.White else GeoDarkText,
                            unfocusedTextColor = if (isDarkTheme) Color.White else GeoDarkText
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            viewModel.profileName.value = editName
                            viewModel.profileEmail.value = editEmail
                            viewModel.profileMobile.value = editMobile
                            showSavedMessage = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GeoPurplePrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Save Profile Changes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    if (showSavedMessage) {
                        Text(
                            text = "✓ Profile info updated successfully!",
                            color = GeoSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        // Additional features card (Monthly Pass, Referral, Clear Data)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GeoBorder.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Premium Features & Tools",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = if (isDarkTheme) Color.White else GeoDarkText
                    )

                    // Monthly pass status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Office Monthly Pass",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDarkTheme) Color.White else GeoDarkText
                            )
                            Text(
                                text = if (hasPass) "Expires on August 4, 2026" else "Save 40% on daily commutes",
                                fontSize = 11.sp,
                                color = GeoGrayLabel
                            )
                        }
                        if (hasPass) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GeoSuccess.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GeoSuccess
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.buyMonthlyPass() },
                                colors = ButtonDefaults.buttonColors(containerColor = GeoPurplePrimary),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Get (₹2500)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = GeoBorder.copy(alpha = 0.25f))

                    // Claim code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Referral Bonus Code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDarkTheme) Color.White else GeoDarkText
                            )
                            Text(
                                text = "Claim free ₹50 wallet cashback",
                                fontSize = 11.sp,
                                color = GeoGrayLabel
                            )
                        }
                        Button(
                            onClick = { viewModel.claimReferralReward() },
                            colors = ButtonDefaults.buttonColors(containerColor = GeoSuccess),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Claim ₹50", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreMapScreen(viewModel: ParkEasyViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lots by viewModel.parkingLots.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    
    var hasLocationPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    
    var bypassPermissionPrompt by remember { mutableStateOf(false) }
    
    // Runtime location permission requesting
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fineGranted || coarseGranted
        android.util.Log.d("ParkEasy", "Fine location: $fineGranted, Coarse: $coarseGranted")
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, Available, EV Charging, Under ₹30
    
    val filteredLots = remember(lots, searchQuery, selectedFilter) {
        val cleanQuery = searchQuery.trim()
            .lowercase()
            .replace("parking lot near me", "")
            .replace("parking near me", "")
            .replace("lot near me", "")
            .replace("near me", "")
            .replace("nearby", "")
            .replace("parking", "")
            .replace("lots", "")
            .replace("lot", "")
            .replace("find", "")
            .replace("show", "")
            .replace("at", "")
            .replace("in", "")
            .trim()
        
        val queryTokens = cleanQuery.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        
        val filtered = lots.filter { lot ->
            val matchesQuery = if (queryTokens.isEmpty()) {
                true
            } else {
                queryTokens.all { token ->
                    lot.name.contains(token, ignoreCase = true) || 
                    lot.location.contains(token, ignoreCase = true)
                }
            }
            val matchesFilter = when (selectedFilter) {
                "Available" -> lot.availableSlots > 0
                "EV Charging" -> lot.hasEVCharging
                "Under ₹30" -> lot.pricePerHour <= 30
                else -> true
            }
            matchesQuery && matchesFilter
        }
        
        // Sort by distance if search is generic, empty, or asks for "near" or "nearby"
        if (searchQuery.contains("near", ignoreCase = true) || searchQuery.isEmpty() || cleanQuery.isEmpty()) {
            filtered.sortedBy { lot -> lot.distanceMeters }
        } else {
            filtered
        }
    }
    
    var selectedLotOnMap by remember { mutableStateOf<ParkingLot?>(null) }
    
    if (hasLocationPermission || bypassPermissionPrompt) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color(0xFF121212) else Color(0xFFF8FAFC))
        ) {
        // Full screen Map View
        LeafletMapView(
            parkingLots = filteredLots,
            onLotSelected = { lot ->
                selectedLotOnMap = lot
            },
            hasLocationPermission = hasLocationPermission,
            modifier = Modifier.fillMaxSize()
        )
        
        // GMaps style Top Search Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Rounded white Card like GMaps desktop/mobile search bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .border(
                        1.dp, 
                        if (isDarkTheme) Color(0xFF2D2544) else GeoBorder.copy(alpha = 0.5f), 
                        RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                ),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search icon",
                        tint = if (isDarkTheme) Color.LightGray else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_search_input"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = if (isDarkTheme) Color.White else GeoDarkText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search parking lot near me...",
                                    fontSize = 15.sp,
                                    color = GeoGrayLabel
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                                tint = if (isDarkTheme) Color.White else Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            
            // Filter Horizontal Scroll Row matching GMaps filter pills
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filtersList = listOf("All", "Available", "EV Charging", "Under ₹30")
                items(filtersList) { filter ->
                    val isSelected = selectedFilter == filter
                    val containerColor = if (isSelected) GeoPurplePrimary else (if (isDarkTheme) Color(0xFF2D2D2D) else Color.White)
                    val contentColor = if (isSelected) Color.White else (if (isDarkTheme) Color.LightGray else GeoDarkText)
                    val borderColor = if (isSelected) Color.Transparent else (if (isDarkTheme) Color(0xFF3D3D3D) else GeoBorder.copy(alpha = 0.3f))
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(containerColor)
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }
            }
        }
        
        // Sliding Bottom Panel
        // If a lot is selected, show details overlay card exactly like Google Maps.
        // Otherwise, show horizontal carousel of nearby matching lots!
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            AnimatedContent(
                targetState = selectedLotOnMap,
                transitionSpec = {
                    slideInVertically { height -> height } togetherWith slideOutVertically { height -> height }
                },
                label = "map_details_overlay"
            ) { lot ->
                if (lot != null) {
                    // Google Maps detailed lot card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .border(1.dp, GeoPurpleLight.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) Color(0xFF1E1B2C) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Header of details overlay
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lot.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDarkTheme) Color.White else GeoDarkText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Dynamic reviews & stars based on ID to simulate GMaps
                                        val rating = when (lot.id % 3) {
                                            0L -> "4.5 ★★★★☆"
                                            1L -> "4.0 ★★★★☆"
                                            else -> "3.8 ★★★☆☆"
                                        }
                                        val reviewsCount = (lot.id * 7 + 12) % 45 + 5
                                        Text(
                                            text = rating,
                                            color = Color(0xFFFFB300),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${reviewsCount} reviews)",
                                            color = GeoGrayLabel,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                
                                IconButton(
                                    onClick = { selectedLotOnMap = null },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFF1F5F9),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close detail",
                                        tint = if (isDarkTheme) Color.White else Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Address details
                            Text(
                                text = lot.location,
                                fontSize = 12.sp,
                                color = GeoGrayLabel
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Features/Chips row (CCTV, EV, Price)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GeoSuccess.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "₹${lot.pricePerHour}/hr",
                                        color = GeoSuccess,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GeoPurpleLight.copy(alpha = 0.25f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${lot.availableSlots} / ${lot.totalSlots} Slots Free",
                                        color = GeoPurpleOnContainer,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                if (lot.hasEVCharging) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0EA5E9).copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.EvStation,
                                                contentDescription = "EV Station",
                                                tint = Color(0xFF0EA5E9),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "EV Support",
                                                color = Color(0xFF0EA5E9),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Bottom Action Buttons: Directions and Book Now
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.selectedLot.value = lot
                                        viewModel.navigateToCustomer("Details")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, GeoPurplePrimary),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = GeoPurplePrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Directions,
                                        contentDescription = "Directions",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Directions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                
                                Button(
                                    onClick = {
                                        viewModel.selectedLot.value = lot
                                        viewModel.navigateToCustomer("BookingFlow")
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GeoPurplePrimary,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Book Slot (₹${lot.pricePerHour}/hr)", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Display Horizontal Carousel of matching lots if no map selection is active
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Swipe to explore nearby lots",
                            fontSize = 11.sp,
                            color = if (isDarkTheme) Color.LightGray else GeoDarkText,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .background(
                                    if (isDarkTheme) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredLots) { lotItem ->
                                Card(
                                    modifier = Modifier
                                        .width(280.dp)
                                        .border(
                                            1.dp,
                                            if (isDarkTheme) Color(0xFF2D2544) else GeoBorder.copy(alpha = 0.3f),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable { selectedLotOnMap = lotItem },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                                    ),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(GeoPurplePrimary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.LocalParking,
                                                contentDescription = "Parking Lot",
                                                tint = GeoPurplePrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = lotItem.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isDarkTheme) Color.White else GeoDarkText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = lotItem.location,
                                                fontSize = 10.sp,
                                                color = GeoGrayLabel,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "₹${lotItem.pricePerHour}/hr",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GeoSuccess
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "• ${lotItem.availableSlots} slots free",
                                                    fontSize = 10.sp,
                                                    color = GeoGrayLabel
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
        }
    }
} else {
        // Beautiful Native Location Permission Prompt Handler
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color(0xFF121212) else Color(0xFFF8FAFC))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant badge / Icon illustration
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(if (isDarkTheme) Color(0xFF1E1B2C) else Color(0xFFEEF2F6)),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing outer ring
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(GeoPurplePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "Location Access Needed",
                        tint = GeoPurplePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Enable Location Services",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = if (isDarkTheme) Color.White else GeoDarkText,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "To find the closest parking garages, view real-time slot availability, and navigate directly to your spot, ParkEasy needs location access.",
                fontSize = 14.sp,
                color = GeoGrayLabel,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Feature highlight list cards
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp, 
                        if (isDarkTheme) Color(0xFF2D2544) else GeoBorder.copy(alpha = 0.3f), 
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Feature 1
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GeoPurplePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Nearby icon",
                                tint = GeoPurplePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Smart Proximity Finder",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else GeoDarkText
                            )
                            Text(
                                text = "Detects available lots closest to your current spot.",
                                fontSize = 11.sp,
                                color = GeoGrayLabel
                            )
                        }
                    }
                    
                    // Feature 2
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GeoPurplePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsCar,
                                contentDescription = "Navigation icon",
                                tint = GeoPurplePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Turn-by-Turn Guidance",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else GeoDarkText
                            )
                            Text(
                                text = "Offers real-time map routes and accurate distance estimations.",
                                fontSize = 11.sp,
                                color = GeoGrayLabel
                            )
                        }
                    }
                    
                    // Feature 3
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GeoPurplePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FlashOn,
                                contentDescription = "Live updates icon",
                                tint = GeoPurplePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Instant Live Parking Rates",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else GeoDarkText
                            )
                            Text(
                                text = "Keeps you updated with live rates and occupancy rates.",
                                fontSize = 11.sp,
                                color = GeoGrayLabel
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(36.dp))
            
            // Primary Enable Button
            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("enable_location_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GeoPurplePrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.GpsFixed,
                    contentDescription = "Location Enable",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Allow Location Access",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Secondary default location button
            TextButton(
                onClick = {
                    bypassPermissionPrompt = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("default_location_button")
            ) {
                Text(
                    text = "Explore with Coimbatore default location",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) GeoPurpleLight else GeoPurplePrimary
                )
            }
        }
    }
}
