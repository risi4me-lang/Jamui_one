package com.example.jamuione.ui.profile

import android.util.Log
import com.example.jamuione.BuildConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jamuione.ui.auth.AuthViewModel
import com.example.jamuione.util.BrandingUtil
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
    
    var name by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("Bihar") }
    var district by remember { mutableStateOf("Jamui") }
    var locality by remember { mutableStateOf("") }

    var nativeState by remember { mutableStateOf("Bihar") }
    var nativeDistrict by remember { mutableStateOf("Jamui") }
    var profession by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isBloodDonor by remember { mutableStateOf(false) }
    var showInCommunity by remember { mutableStateOf(true) }

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

    // Pre-fill from existing profile - EXACTLY ONCE
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
                showInCommunity = user.showInCommunity
                
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(top = 32.dp, bottom = 48.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profile Setup",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        IconButton(onClick = { 
                            viewModel.logout()
                            onProfileSaved()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Complete your profile to get the best experience in your community.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

            // SECTION: IDENTITY
            Text("Basic Info", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 50) name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: CURRENT LOCATION
            Text("Current Location", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            
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
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
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
                                // Reset district when state changes
                                val newDistricts = LocationDataProvider.getDistricts(state)
                                district = if (newDistricts.isNotEmpty()) newDistricts.first() else ""
                                expandedState = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
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
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
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
            
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = locality,
                onValueChange = { if (it.length <= 100) locality = it },
                label = { Text("Locality (Ward/Mohalla/Village)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: NATIVE PLACE
            Text("Your Hometown", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            
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
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
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
                                // Reset native district when state changes
                                val newNativeDistricts = LocationDataProvider.getDistricts(nativeState)
                                nativeDistrict = if (newNativeDistricts.isNotEmpty()) newNativeDistricts.first() else ""
                                expandedNativeState = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
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
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
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

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION: OPTIONAL INFO
            Text("Tell your community more about yourself (optional)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = profession,
                onValueChange = { profession = it },
                label = { Text("Profession (optional)") },
                placeholder = { Text("e.g. Teacher, Engineer, Student") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text("Company (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Short Bio (optional)") },
                placeholder = { Text("Tell us about yourself") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: OPTIONS (optional)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isBloodDonor,
                    onCheckedChange = { isBloodDonor = it }
                )
                Text(
                    text = "I am a Blood Donor (optional)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

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
                text = "Help people from $nativeDistrict find you in $district",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 48.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            Button(
                onClick = { 
                    if (NetworkUtils.isNetworkAvailable(context)) {
                        viewModel.saveProfile(
                            name = name,
                            state = state,
                            district = district,
                            locality = locality,
                            nativeState = nativeState,
                            nativeDistrict = nativeDistrict,
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
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && locality.isNotBlank() && nativeDistrict.isNotBlank() && 
                    profileSavedState !is Resource.Loading && userProfileState !is Resource.Loading
            ) {
                if (profileSavedState is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save & Continue")
                }
            }
        }
    }
}
}
