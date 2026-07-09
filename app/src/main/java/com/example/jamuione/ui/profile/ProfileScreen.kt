package com.example.jamuione.ui.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jamuione.ui.auth.AuthViewModel
import com.example.jamuione.util.BrandingUtil
import com.example.jamuione.util.Resource
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onNavigateToSavedPosts: () -> Unit,
    onNavigateToCommunities: () -> Unit,
    onEditProfile: () -> Unit,
    onViewPrivacyPolicy: () -> Unit,
    onViewTermsOfService: () -> Unit,
    onLogout: () -> Unit
) {
    val userProfileState by viewModel.userProfile.collectAsState()
    val communityName = BrandingUtil.getCommunityName(userProfileState.data?.district)
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteFinalDialog by remember { mutableStateOf(false) }
    var deleteInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchUserProfile()
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("This action cannot be undone. Your profile, posts, notices, comments and saved data may be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    showDeleteFinalDialog = true
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteFinalDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteFinalDialog = false },
            title = { Text("Confirm Deletion") },
            text = {
                Column {
                    Text("Type DELETE to continue")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deleteInput,
                        onValueChange = { deleteInput = it },
                        placeholder = { Text("DELETE") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteFinalDialog = false
                        onLogout()
                    },
                    enabled = deleteInput == "DELETE",
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFinalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    onLogout()
                }) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(communityName) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = userProfileState) {
                is Resource.Loading -> Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is Resource.Success -> {
                    val user = state.data
                    if (user != null) {
                        ProfileHeader(user)
                        
                        ProfileCompletionSection(
                            completion = viewModel.calculateProfileCompletion(user),
                            missingItems = viewModel.getMissingProfileItems(user),
                            onClick = onEditProfile
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // MENU ITEMS
                        ProfileMenuItem(
                            icon = Icons.Default.People,
                            title = "Communities",
                            subtitle = "Join local and professional groups",
                            onClick = onNavigateToCommunities
                        )
                        ProfileMenuItem(
                            icon = Icons.Default.Bookmark,
                            title = "Saved Posts",
                            subtitle = "Posts you've bookmarked",
                            onClick = onNavigateToSavedPosts
                        )
                        ProfileMenuItem(
                            icon = Icons.Default.Share,
                            title = "Invite Neighbors",
                            subtitle = "Grow your community on WhatsApp",
                            onClick = {
                                val inviteMessage = "Join me on Jamui One — the local community app for staying connected! Download here: https://play.google.com/store/apps/details?id=com.example.jamuione"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, inviteMessage)
                                    setPackage("com.whatsapp")
                                }
                                try {
                                    context.startActivity(sendIntent)
                                } catch (e: Exception) {
                                    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, inviteMessage)
                                    }
                                    context.startActivity(Intent.createChooser(fallbackIntent, "Invite via"))
                                }
                            }
                        )

                        ProfileMenuItem(
                            icon = Icons.Default.Delete,
                            title = "Delete Account",
                            subtitle = "Permanently remove your data",
                            onClick = { showDeleteConfirmDialog = true },
                            isDestructive = true
                        )
                        
                        if (com.example.jamuione.BuildConfig.DEBUG) {
                            ProfileMenuItem(
                                icon = Icons.Default.BugReport,
                                title = "Test Crash",
                                subtitle = "Debug only",
                                onClick = { 
                                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Crash Test")
                                    throw RuntimeException("Crash Test")
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        LegalFooter(
                            onViewPrivacyPolicy = onViewPrivacyPolicy,
                            onViewTermsOfService = onViewTermsOfService
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        TextButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logout")
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    } else {
                        GuestState(onLogout)
                    }
                }
                is Resource.Error -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${state.message}")
                        Button(onClick = { viewModel.fetchUserProfile() }) { Text("Retry") }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ProfileHeader(user: com.example.jamuione.domain.model.User) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user.profileImage != null) {
            AsyncImage(
                model = user.profileImage,
                contentDescription = null,
                modifier = Modifier.size(100.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = user.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (user.isVerified) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Text(text = user.profession, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        
        val displayLocality = user.locality.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val displayDistrict = user.district.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        Text(text = "$displayLocality, $displayDistrict", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun ProfileCompletionSection(completion: Int, missingItems: List<String>, onClick: () -> Unit) {
    val isComplete = completion == 100
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Profile Status", style = MaterialTheme.typography.titleSmall)
                    if (isComplete) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
                Text(text = if (isComplete) "Verified Profile" else "$completion% Complete", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { completion / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = if (isComplete) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )
            
            if (missingItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Complete your profile to unlock all benefits.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(8.dp))
                missingItems.take(2).forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = item, style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Great job! Your profile is fully visible to the community.", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun ProfileMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit, isDestructive: Boolean = false) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = if (isDestructive) MaterialTheme.colorScheme.error else Color.Unspecified)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun LegalFooter(onViewPrivacyPolicy: () -> Unit, onViewTermsOfService: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            TextButton(onClick = onViewTermsOfService) {
                Text("Terms of Service", style = MaterialTheme.typography.labelSmall)
            }
            Text(" • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.CenterVertically))
            TextButton(onClick = onViewPrivacyPolicy) {
                Text("Privacy Policy", style = MaterialTheme.typography.labelSmall)
            }
        }
        Text(text = "District One Version 1.0", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun GuestState(onLogout: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "You are browsing as a guest.", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = "Sign in or create an account to unlock all features.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Sign In / Create Account") }
    }
}
