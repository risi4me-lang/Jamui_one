package com.example.jamuione.ui.feed

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.jamuione.ui.components.HomeHeader
import com.example.jamuione.ui.components.PostSkeletonLoader
import com.example.jamuione.util.Resource

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
    val likedPosts by viewModel.likedPosts.collectAsState()
    val isSavedMap by viewModel.isSavedMap.collectAsState()
    val currentScope by viewModel.currentScope.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val memberCount by viewModel.memberCount.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val deleteResult by viewModel.deletePostResult.collectAsState()
    val reportResult by viewModel.reportPostResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!viewModel.isGuest) {
                FloatingActionButton(
                    onClick = onCreatePostClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = "Create Post")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
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
                    placeholder = { Text("Search posts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
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
                        PostCard(
                            post = post,
                            currentUserId = userProfile.data?.uid,
                            isLiked = likedPosts[post.id] ?: false,
                            isSaved = isSavedMap[post.id] ?: false,
                            onLikeClick = { viewModel.toggleLike(post.id) },
                            onCommentClick = { onNavigateToDetail(post.id) },
                            onSaveClick = { viewModel.toggleSavePost(post.id) },
                            onDeleteClick = { viewModel.deletePost(post.id) },
                            onReportClick = { reason -> viewModel.reportPost(post.id, reason) },
                            onDetailClick = { onNavigateToDetail(post.id) }
                        )
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
fun ScopeSelector(
    selectedScope: FeedScope,
    nativeDistrict: String?,
    onScopeSelected: (FeedScope) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedScope.ordinal,
        edgePadding = 16.dp,
        containerColor = Color.Transparent,
        divider = {},
        indicator = {}
    ) {
        FeedScope.values().forEach { scope ->
            if (scope == FeedScope.NATIVE_DISTRICT && nativeDistrict == null) return@forEach
            
            val label = when (scope) {
                FeedScope.LOCALITY -> "My Locality"
                FeedScope.DISTRICT -> "My District"
                FeedScope.STATE -> "My State"
                FeedScope.NATIVE_DISTRICT -> "From $nativeDistrict"
            }
            
            Tab(
                selected = selectedScope == scope,
                onClick = { onScopeSelected(scope) },
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .background(
                        color = if (selectedScope == scope) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                text = {
                    Text(
                        text = label,
                        color = if (selectedScope == scope) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
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
