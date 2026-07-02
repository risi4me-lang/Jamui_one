package com.example.jamuione.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jamuione.domain.model.Post
import com.example.jamuione.util.BrandingUtil
import com.example.jamuione.util.Resource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onCreatePostClick: () -> Unit
) {
    val postsResource by viewModel.posts.collectAsState()
    val cachedPosts by viewModel.cachedPosts.collectAsState()
    val currentScope by viewModel.currentScope.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val communityName = BrandingUtil.getCommunityName(userProfile.data?.district)

    Scaffold(
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
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(displayPosts) { post ->
                            PostItem(post = post)
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

@Composable
fun PostItem(post: Post) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (post.userProfileImage != null) {
                    AsyncImage(
                        model = post.userProfileImage,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = post.userName.take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = post.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "${post.locality} • ${formatTimestamp(post.timestamp)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = { /* Report/Options */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = post.content, fontSize = 14.sp)

            if (!post.imageUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Like", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${post.likesCount}", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comment", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${post.commentsCount}", fontSize = 12.sp)
                }
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
