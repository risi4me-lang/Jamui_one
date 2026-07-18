package com.example.jamuione.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.jamuione.ui.feed.FeedViewModel
import com.example.jamuione.ui.feed.PostCard
import com.example.jamuione.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostsScreen(
    viewModel: FeedViewModel,
    onNavigateToDetail: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    val savedPostsResource by viewModel.savedPosts.collectAsState()
    val likedPosts by viewModel.likedPosts.collectAsState()
    val isSavedMap by viewModel.isSavedMap.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val postAuthors by viewModel.postAuthors.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSavedPosts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Posts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val resource = savedPostsResource) {
                is Resource.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is Resource.Success -> {
                    val posts = resource.data ?: emptyList()
                    if (posts.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No saved posts yet.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(posts) { post ->
                                PostCard(
                                    post = post,
                                    authorProfile = postAuthors[post.userId],
                                    currentUserId = userProfile.data?.uid,
                                    isLiked = likedPosts[post.id] ?: false,
                                    isSaved = isSavedMap[post.id] ?: true,
                                    onLikeClick = { viewModel.toggleLike(post.id) },
                                    onSaveClick = { viewModel.toggleSavePost(post.id) },
                                    onDeleteClick = { viewModel.deletePost(post.id) },
                                    onDetailClick = { onNavigateToDetail(post.id) },
                                    onCommentClick = { onNavigateToDetail(post.id) },
                                    onAuthorClick = { onNavigateToUserProfile(it) }
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Text(
                        text = resource.message ?: "Failed to load saved posts",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                else -> {}
            }
        }
    }
}
