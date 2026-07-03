package com.example.jamuione.ui.notices

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jamuione.util.Resource

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
    var daysToExpiry by remember { mutableStateOf(7) } // Default 1 week
    
    val createResult by viewModel.createNoticeResult.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(createResult) {
        if (createResult is Resource.Success && createResult.data == true) {
            Log.d("NOTICE_DEBUG", "CreateNotice success in UI, navigating back")
            viewModel.resetCreateNoticeResult()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post a Notice") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !it }, // Correcting toggle logic
                modifier = Modifier.fillMaxWidth()
            ) {
                // To fix the dropdown not opening, I'll use a standard toggle
            }
            
            // Actually let's use a simpler implementation if ExposedDropdownMenuBox is tricky
            
            var categoryExpanded by remember { mutableStateOf(false) }
            
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
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    viewModel.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                Log.d("NOTICE_DEBUG", "Category selected: $category")
                                selectedCategory = category
                                categoryExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text("Contact Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Expiry Duration: $daysToExpiry days", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = daysToExpiry.toFloat(),
                onValueChange = { daysToExpiry = it.toInt() },
                valueRange = 1f..30f,
                steps = 29
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { 
                    Log.d("NOTICE_DEBUG", "Post Notice button clicked")
                    viewModel.createNotice(title, description, selectedCategory, contactNumber, daysToExpiry) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && description.isNotBlank() && createResult !is Resource.Loading
            ) {
                if (createResult is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Post Notice")
                }
            }

            if (createResult is Resource.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = createResult.message ?: "Error posting notice",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
