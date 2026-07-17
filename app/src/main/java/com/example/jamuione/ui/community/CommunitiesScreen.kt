package com.example.jamuione.ui.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import com.example.jamuione.ui.auth.AuthViewModel
import com.example.jamuione.util.Resource
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesScreen(
    authViewModel: AuthViewModel,
    orgViewModel: com.example.jamuione.ui.organization.OrganizationViewModel,
    onNavigateToNativeCommunity: () -> Unit,
    onNavigateToLocalityCommunity: () -> Unit,
    onNavigateToDistrictCommunity: () -> Unit,
    onNavigateToOrgDashboard: (String) -> Unit,
    onBack: () -> Unit
) {
    val userProfileState by authViewModel.userProfile.collectAsState()
    val user = (userProfileState as? Resource.Success)?.data
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("People", "Organizations")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Communities", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                if (selectedTab == 0) {
                    PeopleTab(user, onNavigateToNativeCommunity, onNavigateToLocalityCommunity, onNavigateToDistrictCommunity)
                } else {
                    OrganizationsTab(orgViewModel, user, onNavigateToOrgDashboard)
                }
            }
        }
    }
}

@Composable
fun PeopleTab(
    user: com.example.jamuione.domain.model.User,
    onNavigateToNativeCommunity: () -> Unit,
    onNavigateToLocalityCommunity: () -> Unit,
    onNavigateToDistrictCommunity: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val nativeDistrict = user.nativeDistrict.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            CommunityCategoryCard(
                icon = Icons.Default.Home,
                title = "People From $nativeDistrict",
                subtitle = "Migrants from your hometown living nearby",
                onClick = onNavigateToNativeCommunity
            )
        }

        item {
            val locality = user.locality.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            CommunityCategoryCard(
                icon = Icons.Default.LocationOn,
                title = "$locality Residents",
                subtitle = "Connect with your immediate neighbors",
                onClick = onNavigateToLocalityCommunity
            )
        }

        item {
            val district = user.district.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            CommunityCategoryCard(
                icon = Icons.Default.LocationCity,
                title = "$district Neighbors",
                subtitle = "People living in your current district",
                onClick = onNavigateToDistrictCommunity
            )
        }

        item {
            Text(
                text = "Explore More",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        val soonCommunities = listOf(
            Triple(Icons.Default.Work, "Professionals", "Network with industry peers"),
            Triple(Icons.Default.Favorite, "Blood Donors", "Find or volunteer help"),
            Triple(Icons.Default.School, "Alumni Groups", "Connect with fellow graduates"),
            Triple(Icons.Default.SportsCricket, "Sports & Hobbies", "Find players and matches nearby")
        )

        items(soonCommunities) { (icon, title, subtitle) ->
            CommunityCategoryCard(
                icon = icon,
                title = title,
                subtitle = subtitle,
                isComingSoon = true,
                onClick = {}
            )
        }
    }
}

@Composable
fun OrganizationsTab(
    viewModel: com.example.jamuione.ui.organization.OrganizationViewModel,
    user: com.example.jamuione.domain.model.User,
    onNavigateToOrgDashboard: (String) -> Unit
) {
    val discoveryOrgs by viewModel.discoveryOrgs.collectAsState()
    val isFollowingMap by viewModel.isFollowingMap.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadOrganizationsForDiscovery(state = user.state, district = user.district)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (val resource = discoveryOrgs) {
            is Resource.Loading -> {
                item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            }
            is Resource.Success -> {
                val orgs = resource.data ?: emptyList()
                if (orgs.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { Text("No organizations found in your area.", color = MaterialTheme.colorScheme.secondary) } }
                } else {
                    items(orgs) { org ->
                        OrganizationCard(
                            org = org,
                            isFollowing = isFollowingMap[org.organizationId] ?: false,
                            onFollowClick = { viewModel.toggleFollow(org.organizationId) },
                            onClick = { 
                                if (org.createdBy == user.uid) onNavigateToOrgDashboard(org.organizationId)
                                // else onNavigateToOrgProfile(org.organizationId) 
                            }
                        )
                    }
                }
            }
            is Resource.Error -> {
                item { Text("Error: ${resource.message}", color = MaterialTheme.colorScheme.error) }
            }
            else -> {}
        }
    }
}

@Composable
fun OrganizationCard(
    org: com.example.jamuione.domain.model.Organization,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (org.logoUrl != null) {
                AsyncImage(model = org.logoUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Surface(modifier = Modifier.size(50.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(org.name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = org.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (org.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, contentDescription = "Verified", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(text = "${org.type.name} • ${org.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Text(text = "${org.followerCount} followers • ${org.district}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            
            Button(
                onClick = onFollowClick,
                colors = if (isFollowing) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) else ButtonDefaults.buttonColors(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(if (isFollowing) "Following" else "Follow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CommunityCategoryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isComingSoon: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = if (!isComingSoon) onClick else ({}),
        modifier = Modifier.fillMaxWidth(),
        enabled = !isComingSoon,
        colors = CardDefaults.cardColors(
            containerColor = if (isComingSoon) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        ),
        elevation = if (isComingSoon) CardDefaults.cardElevation(0.dp) else CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isComingSoon) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = if (isComingSoon) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            
            if (isComingSoon) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text("Soon", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
