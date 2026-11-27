package com.example.jamuione.ui.notices

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.jamuione.ui.components.AudienceOption
import com.example.jamuione.ui.components.AudiencePickerRow
import com.example.jamuione.util.NetworkUtils
import com.example.jamuione.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoticeScreen(
    viewModel: NoticeViewModel,
    preselectedCategory: String? = null,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(preselectedCategory ?: "Announcement") }
    var contactNumber by remember { mutableStateOf("") }
    var daysToExpiry by remember { mutableIntStateOf(7) }
    
    var isPoll by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOptions by remember { mutableStateOf(listOf("", "")) }
    var pollDurationDays by remember { mutableStateOf(7) }

    val createResult by viewModel.createNoticeResult.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val user = (userProfile as? Resource.Success)?.data

    var selectedAudience by remember(user) {
        mutableStateOf<AudienceOption>(
            if (user?.locality?.isNotBlank() == true && user.locality != "unknown") {
                AudienceOption.Locality(user.locality)
            } else if (user?.district?.isNotBlank() == true) {
                AudienceOption.District(user.district)
            } else {
                AudienceOption.Locality("")
            }
        )
    }

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
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            LocalContext.current,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                eventDate = cal.timeInMillis
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
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
                                val (st, dist, loc) = when (val aud = selectedAudience) {
                                    is AudienceOption.Locality -> Triple(user?.state ?: "", user?.district ?: "", aud.value)
                                    is AudienceOption.District -> Triple(user?.state ?: "", aud.value, "")
                                    is AudienceOption.Hometown -> Triple(user?.nativeState ?: "", aud.value, "")
                                }
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
                                    eventLocation = eventLocation.takeIf { selectedCategory == "Event" && it.isNotBlank() },
                                    state = st,
                                    district = dist,
                                    locality = loc
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
            AudiencePickerRow(
                user = user,
                selectedAudience = selectedAudience,
                onAudienceSelected = { selectedAudience = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(viewModel.categories.size) { index ->
                    val cat = viewModel.categories[index]
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
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
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 2000) description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Additional Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text("Contact Phone (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isPoll,
                    onCheckedChange = { isPoll = it }
                )
                Text("Attach a poll to this notice")
            }

            if (isPoll) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pollQuestion,
                    onValueChange = { pollQuestion = it },
                    label = { Text("Poll Question") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                pollOptions.forEachIndexed { idx, opt ->
                    OutlinedTextField(
                        value = opt,
                        onValueChange = { newText ->
                            pollOptions = pollOptions.toMutableList().also { it[idx] = newText }
                        },
                        label = { Text("Option ${idx + 1}") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
                if (pollOptions.size < 5) {
                    TextButton(onClick = { pollOptions = pollOptions + "" }) {
                        Text("+ Add Option")
                    }
                }
            }
        }
    }

    if (showProgressOverlay) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
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
                    CircularProgressIndicator(modifier = Modifier.size(48.dp), strokeWidth = 4.dp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (currentStage == 2) "Publishing notice..." else "Updating board...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
