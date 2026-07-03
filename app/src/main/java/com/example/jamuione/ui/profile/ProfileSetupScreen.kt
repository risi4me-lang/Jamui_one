package com.example.jamuione.ui.profile

import android.util.Log
import com.example.jamuione.BuildConfig
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jamuione.ui.auth.AuthViewModel
import com.example.jamuione.util.BrandingUtil
import com.example.jamuione.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    viewModel: AuthViewModel,
    onProfileSaved: () -> Unit
) {
    val userProfileState by viewModel.userProfile.collectAsState()
    
    var name by remember { mutableStateOf("") }
    val state = "Bihar"
    var district by remember { mutableStateOf("Jamui") }
    var locality by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }
    val districts = listOf(
        "Araria", "Arwal", "Aurangabad", "Banka", "Begusarai", "Bhagalpur", "Bhojpur", "Buxar",
        "Darbhanga", "East Champaran", "Gaya", "Gopalganj", "Jamui", "Jehanabad", "Kaimur",
        "Katihar", "Khagaria", "Kishanganj", "Lakhisarai", "Madhepura", "Madhubani", "Munger",
        "Muzaffarpur", "Nalanda", "Nawada", "Patna", "Purnia", "Rohtas", "Saharsa",
        "Samastipur", "Saran", "Sheikhpura", "Sheohar", "Sitamarhi", "Siwan", "Supaul",
        "Vaishali", "West Champaran"
    )

    val profileSavedState by viewModel.profileSaved.collectAsState()
    val communityName = BrandingUtil.getCommunityName(district)
    val snackbarHostState = remember { SnackbarHostState() }

    // Pre-fill name from Google profile
    LaunchedEffect(userProfileState) {
        if (userProfileState is Resource.Success) {
            val user = (userProfileState as Resource.Success).data
            if (user != null && name.isEmpty()) {
                name = user.name
            }
        }
    }

    LaunchedEffect(profileSavedState) {
        if (profileSavedState is Resource.Success && profileSavedState.data == true) {
            if (BuildConfig.DEBUG) {
                Log.d("AUTH_DEBUG", "Navigation triggered")
            }
            snackbarHostState.showSnackbar("Profile saved!")
            onProfileSaved()
        } else if (profileSavedState is Resource.Error) {
            snackbarHostState.showSnackbar(profileSavedState.message ?: "Failed to save profile")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Complete Profile") },
                actions = {
                    IconButton(onClick = { 
                        viewModel.logout()
                        onProfileSaved() // This usually triggers navigation to login
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to $communityName!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = "Let's personalize your experience.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state,
                onValueChange = { },
                label = { Text("State") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = district,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("District") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    districts.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                district = selectionOption
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = locality,
                onValueChange = { locality = it },
                label = { Text("Locality (Ward/Mohalla/Village)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { 
                    Log.d("AUTH_DEBUG", "Save Profile button clicked")
                    viewModel.saveProfile(name, state, district, locality) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && locality.isNotBlank() && profileSavedState !is Resource.Loading
            ) {
                if (profileSavedState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save & Continue")
                }
            }

            if (profileSavedState is Resource.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = profileSavedState.message ?: "Failed to save profile",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
