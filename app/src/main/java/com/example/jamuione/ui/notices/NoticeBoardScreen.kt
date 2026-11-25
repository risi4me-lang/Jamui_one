package com.example.jamuione.ui.notices

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jamuione.BuildConfig
import com.example.jamuione.domain.model.Notice
import com.example.jamuione.ui.components.*
import com.example.jamuione.util.BrandingUtil
import com.example.jamuione.util.RelativeTime
import com.example.jamuione.util.Resource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeBoardScreen(
    viewModel: NoticeViewModel,
    isAdmin: Boolean = false,
    highlightNoticeId: String? = null,
    onCreateNoticeClick: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToTravel: () -> Unit = {}
) {
    val noticesResource by viewModel.notices.collectAsState()
    val currentScope by viewModel.currentScope.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val rsvpedNotices by viewModel.rsvpedNotices.collectAsState()
    val helpfulNotices by viewModel.helpfulNotices.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val deleteResult by viewModel.deleteNoticeResult.collectAsState()
    val reportResult by viewModel.reportNoticeResult.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

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

    LaunchedEffect(noticesResource, highlightNoticeId) {
        if (noticesResource is Resource.Success && highlightNoticeId != null) {
            val index = noticesResource.data?.indexOfFirst { it.id == highlightNoticeId } ?: -1
            if (index != -1) {
                listState.animateScrollToItem(index + 3)
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CompactTopBar(
                user = userProfile.data,
                currentScope = currentScope,
                onScopeSelected = { viewModel.setScope(it) },
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    viewModel.loadNotices()
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                )
            ) {
                item {
                    CategorySelector(
                        categories = viewModel.categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { cat ->
                            if (cat.equals("Travel", ignoreCase = true)) {
                                onNavigateToTravel()
                            } else {
                                viewModel.setCategory(cat)
                            }
                        }
                    )
                }

                item {
                    OfficialUpdatesRow()
                }

                if (noticesResource is Resource.Loading && !isRefreshing) {
                    item {
                        Column {
                            repeat(3) {
                                NoticeBoardSkeletonLoader()
                            }
                        }
                    }
                } else if (noticesResource is Resource.Success) {
                    val notices = noticesResource.data ?: emptyList()
                    if (notices.isEmpty()) {
                        item {
                            val context = LocalContext.current
                            EmptyState(
                                icon = Icons.Default.Campaign,
                                title = "Nothing here yet",
                                message = "Post the first notice in your area",
                                customContent = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val bloodStyle = com.example.jamuione.ui.theme.categoryStyle("Blood Donation")
                                            val travelStyle = com.example.jamuione.ui.theme.categoryStyle("Travel")
                                            QuickStartTile("Need blood?", bloodStyle.icon, Modifier.weight(1f), bloodStyle.color) { onCreateNoticeClick() }
                                            QuickStartTile("Travel", travelStyle.icon, Modifier.weight(1f), travelStyle.color) { onCreateNoticeClick() }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val lostStyle = com.example.jamuione.ui.theme.categoryStyle("Lost & Found")
                                            val roomStyle = com.example.jamuione.ui.theme.categoryStyle("Rent/Flatmate")
                                            QuickStartTile("Lost something?", lostStyle.icon, Modifier.weight(1f), lostStyle.color) { onCreateNoticeClick() }
                                            QuickStartTile("List a room", roomStyle.icon, Modifier.weight(1f), roomStyle.color) { onCreateNoticeClick() }
                                        }
                                    }
                                },
                                secondaryActionText = "Invite Neighbors",
                                onSecondaryAction = {
                                    val communityName = BrandingUtil.getCommunityName(userProfile.data?.district)
                                    val inviteMessage = "Join me on $communityName — download here: https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, inviteMessage)
                                        setPackage("com.whatsapp")
                                    }
                                    try {
                                        context.startActivity(sendIntent)
                                    } catch (e: Exception) {
                                        val fallback = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, inviteMessage)
                                        }
                                        context.startActivity(Intent.createChooser(fallback, "Invite via"))
                                    }
                                }
                            )
                        }
                    } else {
                        items(notices) { notice ->
                            AnimateIn {
                                NoticeItem(
                                    notice = notice,
                                    currentUserId = userProfile.data?.uid,
                                    isRsvped = rsvpedNotices[notice.id] ?: false,
                                    isHelpful = helpfulNotices[notice.id] ?: false,
                                    onDeleteClick = { viewModel.deleteNotice(notice.id) },
                                    onReportClick = { reason: String -> viewModel.reportNotice(notice.id, reason) },
                                    onVoteClick = { index: Int -> viewModel.voteInPoll(notice.id, index) },
                                    onRsvpClick = { viewModel.toggleRsvp(notice.id) },
                                    onHelpfulClick = { viewModel.toggleHelpful(notice.id) }
                                )
                            }
                            if (notice.id == highlightNoticeId) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 2.dp)
                            }
                        }
                    }
                } else if (noticesResource is Resource.Error) {
                    item {
                        ErrorState(
                            message = noticesResource.message ?: "Failed to load notices",
                            onRetry = { viewModel.loadNotices() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStartTile(title: String, icon: ImageVector, modifier: Modifier = Modifier, iconTint: Color = MaterialTheme.colorScheme.primary, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = iconTint)
            Text(text = title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
            val style = com.example.jamuione.ui.theme.categoryStyle(category)
            val isSelected = selectedCategory == category
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = style.color.copy(alpha = 0.2f),
                    selectedLabelColor = style.color
                )
            )
        }
    }
}

@Composable
fun NoticeItem(
    notice: Notice,
    currentUserId: String? = null,
    isRsvped: Boolean = false,
    isHelpful: Boolean = false,
    onDeleteClick: () -> Unit = {},
    onReportClick: (String) -> Unit = {},
    onVoteClick: (Int) -> Unit = {},
    onRsvpClick: () -> Unit = {},
    onHelpfulClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    if (notice.isDeleted) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(20.dp)
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

    val style = com.example.jamuione.ui.theme.categoryStyle(notice.category)
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .drawBehind {
                drawRect(
                    color = style.color,
                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Category chip and relative time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = style.color.copy(alpha = 0.15f),
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

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = RelativeTime.format(notice.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title in titleMedium Bold
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description in at most 3 lines
            Text(
                text = notice.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Meta row of small chips: location and expiry chip
            val daysLeft = ((notice.expiryDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).toInt()
            val expiryText = when {
                daysLeft < 0 -> "Expired"
                daysLeft == 0 -> "Expires today"
                else -> "$daysLeft days left"
            }
            val expiryColor = when {
                daysLeft < 0 -> MaterialTheme.colorScheme.outline
                daysLeft < 2 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!notice.locality.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = notice.locality, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = expiryColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = expiryText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = expiryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom full-width action buttons 'Call' and 'WhatsApp' when phone number exists
            if (!notice.contactNumber.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${notice.contactNumber}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call")
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${notice.contactNumber}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp")
                    }
                }
            }

            // Helpful and RSVP controls
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHelpfulClick()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isHelpful) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(
                            imageVector = if (isHelpful) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Helpful",
                            modifier = Modifier.size(16.dp),
                            tint = if (isHelpful) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Helpful (${notice.helpfulCount})", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (notice.category == "Event") {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRsvpClick()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRsvped) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                            contentColor = if (isRsvped) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = if (isRsvped) "RSVP'd" else "RSVP")
                    }
                }
            }
        }
    }
}
