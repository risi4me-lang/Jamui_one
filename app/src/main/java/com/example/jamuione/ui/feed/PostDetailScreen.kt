package com.example.jamuione.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jamuione.domain.model.Comment
import com.example.jamuione.domain.model.Like
import com.example.jamuione.domain.model.Post
import com.example.jamuione.util.Resource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    viewModel: PostDetailViewModel,
    feedViewModel: FeedViewModel,
    onBack: () -> Unit
) {
    val postsResource by feedViewModel.posts.collectAsState()
    val commentsResource by viewModel.comments.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()
    val isSavedMap by feedViewModel.isSavedMap.collectAsState()
    val likersResource by viewModel.likers.collectAsState()
    val reportResult by viewModel.reportPostResult.collectAsState()
    val userProfile by feedViewModel.userProfile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val post = remember(postsResource) {
        if (postsResource is Resource.Success) {
            postsResource.data?.find { it.id == postId }
        } else null
    }

    var commentText by remember { mutableStateOf("") }
    var replyingToCommentId by remember { mutableStateOf<String?>(null) }
    var showLikersSheet by remember { mutableStateOf(false) }

    LaunchedEffect(postId) {
        viewModel.loadPostDetails(postId)
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

    if (showLikersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLikersSheet = false }
        ) {
            LikersList(likersResource)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Post Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            CommentInput(
                text = commentText,
                onTextChange = { if (it.length <= 500) commentText = it },
                onSend = {
                    viewModel.addComment(postId, commentText, replyingToCommentId)
                    commentText = ""
                    replyingToCommentId = null
                },
                replyingToName = if (replyingToCommentId != null) {
                    (commentsResource as? Resource.Success)?.data?.find { it.id == replyingToCommentId }?.userName
                } else null,
                onCancelReply = { replyingToCommentId = null }
            )
        }
    ) { innerPadding ->
        if (post == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                item {
                    PostCard(
                        post = post,
                        isLiked = isLiked,
                        isSaved = isSavedMap[postId] ?: false,
                        currentUserId = userProfile.data?.uid,
                        onLikeClick = { viewModel.toggleLike(postId) },
                        onSaveClick = { feedViewModel.toggleSavePost(postId) },
                        onReportClick = { reason -> viewModel.reportPost(postId, reason) },
                        onCommentClick = { /* Already here */ },
                        onDetailClick = { /* Already here */ }
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${post.likesCount} likes",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clickable {
                                viewModel.fetchLikers(postId)
                                showLikersSheet = true
                            }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "${post.commentsCount} comments",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Divider()
                }

                when (commentsResource) {
                    is Resource.Loading -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is Resource.Success -> {
                        val comments = commentsResource.data ?: emptyList()
                        val topLevelComments = comments.filter { it.parentCommentId == null }
                        val repliesMap = comments.filter { it.parentCommentId != null }.groupBy { it.parentCommentId }

                        items(topLevelComments) { comment ->
                            CommentItem(
                                comment = comment,
                                replies = repliesMap[comment.id] ?: emptyList(),
                                onReplyClick = { replyingToCommentId = comment.id }
                            )
                        }
                    }
                    is Resource.Error -> {
                        item {
                            Text(
                                text = "Failed to load comments",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    replies: List<Comment>,
    onReplyClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            UserAvatar(imageUrl = comment.userProfileImage, name = comment.userName, size = 32.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = comment.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (comment.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(text = comment.content, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = formatTime(comment.timestamp), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Reply",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onReplyClick() }
                    )
                }
            }
        }
        
        replies.forEach { reply ->
            Row(modifier = Modifier.padding(start = 40.dp, top = 12.dp)) {
                UserAvatar(imageUrl = reply.userProfileImage, name = reply.userName, size = 24.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = reply.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (reply.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(text = reply.content, fontSize = 13.sp)
                    Text(text = formatTime(reply.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun UserAvatar(imageUrl: String?, name: String, size: androidx.compose.ui.unit.Dp) {
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = (size.value * 0.4).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CommentInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    replyingToName: String?,
    onCancelReply: () -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (replyingToName != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Replying to $replyingToName", style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = onCancelReply) {
                        Text("Cancel", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a comment...") },
                    maxLines = 4,
                    supportingText = { Text("${text.length}/500", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End) }
                )
                IconButton(onClick = onSend, enabled = text.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
fun LikersList(likersResource: Resource<List<Like>>) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).padding(16.dp)) {
        Text(text = "Likers", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        when (likersResource) {
            is Resource.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            is Resource.Success -> {
                LazyColumn {
                    items(likersResource.data ?: emptyList()) { like ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(imageUrl = like.userProfileImage, name = like.userName, size = 40.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = like.userName, fontWeight = FontWeight.Bold)
                            if (like.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
