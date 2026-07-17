package com.example.jamuione.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.jamuione.ui.auth.AuthViewModel
import com.example.jamuione.ui.auth.LoginScreen
import com.example.jamuione.ui.profile.ProfileSetupScreen
import com.example.jamuione.ui.profile.ProfileScreen
import com.example.jamuione.ui.profile.SavedPostsScreen
import com.example.jamuione.ui.legal.PrivacyPolicyScreen
import com.example.jamuione.ui.legal.TermsOfServiceScreen
import com.example.jamuione.ui.community.CommunitiesScreen
import com.example.jamuione.ui.community.NativeCommunityScreen
import com.example.jamuione.ui.community.NativeCommunityViewModel
import com.example.jamuione.ui.community.LocalityCommunityScreen
import com.example.jamuione.ui.community.DistrictCommunityScreen
import com.example.jamuione.ui.notifications.NotificationsScreen
import com.example.jamuione.ui.notifications.NotificationsViewModel
import com.example.jamuione.ui.organization.CreateOrganizationScreen
import com.example.jamuione.ui.organization.OrganizationDashboardScreen
import com.example.jamuione.ui.organization.OrganizationViewModel
import com.example.jamuione.ui.feed.FeedScreen
import com.example.jamuione.ui.feed.FeedViewModel
import com.example.jamuione.ui.feed.CreatePostScreen
import com.example.jamuione.ui.feed.PostDetailScreen
import com.example.jamuione.ui.feed.PostDetailViewModel
import com.example.jamuione.ui.notices.NoticeBoardScreen
import com.example.jamuione.ui.notices.NoticeViewModel
import com.example.jamuione.ui.notices.CreateNoticeScreen
import com.example.jamuione.util.BrandingUtil
import com.example.jamuione.util.Resource
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination : NavKey {
    @Serializable
    data object Splash : Destination
    @Serializable
    data object Login : Destination
    @Serializable
    data object ProfileSetup : Destination
    @Serializable
    data object MainFeed : Destination
    @Serializable
    data object CreatePost : Destination
    @Serializable
    data object NoticeBoard : Destination
    @Serializable
    data object CreateNotice : Destination
    @Serializable
    data object Profile : Destination
    @Serializable
    data object SavedPosts : Destination
    @Serializable
    data object Communities : Destination
    @Serializable
    data object NativeCommunity : Destination
    @Serializable
    data object LocalityCommunity : Destination
    @Serializable
    data object DistrictCommunity : Destination
    @Serializable
    data object Notifications : Destination
    @Serializable
    data object CreateOrganization : Destination
    @Serializable
    data class OrganizationDashboard(val orgId: String) : Destination
    @Serializable
    data object PrivacyPolicy : Destination
    @Serializable
    data object TermsOfService : Destination
    @Serializable
    data class PostDetail(val postId: String) : Destination
}

