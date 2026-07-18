package com.example.jamuione.ui.notices

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.jamuione.util.NetworkUtils
import com.example.jamuione.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoticeScreen(
    viewModel: NoticeViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Announcement") }
    var contactNumber by remember { mutableStateOf("") }
    var daysToExpiry by remember { mutableIntStateOf(7) }
    
    var isPoll by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOptions by remember { mutableStateOf(listOf("", "")) }
    var pollDurationDays by remember { mutableStateOf(7) }

    val createResult by viewModel.createNoticeResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showProgressOverlay by remember { mutableStateOf(false) }
    var currentStage by remember { mutableStateOf(1) }

    var showDatePicker by remember { mutableStateOf(false) }
    var eventDate by remember { mutableStateOf<Long?>(null) }
    var eventLocation by remember { mutableStateOf("") }

    LaunchedEffect(createResult) {
        if (createResult is Resource.Success && createResult.data == true) {
            currentStage = 3
            delay(800)
            showProgressOverlay = false
            snackbarHostState.showSnackbar("Notice posted successfully!")
            viewModel.resetCreateNoticeResult()
            onBack()
        } else if (createResult is Resource.Error) {
            showProgressOverlay = false
            snackbarHostState.showSnackbar((createResult as Resource.Error).message ?: "Failed to post notice")
        } else if (createResult is Resource.Loading) {
            showProgressOverlay = true
            currentStage = 2
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create Notice") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val context = LocalContext.current
                    TextButton(
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
                                scope.launch {
                                    snackbarHostState.showSnackbar("You're offline.")
                                }
                            }
                        },
                        enabled = title.isNotBlank() && description.isNotBlank() && !showProgressOverlay
                    ) {
                        Text("POST")
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
            Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    viewModel.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedCategory == "Event") {
                Text("Event Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (eventDate != null) java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(eventDate!!)) else "Select Date")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = eventLocation,
                    onValueChange = { eventLocation = it },
                    label = { Text("Event Location (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text("Basic Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 100) title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("${title.length}/100") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                supportingText = { Text("${description.length}/500") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = contactNumber,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '+' || char == '-' }) contactNumber = it },
                label = { Text("Contact Number (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(text = "Expires in: $daysToExpiry days", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = daysToExpiry.toFloat(),
                onValueChange = { daysToExpiry = it.toInt() },
                valueRange = 1f..30f,
                steps = 29
            )

            Spacer(modifier = Modifier.height(24.dp))

            // POLL SECTION
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isPoll, onCheckedChange = { isPoll = it })
                Text("Add a Poll")
            }

            if (isPoll) {
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    OutlinedTextField(
                        value = pollQuestion,
                        onValueChange = { pollQuestion = it },
                        label = { Text("Poll Question") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    pollOptions.forEachIndexed { index, option ->
                        OutlinedTextField(
                            value = option,
                            onValueChange = { newVal ->
                                val newList = pollOptions.toMutableList()
                                newList[index] = newVal
                                pollOptions = newList
                            },
                            label = { Text("Option ${index + 1}") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            trailingIcon = {
                                if (pollOptions.size > 2) {
                                    IconButton(onClick = {
                                        pollOptions = pollOptions.toMutableList().apply { removeAt(index) }
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.drawclip(CircleShape)) // Actually need Close icon
                                    }
                                }
                            }
                        )
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
        }
    }

    if (showProgressOverlay) {
        PostingProgressOverlay(stage = currentStage)
    }
}

@Composable
fun PostingProgressOverlay(stage: Int) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { },
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.width(280.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val text = when (stage) {
                    1 -> "Uploading image..."
                    2 -> "Publishing notice..."
                    3 -> "Updating board..."
                    else -> "Processing..."
                }
                
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Please wait while we share your notice with the community.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

fun Modifier.drawclip(shape: androidx.compose.ui.graphics.Shape): Modifier = this.clip(shape)
