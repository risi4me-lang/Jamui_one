package com.example.jamuione.ui.notices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
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
import com.example.jamuione.domain.model.Notice
import com.example.jamuione.ui.feed.FeedScope
import com.example.jamuione.ui.feed.ScopeSelector
import com.example.jamuione.util.BrandingUtil
import com.example.jamuione.util.Resource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeBoardScreen(
    viewModel: NoticeViewModel,
    onCreateNoticeClick: () -> Unit
) {
    val noticesResource by viewModel.notices.collectAsState()
    val currentScope by viewModel.currentScope.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
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
                FloatingActionButton(onClick = onCreateNoticeClick) {
                    Icon(Icons.Default.Add, contentDescription = "Create Notice")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            ScopeSelector(
                selectedScope = currentScope,
                onScopeSelected = { viewModel.setScope(it) }
            )

            CategorySelector(
                categories = viewModel.categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.setCategory(it) }
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (noticesResource is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (noticesResource is Resource.Success) {
                    val notices = noticesResource.data ?: emptyList()
                    if (notices.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No notices in this category/area.\nCheck back later for updates!",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(notices) { notice ->
                                NoticeItem(notice = notice)
                            }
                        }
                    }
                } else if (noticesResource is Resource.Error) {
                    Text(
                        text = noticesResource.message ?: "Error loading notices",
                        modifier = Modifier.align(Alignment.Center),
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

@Composable
fun NoticeItem(notice: Notice) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge { Text(notice.category) }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Expires: ${formatDate(notice.expiryDate)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = notice.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = notice.description, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
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
                                .size(24.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = notice.userName, fontSize = 12.sp)
                }
                if (notice.contactNumber.isNotBlank()) {
                    Button(
                        onClick = { /* Call intent */ },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Contact", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
