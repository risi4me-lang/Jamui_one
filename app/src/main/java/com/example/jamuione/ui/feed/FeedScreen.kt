package com.example.jamuione.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jamuione.domain.model.Post
import com.example.jamuione.util.BrandingUtil
import com.example.jamuione.util.Resource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onCreatePostClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val postsResource by viewModel.posts.collectAsState()
    val cachedPosts by viewModel.cachedPosts.collectAsState()
    val likedPosts by viewModel.likedPosts.collectAsState()
    val isSavedMap by viewModel.isSavedMap.collectAsState()
    val currentScope by viewModel.currentScope.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val deleteResult by viewModel.deletePostResult.collectAsState()
    val reportResult by viewModel.reportPostResult.collectAsState()
    val communityName = BrandingUtil.getCommunityName(userProfile.data?.district)
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
                FloatingActionButton(onClick = onCreatePostClick) {
                    Icon(Icons.Default.Add, contentDescription = "Create Post")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            ScopeSelector(
                selectedScope = currentScope,
                onScopeSelected = { viewModel.setScope(it) }
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val displayPosts = if (postsResource is Resource.Success) {
                    postsResource.data ?: emptyList()
                } else {
                    if (currentScope == FeedScope.LOCALITY) cachedPosts else emptyList()
                }

                if (displayPosts.isEmpty() && postsResource is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (displayPosts.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No posts found for this area.\nBe the first to share what's happening!",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else if (postsResource is Resource.Error) {
                    Text(
                        text = postsResource.message ?: "Failed to load posts",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp).align(Alignment.Center)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScopeSelector(
    selectedScope: FeedScope,
    onScopeSelected: (FeedScope) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
        item {
            FilterChip(
                selected = selectedScope == FeedScope.STATE,
                onClick = { onScopeSelected(FeedScope.STATE) },
                label = { Text("State") }
            )
        }
    }
}
