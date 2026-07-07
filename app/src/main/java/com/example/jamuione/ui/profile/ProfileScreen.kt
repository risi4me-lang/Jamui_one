package com.example.jamuione.ui.profile

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onLogout: () -> Unit
) {
    val userProfileState by viewModel.userProfile.collectAsState()
    val communityName = BrandingUtil.getCommunityName(userProfileState.data?.district)
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchUserProfile()
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
            TopAppBar(
                title = { Text(communityName) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = userProfileState) {
                is Resource.Loading -> CircularProgressIndicator()
                is Resource.Success -> {
                    val user = state.data
                    if (user != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            if (user.profileImage != null) {
                                AsyncImage(
                                    model = user.profileImage,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(120.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (user.isVerified) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val displayLocality = user.locality.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            val displayDistrict = user.district.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            val displayState = user.state.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            
                            Text(
                                text = "$displayLocality, $displayDistrict",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            
                            Text(
                                text = displayState,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = {
                                    val inviteMessage = "Join me on Jamui One — the local community app for staying connected! Download here: https://play.google.com/store/apps/details?id=com.example.jamuione"
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
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Invite Neighbors")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = onNavigateToSavedPosts,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saved Posts")
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { showLogoutDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text("Logout")
                            }
                        }
                    } else {
                        // Guest mode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "You are browsing as a guest.",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sign in or create an account to post, create notices, and receive community updates.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = onLogout, // onLogout in Navigation goes to Login
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sign In / Create Account")
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${state.message}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.fetchUserProfile() }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