@Composable
fun JamuiOneNavigation(initialPostId: String? = null) {
    val backStack = rememberNavBackStack(Destination.Splash as NavKey)

    LaunchedEffect(initialPostId) {
        if (initialPostId != null) {
            // If we are already past Splash/Login, navigate immediately
            val current = backStack.lastOrNull()
            if (current is Destination.MainFeed || current is Destination.Profile || current is Destination.NoticeBoard) {
                backStack.add(Destination.PostDetail(initialPostId))
            }
        }
    }

    val myEntryProvider = entryProvider<NavKey> {
        entry<Destination.Splash> {
            val authViewModel: AuthViewModel = viewModel()
            SplashScreen(
                authViewModel = authViewModel,
                onNavigate = { nextKey ->
                    Log.d("AUTH_TRACE", "Navigation triggered: Splash to $nextKey")
                    backStack.clear()
                    backStack.add(nextKey)
                    
                    // If we have an initialPostId and we just authenticated, jump to detail
                    if (initialPostId != null && nextKey == Destination.MainFeed) {
                        backStack.add(Destination.PostDetail(initialPostId))
                    }
                }
            )
        }
        entry<Destination.Login> {
            val authViewModel: AuthViewModel = viewModel()
            LoginScreen(
                viewModel = authViewModel,
                onAuthSuccess = { 
                    Log.d("AUTH_TRACE", "onAuthSuccess callback in LoginScreen, returning to Splash")
                    backStack.clear()
                    backStack.add(Destination.Splash)
                },
                onSkipLogin = {
                    Log.d("AUTH_TRACE", "Guest mode: skipping login")
                    backStack.clear()
                    backStack.add(Destination.MainFeed)
                },
                onViewPrivacyPolicy = { backStack.add(Destination.PrivacyPolicy) },
                onViewTermsOfService = { backStack.add(Destination.TermsOfService) }
            )
        }
        entry<Destination.ProfileSetup> {
            val authViewModel: AuthViewModel = viewModel()
            ProfileSetupScreen(
                viewModel = authViewModel,
                onProfileSaved = {
                    Log.d("AUTH_TRACE", "Profile saved, returning to Splash")
                    backStack.clear()
                    backStack.add(Destination.Splash)
                }
            )
        }
        entry<Destination.MainFeed> {
            val feedViewModel: FeedViewModel = viewModel()
            AdaptiveScaffoldWrapper(
                currentDestination = Destination.MainFeed,
                onNavigate = { 
                    backStack.clear()
                    backStack.add(it) 
                }
            ) {
                FeedScreen(
                    viewModel = feedViewModel,
                    onCreatePostClick = { backStack.add(Destination.CreatePost) },
                    onNavigateToDetail = { postId -> backStack.add(Destination.PostDetail(postId)) },
                    onNavigateToNotifications = { backStack.add(Destination.Notifications) },
                    onProfileClick = { 
                        backStack.clear()
                        backStack.add(Destination.Profile)
                    }
                )
            }
        }
        entry<Destination.NoticeBoard> {
            val noticeViewModel: NoticeViewModel = viewModel()
            AdaptiveScaffoldWrapper(
                currentDestination = Destination.NoticeBoard,
                onNavigate = { 
                    backStack.clear()
                    backStack.add(it) 
                }
            ) {
                NoticeBoardScreen(
                    viewModel = noticeViewModel,
                    onCreateNoticeClick = { backStack.add(Destination.CreateNotice) },
                    onNavigateToNotifications = { backStack.add(Destination.Notifications) },
                    onProfileClick = {
                        backStack.clear()
                        backStack.add(Destination.Profile)
                    }
                )
            }
        }
        entry<Destination.Profile> {
            val authViewModel: AuthViewModel = viewModel()
            AdaptiveScaffoldWrapper(
                currentDestination = Destination.Profile,
                onNavigate = { 
                    backStack.clear()
                    backStack.add(it) 
                }
            ) {
                ProfileScreen(
                    viewModel = authViewModel,
                    onNavigateToSavedPosts = { backStack.add(Destination.SavedPosts) },
                    onNavigateToCommunities = { backStack.add(Destination.Communities) },
                    onNavigateToCreateOrg = { backStack.add(Destination.CreateOrganization) },
                    onEditProfile = { backStack.add(Destination.ProfileSetup) },
                    onViewPrivacyPolicy = { backStack.add(Destination.PrivacyPolicy) },
                    onViewTermsOfService = { backStack.add(Destination.TermsOfService) },
                    onLogout = {
                        Log.d("AUTH_TRACE", "Logout triggered from Profile, returning to Login")
                        backStack.clear()
                        backStack.add(Destination.Login)
                    }
                )
            }
        }
        entry<Destination.Communities> {
            val authViewModel: AuthViewModel = viewModel()
            val orgViewModel: OrganizationViewModel = viewModel()
            CommunitiesScreen(
                authViewModel = authViewModel,
                orgViewModel = orgViewModel,
                onNavigateToNativeCommunity = { backStack.add(Destination.NativeCommunity) },
                onNavigateToLocalityCommunity = { backStack.add(Destination.LocalityCommunity) },
                onNavigateToDistrictCommunity = { backStack.add(Destination.DistrictCommunity) },
                onNavigateToOrgDashboard = { orgId -> backStack.add(Destination.OrganizationDashboard(orgId)) },
                onBack = { backStack.removeAt(backStack.size - 1) }
            )
        }
        entry<Destination.NativeCommunity> {
            val communityViewModel: NativeCommunityViewModel = viewModel()
            NativeCommunityScreen(
                viewModel = communityViewModel,
                onBack = { backStack.removeAt(backStack.size - 1) }
            )
        }
        entry<Destination.LocalityCommunity> {
            val communityViewModel: NativeCommunityViewModel = viewModel()
            LocalityCommunityScreen(
                viewModel = communityViewModel,
                onBack = { backStack.removeAt(backStack.size - 1) }
            )
        }
        entry<Destination.DistrictCommunity> {
            val communityViewModel: NativeCommunityViewModel = viewModel()
            DistrictCommunityScreen(
                viewModel = communityViewModel,
                onBack = { backStack.removeAt(backStack.size - 1) }
            )
        }
        entry<Destination.Notifications> {
            val notificationsViewModel: NotificationsViewModel = viewModel()
            NotificationsScreen(
                viewModel = notificationsViewModel,
                onNavigateToPost = { postId -> backStack.add(Destination.PostDetail(postId)) },
                onNavigateToNotice = { noticeId -> 
                    // For now, jumping to notice board might be enough, or we need a NoticeDetail
                    // Given our current structure, jumping to board is fine, or we could just stay
                    backStack.add(Destination.NoticeBoard)
                },
                onBack = { backStack.removeAt(backStack.size - 1) }
            )
        }
        entry<Destination.PrivacyPolicy> {
            PrivacyPolicyScreen(onBack = { backStack.removeAt(backStack.size - 1) })
        }
        entry<Destination.TermsOfService> {
            TermsOfServiceScreen(onBack = { backStack.removeAt(backStack.size - 1) })
        }
        entry<Destination.SavedPosts> {
            val feedViewModel: FeedViewModel = viewModel()
            SavedPostsScreen(
                viewModel = feedViewModel,
                onNavigateToDetail = { postId -> backStack.add(Destination.PostDetail(postId)) },
                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
            )
        }
        entry<Destination.CreatePost> {
            val feedViewModel: FeedViewModel = viewModel()
            CreatePostScreen(
                viewModel = feedViewModel,
                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
            )
        }
        entry<Destination.CreateNotice> {
            val noticeViewModel: NoticeViewModel = viewModel()
            CreateNoticeScreen(
                viewModel = noticeViewModel,
                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
            )
        }
        entry<Destination.CreateOrganization> {
            val orgViewModel: OrganizationViewModel = viewModel()
            CreateOrganizationScreen(
                viewModel = orgViewModel,
                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                onSuccess = { orgId ->
                    backStack.removeAt(backStack.size - 1)
                    backStack.add(Destination.OrganizationDashboard(orgId))
                }
            )
        }
        entry<Destination.OrganizationDashboard> { key ->
            val orgViewModel: OrganizationViewModel = viewModel()
            OrganizationDashboardScreen(
                orgId = key.orgId,
                viewModel = orgViewModel,
                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
            )
        }
        entry<Destination.PostDetail> { key ->
            val postDetailViewModel: PostDetailViewModel = viewModel()
            val feedViewModel: FeedViewModel = viewModel()
            PostDetailScreen(
                postId = key.postId,
                viewModel = postDetailViewModel,
                feedViewModel = feedViewModel,
                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
            )
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
        entryProvider = myEntryProvider
    )
}

@Composable
fun AdaptiveScaffoldWrapper(
    currentDestination: Destination,
    onNavigate: (Destination) -> Unit,
    content: @Composable () -> Unit
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                selected = currentDestination == Destination.MainFeed,
                onClick = { onNavigate(Destination.MainFeed) },
                icon = { Icon(Icons.Default.Home, contentDescription = "Feed") },
                label = { Text("Feed") }
            )
            item(
                selected = currentDestination == Destination.NoticeBoard,
                onClick = { onNavigate(Destination.NoticeBoard) },
                icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Events") },
                label = { Text("Events") }
            )
            item(
                selected = currentDestination == Destination.Profile,
                onClick = { onNavigate(Destination.Profile) },
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") }
            )
        }
    ) {
        content()
    }
}

