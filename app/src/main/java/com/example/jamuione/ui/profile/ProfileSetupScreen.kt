package com.example.jamuione.ui.profile

import android.util.Log
import com.example.jamuione.BuildConfig
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jamuione.ui.auth.AuthViewModel
import com.example.jamuione.util.BrandingUtil
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
    val state = "Bihar"
    var district by remember { mutableStateOf("Jamui") }
    var locality by remember { mutableStateOf("") }

    var nativeState by remember { mutableStateOf("Bihar") }
    var nativeDistrict by remember { mutableStateOf("Jamui") }
    var profession by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var showInCommunity by remember { mutableStateOf(true) }

    var expandedDistrict by remember { mutableStateOf(false) }
    var expandedNativeDistrict by remember { mutableStateOf(false) }

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

    // Pre-fill from existing profile
    LaunchedEffect(userProfileState) {
        if (userProfileState is Resource.Success) {
            val user = (userProfileState as Resource.Success).data
            if (user != null) {
                if (name.isEmpty()) name = user.name
                if (locality.isEmpty() && user.locality.isNotEmpty()) {
                    locality = user.locality.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
                if (user.district.isNotEmpty()) {
                    val formattedDistrict = user.district.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    if (districts.contains(formattedDistrict)) district = formattedDistrict
                }
                if (user.nativeDistrict.isNotEmpty()) {
                    val formattedNative = user.nativeDistrict.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    if (districts.contains(formattedNative)) nativeDistrict = formattedNative
                }
                if (profession.isEmpty()) profession = user.profession
                if (company.isEmpty()) company = user.company ?: ""
                showInCommunity = user.showInCommunity
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Complete Profile") },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Personalize your experience",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

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
            Text("Native Place (Hometown)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
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
                    districts.forEach { selectionOption ->
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

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: PROFESSION
            Text("Professional Info", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = profession,
                onValueChange = { profession = it },
                label = { Text("Profession (e.g. Teacher, Engineer, Student)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text("Company (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: VISIBILITY
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
                            showInCommunity = showInCommunity
                        )
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("You're offline.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && locality.isNotBlank() && profession.isNotBlank() && profileSavedState !is Resource.Loading
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
