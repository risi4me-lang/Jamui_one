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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jamuione.ui.auth.AuthViewModel
import com.example.jamuione.util.Resource
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesScreen(
    authViewModel: AuthViewModel,
    onNavigateToNativeCommunity: () -> Unit,
    onNavigateToLocalityCommunity: () -> Unit,
    onNavigateToDistrictCommunity: () -> Unit,
    onBack: () -> Unit
) {
    val userProfileState by authViewModel.userProfile.collectAsState()
    val user = (userProfileState as? Resource.Success)?.data

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Communities", style = MaterialTheme.typography.titleLarge) },
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
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
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
