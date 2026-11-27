package com.example.jamuione.ui.profile

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import coil.compose.AsyncImage
import com.example.jamuione.BuildConfig
import com.example.jamuione.domain.model.User
import com.example.jamuione.ui.auth.AuthViewModel
import com.example.jamuione.ui.components.*
import com.example.jamuione.util.BrandingUtil
import com.example.jamuione.util.Resource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    isAdmin: Boolean = false,
    isOperator: Boolean = false,
    onNavigateToSavedPosts: () -> Unit,
    onNavigateToCommunities: () -> Unit,
    onNavigateToMyTrips: () -> Unit,
    onNavigateToAccountSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onViewPrivacyPolicy: () -> Unit,
    onViewTermsOfService: () -> Unit,
    onLogout: () -> Unit
) {
    val userProfileState by viewModel.userProfile.collectAsState()
    val verificationEmailState by viewModel.verificationEmailState.collectAsState()
    val communityName = BrandingUtil.getCommunityName(userProfileState.data?.district)
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var resendCooldown by remember { mutableIntStateOf(0) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(communityName) },
                actions = {
                    IconButton(onClick = onNavigateToAccountSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val user = (userProfileState as? Resource.Success)?.data
        val completion = user?.let { viewModel.calculateProfileCompletion(it) } ?: 0

        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (completion == 100) {
                ConfettiEffect()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (userProfileState) {
                    is Resource.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is Resource.Success -> {
                        if (user != null) {
                            if (viewModel.isEmailPasswordUser() && !viewModel.isEmailVerified()) {
                                EmailVerificationBanner(
                                    onResendClick = { viewModel.resendVerificationEmail() },
                                    cooldown = resendCooldown
                                )
                            }

                            ProfileHeader(user, onEditProfile)

                            ProfileStatsRow(user)

                            if (completion < 100) {
                                ProfileCompletionCard(
                                    completion = completion,
                                    missingItems = viewModel.getMissingProfileItems(user),
                                    onClick = onEditProfile
                                )
                            }

                            MenuGroupSection(title = "My activity") {
                                MenuRow(
                                    icon = Icons.Default.Bookmark,
                                    title = "Saved posts",
                                    subtitle = "Bookmarks and saved items",
                                    onClick = onNavigateToSavedPosts
                                )
                            }

                            MenuGroupSection(title = "Community") {
                                MenuRow(
                                    icon = Icons.Default.People,
                                    title = "Communities",
                                    subtitle = "Local and professional groups",
                                    onClick = onNavigateToCommunities
                                )
                                if (isOperator) {
                                    MenuRow(
                                        icon = Icons.Default.ListAlt,
                                        title = "My Trips",
                                        subtitle = "Manage your offered rides and seats",
                                        onClick = onNavigateToMyTrips
                                    )
                                }
                                MenuRow(
                                    icon = Icons.Default.Share,
                                    title = "Invite neighbors",
                                    subtitle = "Grow community on WhatsApp",
                                    onClick = {
                                        val inviteMessage = "Join me on $communityName — local community app! Download here: https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, inviteMessage)
                                            setPackage("com.whatsapp")
                                        }
                                        try {
                                            context.startActivity(sendIntent)
                                        } catch (e: Exception) {
                                            val fallback = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, inviteMessage)
                                            }
                                            context.startActivity(Intent.createChooser(fallback, "Invite via"))
                                        }
                                    }
                                )
                            }

                            MenuGroupSection(title = "Account") {
                                MenuRow(
                                    icon = Icons.Default.Settings,
                                    title = "Settings",
                                    subtitle = "Language, notifications & account security",
                                    onClick = onNavigateToAccountSettings
                                )
                                MenuRow(
                                    icon = Icons.Default.PrivacyTip,
                                    title = "Privacy policy",
                                    subtitle = "Data handling and security",
                                    onClick = onViewPrivacyPolicy
                                )
                                MenuRow(
                                    icon = Icons.Default.Description,
                                    title = "Terms of service",
                                    subtitle = "Community guidelines and rules",
                                    onClick = onViewTermsOfService
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            GuestState(onLoginClick = onLogout)
                        }
                    }
                    is Resource.Error -> {
                        ErrorState(
                            message = (userProfileState as Resource.Error).message ?: "Failed to load profile",
                            onRetry = { viewModel.reloadUser() }
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun EmailVerificationBanner(onResendClick: () -> Unit, cooldown: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Email not verified", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text(text = "Please verify your email address for full access.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onResendClick, enabled = cooldown == 0) {
                Text(if (cooldown > 0) "${cooldown}s" else "Verify")
            }
        }
    }
}

@Composable
fun ConfettiEffect() {
    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.primary))
}

@Composable
fun GuestState(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Guest Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Sign in to access your profile and community.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onLoginClick, shape = RoundedCornerShape(12.dp)) {
            Text("Sign In")
        }
    }
}

@Composable
fun ProfileHeader(user: User, onEditProfile: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            if (!user.profileImage.isNullOrBlank()) {
                AsyncImage(
                    model = user.profileImage,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onEditProfile),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier
                        .size(96.dp)
                        .clickable(onClick = onEditProfile),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onEditProfile),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (user.isVerified) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val localityDisplay = user.locality.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            val districtDisplay = user.district.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            val nativeDisplay = user.nativeDistrict.replaceFirstChar { it.titlecase(Locale.getDefault()) }

            if (localityDisplay.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "$localityDisplay, $districtDisplay",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (nativeDisplay.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "From $nativeDisplay",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (!user.bio.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = user.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProfileStatsRow(user: User) {
    val joinedText = if (user.joinedAt > 0L) {
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(user.joinedAt))
    } else {
        "Recently"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Community Member",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Active Status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            VerticalDivider(modifier = Modifier.height(32.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Joined $joinedText",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Member Since",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProfileCompletionCard(
    completion: Int,
    missingItems: List<String>,
    onClick: () -> Unit
) {
    val nextItem = missingItems.firstOrNull() ?: "Add more details"
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(48.dp)) {
                    val strokeWidth = 4.dp.toPx()
                    drawCircle(color = trackColor, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth))
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = (completion / 100f) * 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
                Text(
                    text = "$completion%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Complete your profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Next: $nextItem",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun MenuGroupSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

@Composable
fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
