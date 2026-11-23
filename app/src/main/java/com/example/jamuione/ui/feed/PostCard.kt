package com.example.jamuione.ui.feed

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.example.jamuione.domain.model.Post
import com.example.jamuione.domain.model.User
import com.example.jamuione.util.RelativeTime
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

@Composable
fun PostCard(
    post: Post,
    authorProfile: User? = null,
    currentUserId: String? = null,
    isLiked: Boolean = false,
    isSaved: Boolean = false,
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onReportClick: (String) -> Unit = {},
    onDetailClick: () -> Unit = {},
    onAuthorClick: (String) -> Unit = {}
) {
    if (post.isDeleted) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = "This post has been deleted.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    val haptic = LocalHapticFeedback.current

    var isImageLoaded by remember { mutableStateOf(post.imageUrl.isNullOrEmpty()) }
    var isSharing by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var showReadMore by remember { mutableStateOf(false) }

    // Shimmer for image placeholder
    val transition = rememberInfiniteTransition(label = "postcard_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    // Animations
    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "like_scale"
    )
    val saveScale by animateFloatAsState(
        targetValue = if (isSaved) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "save_scale"
    )

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Post") },
            text = {
                Column {
                    Text("Please provide a reason for reporting this post:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Reason (e.g. Spam, Harassment)") }
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
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post?") },
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

    // Off-screen shareable card for capture
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(0, 0) {
                    placeable.place(2000, 2000)
                }
            }
            .drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
            }
    ) {
        ShareableCard(post = post, authorProfile = authorProfile)
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (post.isOfficial) {
                    Modifier.drawBehind {
                        drawRect(
                            color = primaryColor,
                            size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height)
                        )
                    }
                } else Modifier
            )
            .clickable { onDetailClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val authorName = post.organizationName ?: post.userName
            val authorImage = post.authorLogoUrl ?: post.userProfileImage
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (authorImage != null) {
                    AsyncImage(
                        model = authorImage,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onAuthorClick(post.userId) },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { onAuthorClick(post.userId) },
                        shape = CircleShape,
                        color = if (post.organizationId != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = authorName.take(1).uppercase(),
                                color = if (post.organizationId != null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = authorName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clickable { onAuthorClick(post.userId) }
                        )
                        if (post.isVerified || post.organizationId != null) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (post.isOfficial) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Official",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    val displayLocality = post.locality.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    val profession = authorProfile?.profession?.takeIf { it.isNotBlank() }
                    val subtitle = if (profession != null) "$profession · $displayLocality" else displayLocality
                    
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = RelativeTime.format(post.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (post.userId == currentUserId) {
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

            Column {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (expanded) Int.MAX_VALUE else 6,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result ->
                        if (!expanded && result.hasVisualOverflow) {
                            showReadMore = true
                        }
                    }
                )
                if (showReadMore && !expanded) {
                    TextButton(
                        onClick = { expanded = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Read more",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!post.imageUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Post Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                        contentScale = ContentScale.Crop,
                        onState = { state ->
                            if (state is AsyncImagePainter.State.Success) {
                                isImageLoaded = true
                            }
                        }
                    )
                    if (!isImageLoaded) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(shimmerBrush)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLikeClick()
                        },
                        modifier = Modifier.size(48.dp).scale(likeScale)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (post.likesCount > 0) {
                        Text(
                            text = "${post.likesCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    IconButton(onClick = onCommentClick, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${post.commentsCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onSaveClick, modifier = Modifier.size(48.dp).scale(saveScale)) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            if (isImageLoaded) {
                                isSharing = true
                                scope.launch {
                                    try {
                                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                        val cachePath = File(context.cacheDir, "shared_images")
                                        cachePath.mkdirs()
                                        val file = File(cachePath, "post_${post.id}.png")
                                        FileOutputStream(file).use { out ->
                                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                        }
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(Intent.EXTRA_TEXT, "Shared from District One - https://play.google.com/store/apps/details?id=${com.example.jamuione.BuildConfig.APPLICATION_ID}")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share post"))
                                    } catch (e: Exception) {
                                        Log.e("POST_DEBUG", "Failed to share image", e)
                                    } finally {
                                        isSharing = false
                                    }
                                }
                            } else {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${post.content}\n\nShared from District One - https://play.google.com/store/apps/details?id=${com.example.jamuione.BuildConfig.APPLICATION_ID}")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share post"))
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        enabled = !isSharing
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
