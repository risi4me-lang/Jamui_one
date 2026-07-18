package com.example.jamuione.ui.feed

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.jamuione.util.NetworkUtils
import com.example.jamuione.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: FeedViewModel,
    onBack: () -> Unit,
    onSuccess: (String, String) -> Unit = { _, _ -> }
) {
    var content by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val createResult by viewModel.createPostResult.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showProgressOverlay by remember { mutableStateOf(false) }
    var currentStage by remember { mutableStateOf(1) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(createResult) {
        if (createResult is Resource.Success && createResult.data == true) {
            currentStage = 3
            delay(800)
            showProgressOverlay = false
            val locality = userProfile.data?.locality ?: "your area"
            onSuccess(locality, "") 
            viewModel.resetCreatePostResult()
            onBack()
        } else if (createResult is Resource.Error) {
            showProgressOverlay = false
            snackbarHostState.showSnackbar((createResult as Resource.Error).message ?: "Failed to create post")
        } else if (createResult is Resource.Loading) {
            showProgressOverlay = true
            currentStage = if (selectedImageUri != null) 1 else 2
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create Post") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val context = LocalContext.current
                    TextButton(
                        onClick = { 
                            if (NetworkUtils.isNetworkAvailable(context)) {
                                viewModel.createPost(content, selectedImageUri)
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("You're offline. Please check your connection and try again.")
                                }
                            }
                        },
                        enabled = content.isNotBlank() && !showProgressOverlay
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
            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= 1000) content = it },
                placeholder = { Text("What's happening in your locality?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),
                supportingText = {
                    Text(
                        text = "${content.length}/1000",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                isError = content.length > 1000
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedImageUri != null) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove Image")
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Image")
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
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val text = when (stage) {
                    1 -> "Uploading image..."
                    2 -> "Publishing post..."
                    3 -> "Updating feed..."
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
                    text = "Please wait while we share your update with neighbors.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
