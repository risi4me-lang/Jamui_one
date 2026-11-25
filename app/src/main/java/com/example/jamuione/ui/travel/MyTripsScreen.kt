package com.example.jamuione.ui.travel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jamuione.domain.model.Trip
import com.example.jamuione.util.Resource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTripsScreen(
    viewModel: MyTripsViewModel,
    onBack: () -> Unit
) {
    val tripsResource by viewModel.myTrips.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Upcoming", "Past")

    val now = System.currentTimeMillis()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Trips", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (val resource = tripsResource) {
                is Resource.Loading, is Resource.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is Resource.Success -> {
                    val allTrips = resource.data ?: emptyList()
                    val filtered = if (selectedTab == 0) {
                        allTrips.filter { it.departureAt >= now && it.status != "cancelled" }
                    } else {
                        allTrips.filter { it.departureAt < now || it.status == "cancelled" }
                    }

                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No trips found.", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filtered, key = { it.id }) { trip ->
                                MyTripCard(
                                    trip = trip,
                                    onSeatMinus = { viewModel.updateSeats(trip.id, trip.seatsLeft, -1) },
                                    onMarkFull = { viewModel.markFull(trip.id) },
                                    onCancel = { viewModel.cancelTrip(trip.id) },
                                    onRepeatTomorrow = { viewModel.repeatTomorrow(trip) }
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${resource.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun MyTripCard(
    trip: Trip,
    onSeatMinus: () -> Unit,
    onMarkFull: () -> Unit,
    onCancel: () -> Unit,
    onRepeatTomorrow: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${trip.fromName} → ${trip.toName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (trip.status == "open") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = trip.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = if (trip.status == "open") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(text = "Departure: ${dateFormatter.format(Date(trip.departureAt))}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Vehicle: ${trip.vehicle} | Seats left: ${trip.seatsLeft}/${trip.seatsTotal}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Fare: ${if (trip.farePerSeat == 0) "Ask" else "₹${trip.farePerSeat}"}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(onClick = onSeatMinus, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text("Seats -1", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = onMarkFull, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text("Mark full", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Cancel", style = MaterialTheme.typography.labelSmall)
                }
            }
            Button(onClick = onRepeatTomorrow, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Text("Repeat tomorrow")
            }
        }
    }
}
