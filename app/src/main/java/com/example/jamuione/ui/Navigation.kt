package com.example.jamuione.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.example.jamuione.ui.profile.AccountSettingsScreen
import com.example.jamuione.ui.profile.other.UserProfileScreen
import com.example.jamuione.ui.profile.other.UserProfileViewModel
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
import com.example.jamuione.ui.organization.OrganizationProfileScreen
import com.example.jamuione.ui.organization.OrganizationViewModel
import com.example.jamuione.ui.feed.FeedScreen
import com.example.jamuione.ui.travel.TravelScreen
import com.example.jamuione.ui.travel.TravelViewModel
import com.example.jamuione.ui.travel.PostTripScreen
import com.example.jamuione.ui.travel.PostTripViewModel
import com.example.jamuione.ui.travel.MyTripsScreen
import com.example.jamuione.ui.travel.MyTripsViewModel
import com.example.jamuione.ui.travel.RequestRideScreen
import com.example.jamuione.ui.travel.RequestRideViewModel
import com.example.jamuione.ui.feed.FeedViewModel
import com.example.jamuione.ui.feed.CreatePostScreen
import com.example.jamuione.ui.feed.PostDetailScreen
import com.example.jamuione.ui.feed.PostDetailViewModel
import com.example.jamuione.ui.notices.NoticeBoardScreen
import com.example.jamuione.ui.notices.NoticeViewModel
import com.example.jamuione.ui.notices.CreateNoticeScreen
import com.example.jamuione.util.BrandingUtil
import com.example.jamuione.util.Resource
import com.example.jamuione.util.TravelConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

private fun <T> MutableList<T>.pop() {
    if (this.size > 1) {
        this.removeAt(this.size - 1)
    }
}

private fun navigateToTab(backStack: MutableList<NavKey>, dest: Destination) {
    backStack.clear()
    backStack.add(Destination.MainFeed() as NavKey)
    if (dest !is Destination.MainFeed) {
        backStack.add(dest as NavKey)
    }
}

@Serializable
sealed interface Destination : NavKey {
    @Serializable
    data object Splash : Destination
    @Serializable
    data object Login : Destination
    @Serializable
    data object ProfileSetup : Destination
    @Serializable
    data class MainFeed(val highlightPostId: String? = null) : Destination
    @Serializable
    data object CreatePost : Destination
    @Serializable
    data class NoticeBoard(val highlightId: String? = null, val category: String? = null) : Destination
    @Serializable
    data object CreateNotice : Destination
    @Serializable
    data object Profile : Destination
    @Serializable
    data class UserProfile(val userId: String) : Destination
    @Serializable
    data object SavedPosts : Destination
    @Serializable
    data object Communities : Destination
    @Serializable
    data class OrganizationProfile(val orgId: String) : Destination
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
    data object AccountSettings : Destination
    @Serializable
    data class OrganizationDashboard(val orgId: String) : Destination
    @Serializable
    data class CreateOrganizationAnnouncement(val orgId: String) : Destination
    @Serializable
    data object PrivacyPolicy : Destination
    @Serializable
    data object TermsOfService : Destination
    @Serializable
    data class PostDetail(val postId: String) : Destination
    @Serializable
    data class CreateNoticeCategory(val category: String) : Destination
    @Serializable
    data class Travel(val initialTab: Int = 0) : Destination
    @Serializable
    data object PostTrip : Destination
    @Serializable
    data object MyTrips : Destination
    @Serializable
    data object RequestRide : Destination
}

