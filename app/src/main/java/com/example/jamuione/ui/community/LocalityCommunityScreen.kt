package com.example.jamuione.ui.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jamuione.ui.components.MemberSkeletonLoader
import com.example.jamuione.util.Resource
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalityCommunityScreen(
    viewModel: NativeCommunityViewModel,
    onNavigateToProfile: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val residentsResource by viewModel.residents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val localityName = user?.locality?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Neighborhood"

    LaunchedEffect(user) {
        if (user != null) {
            viewModel.loadLocalityData()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📍 $localityName Residents", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search neighbors...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = CircleShape,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val filtered = viewModel.filterList(residentsResource, searchQuery)
                
                if (residentsResource is Resource.Loading && filtered.isEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        items(5) { MemberSkeletonLoader() }
                    }
                } else if (residentsResource is Resource.Error) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = residentsResource.message ?: "Failed to load residents",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                } else if (filtered.isEmpty() && residentsResource is Resource.Success) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No residents found.", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered) { member ->
                            CommunityMemberCard(member, onProfileClick = { onNavigateToProfile(member.uid) })
                        }
                    }
                }
            }
        }
    }
}
