package com.example.jamuione.ui.community

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jamuione.ui.auth.AuthViewModel
import com.example.jamuione.ui.components.AnimateIn
import com.example.jamuione.util.BrandingUtil
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
    onNavigateToCreateOrg: () -> Unit,
    onNavigateToOrgDashboard: (String) -> Unit,
    onNavigateToOrgProfile: (String) -> Unit,
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
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
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
                    OrganizationsTab(orgViewModel, user, onNavigateToCreateOrg, onNavigateToOrgDashboard, onNavigateToOrgProfile)
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
    val context = LocalContext.current
    val nativeDistrict = user.nativeDistrict.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    val locality = user.locality.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    val district = user.district.replaceFirstChar { it.titlecase(Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card for Native Community with gradient
        item {
            AnimateIn {
                Card(
                    onClick = onNavigateToNativeCommunity,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "People From $nativeDistrict",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Be among the first neighbours",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Compact list rows for Locality & District
        item {
            AnimateIn {
                CompactCommunityRow(
                    icon = Icons.Default.LocationOn,
                    title = "$locality Residents",
                    subtitle = "Be among the first neighbours",
                    onClick = onNavigateToLocalityCommunity
                )
            }
        }

        item {
            AnimateIn {
                CompactCommunityRow(
                    icon = Icons.Default.LocationCity,
                    title = "$district Neighbors",
                    subtitle = "Be among the first neighbours",
                    onClick = onNavigateToDistrictCommunity
                )
            }
        }

        // Missing a community? Tell us row at bottom
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BrandingUtil.SUPPORT_WHATSAPP_URL))
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${BrandingUtil.SUPPORT_EMAIL}?subject=Missing Community"))
                        context.startActivity(emailIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Missing a community? Tell us",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun CompactCommunityRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun OrganizationsTab(
    viewModel: com.example.jamuione.ui.organization.OrganizationViewModel,
    user: com.example.jamuione.domain.model.User,
    onNavigateToCreateOrg: () -> Unit,
    onNavigateToOrgDashboard: (String) -> Unit,
    onNavigateToOrgProfile: (String) -> Unit
) {
    val discoveryOrgs by viewModel.discoveryOrgs.collectAsState()
    val managedOrgsResource by viewModel.managedOrgs.collectAsState()
    val isFollowingMap by viewModel.isFollowingMap.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadOrganizationsForDiscovery(state = user.state, district = user.district)
        viewModel.loadManagedOrganizations()
    }

    val managedOrgs = (managedOrgsResource as? Resource.Success)?.data ?: emptyList()
    val discoveryList = (discoveryOrgs as? Resource.Success)?.data ?: emptyList()

    val filteredDiscoveryOrgs = discoveryList.filter { discoveryOrg ->
        managedOrgs.none { it.organizationId == discoveryOrg.organizationId }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onNavigateToCreateOrg,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create organization")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (managedOrgs.isNotEmpty()) {
            item {
                Text(
                    text = "My Organizations",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
            items(managedOrgs) { org ->
                OrganizationCard(
                    org = org,
                    isFollowing = isFollowingMap[org.organizationId] ?: false,
                    onFollowClick = { viewModel.toggleFollow(org.organizationId) },
                    onClick = { onNavigateToOrgDashboard(org.organizationId) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "In your area",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
        }

        when (val resource = discoveryOrgs) {
            is Resource.Loading -> {
                if (managedOrgs.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                }
            }
            is Resource.Success -> {
                if (filteredDiscoveryOrgs.isEmpty() && managedOrgs.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No organizations found in your area.", color = MaterialTheme.colorScheme.secondary) } }
                } else {
                    items(filteredDiscoveryOrgs) { org ->
                        OrganizationCard(
                            org = org,
                            isFollowing = isFollowingMap[org.organizationId] ?: false,
                            onFollowClick = { viewModel.toggleFollow(org.organizationId) },
                            onClick = { 
                                if (org.createdBy == user.uid) {
                                    onNavigateToOrgDashboard(org.organizationId)
                                } else {
                                    onNavigateToOrgProfile(org.organizationId)
                                }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (org.logoUrl != null) {
                AsyncImage(model = org.logoUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Surface(modifier = Modifier.size(50.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(org.name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${org.type.name} • ${org.category}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "${org.followerCount} followers • ${org.district}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            
            Button(
                onClick = onFollowClick,
                colors = if (isFollowing) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isFollowing) "Following" else "Follow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
