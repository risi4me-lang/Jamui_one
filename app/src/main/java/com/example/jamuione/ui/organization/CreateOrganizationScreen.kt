package com.example.jamuione.ui.organization

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.jamuione.domain.model.OrganizationType
import com.example.jamuione.util.LocationDataProvider
import com.example.jamuione.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrganizationScreen(
    viewModel: OrganizationViewModel,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    
    // Step 1: Type
    var selectedType by remember { mutableStateOf<OrganizationType?>(null) }
    
    // Step 2: Basic Details
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    // Step 3: Location
    var state by remember { mutableStateOf("Bihar") }
    var district by remember { mutableStateOf("Jamui") }
    var locality by remember { mutableStateOf("") }
    
    // Step 4: Logo
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    
    val createResult by viewModel.createOrgResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(createResult) {
        if (createResult is Resource.Success) {
            onSuccess((createResult as Resource.Success<String>).data!!)
            viewModel.resetCreateResult()
        } else if (createResult is Resource.Error) {
            snackbarHostState.showSnackbar((createResult as Resource.Error).message ?: "Error")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create Organization (Step $step/5)") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step > 1) step -= 1 else onBack()
                    }) {
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                1 -> Step1Type(selectedType) { selectedType = it; step = 2 }
                2 -> Step2Details(name, category, description, { n, c, d -> name = n; category = c; description = d; step = 3 })
                3 -> Step3Location(state, district, locality, { s, d, l -> state = s; district = d; locality = l; step = 4 })
                4 -> Step4Logo(logoUri, { logoUri = it; step = 5 })
                5 -> Step5Review(selectedType!!, name, category, description, state, district, locality, logoUri, 
                    isLoading = createResult is Resource.Loading,
                    onSubmit = {
                        viewModel.createOrganization(name, selectedType!!, description, category, state, district, locality, logoUri)
                    }
                )
            }
        }
    }
}

@Composable
fun Step1Type(selected: OrganizationType?, onSelect: (OrganizationType) -> Unit) {
    Text("What kind of organization is this?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))
    
    OrganizationTypeCard(
        title = "Community",
        desc = "NGOs, Resident Associations, Samajes",
        icon = Icons.Default.People,
        isSelected = selected == OrganizationType.COMMUNITY,
        onClick = { onSelect(OrganizationType.COMMUNITY) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    OrganizationTypeCard(
        title = "Institution",
        desc = "Schools, Hospitals, Government Offices",
        icon = Icons.Default.AccountBalance,
        isSelected = selected == OrganizationType.INSTITUTION,
        onClick = { onSelect(OrganizationType.INSTITUTION) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    OrganizationTypeCard(
        title = "Business",
        desc = "Local Shops, Services, Freelancers",
        icon = Icons.Default.Storefront,
        isSelected = selected == OrganizationType.BUSINESS,
        onClick = { onSelect(OrganizationType.BUSINESS) }
    )
}

@Composable
fun OrganizationTypeCard(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = desc, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun Step2Details(initialName: String, initialCategory: String, initialDesc: String, onNext: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var category by remember { mutableStateOf(initialCategory) }
    var description by remember { mutableStateOf(initialDesc) }

    Text("Basic Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))
    
    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Organization Name") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (e.g. Healthcare, Grocery)") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
    
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = { onNext(name, category, description) }, modifier = Modifier.fillMaxWidth(), enabled = name.isNotBlank() && category.isNotBlank()) {
        Text("Continue")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step3Location(initialState: String, initialDistrict: String, initialLocality: String, onNext: (String, String, String) -> Unit) {
    var state by remember { mutableStateOf(initialState) }
    var district by remember { mutableStateOf(initialDistrict) }
    var locality by remember { mutableStateOf(initialLocality) }
    
    var stateExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    
    val states = LocationDataProvider.getStates()
    val districts = LocationDataProvider.getDistricts(state)

    Text("Where is it located?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))

    ExposedDropdownMenuBox(expanded = stateExpanded, onExpandedChange = { stateExpanded = !stateExpanded }, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(value = state, onValueChange = {}, readOnly = true, label = { Text("State") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(stateExpanded) }, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth())
        ExposedDropdownMenu(expanded = stateExpanded, onDismissRequest = { stateExpanded = false }) {
            states.forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { state = s; stateExpanded = false }) }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    ExposedDropdownMenuBox(expanded = districtExpanded, onExpandedChange = { districtExpanded = !districtExpanded }, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(value = district, onValueChange = {}, readOnly = true, label = { Text("District") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(districtExpanded) }, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth())
        ExposedDropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
            districts.forEach { d -> DropdownMenuItem(text = { Text(d) }, onClick = { district = d; districtExpanded = false }) }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(value = locality, onValueChange = { locality = it }, label = { Text("Locality") }, modifier = Modifier.fillMaxWidth())
    
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = { onNext(state, district, locality) }, modifier = Modifier.fillMaxWidth(), enabled = locality.isNotBlank()) {
        Text("Continue")
    }
}

@Composable
fun Step4Logo(initialUri: Uri?, onNext: (Uri?) -> Unit) {
    var uri by remember { mutableStateOf(initialUri) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }

    Text("Logo Upload", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))
    
    Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
        if (uri != null) {
            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(40.dp))
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text("Tap to upload logo (optional)", style = MaterialTheme.typography.bodySmall)
    
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = { onNext(uri) }, modifier = Modifier.fillMaxWidth()) {
        Text("Review")
    }
}

@Composable
fun Step5Review(type: OrganizationType, name: String, category: String, desc: String, state: String, district: String, locality: String, logo: Uri?, isLoading: Boolean, onSubmit: () -> Unit) {
    Text("Review Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Type: ${type.name}", style = MaterialTheme.typography.bodyMedium)
            Text("Name: $name", style = MaterialTheme.typography.bodyMedium)
            Text("Category: $category", style = MaterialTheme.typography.bodyMedium)
            Text("Location: $locality, $district, $state", style = MaterialTheme.typography.bodyMedium)
        }
    }
    
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) else Text("Create Organization")
    }
}
