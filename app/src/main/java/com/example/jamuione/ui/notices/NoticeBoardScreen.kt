package com.example.jamuione.ui.notices

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
import coil.compose.AsyncImage
import com.example.jamuione.domain.model.Notice
import com.example.jamuione.ui.notices.NoticeViewModel
import com.example.jamuione.ui.components.HomeHeader
import com.example.jamuione.ui.components.PostSkeletonLoader
import com.example.jamuione.ui.feed.FeedScope
import com.example.jamuione.ui.feed.ScopeSelector
import com.example.jamuione.util.BrandingUtil
import com.example.jamuione.util.Resource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeBoardScreen(
    viewModel: NoticeViewModel,
    highlightNoticeId: String? = null,
    onCreateNoticeClick: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onProfileClick: () -> Unit
) {
    val noticesResource by viewModel.notices.collectAsState()
    val currentScope by viewModel.currentScope.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val rsvpedNotices by viewModel.rsvpedNotices.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val memberCount by viewModel.memberCount.collectAsState()
    val deleteResult by viewModel.deleteNoticeResult.collectAsState()
    val reportResult by viewModel.reportNoticeResult.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(deleteResult) {
        if (deleteResult is Resource.Success && deleteResult.data == true) {
            snackbarHostState.showSnackbar("Event deleted")
            viewModel.resetDeleteNoticeResult()
        } else if (deleteResult is Resource.Error) {
            snackbarHostState.showSnackbar(deleteResult.message ?: "Failed to delete event")
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

    // Deep link scrolling
    LaunchedEffect(noticesResource, highlightNoticeId) {
        if (noticesResource is Resource.Success && highlightNoticeId != null) {
            val index = noticesResource.data?.indexOfFirst { it.id == highlightNoticeId } ?: -1
            if (index != -1) {
                // +4 to account for header items (HomeHeader, Search, Scope, Category)
                listState.animateScrollToItem(index + 4)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!viewModel.isGuest) {
                FloatingActionButton(
                    onClick = onCreateNoticeClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Notice")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            item {
                HomeHeader(
                    user = userProfile.data,
                    memberCount = memberCount,
                    unreadCount = unreadCount,
                    onNotificationsClick = onNavigateToNotifications,
                    onProfileClick = onProfileClick
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search events...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            item {
                ScopeSelector(
                    selectedScope = currentScope,
                    nativeDistrict = userProfile.data?.nativeDistrict,
                    onScopeSelected = { viewModel.setScope(it) }
                )
            }

            item {
                CategorySelector(
                    categories = viewModel.categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.setCategory(it) }
                )
            }

            if (noticesResource is Resource.Loading) {
                item {
                    Column {
                        repeat(3) {
                            PostSkeletonLoader()
                        }
                    }
                }
            } else if (noticesResource is Resource.Success) {
                val notices = noticesResource.data ?: emptyList()
                if (notices.isEmpty()) {
                    item {
                        EmptyNoticeState(
                            searchQuery = searchQuery,
                            onInviteClick = { /* existing invite logic */ }
                        )
                    }
                } else {
                    items(notices) { notice ->
                        NoticeItem(
                            notice = notice,
                            currentUserId = userProfile.data?.uid,
                            isRsvped = rsvpedNotices[notice.id] ?: false,
                            onDeleteClick = {
                                viewModel.deleteNotice(notice.id)
                            },
                            onReportClick = { reason ->
                                viewModel.reportNotice(notice.id, reason)
                            },
                            onVoteClick = { index ->
                                viewModel.voteInPoll(notice.id, index)
                            },
                            onRsvpClick = {
                                viewModel.toggleRsvp(notice.id)
                            }
                        )
                        if (notice.id == highlightNoticeId) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 2.dp)
                        }
                    }
                }
            } else if (noticesResource is Resource.Error) {
                item {
                    Text(
                        text = noticesResource.message ?: "Error loading notices",
                        modifier = Modifier.padding(16.dp),
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

data class CategoryStyle(
    val icon: ImageVector,
    val color: Color
)

@Composable
fun getCategoryStyle(category: String): CategoryStyle {
    return when (category.lowercase()) {
        "blood donation" -> CategoryStyle(Icons.Default.Bloodtype, Color(0xFFE91E63))
        "event" -> CategoryStyle(Icons.Default.Event, Color(0xFF4CAF50))
        "help needed" -> CategoryStyle(Icons.Default.Favorite, Color(0xFFF44336))
        "jobs" -> CategoryStyle(Icons.Default.Work, Color(0xFF2196F3))
        "rent/flatmate" -> CategoryStyle(Icons.Default.Home, Color(0xFF4CAF50))
        "buy & sell" -> CategoryStyle(Icons.Default.ShoppingCart, Color(0xFFFF9800))
        "announcement" -> CategoryStyle(Icons.Default.Announcement, Color(0xFF9C27B0))
        else -> CategoryStyle(Icons.Default.Info, MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun NoticeItem(
    notice: Notice,
    currentUserId: String? = null,
    isRsvped: Boolean = false,
    onDeleteClick: () -> Unit = {},
    onReportClick: (String) -> Unit = {},
    onVoteClick: (Int) -> Unit = {},
    onRsvpClick: () -> Unit = {}
) {
    val context = LocalContext.current

    if (notice.isDeleted) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "This notice has been deleted.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }

    val style = getCategoryStyle(notice.category)
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = style.color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(style.icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = style.color)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = displayCategory,
                                style = MaterialTheme.typography.labelSmall,
                                color = style.color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    val daysLeft = ((notice.expiryDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).toInt()
                    val expiryText = if (daysLeft <= 0) "Expires today" else "Expires in $daysLeft days"
                    
                    Text(
                        text = expiryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (daysLeft < 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.outline)
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
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (notice.category == "Event" && notice.eventDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SimpleDateFormat("EEE, MMM dd • hh:mm a", Locale.getDefault()).format(Date(notice.eventDate)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!notice.eventLocation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = notice.eventLocation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notice.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // POLL SECTION
            if (notice.pollQuestion != null && notice.pollOptions != null) {
                Spacer(modifier = Modifier.height(16.dp))
                PollView(
                    question = notice.pollQuestion,
                    options = notice.pollOptions,
                    votes = notice.pollVotes ?: emptyMap(),
                    userVote = notice.userVotes?.get(currentUserId ?: ""),
                    onVoteClick = onVoteClick
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

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
                                .size(28.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = notice.userName, style = MaterialTheme.typography.labelMedium)
                    if (notice.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (notice.category == "Event") {
                        Button(
                            onClick = onRsvpClick,
                            colors = if (isRsvped) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            else ButtonDefaults.buttonColors(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isRsvped) Icons.Default.CheckCircle else Icons.Default.HowToReg,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRsvped) "Going" else "RSVP",
                                fontSize = 11.sp
                            )
                            if (notice.rsvpCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "(${notice.rsvpCount})", fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (notice.contactNumber.isNotBlank()) {
                        Button(
                            onClick = { 
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${notice.contactNumber}")
                                    }
                                    context.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    // Handle failure if dialer is not available
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Contact", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PollView(
    question: String,
    options: List<String>,
    votes: Map<String, Long>,
    userVote: Long?,
    onVoteClick: (Int) -> Unit
) {
    val totalVotes = votes.values.sum()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(text = question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        options.forEachIndexed { index, option ->
            val voteCount = votes[index.toString()] ?: 0L
            val percentage = if (totalVotes > 0) (voteCount.toFloat() / totalVotes) else 0f
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onVoteClick(index) }
                    .border(
                        width = if (userVote == index.toLong()) 2.dp else 1.dp,
                        color = if (userVote == index.toLong()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentage)
                        .matchParentSize()
                        .background(
                            if (userVote == index.toLong()) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(text = option, style = MaterialTheme.typography.bodyMedium)
                        if (userVote == index.toLong()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(
                        text = "${(percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$totalVotes votes",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun EmptyNoticeState(searchQuery: String, onInviteClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp, start = 32.dp, end = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.SpeakerNotesOff,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (searchQuery.isNotEmpty()) "No matches found" else "Nobody has posted yet.",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (searchQuery.isNotEmpty()) "Try searching for something else or check your spelling." else "Be the first neighbor to share something important with the community.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        
        if (searchQuery.isEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onInviteClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Invite Neighbors")
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
