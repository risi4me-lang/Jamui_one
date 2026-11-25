package com.example.jamuione.ui.travel

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jamuione.util.Resource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestRideScreen(
    viewModel: RequestRideViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val fromKey by viewModel.fromKey.collectAsState()
    val toKey by viewModel.toKey.collectAsState()
    val fromName by viewModel.fromName.collectAsState()
    val toName by viewModel.toName.collectAsState()
    val departureDate by viewModel.departureDate.collectAsState()
    val seatsNeeded by viewModel.seatsNeeded.collectAsState()
    val timeWindow by viewModel.timeWindow.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val createResult by viewModel.createResult.collectAsState()

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(createResult) {
        if (createResult is Resource.Success) {
            snackbarHostState.showSnackbar("Ride request posted successfully!")
            onSuccess()
        } else if (createResult is Resource.Error) {
            snackbarHostState.showSnackbar(createResult.message ?: "Failed to post request")
        }
    }

    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Request a Ride", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Route", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedCard(
                        onClick = { showFromPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("From", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(fromName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedCard(
                        onClick = { showToPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("To", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(toName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text("Schedule & Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                var showDatePicker by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Date: ${dateFormatter.format(Date(departureDate))}")
                }
                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = departureDate)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { viewModel.setDepartureDate(it) }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
            }
            item {
                Text("Time Window", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Morning", "Afternoon", "Evening", "Anytime").forEach { tw ->
                        FilterChip(
                            selected = timeWindow == tw,
                            onClick = { viewModel.setTimeWindow(tw) },
                            label = { Text(tw) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Text("Request Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                OutlinedTextField(
                    value = seatsNeeded.toString(),
                    onValueChange = { it.toIntOrNull()?.let { s -> viewModel.setSeatsNeeded(s) } },
                    label = { Text("Seats Needed") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { viewModel.setPhone(it) },
                    label = { Text("Contact Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { viewModel.setNotes(it) },
                    label = { Text("Notes / Additional Info (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.postRequest() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = createResult !is Resource.Loading && fromKey.isNotBlank() && toKey.isNotBlank() && phone.isNotBlank()
                ) {
                    if (createResult is Resource.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Post Ride Request", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                viewModel.setFrom(place.key, place.name)
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
                viewModel.setTo(place.key, place.name)
                showToPicker = false
            }
        )
    }
}