@Composable
fun JamuiOneNavigation(initialPostId: String? = null) {
    val backStack = rememberNavBackStack(Destination.Splash as NavKey)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialPostId) {
        if (initialPostId != null) {
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
                    
                    if (initialPostId != null && nextKey is Destination.MainFeed) {
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
                    backStack.add(Destination.MainFeed())
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
        entry<Destination.MainFeed> { key ->
            val feedViewModel: FeedViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            val isAdmin by authViewModel.isAdmin.collectAsState()
            AdaptiveScaffoldWrapper(
                currentDestination = key,
                onNavigate = { navigateToTab(backStack, it) },
                onCreate = { backStack.add(it) },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) {
                FeedScreen(
                    viewModel = feedViewModel,
                    isAdmin = isAdmin,
                    onCreatePostClick = { backStack.add(Destination.CreatePost) },
                    onNavigateToDetail = { postId -> backStack.add(Destination.PostDetail(postId)) },
                    onNavigateToNotifications = { navigateToTab(backStack, Destination.Notifications) },
                    onProfileClick = { backStack.add(Destination.Profile) },
                    onNavigateToUserProfile = { userId -> backStack.add(Destination.UserProfile(userId)) },
                    onNavigateToTravel = { navigateToTab(backStack, Destination.Travel()) },
                    onCreateNoticeCategory = { category -> backStack.add(Destination.CreateNoticeCategory(category)) },
                    onNavigateToNoticeBoardCategory = { category -> backStack.add(Destination.NoticeBoard(category = category)) },
                    highlightPostId = key.highlightPostId
                )
            }
        }
        entry<Destination.NoticeBoard> { key ->
            val noticeViewModel: NoticeViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            val isAdmin by authViewModel.isAdmin.collectAsState()

            LaunchedEffect(key.category) {
                if (key.category != null) {
                    noticeViewModel.setCategory(key.category)
                }
            }

            AdaptiveScaffoldWrapper(
                currentDestination = key,
                onNavigate = { navigateToTab(backStack, it) },
                onCreate = { backStack.add(it) },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) {
                NoticeBoardScreen(
                    viewModel = noticeViewModel,
                    isAdmin = isAdmin,
                    highlightNoticeId = key.highlightId,
                    onCreateNoticeClick = { backStack.add(Destination.CreateNotice) },
                    onNavigateToNotifications = { navigateToTab(backStack, Destination.Notifications) },
                    onProfileClick = { backStack.add(Destination.Profile) },
                    onNavigateToTravel = { navigateToTab(backStack, Destination.Travel()) }
                )
            }
        }
        entry<Destination.Profile> {
            val authViewModel: AuthViewModel = viewModel()
            val isAdmin by authViewModel.isAdmin.collectAsState()
            val isOperator by authViewModel.isOperator.collectAsState()
            AdaptiveScaffoldWrapper(
                currentDestination = Destination.Profile,
                onNavigate = { navigateToTab(backStack, it) },
                onCreate = { backStack.add(it) },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) {
                ProfileScreen(
                    viewModel = authViewModel,
                    isAdmin = isAdmin,
                    isOperator = isOperator,
                    onNavigateToSavedPosts = { backStack.add(Destination.SavedPosts) },
                    onNavigateToCommunities = { backStack.add(Destination.Communities) },
                    onNavigateToMyTrips = { backStack.add(Destination.MyTrips) },
                    onNavigateToAccountSettings = { backStack.add(Destination.AccountSettings) },
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
        entry<Destination.Travel> { key ->
            val travelViewModel: TravelViewModel = viewModel()
            AdaptiveScaffoldWrapper(
                currentDestination = key,
                onNavigate = { navigateToTab(backStack, it) },
                onCreate = { backStack.add(it) },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) {
                TravelScreen(
                    viewModel = travelViewModel,
                    onRequestRideClick = { backStack.add(Destination.RequestRide) },
                    onPostTripClick = { backStack.add(Destination.PostTrip) },
                    onMyTripsClick = { backStack.add(Destination.MyTrips) },
                    initialTab = key.initialTab,
                    onBack = { backStack.pop() }
                )
            }
        }
        entry<Destination.PostTrip> {
            val postTripViewModel: PostTripViewModel = viewModel()
            PostTripScreen(
                viewModel = postTripViewModel,
                onBack = { backStack.pop() },
                onSuccess = { backStack.pop() }
            )
        }
        entry<Destination.MyTrips> {
            val myTripsViewModel: MyTripsViewModel = viewModel()
            MyTripsScreen(
                viewModel = myTripsViewModel,
                onBack = { backStack.pop() }
            )
        }
        entry<Destination.RequestRide> {
            val requestRideViewModel: RequestRideViewModel = viewModel()
            RequestRideScreen(
                viewModel = requestRideViewModel,
                onBack = { backStack.pop() },
                onSuccess = {
                    backStack.clear()
                    backStack.add(Destination.Travel(initialTab = 1))
                    scope.launch {
                        snackbarHostState.showSnackbar("Request posted")
                    }
                }
            )
        }
        entry<Destination.AccountSettings> {
            val authViewModel: AuthViewModel = viewModel()
            AccountSettingsScreen(
                onLogout = {
                    authViewModel.logout()
                    backStack.clear()
                    backStack.add(Destination.Login)
                },
                onDeleteAccount = {
                    authViewModel.deleteAccount()
                    backStack.clear()
                    backStack.add(Destination.Login)
                },
                onBack = { backStack.pop() }
            )
        }
        entry<Destination.UserProfile> { key ->
            val userProfileViewModel: UserProfileViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            val isAdmin by authViewModel.isAdmin.collectAsState()
            UserProfileScreen(
                userId = key.userId,
                viewModel = userProfileViewModel,
                isAdmin = isAdmin,
                onBack = { backStack.pop() }
            )
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
                onNavigateToCreateOrg = { backStack.add(Destination.CreateOrganization) },
                onNavigateToOrgDashboard = { orgId -> backStack.add(Destination.OrganizationDashboard(orgId)) },
                onNavigateToOrgProfile = { orgId -> backStack.add(Destination.OrganizationProfile(orgId)) },
                onBack = { backStack.pop() }
            )
        }
        entry<Destination.OrganizationProfile> { key ->
            val orgViewModel: OrganizationViewModel = viewModel()
            OrganizationProfileScreen(
                orgId = key.orgId,
                viewModel = orgViewModel,
                onBack = { backStack.pop() }
            )
        }
        entry<Destination.NativeCommunity> {
            val communityViewModel: NativeCommunityViewModel = viewModel()
            NativeCommunityScreen(
                viewModel = communityViewModel,
                onNavigateToProfile = { userId -> backStack.add(Destination.UserProfile(userId)) },
                onBack = { backStack.pop() }
            )
        }
        entry<Destination.LocalityCommunity> {
            val communityViewModel: NativeCommunityViewModel = viewModel()
            LocalityCommunityScreen(
                viewModel = communityViewModel,
                onNavigateToProfile = { userId -> backStack.add(Destination.UserProfile(userId)) },
                onBack = { backStack.pop() }
            )
        }
        entry<Destination.DistrictCommunity> {
            val communityViewModel: NativeCommunityViewModel = viewModel()
            DistrictCommunityScreen(
                viewModel = communityViewModel,
                onNavigateToProfile = { userId -> backStack.add(Destination.UserProfile(userId)) },
                onBack = { backStack.pop() }
            )
        }
        entry<Destination.Notifications> { key ->
            val notificationsViewModel: NotificationsViewModel = viewModel()
            AdaptiveScaffoldWrapper(
                currentDestination = key,
                onNavigate = { navigateToTab(backStack, it) },
                onCreate = { backStack.add(it) },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) {
                NotificationsScreen(
                    viewModel = notificationsViewModel,
                    onNavigateToPost = { postId -> backStack.add(Destination.PostDetail(postId)) },
                    onNavigateToNotice = { noticeId ->
                        backStack.add(Destination.NoticeBoard(highlightId = noticeId))
                    },
                    onBack = { backStack.pop() },
                    showBackArrow = false
                )
            }
        }
        entry<Destination.PrivacyPolicy> {
            PrivacyPolicyScreen(onBack = { backStack.pop() })
        }
        entry<Destination.TermsOfService> {
            TermsOfServiceScreen(onBack = { backStack.pop() })
        }
        entry<Destination.SavedPosts> {
            val feedViewModel: FeedViewModel = viewModel()
            SavedPostsScreen(
                viewModel = feedViewModel,
                onNavigateToDetail = { postId -> backStack.add(Destination.PostDetail(postId)) },
                onBack = { backStack.pop() },
                onNavigateToUserProfile = { userId -> backStack.add(Destination.UserProfile(userId)) }
            )
        }
        entry<Destination.CreatePost> {
            val feedViewModel: FeedViewModel = viewModel()
            CreatePostScreen(
                viewModel = feedViewModel,
                onBack = { backStack.pop() },
                onSuccess = { locality, postId ->
                    backStack.clear()
                    backStack.add(Destination.MainFeed(highlightPostId = postId))
                    scope.launch {
                        snackbarHostState.showSnackbar("🎉 Your post is now live in $locality")
                    }
                }
            )
        }
        entry<Destination.CreateNotice> {
            val noticeViewModel: NoticeViewModel = viewModel()
            CreateNoticeScreen(
                viewModel = noticeViewModel,
                onBack = { backStack.pop() }
            )
        }
        entry<Destination.CreateNoticeCategory> { key ->
            val noticeViewModel: NoticeViewModel = viewModel()
            CreateNoticeScreen(
                viewModel = noticeViewModel,
                preselectedCategory = key.category,
                onBack = { backStack.pop() }
            )
        }
        entry<Destination.CreateOrganization> {
            val orgViewModel: OrganizationViewModel = viewModel()
            CreateOrganizationScreen(
                viewModel = orgViewModel,
                onBack = { backStack.pop() },
                onSuccess = { orgId ->
                    backStack.clear()
                    backStack.add(Destination.OrganizationDashboard(orgId))
                }
            )
        }
        entry<Destination.OrganizationDashboard> { key ->
            val orgViewModel: OrganizationViewModel = viewModel()
            OrganizationDashboardScreen(
                orgId = key.orgId,
                viewModel = orgViewModel,
                onNavigateToCreateAnnouncement = { backStack.add(Destination.CreateOrganizationAnnouncement(key.orgId)) },
                onBack = { backStack.pop() }
            )
        }
        entry<Destination.CreateOrganizationAnnouncement> { key ->
            val orgViewModel: OrganizationViewModel = viewModel()
            com.example.jamuione.ui.organization.CreateOrganizationAnnouncementScreen(
                orgId = key.orgId,
                viewModel = orgViewModel,
                onBack = { backStack.pop() }
            )
        }
        entry<Destination.PostDetail> { key ->
            val postDetailViewModel: PostDetailViewModel = viewModel()
            val feedViewModel: FeedViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            val isAdmin by authViewModel.isAdmin.collectAsState()
            PostDetailScreen(
                postId = key.postId,
                viewModel = postDetailViewModel,
                feedViewModel = feedViewModel,
                isAdmin = isAdmin,
                onBack = { backStack.pop() },
                onNavigateToUserProfile = { userId -> backStack.add(Destination.UserProfile(userId)) }
            )
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.pop() },
        entryProvider = myEntryProvider
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveScaffoldWrapper(
    currentDestination: Destination,
    onNavigate: (Destination) -> Unit,
    onCreate: (Destination) -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    val notificationsViewModel: NotificationsViewModel = viewModel()
    val hasUnread by notificationsViewModel.hasUnread.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                // 1. Home
                val isHomeSelected = currentDestination is Destination.MainFeed
                NavigationBarItem(
                    selected = isHomeSelected,
                    onClick = { onNavigate(Destination.MainFeed()) },
                    icon = {
                        Icon(
                            imageVector = if (isHomeSelected) Icons.Default.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { 
                        Text(
                            text = "Home", 
                            color = if (isHomeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                )

                // 2. Board
                val isBoardSelected = currentDestination is Destination.NoticeBoard
                NavigationBarItem(
                    selected = isBoardSelected,
                    onClick = { onNavigate(Destination.NoticeBoard()) },
                    icon = {
                        Icon(
                            imageVector = if (isBoardSelected) Icons.AutoMirrored.Filled.Assignment else Icons.AutoMirrored.Outlined.Assignment,
                            contentDescription = "Board"
                        )
                    },
                    label = { 
                        Text(
                            text = "Board", 
                            color = if (isBoardSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                )

                // 3. Create (Centre Button)
                NavigationBarItem(
                    selected = false,
                    onClick = { showCreateSheet = true },
                    icon = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(46.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "Create", modifier = Modifier.size(24.dp))
                            }
                        }
                    },
                    label = { 
                        Text(
                            text = "Create",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent
                    )
                )

                // 4. Alerts
                val isAlertsSelected = currentDestination is Destination.Notifications
                NavigationBarItem(
                    selected = isAlertsSelected,
                    onClick = { onNavigate(Destination.Notifications) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (hasUnread) {
                                    Badge(modifier = Modifier.size(8.dp))
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isAlertsSelected) Icons.Default.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "Alerts"
                            )
                        }
                    },
                    label = { 
                        Text(
                            text = "Alerts", 
                            color = if (isAlertsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                )

                // 5. Travel
                val isTravelSelected = currentDestination is Destination.Travel
                NavigationBarItem(
                    selected = isTravelSelected,
                    onClick = { onNavigate(Destination.Travel()) },
                    icon = {
                        Icon(
                            imageVector = if (isTravelSelected) Icons.Default.DirectionsCar else Icons.Outlined.DirectionsCar,
                            contentDescription = "Travel"
                        )
                    },
                    label = { 
                        Text(
                            text = "Travel",
                            color = if (isTravelSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            content()
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                snackbarHost()
            }
        }
    }

    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "What do you want to share?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                CreateActionRow(
                    icon = Icons.Default.PostAdd,
                    title = "Post",
                    subtitle = "Share updates with your community",
                    onClick = {
                        showCreateSheet = false
                        onCreate(Destination.CreatePost)
                    }
                )
                CreateActionRow(
                    icon = Icons.Default.Campaign,
                    title = "Notice",
                    subtitle = "Post announcements and events",
                    onClick = {
                        showCreateSheet = false
                        onCreate(Destination.CreateNotice)
                    }
                )
                CreateActionRow(
                    icon = Icons.Default.Bloodtype,
                    title = "Blood request",
                    subtitle = "Request or offer blood donations",
                    onClick = {
                        showCreateSheet = false
                        onCreate(Destination.CreateNoticeCategory("Blood Donation"))
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SplashScreen(
    authViewModel: AuthViewModel,
    onNavigate: (Destination) -> Unit
) {
    val userProfile by authViewModel.userProfile.collectAsState()
    var showTimeoutError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("AUTH_TRACE", "Splash decision: Checking auth status")
        if (authViewModel.isUserLoggedIn()) {
            authViewModel.fetchUserProfile()
        } else {
            onNavigate(Destination.Login)
        }
    }

    LaunchedEffect(userProfile) {
        when (userProfile) {
            is Resource.Success -> {
                showTimeoutError = false
                val profile = userProfile.data
                if (profile == null || !profile.profileCompleted) {
                    onNavigate(Destination.ProfileSetup)
                } else {
                    onNavigate(Destination.MainFeed())
                }
            }
            is Resource.Error -> onNavigate(Destination.Login)
            else -> {}
        }
    }

    LaunchedEffect(userProfile) {
        if (userProfile is Resource.Loading || userProfile is Resource.Idle) {
            delay(8000L)
            if (userProfile is Resource.Loading || userProfile is Resource.Idle) {
                showTimeoutError = true
            }
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
            if (showTimeoutError) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Taking longer than usual",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        showTimeoutError = false
                        authViewModel.fetchUserProfile()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Try again")
                }
            }
        }
    }
}

@Composable
fun CreateActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
