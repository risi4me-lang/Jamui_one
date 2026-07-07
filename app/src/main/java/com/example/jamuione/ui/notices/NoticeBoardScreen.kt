package com.example.jamuione.ui.notices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jamuione.domain.model.Notice
import com.example.jamuione.ui.feed.FeedScope
import com.example.jamuione.ui.feed.ScopeSelector
import com.example.jamuione.util.BrandingUtil
import com.example.jamuione.util.Resource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeBoardScreen(
    viewModel: NoticeViewModel,
    onCreateNoticeClick: () -> Unit
) {
    val noticesResource by viewModel.notices.collectAsState()
    val currentScope by viewModel.currentScope.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val deleteResult by viewModel.deleteNoticeResult.collectAsState()
    val reportResult by viewModel.reportNoticeResult.collectAsState()
    val communityName = BrandingUtil.getCommunityName(userProfile.data?.district)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(deleteResult) {
        if (deleteResult is Resource.Success && deleteResult.data == true) {
            snackbarHostState.showSnackbar("Notice deleted")
            viewModel.resetDeleteNoticeResult()
        } else if (deleteResult is Resource.Error) {
            snackbarHostState.showSnackbar(deleteResult.message ?: "Failed to delete notice")
            viewModel.resetDeleteNoticeResult()
        }
    }

    LaunchedEffect(reportResult) {
        if (reportResult is Resource.Success && reportResult.data == true) {
            snackbarHostState.showSnackbar("Report submitted. Thank you.")
            viewModel.resetReportNoticeResult()
        } else if (reportResult is Resource.Error) {
            snackbarHostState.showSnackbar(reportResult.message ?: "Failed to submit report")
            viewModel.resetReportNoticeResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(communityName) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (!viewModel.isGuest) {
                FloatingActionButton(onClick = onCreateNoticeClick) {
                    Icon(Icons.Default.Add, contentDescription = "Create Notice")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            ScopeSelector(
                selectedScope = currentScope,
                onScopeSelected = { viewModel.setScope(it) }
            )

            CategorySelector(
                categories = viewModel.categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.setCategory(it) }
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (noticesResource is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (noticesResource is Resource.Success) {
                    val notices = noticesResource.data ?: emptyList()
                    if (notices.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No notices in this category/area.\nCheck back later for updates!",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(notices) { notice ->
                                NoticeItem(
                                    notice = notice,
                                    currentUserId = userProfile.data?.uid,
                                    onDeleteClick = {
                                        viewModel.deleteNotice(notice.id)
                                    },
                                    onReportClick = { reason ->
                                        viewModel.reportNotice(notice.id, reason)
                                    }
                                )
                            }
                        }
                    }
                } else if (noticesResource is Resource.Error) {
                    Text(
                        text = noticesResource.message ?: "Error loading notices",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") }
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category) }
            )
        }
    }
}

@Composable
fun NoticeItem(
    notice: Notice,
    currentUserId: String? = null,
    onDeleteClick: () -> Unit = {},
    onReportClick: (String) -> Unit = {}
) {
    val displayCategory = notice.category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Notice") },
            text = {
                Column {
                    Text("Please provide a reason for reporting this notice:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Reason (e.g. Inaccurate, Spam)") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (reportReason.isNotBlank()) {
                            onReportClick(reportReason)
                            showReportDialog = false
                            reportReason = ""
                        }
                    },
                    enabled = reportReason.isNotBlank()
                ) {
                    Text("Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Notice") },
            text = { Text("Are you sure you want to delete this notice?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteClick()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge { Text(displayCategory) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Expires: ${formatDate(notice.expiryDate)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (notice.userId == currentUserId) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("Report") },
                            onClick = {
                                showMenu = false
                                showReportDialog = true
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = notice.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = notice.description, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (notice.userProfileImage != null) {
                        AsyncImage(
                            model = notice.userProfileImage,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = notice.userName, fontSize = 12.sp)
                    if (notice.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (notice.contactNumber.isNotBlank()) {
                    Button(
                        onClick = { /* Call intent */ },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Contact", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
