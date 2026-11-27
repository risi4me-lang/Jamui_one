package com.example.jamuione.ui.travel

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jamuione.ui.components.ErrorState
import com.example.jamuione.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyOperatorScreen(
    viewModel: ApplyOperatorViewModel,
    onBack: () -> Unit
) {
    val applicationRes by viewModel.application.collectAsState()
    val submitResult by viewModel.submitResult.collectAsState()

    val businessName by viewModel.businessName.collectAsState()
    val vehicleType by viewModel.vehicleType.collectAsState()
    val vehicleNumber by viewModel.vehicleNumber.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val routes by viewModel.routes.collectAsState()
    val notes by viewModel.notes.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(submitResult) {
        if (submitResult is Resource.Error) {
            snackbarHostState.showSnackbar(submitResult.message ?: "Failed to submit application")
        } else if (submitResult is Resource.Success) {
            snackbarHostState.showSnackbar("Application submitted successfully!")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Become a Verified Operator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val appResource = applicationRes) {
                is Resource.Loading, is Resource.Idle -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is Resource.Success -> {
                    val app = appResource.data
                    if (app != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Application received. We will contact you.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedButton(
                                onClick = { viewModel.withdrawApplication() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Withdraw Application")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text("List your sumo, bus or cab to start offering seats to passengers.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            item {
                                OutlinedTextField(
                                    value = businessName,
                                    onValueChange = { viewModel.setBusinessName(it) },
                                    label = { Text("Business / Operator Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = vehicleType,
                                    onValueChange = { viewModel.setVehicleType(it) },
                                    label = { Text("Vehicle Type (e.g. Sumo, Bus, Car)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = vehicleNumber,
                                    onValueChange = { viewModel.setVehicleNumber(it) },
                                    label = { Text("Vehicle Number (e.g. BR01AB1234)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { viewModel.setPhone(it) },
                                    label = { Text("Mobile Number (10 digits)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = routes,
                                    onValueChange = { viewModel.setRoutes(it) },
                                    label = { Text("Routes you run (e.g. Jamui to Patna daily)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { viewModel.setNotes(it) },
                                    label = { Text("Additional Notes (optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.submitApplication() },
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = submitResult !is Resource.Loading && businessName.isNotBlank() && phone.isNotBlank()
                                ) {
                                    if (submitResult is Resource.Loading) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Text("Submit Application", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    ErrorState(message = appResource.message ?: "Error loading application", onRetry = {})
                }
            }
        }
    }
}
