package com.example.jamuione.ui.feed

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jamuione.ui.components.CompactTopBar
import com.example.jamuione.ui.components.OfficialUpdatesRow
import com.example.jamuione.ui.components.ScopeSelector
import com.example.jamuione.ui.components.PostSkeletonLoader
import com.example.jamuione.ui.theme.categoryStyle
import com.example.jamuione.util.Resource
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    isAdmin: Boolean = false,
    onCreatePostClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToTravel: () -> Unit = {},
    onCreateNoticeCategory: (String) -> Unit = {},
    onNavigateToNoticeBoardCategory: (String) -> Unit = {},
    highlightPostId: String? = null
) {
    val postsResource by viewModel.posts.collectAsState()
    val likedPosts by viewModel.likedPosts.collectAsState()
    val isSavedMap by viewModel.isSavedMap.collectAsState()
    val currentScope by viewModel.currentScope.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val postAuthors by viewModel.postAuthors.collectAsState()
    val deleteResult by viewModel.deletePostResult.collectAsState()
    val reportResult by viewModel.reportPostResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var activeHighlightId by remember { mutableStateOf(highlightPostId) }

    LaunchedEffect(highlightPostId) {
        if (highlightPostId != null) {
            activeHighlightId = highlightPostId
            delay(5.seconds)
            activeHighlightId = null
        }
    }

    LaunchedEffect(deleteResult) {
        if (deleteResult is Resource.Success && deleteResult.data == true) {
            snackbarHostState.showSnackbar("Post deleted")
            viewModel.resetDeletePostResult()
        } else if (deleteResult is Resource.Error) {
            snackbarHostState.showSnackbar(deleteResult.message ?: "Failed to delete post")
            viewModel.resetDeletePostResult()
        }
    }

    LaunchedEffect(reportResult) {
        if (reportResult is Resource.Success && reportResult.data == true) {
            snackbarHostState.showSnackbar("Report submitted. Thank you for keeping the community safe.")
            viewModel.resetReportPostResult()
        } else if (reportResult is Resource.Error) {
            snackbarHostState.showSnackbar(reportResult.message ?: "Failed to submit report")
            viewModel.resetReportPostResult()
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
                onProfileClick = onProfileClick,
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            item {
                ScopeSelector(
                    selectedScope = currentScope,
                    nativeDistrict = userProfile.data?.nativeDistrict,
                    onScopeSelected = { viewModel.setScope(it) }
                )
            }

            item {
                val posts = (postsResource as? Resource.Success)?.data ?: emptyList()
                val hasOfficialPosts = posts.any { it.isOfficial }
                HomeQuickActionsRow(
                    hasOfficialPosts = hasOfficialPosts,
                    onRidesClick = onNavigateToTravel,
                    onBloodClick = { onCreateNoticeCategory("Blood Donation") },
                    onJobsClick = { onNavigateToNoticeBoardCategory("Jobs") },
                    onOfficialClick = {}
                )
            }

            item {
                OfficialUpdatesRow()
            }

            if (postsResource is Resource.Loading) {
                item {
                    Column {
                        repeat(3) {
                            PostSkeletonLoader()
                        }
                    }
                }
            } else if (postsResource is Resource.Success) {
                val posts = postsResource.data ?: emptyList()
                if (posts.isEmpty()) {
                    item {
                        EmptyFeedState(searchQuery = searchQuery)
                    }
                } else {
                    items(posts) { post ->
                        val isHighlighted = post.id == activeHighlightId
                        val animatedBorderColor by animateColorAsState(
                            targetValue = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
                            animationSpec = tween(durationMillis = 1000),
                            label = "border_color"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            PostCard(
                                post = post,
                                authorProfile = postAuthors[post.userId],
                                currentUserId = userProfile.data?.uid,
                                isLiked = likedPosts[post.id] ?: false,
                                isSaved = isSavedMap[post.id] ?: false,
                                onLikeClick = { viewModel.toggleLike(post.id) },
                                onCommentClick = { onNavigateToDetail(post.id) },
                                onSaveClick = { viewModel.toggleSavePost(post.id) },
                                onDeleteClick = { viewModel.deletePost(post.id) },
                                onReportClick = { reason -> viewModel.reportPost(post.id, reason) },
                                onDetailClick = { onNavigateToDetail(post.id) },
                                onAuthorClick = { onNavigateToUserProfile(it) }
                            )
                            
                            if (isHighlighted) {
                                Surface(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(2.dp, animatedBorderColor),
                                    shape = RoundedCornerShape(16.dp)
                                ) {}
                            }
                        }
                    }
                }
            } else if (postsResource is Resource.Error) {
                item {
                    Text(
                        text = postsResource.message ?: "Error loading posts",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun HomeQuickActionsRow(
    hasOfficialPosts: Boolean,
    onRidesClick: () -> Unit,
    onBloodClick: () -> Unit,
    onJobsClick: () -> Unit,
    onOfficialClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val travelStyle = categoryStyle("Travel")
        QuickActionTile(
            title = "Rides",
            icon = travelStyle.icon,
            tint = travelStyle.color,
            modifier = Modifier.weight(1f),
            onClick = onRidesClick
        )

        val bloodStyle = categoryStyle("Blood Donation")
        QuickActionTile(
            title = "Blood",
            icon = bloodStyle.icon,
            tint = bloodStyle.color,
            modifier = Modifier.weight(1f),
            onClick = onBloodClick
        )

        val jobsStyle = categoryStyle("Jobs")
        QuickActionTile(
            title = "Jobs",
            icon = jobsStyle.icon,
            tint = jobsStyle.color,
            modifier = Modifier.weight(1f),
            onClick = onJobsClick
        )

        if (hasOfficialPosts) {
            val announcementStyle = categoryStyle("Announcement")
            QuickActionTile(
                title = "Official",
                icon = announcementStyle.icon,
                tint = announcementStyle.color,
                modifier = Modifier.weight(1f),
                onClick = onOfficialClick
            )
        }
    }
}

@Composable
fun QuickActionTile(
    title: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun EmptyFeedState(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (searchQuery.isNotEmpty()) "No results found for \"$searchQuery\"" else "No posts yet.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
