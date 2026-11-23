package com.example.jamuione.ui.organization

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.jamuione.domain.model.OrganizationAnnouncement
import com.example.jamuione.util.Resource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationProfileScreen(
    orgId: String,
    viewModel: OrganizationViewModel,
    onBack: () -> Unit
) {
    val orgResource by viewModel.selectedOrg.collectAsState()
    val announcementsResource by viewModel.announcements.collectAsState()
    val isFollowingMap by viewModel.isFollowingMap.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(orgId) {
        viewModel.loadOrganizationDetails(orgId)
        viewModel.loadAnnouncements(orgId)
    }

    val isFollowing = isFollowingMap[orgId] ?: false

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val org = (orgResource as? Resource.Success)?.data
                    Text(org?.name ?: "Organization Profile")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (orgResource) {
            is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Success -> {
                val org = (orgResource as Resource.Success).data!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (org.logoUrl != null) {
                                        AsyncImage(
                                            model = org.logoUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Surface(
                                            modifier = Modifier.size(64.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(org.name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = org.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (org.isVerified) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    Icons.Default.Verified,
                                                    contentDescription = "Verified",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${org.type.name} • ${org.category}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "${org.district}, ${org.state}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                if (org.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = org.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column {
                                            Text(text = "${org.followerCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(text = "Followers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Column {
                                            Text(text = org.status, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(text = "Status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }

                                    Button(
                                        onClick = { 
                                            viewModel.toggleFollow(orgId) { msg ->
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                            }
                                        },
                                        colors = if (isFollowing) {
                                            ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        } else {
                                            ButtonDefaults.buttonColors()
                                        }
                                    ) {
                                        Text(if (isFollowing) "Following" else "Follow")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Announcements",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    when (val annResource = announcementsResource) {
                        is Resource.Loading -> {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        is Resource.Success -> {
                            val announcements = annResource.data ?: emptyList()
                            if (announcements.isEmpty()) {
                                item {
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                            Text("No announcements yet.", color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                            } else {
                                items(announcements) { ann ->
                                    AnnouncementCard(ann = ann)
                                }
                            }
                        }
                        is Resource.Error -> {
                            item {
                                Text(text = "Error loading announcements: ${annResource.message}", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        else -> {}
                    }
                }
            }
            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${orgResource.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {}
        }
    }
}

@Composable
fun AnnouncementCard(ann: OrganizationAnnouncement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = ann.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                val dateStr = remember(ann.timestamp) {
                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(ann.timestamp))
                }
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ann.content,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!ann.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = ann.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
