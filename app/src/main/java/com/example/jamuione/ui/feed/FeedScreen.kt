package com.example.jamuione.ui.feed

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jamuione.ui.components.HomeHeader
import com.example.jamuione.ui.components.PostSkeletonLoader
import com.example.jamuione.domain.model.Post
import com.example.jamuione.util.BrandingUtil
import com.example.jamuione.util.Resource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onCreatePostClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onProfileClick: () -> Unit
) {
    val postsResource by viewModel.posts.collectAsState()
    val cachedPosts by viewModel.cachedPosts.collectAsState()
    val likedPosts by viewModel.likedPosts.collectAsState()
    val isSavedMap by viewModel.isSavedMap.collectAsState()
    val currentScope by viewModel.currentScope.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val memberCount by viewModel.memberCount.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val deleteResult by viewModel.deletePostResult.collectAsState()
    val reportResult by viewModel.reportPostResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
            snackbarHostState.showSnackbar("Report submitted. Thank you.")
            viewModel.resetReportPostResult()
        } else if (reportResult is Resource.Error) {
            snackbarHostState.showSnackbar(reportResult.message ?: "Failed to submit report")
            viewModel.resetReportPostResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!viewModel.isGuest) {
                FloatingActionButton(
                    onClick = onCreatePostClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Post")
                }
            }
        }
    ) { innerPadding ->
        val welcomeAlpha by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000),
            label = "welcome_alpha"
        )
        
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .graphicsLayer(alpha = welcomeAlpha)
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
                ScopeSelector(
                    selectedScope = currentScope,
                    nativeDistrict = userProfile.data?.nativeDistrict,
                    onScopeSelected = { viewModel.setScope(it) }
                )
            }

            val displayPosts = if (postsResource is Resource.Success) {
                postsResource.data ?: emptyList()
            } else {
                if (currentScope == FeedScope.LOCALITY) cachedPosts else emptyList()
            }

            if (displayPosts.isEmpty() && postsResource is Resource.Loading) {
                items(3) {
                    PostSkeletonLoader()
                }
            } else if (displayPosts.isEmpty()) {
                item {
                    EmptyFeedState(onCreatePostClick = onCreatePostClick, isGuest = viewModel.isGuest)
                }
            } else {
                items(displayPosts) { post ->
                    PostCard(
                        post = post,
                        currentUserId = userProfile.data?.uid,
                        isLiked = likedPosts[post.id] ?: false,
                        isSaved = isSavedMap[post.id] ?: false,
                        onDeleteClick = {
                            viewModel.deletePost(post.id)
                        },
                        onSaveClick = {
                            viewModel.toggleSavePost(post.id)
                        },
                        onReportClick = { reason ->
                            viewModel.reportPost(post.id, reason)
                        },
                        onDetailClick = {
                            onNavigateToDetail(post.id)
                        },
                        onCommentClick = {
                            onNavigateToDetail(post.id)
                        },
                        onLikeClick = {
                            viewModel.toggleLike(post.id)
                        }
                    )
                }
            }
            
            if (postsResource is Resource.Error) {
                item {
                    Text(
                        text = postsResource.message ?: "Failed to load posts",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyFeedState(onCreatePostClick: () -> Unit, isGuest: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp, start = 32.dp, end = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PostAdd,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Nobody has posted yet.",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Be the first neighbor to share what's happening around you!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        
        if (!isGuest) {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onCreatePostClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(54.dp).fillMaxWidth(0.7f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Something", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScopeSelector(
    selectedScope: FeedScope,
    nativeDistrict: String? = null,
    onScopeSelected: (FeedScope) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedScope == FeedScope.LOCALITY,
                onClick = { onScopeSelected(FeedScope.LOCALITY) },
                label = { Text("Locality") }
            )
        }
        item {
            FilterChip(
                selected = selectedScope == FeedScope.DISTRICT,
                onClick = { onScopeSelected(FeedScope.DISTRICT) },
                label = { Text("District") }
            )
        }
        if (!nativeDistrict.isNullOrBlank()) {
            item {
                FilterChip(
                    selected = selectedScope == FeedScope.NATIVE_DISTRICT,
                    onClick = { onScopeSelected(FeedScope.NATIVE_DISTRICT) },
                    label = { Text("Hometown") }
                )
            }
        }
        item {
            FilterChip(
                selected = selectedScope == FeedScope.STATE,
                onClick = { onScopeSelected(FeedScope.STATE) },
                label = { Text("State") }
            )
        }
    }
}
