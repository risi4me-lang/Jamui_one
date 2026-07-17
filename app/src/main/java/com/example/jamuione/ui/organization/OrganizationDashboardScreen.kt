package com.example.jamuione.ui.organization

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jamuione.domain.model.Organization
import com.example.jamuione.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationDashboardScreen(
    orgId: String,
    viewModel: OrganizationViewModel,
    onBack: () -> Unit
) {
    val orgResource by viewModel.selectedOrg.collectAsState()

    // Dashboard sections
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Content", "Followers", "Analytics")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }
            
            when (selectedTab) {
                0 -> OverviewSection(orgResource)
                1 -> ContentSection()
                2 -> FollowersSection(orgResource)
                3 -> AnalyticsSection()
            }
        }
    }
}

@Composable
fun OverviewSection(resource: Resource<Organization>) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        if (resource is Resource.Success) {
            val org = resource.data!!
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (org.logoUrl != null) {
                    AsyncImage(model = org.logoUrl, contentDescription = null, modifier = Modifier.size(64.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(org.name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = org.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = org.type.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Followers", org.followerCount.toString(), Icons.Default.People)
                StatCard(Modifier.weight(1f), "Status", org.status, Icons.Default.CheckCircle)
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ContentSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Campaign, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Announcement")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Event, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Event")
        }
    }
}

@Composable
fun FollowersSection(resource: Resource<Organization>) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Follower list coming soon")
    }
}

@Composable
fun AnalyticsSection() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Analytics coming soon")
    }
}