@Composable
fun SplashScreen(
    authViewModel: AuthViewModel,
    onNavigate: (Destination) -> Unit
) {
    val userProfile by authViewModel.userProfile.collectAsState()
    
    LaunchedEffect(Unit) {
        Log.d("AUTH_TRACE", "Splash decision: Checking auth status")
        if (authViewModel.isUserLoggedIn()) {
            Log.d("AUTH_TRACE", "Splash decision: User logged in, fetching profile")
            authViewModel.fetchUserProfile()
        } else {
            Log.d("AUTH_TRACE", "Splash decision: No user logged in, going to Login")
            onNavigate(Destination.Login)
        }
    }
    
    LaunchedEffect(userProfile) {
        when (userProfile) {
            is Resource.Success -> {
                val profile = userProfile.data
                Log.d("AUTH_TRACE", "Splash decision: Profile check Success. profileCompleted=${profile?.profileCompleted}")
                if (profile == null) {
                    Log.d("AUTH_TRACE", "Splash decision: Profile null, going to ProfileSetup")
                    onNavigate(Destination.ProfileSetup)
                } else if (!profile.profileCompleted) {
                    Log.d("AUTH_TRACE", "Splash decision: Profile incomplete, going to ProfileSetup")
                    onNavigate(Destination.ProfileSetup)
                } else {
                    Log.d("AUTH_TRACE", "Main feed opened")
                    onNavigate(Destination.MainFeed)
                }
            }
            is Resource.Error -> {
                Log.e("AUTH_TRACE", "Splash decision: Profile fetch Error: ${userProfile.message}")
                onNavigate(Destination.Login)
            }
            is Resource.Loading -> {
                Log.d("AUTH_TRACE", "Splash decision: Profile fetch is Loading")
            }
            else -> {}
        }
    }

    val communityName = BrandingUtil.getCommunityName(userProfile.data?.district)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = communityName,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
        }
    }
}
