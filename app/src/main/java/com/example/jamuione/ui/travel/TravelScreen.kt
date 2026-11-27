package com.example.jamuione.ui.travel

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jamuione.domain.model.Place
import com.example.jamuione.domain.model.Trip
import com.example.jamuione.ui.components.EmptyState
import com.example.jamuione.ui.components.ErrorState
import com.example.jamuione.util.PlaceCatalog
import com.example.jamuione.util.Resource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelScreen(
    viewModel: TravelViewModel,
    onRequestRideClick: () -> Unit,
    onPostTripClick: () -> Unit,
    onMyTripsClick: () -> Unit,
    onApplyOperatorClick: () -> Unit,
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val fromKey by viewModel.fromKey.collectAsState()
    val toKey by viewModel.toKey.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val hideFullTrips by viewModel.hideFullTrips.collectAsState()
    val bannerDismissed by viewModel.bannerDismissed.collectAsState()
    val tripsResource by viewModel.trips.collectAsState()
    val isOperator by viewModel.isOperator.collectAsState()
    val requestCountRes by viewModel.requestCount.collectAsState()
    val myRequestsRes by viewModel.myRequests.collectAsState()
    val operatorDemandRes by viewModel.operatorDemand.collectAsState()

    LaunchedEffect(initialTab) {
        viewModel.setTab(initialTab)
    }

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val fromPlace = PlaceCatalog.byKey(context, fromKey) ?: Place(fromKey, fromKey, fromKey, "city")
    val toPlace = PlaceCatalog.byKey(context, toKey) ?: Place(toKey, toKey, toKey, "city")

    val calendar = Calendar.getInstance()
    val todayMillis = calendar.apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val tomorrowMillis = calendar.apply {
        timeInMillis = todayMillis
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Travel & Rides", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onRequestRideClick) {
                        Text("I need a ride")
                    }
                    if (isOperator) {
                        TextButton(onClick = onPostTripClick) {
                            Text("Post a trip")
                        }
                        TextButton(onClick = onMyTripsClick) {
                            Text("My trips")
                        }
                    } else {
                        TextButton(onClick = onApplyOperatorClick) {
                            Text("Offer seats")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                isRefreshing = false
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Banner
                if (!bannerDismissed) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Operators are independent. District One does not take bookings or payments. Call the operator to confirm.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.dismissBanner() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }

                // Search Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("From", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedCard(
                                        onClick = { showFromPicker = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(text = fromPlace.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(text = fromPlace.nameHi, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.swapRoutes() },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Icon(Icons.Default.SwapVert, contentDescription = "Swap Routes", tint = MaterialTheme.colorScheme.primary)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("To", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedCard(
                                        onClick = { showToPicker = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(text = toPlace.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(text = toPlace.nameHi, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }

                            // Date Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = selectedDate == todayMillis,
                                    onClick = { viewModel.setDate(todayMillis) },
                                    label = { Text("Today") }
                                )
                                FilterChip(
                                    selected = selectedDate == tomorrowMillis,
                                    onClick = { viewModel.setDate(tomorrowMillis) },
                                    label = { Text("Tomorrow") }
                                )
                                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
                                var showDatePickerDialog by remember { mutableStateOf(false) }

                                OutlinedButton(
                                    onClick = { showDatePickerDialog = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(dateFormatter.format(Date(selectedDate)))
                                }

                                if (showDatePickerDialog) {
                                    DatePickerDialog(
                                        onDismissRequest = { showDatePickerDialog = false },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                datePickerState.selectedDateMillis?.let { viewModel.setDate(it) }
                                                showDatePickerDialog = false
                                            }) {
                                                Text("OK")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDatePickerDialog = false }) {
                                                Text("Cancel")
                                            }
                                        }
                                    ) {
                                        DatePicker(state = datePickerState)
                                    }
                                }
                            }

                            // Segmented control: 'Find a ride' | 'Requests'
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                SegmentedButton(
                                    selected = selectedTab == 0,
                                    onClick = { viewModel.setTab(0) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                ) {
                                    Text("Find a ride")
                                }
                                SegmentedButton(
                                    selected = selectedTab == 1,
                                    onClick = { viewModel.setTab(1) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) {
                                    Text(if (isOperator) "Demand" else "My requests")
                                }
                            }

                            if (selectedTab == 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Hide full trips (Seats left > 0)", style = MaterialTheme.typography.bodyMedium)
                                    Switch(
                                        checked = hideFullTrips,
                                        onCheckedChange = { viewModel.setHideFullTrips(it) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Tab Content
                if (selectedTab == 0) {
                    // Find a ride
                    if (requestCountRes is Resource.Success && (requestCountRes.data ?: 0L) > 0L) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "${requestCountRes.data} people are looking for this route on this day",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    when (val resource = tripsResource) {
                        is Resource.Loading, is Resource.Idle -> {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        is Resource.Success -> {
                            val trips = resource.data ?: emptyList()
                            if (trips.isEmpty()) {
                                item {
                                    EmptyState(
                                        icon = Icons.Default.DirectionsCar,
                                        title = "No trips posted for this route yet",
                                        message = "Be the first to post a trip or request a ride.",
                                        primaryActionText = "I need a ride",
                                        onPrimaryAction = onRequestRideClick,
                                        secondaryActionText = "Try another day",
                                        onSecondaryAction = { viewModel.setDate(tomorrowMillis) }
                                    )
                                }
                            } else {
                                items(trips, key = { it.id }) { trip ->
                                    TripCard(
                                        trip = trip,
                                        currentUserId = viewModel.currentUserId,
                                        onMarkFull = { tripId -> viewModel.markFull(tripId) }
                                    )
                                }
                            }
                        }
                        is Resource.Error -> {
                            item {
                                ErrorState(
                                    message = resource.message ?: "Failed to load trips",
                                    onRetry = {}
                                )
                            }
                        }
                    }
                } else {
                    // Requests tab
                    if (!isOperator) {
                        // Passenger: My requests
                        when (val resource = myRequestsRes) {
                            is Resource.Loading, is Resource.Idle -> {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            is Resource.Success -> {
                                val requests = resource.data ?: emptyList()
                                if (requests.isEmpty()) {
                                    item {
                                        EmptyState(
                                            icon = Icons.Default.BookmarkBorder,
                                            title = "No ride requests yet",
                                            message = "Post a request when you need a ride.",
                                            primaryActionText = "I need a ride",
                                            onPrimaryAction = onRequestRideClick
                                        )
                                    }
                                } else {
                                    items(requests, key = { it.id }) { req ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("${req.fromName} → ${req.toName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                    Text("Seats needed: ${req.seatsNeeded}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Text("Date: ${dateFormatter.format(Date(req.departureAt))} (${req.timeWindow})", style = MaterialTheme.typography.bodySmall)
                                                if (req.notes.isNotBlank()) {
                                                    Text("Notes: ${req.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                OutlinedButton(
                                                    onClick = { viewModel.cancelRequest(req.id) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                                ) {
                                                    Text("Cancel Request")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is Resource.Error -> {
                                item { ErrorState(message = resource.message ?: "Failed to load requests", onRetry = {}) }
                            }
                        }
                    } else {
                        // Operator: Demand for next 7 days
                        when (val resource = operatorDemandRes) {
                            is Resource.Loading, is Resource.Idle -> {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            is Resource.Success -> {
                                val demands = resource.data ?: emptyList()
                                if (demands.isEmpty()) {
                                    item {
                                        EmptyState(
                                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                                            title = "No passenger demand found",
                                            message = "Check back later for ride requests in your area."
                                        )
                                    }
                                } else {
                                    items(demands, key = { it.id }) { demand ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("${demand.fromName} → ${demand.toName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                    Text("${demand.seatsNeeded} seats requested", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                }
                                                Text("Date: ${dateFormatter.format(Date(demand.departureAt))} (${demand.timeWindow})", style = MaterialTheme.typography.bodySmall)
                                                if (demand.notes.isNotBlank()) {
                                                    Text("Passenger notes: ${demand.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(
                                                    onClick = onPostTripClick,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("Post a trip for this")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is Resource.Error -> {
                                item { ErrorState(message = resource.message ?: "Failed to load demand", onRetry = {}) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFromPicker) {
        PlacePickerDialog(
            title = "Select Departure Place",
            context = context,
            onDismiss = { showFromPicker = false },
            onSelect = { place ->
                viewModel.setFrom(place.key)
                showFromPicker = false
            }
        )
    }

    if (showToPicker) {
        PlacePickerDialog(
            title = "Select Destination Place",
            context = context,
            onDismiss = { showToPicker = false },
            onSelect = { place ->
                viewModel.setTo(place.key)
                showToPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacePickerDialog(
    title: String,
    context: android.content.Context,
    onDismiss: () -> Unit,
    onSelect: (Place) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val places = PlaceCatalog.search(context, query)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search place...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(places) { place ->
                    Card(
                        onClick = { onSelect(place) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(place.nameHi, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Text(place.type.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
