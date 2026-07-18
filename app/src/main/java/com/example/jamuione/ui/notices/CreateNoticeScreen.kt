package com.example.jamuione.ui.notices

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jamuione.domain.model.Notice
import com.example.jamuione.util.NetworkUtils
import com.example.jamuione.util.Resource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoticeScreen(
    viewModel: NoticeViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(viewModel.categories.first()) }
    var contactNumber by remember { mutableStateOf("") }
    var daysToExpiry by remember { mutableStateOf(7) }
    
    // Event state
    var eventDate by remember { mutableStateOf<Long?>(null) }
    var eventLocation by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Poll state
    var isPoll by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOptions by remember { mutableStateOf(listOf("", "")) }
    var pollDurationDays by remember { mutableStateOf(7) }

    val createResult by viewModel.createNoticeResult.collectAsState()
    var categoryExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = eventDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    eventDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LaunchedEffect(createResult) {
        if (createResult is Resource.Success && createResult.data == true) {
            snackbarHostState.showSnackbar("Notice posted successfully!")
            viewModel.resetCreateNoticeResult()
            onBack()
        } else if (createResult is Resource.Error) {
            snackbarHostState.showSnackbar(createResult.message ?: "Failed to post notice")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Post an Event") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 100) title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("${title.length}/100", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    viewModel.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 2000) description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                supportingText = { Text("${description.length}/2000", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End) }
            )

            if (selectedCategory == "Event") {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Event Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = if (eventDate != null) {
                        java.text.SimpleDateFormat("EEE, MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(eventDate!!))
                    } else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Event Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.Event, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = eventLocation,
                    onValueChange = { eventLocation = it },
                    label = { Text("Event Location") },
                    placeholder = { Text("e.g. Community Center, Main Street") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // POLL TOGGLE
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isPoll, onCheckedChange = { isPoll = it })
                Text("Add a Poll", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(visible = isPoll) {
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    OutlinedTextField(
                        value = pollQuestion,
                        onValueChange = { pollQuestion = it },
                        label = { Text("Poll Question") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    pollOptions.forEachIndexed { index, option ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            OutlinedTextField(
                                value = option,
                                onValueChange = { newValue ->
                                    val newList = pollOptions.toMutableList()
                                    newList[index] = newValue
                                    pollOptions = newList
                                },
                                label = { Text("Option ${index + 1}") },
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    if (pollOptions.size > 2) {
                                        IconButton(onClick = {
                                            pollOptions = pollOptions.toMutableList().apply { removeAt(index) }
                                        }) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            )
                        }
                    }
                    if (pollOptions.size < 5) {
                        TextButton(onClick = { pollOptions = pollOptions + "" }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Option")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Poll closes in: $pollDurationDays days", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = pollDurationDays.toFloat(),
                        onValueChange = { pollDurationDays = it.toInt() },
                        valueRange = 1f..14f,
                        steps = 13
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = contactNumber,
                onValueChange = { if (it.length <= 15) contactNumber = it },
                label = { Text("Contact Number (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Expiry: $daysToExpiry days", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = daysToExpiry.toFloat(),
                onValueChange = { daysToExpiry = it.toInt() },
                valueRange = 1f..30f,
                steps = 29
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            Button(
                onClick = { 
                    if (NetworkUtils.isNetworkAvailable(context)) {
                        viewModel.createNotice(
                            title = title,
                            description = description,
                            category = selectedCategory,
                            contact = contactNumber,
                            daysToExpiry = daysToExpiry,
                            pollQuestion = pollQuestion.takeIf { isPoll && it.isNotBlank() },
                            pollOptions = pollOptions.filter { it.isNotBlank() }.takeIf { isPoll && it.size >= 2 },
                            pollClosesAt = if (isPoll) System.currentTimeMillis() + (pollDurationDays * 24 * 60 * 60 * 1000L) else null,
                            eventDate = eventDate.takeIf { selectedCategory == "Event" },
                            eventLocation = eventLocation.takeIf { selectedCategory == "Event" && it.isNotBlank() }
                        )
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("You're offline.") }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = title.isNotBlank() && description.isNotBlank() && createResult !is Resource.Loading &&
                    (!isPoll || (pollQuestion.isNotBlank() && pollOptions.count { it.isNotBlank() } >= 2))
            ) {
                if (createResult is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Post to Board", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
