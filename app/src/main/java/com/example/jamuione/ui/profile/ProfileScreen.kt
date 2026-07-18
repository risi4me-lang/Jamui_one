package com.example.jamuione.ui.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
    onNavigateToCreateOrg: () -> Unit,
    onEditProfile: () -> Unit,
    onViewPrivacyPolicy: () -> Unit,
    onViewTermsOfService: () -> Unit,
    onLogout: () -> Unit
) {
    val userProfileState by viewModel.userProfile.collectAsState()
    val verificationEmailState by viewModel.verificationEmailState.collectAsState()
    val communityName = BrandingUtil.getCommunityName(userProfileState.data?.district)
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteFinalDialog by remember { mutableStateOf(false) }
    var deleteInput by remember { mutableStateOf("") }
    var resendCooldown by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.reloadUser()
    }

    LaunchedEffect(verificationEmailState) {
        if (verificationEmailState is Resource.Success) {
            snackbarHostState.showSnackbar("Verification email sent!")
            viewModel.resetVerificationEmailState()
            resendCooldown = 60
        } else if (verificationEmailState is Resource.Error) {
            snackbarHostState.showSnackbar((verificationEmailState as Resource.Error).message ?: "Failed to send email")
            viewModel.resetVerificationEmailState()
        }
    }

    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            kotlinx.coroutines.delay(1000L)
            resendCooldown -= 1
        }
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val completion = (userProfileState as? Resource.Success)?.data?.let { viewModel.calculateProfileCompletion(it) } ?: 0
        
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (completion == 100) {
                ConfettiEffect()
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = userProfileState) {
                    is Resource.Loading -> Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is Resource.Success -> {
                        val user = state.data
                        if (user != null) {
                            // Email Verification Banner
                            if (viewModel.isEmailPasswordUser() && !viewModel.isEmailVerified()) {
                                EmailVerificationBanner(
                                    onResendClick = { viewModel.resendVerificationEmail() },
                                    cooldown = resendCooldown
                                )
                            }

                            ProfileHeader(user)
                            
                            ProfileCompletionSection(
                                completion = completion,
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
                                icon = Icons.Default.Business,
                                title = "Create Organization",
                                subtitle = "Business, Institution or Community",
                                onClick = onNavigateToCreateOrg
                            )
                            ProfileMenuItem(
                                icon = Icons.Default.Share,
                                title = "Invite Neighbors",
                                subtitle = "Grow your community on WhatsApp",
                                onClick = {
                                    val inviteMessage = "Join me on $communityName — the local community app for staying connected! Download here: https://play.google.com/store/apps/details?id=com.example.jamuione"
                                // TODO: update with actual Play Store link once published
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
}

@Composable
fun ConfettiEffect() {
    val transition = rememberInfiniteTransition(label = "confetti")
    val yAnim by transition.animateFloat(
        initialValue = -100f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_y"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color(0xFFFF9800), Color(0xFFE91E63), Color.Cyan)
        repeat(60) { i ->
            val x = (i * 45f + (i * i * 2f)) % size.width
            val speed = (i % 7 + 1) * 0.4f
            val y = (yAnim * speed + (i * 80f)) % size.height
            val radius = (i % 5 + 5).toFloat()
            drawCircle(
                color = colors[i % colors.size].copy(alpha = 0.7f),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun EmailVerificationBanner(onResendClick: () -> Unit, cooldown: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Please verify your email address",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Check your inbox for a verification link.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
            TextButton(
                onClick = onResendClick,
                enabled = cooldown == 0,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (cooldown > 0) "Resend in ${cooldown}s" else "Resend")
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
    val isComplete = completion >= 100
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile Strength",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isComplete) "Verified" else "$completion%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = { completion / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = if (isComplete) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        if (!isComplete && missingItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Complete these tasks to reach 100%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                missingItems.forEach { task ->
                    Surface(
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = task,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        } else if (isComplete) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your profile is fully optimized!",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
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
