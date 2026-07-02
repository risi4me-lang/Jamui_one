package com.example.jamuione.ui.notices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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
    var showCategoryMenu by remember { mutableStateOf(false) }

    LaunchedEffect(createResult) {
        if (createResult is Resource.Success && createResult.data == true) {
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

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = { },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showCategoryMenu = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                )
                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    viewModel.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                showCategoryMenu = false
                            }
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
