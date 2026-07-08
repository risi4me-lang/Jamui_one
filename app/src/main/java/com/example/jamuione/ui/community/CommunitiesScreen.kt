package com.example.jamuione.ui.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    onBack: () -> Unit
) {
    val userProfileState by authViewModel.userProfile.collectAsState()
    val user = (userProfileState as? Resource.Success)?.data

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Communities") },
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
                        subtitle = "Migrants from your hometown",
                        onClick = onNavigateToNativeCommunity
                    )
                }

                item {
                    val locality = user.locality.replaceFirstChar { it.titlecase(Locale.getDefault()) }
                    CommunityCategoryCard(
                        icon = Icons.Default.LocationOn,
                        title = "$locality Residents",
                        subtitle = "People living in your neighborhood",
                        onClick = onNavigateToLocalityCommunity
                    )
                }

                val soonCommunities = listOf(
                    Triple(Icons.Default.Work, "Finance Professionals", "Network with industry peers"),
                    Triple(Icons.Default.Favorite, "Blood Donors", "Save lives in your area"),
                    Triple(Icons.Default.School, "NIT Patna Alumni", "Connect with fellow graduates"),
                    Triple(Icons.Default.SportsCricket, "Cricket Lovers", "Find players and matches nearby")
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
        enabled = !isComingSoon
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            
            if (isComingSoon) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text("Coming Soon", modifier = Modifier.padding(horizontal = 4.dp))
                }
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
