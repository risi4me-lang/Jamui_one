package com.example.jamuione.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jamuione.ui.auth.AuthViewModel
import com.example.jamuione.util.LocationDataProvider
import com.example.jamuione.util.NetworkUtils
import com.example.jamuione.util.Resource
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    viewModel: AuthViewModel,
    onProfileSaved: () -> Unit
) {
    val userProfileState by viewModel.userProfile.collectAsState()
    
    var currentStep by remember { mutableIntStateOf(1) }

    var name by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("Bihar") }
    var district by remember { mutableStateOf("Jamui") }
    var locality by remember { mutableStateOf("") }

    var nativeState by remember { mutableStateOf("Bihar") }
    var nativeDistrict by remember { mutableStateOf("Jamui") }
    var sameAsCurrent by remember { mutableStateOf(true) }

    var profession by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isBloodDonor by remember { mutableStateOf(false) }
    var showInCommunity by remember { mutableStateOf(false) } // OFF by default per instructions

    var expandedState by remember { mutableStateOf(false) }
    var expandedDistrict by remember { mutableStateOf(false) }
    var expandedNativeState by remember { mutableStateOf(false) }
    var expandedNativeDistrict by remember { mutableStateOf(false) }

    val states = LocationDataProvider.getStates()
    val districts = LocationDataProvider.getDistricts(state)
    val nativeDistricts = LocationDataProvider.getDistricts(nativeState)

    val profileSavedState by viewModel.profileSaved.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var hasInitializedFromProfile by remember { mutableStateOf(false) }

    LaunchedEffect(userProfileState) {
        if (!hasInitializedFromProfile && userProfileState is Resource.Success) {
            val user = (userProfileState as Resource.Success).data
            if (user != null) {
                name = user.name
                
                if (user.state.isNotEmpty()) {
                    val formattedState = states.find { it.equals(user.state, ignoreCase = true) }
                    if (formattedState != null) state = formattedState
                }
                
                if (user.locality.isNotEmpty()) {
                    locality = user.locality.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
                
                if (user.district.isNotEmpty()) {
                    val currentDistricts = LocationDataProvider.getDistricts(state)
                    val formattedDistrict = currentDistricts.find { it.equals(user.district, ignoreCase = true) }
                    if (formattedDistrict != null) district = formattedDistrict
                }
                
                if (user.nativeState.isNotEmpty()) {
                    val formattedNativeState = states.find { it.equals(user.nativeState, ignoreCase = true) }
                    if (formattedNativeState != null) nativeState = formattedNativeState
                }

                if (user.nativeDistrict.isNotEmpty()) {
                    val currentNativeDistricts = LocationDataProvider.getDistricts(nativeState)
                    val formattedNative = currentNativeDistricts.find { it.equals(user.nativeDistrict, ignoreCase = true) }
                    if (formattedNative != null) nativeDistrict = formattedNative
                }
                
                profession = user.profession
                company = user.company ?: ""
                bio = user.bio ?: ""
                isBloodDonor = user.isBloodDonor
                showInCommunity = if (user.profileCompleted) user.showInCommunity else false
                sameAsCurrent = nativeState.equals(state, ignoreCase = true) && nativeDistrict.equals(district, ignoreCase = true)

                hasInitializedFromProfile = true
            }
        }
    }

    LaunchedEffect(profileSavedState) {
        if (profileSavedState is Resource.Success && profileSavedState.data == true) {
            snackbarHostState.showSnackbar("Profile saved!")
            onProfileSaved()
        } else if (profileSavedState is Resource.Error) {
            snackbarHostState.showSnackbar(profileSavedState.message ?: "Failed to save profile")
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val submitProfile = {
        if (NetworkUtils.isNetworkAvailable(context)) {
            val finalNativeState = if (sameAsCurrent) state else nativeState
            val finalNativeDistrict = if (sameAsCurrent) district else nativeDistrict
            viewModel.saveProfile(
                name = name,
                state = state,
                district = district,
                locality = locality,
                nativeState = finalNativeState,
                nativeDistrict = finalNativeDistrict,
                profession = profession,
                company = company.takeIf { it.isNotBlank() },
                bio = bio.takeIf { it.isNotBlank() },
                isBloodDonor = isBloodDonor,
                showInCommunity = showInCommunity
            )
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("You're offline.")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val stepTitle = when (currentStep) {
                        1 -> "What should we call you?"
                        2 -> "Where do you live now?"
                        else -> "Where are you from?"
                    }
                    Text(stepTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep -= 1 }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        onProfileSaved()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Bar
            LinearProgressIndicator(
                progress = { currentStep / 3f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (currentStep) {
                1 -> {
                    // Step 1: 'What should we call you?'
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.padding(bottom = 24.dp)) {
                            Surface(
                                modifier = Modifier.size(96.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Change photo",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= 50) name = it },
                            label = { Text("Full Name (Required)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                2 -> {
                    // Step 2: 'Where do you live now?'
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Please specify your current place of residence.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        // State Selection
                        ExposedDropdownMenuBox(
                            expanded = expandedState,
                            onExpandedChange = { expandedState = !expandedState },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = state,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Current State") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedState) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedState,
                                onDismissRequest = { expandedState = false }
                            ) {
                                states.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            state = selectionOption
                                            val newDistricts = LocationDataProvider.getDistricts(state)
                                            district = if (newDistricts.isNotEmpty()) newDistricts.first() else ""
                                            expandedState = false
                                        }
                                    )
                                }
                            }
                        }

                        // District Selection
                        ExposedDropdownMenuBox(
                            expanded = expandedDistrict,
                            onExpandedChange = { expandedDistrict = !expandedDistrict },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = district,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Current District") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDistrict) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDistrict,
                                onDismissRequest = { expandedDistrict = false }
                            ) {
                                districts.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            district = selectionOption
                                            expandedDistrict = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = locality,
                            onValueChange = { if (it.length <= 100) locality = it },
                            label = { Text("Locality (Ward/Mohalla/Village) (Required)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                3 -> {
                    // Step 3: 'Where are you from?'
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Same as where I live now", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = sameAsCurrent,
                                onCheckedChange = { sameAsCurrent = it }
                            )
                        }

                        if (!sameAsCurrent) {
                            // Native State Selection
                            ExposedDropdownMenuBox(
                                expanded = expandedNativeState,
                                onExpandedChange = { expandedNativeState = !expandedNativeState },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = nativeState,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Native State") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNativeState) },
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedNativeState,
                                    onDismissRequest = { expandedNativeState = false }
                                ) {
                                    states.forEach { selectionOption ->
                                        DropdownMenuItem(
                                            text = { Text(selectionOption) },
                                            onClick = {
                                                nativeState = selectionOption
                                                val newNativeDistricts = LocationDataProvider.getDistricts(nativeState)
                                                nativeDistrict = if (newNativeDistricts.isNotEmpty()) newNativeDistricts.first() else ""
                                                expandedNativeState = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Native District Selection
                            ExposedDropdownMenuBox(
                                expanded = expandedNativeDistrict,
                                onExpandedChange = { expandedNativeDistrict = !expandedNativeDistrict },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = nativeDistrict,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Native District") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNativeDistrict) },
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedNativeDistrict,
                                    onDismissRequest = { expandedNativeDistrict = false }
                                ) {
                                    nativeDistricts.forEach { selectionOption ->
                                        DropdownMenuItem(
                                            text = { Text(selectionOption) },
                                            onClick = {
                                                nativeDistrict = selectionOption
                                                expandedNativeDistrict = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))

                        Text("About You (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = profession,
                            onValueChange = { profession = it },
                            label = { Text("Profession") },
                            placeholder = { Text("e.g. Teacher, Engineer, Student") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = company,
                            onValueChange = { company = it },
                            label = { Text("Company / College") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Short Bio") },
                            placeholder = { Text("Tell us about yourself") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isBloodDonor,
                                onCheckedChange = { isBloodDonor = it }
                            )
                            Text(
                                text = "I am a Blood Donor",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = showInCommunity,
                                    onCheckedChange = { showInCommunity = it }
                                )
                                Text(
                                    text = "Show me in Native Community",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = "Other people from your hometown can see your name and profession. You can change this any time in Settings.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 48.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation Buttons (Back / Next / Finish / Skip for now)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep -= 1 },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Back")
                    }
                }

                if (currentStep < 3) {
                    val isStepValid = when (currentStep) {
                        1 -> name.isNotBlank()
                        2 -> state.isNotBlank() && district.isNotBlank() && locality.isNotBlank()
                        else -> true
                    }
                    Button(
                        onClick = { if (isStepValid) currentStep += 1 },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = isStepValid
                    ) {
                        Text("Next")
                    }
                } else {
                    OutlinedButton(
                        onClick = { submitProfile() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Skip for now")
                    }
                    Button(
                        onClick = { submitProfile() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = profileSavedState !is Resource.Loading && userProfileState !is Resource.Loading
                    ) {
                        if (profileSavedState is Resource.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Finish")
                        }
                    }
                }
            }
        }
    }
}
