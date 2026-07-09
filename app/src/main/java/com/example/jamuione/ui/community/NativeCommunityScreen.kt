package com.example.jamuione.ui.community

import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jamuione.ui.components.MemberSkeletonLoader
import com.example.jamuione.domain.model.CommunityStats
import com.example.jamuione.domain.model.User
import com.example.jamuione.util.Resource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeCommunityScreen(
    viewModel: NativeCommunityViewModel,
    onBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val localityMembers by viewModel.localityMembers.collectAsState()
    val districtMembers by viewModel.districtMembers.collectAsState()
    val everywhereMembers by viewModel.everywhereMembers.collectAsState()
    val statsResource by viewModel.stats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    LaunchedEffect(user) {
        if (user != null) {
            viewModel.loadHometownData()
        }
    }

    val nativeDistrictName = user?.nativeDistrict?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Home"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🏠 People From $nativeDistrictName", style = MaterialTheme.typography.titleMedium)
                        if (statsResource is Resource.Success) {
                            Text(
                                text = "${(statsResource as Resource.Success).data?.totalMembers ?: 0} Members",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Search by name, profession or company") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = CircleShape,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }

            if (searchQuery.isBlank()) {
                item {
                    CommunityStatsSection(statsResource)
                }
            }

            // SECTION 1: SAME LOCALITY
            val filteredLocality = viewModel.filterList(localityMembers, searchQuery)
            if (localityMembers is Resource.Loading && filteredLocality.isEmpty()) {
                item { SectionHeader("📍 Same Locality", 0) }
                items(3) { MemberSkeletonLoader() }
            } else if (filteredLocality.isNotEmpty()) {
                item {
                    SectionHeader("📍 Same Locality", filteredLocality.size)
                }
                items(filteredLocality) { member ->
                    CommunityMemberCard(member)
                }
            }

            // SECTION 2: SAME DISTRICT
            val filteredDistrict = viewModel.filterList(districtMembers, searchQuery)
                .filter { distMem -> filteredLocality.none { locMem -> locMem.uid == distMem.uid } }
            
            if (districtMembers is Resource.Loading && filteredDistrict.isEmpty()) {
                item { SectionHeader("🏙 Same District", 0) }
                items(3) { MemberSkeletonLoader() }
            } else if (filteredDistrict.isNotEmpty()) {
                item {
                    SectionHeader("🏙 Same District", filteredDistrict.size)
                }
                items(filteredDistrict) { member ->
                    CommunityMemberCard(member)
                }
            }

            // SECTION 3: EVERYWHERE ELSE
            val filteredEverywhere = viewModel.filterList(everywhereMembers, searchQuery)
                .filter { evMem -> filteredLocality.none { it.uid == evMem.uid } && filteredDistrict.none { it.uid == evMem.uid } }
            
            if (everywhereMembers is Resource.Loading && filteredEverywhere.isEmpty()) {
                item { SectionHeader("🌍 Everywhere Else", 0) }
                items(3) { MemberSkeletonLoader() }
            } else if (filteredEverywhere.isNotEmpty()) {
                item {
                    SectionHeader("🌍 Everywhere Else", filteredEverywhere.size)
                }
                items(filteredEverywhere) { member ->
                    CommunityMemberCard(member)
                }
            }

            if (filteredLocality.isEmpty() && filteredDistrict.isEmpty() && filteredEverywhere.isEmpty() && everywhereMembers is Resource.Success) {
                item {
                    EmptyCommunityState(nativeDistrictName)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = CircleShape
        ) {
            Text(
                text = "$count Members",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun CommunityStatsSection(resource: Resource<CommunityStats>) {
    if (resource is Resource.Success) {
        val stats = resource.data!!
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Community Stats", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(Icons.Default.People, "${stats.totalMembers} Members", Modifier.weight(1f))
                StatChip(Icons.Default.Verified, "${stats.verifiedMembers} Verified", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(Icons.Default.Work, "${stats.professionals} Professionals", Modifier.weight(1f))
                StatChip(Icons.Default.Favorite, "${stats.bloodDonors} Blood Donors", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatChip(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier = modifier.height(44.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CommunityMemberCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (user.profileImage != null) {
                    AsyncImage(
                        model = user.profileImage,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = user.name.take(1).uppercase(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (user.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(text = user.profession, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    if (!user.company.isNullOrBlank()) {
                        Text(text = "@ ${user.company}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Button(
                    onClick = { /* View Profile */ },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("View", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Native", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Text(text = user.nativeDistrict.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lives", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    val currentLocality = if (user.isDeleted) "Redacted" else user.locality.replaceFirstChar { it.uppercase() }
                    Text(text = currentLocality, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "District One Member Since ${formatDate(user.joinedAt)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Recent"
    val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun EmptyCommunityState(district: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.PeopleOutline, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "You're the first member here.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(text = "Invite friends from $district to join you in this neighborhood!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                val inviteMessage = "Join me on Jamui One — the local community app for people from $district! Download here: https://play.google.com/store/apps/details?id=com.example.jamuione"
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, inviteMessage)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Invite via"))
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Invite via WhatsApp")
        }
    }
}
