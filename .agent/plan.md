# Project Plan

Build an Android application named "District One".
Architecture: MVVM, Jetpack Compose, Material 3, Repository Pattern, Firebase backend (Authentication, Firestore, Storage, FCM).
Features:
1. Authentication: Google Sign-In (Firebase), Session persistence, Logout.
2. User Profile: Name (pre-filled), Email (pre-filled), State, District, Locality, profileCompleted flag, role, fcmToken, isActive, profileImage (pre-filled).
3. News Feed: View posts, Create posts, Image upload, Like, Comment, Report.
4. Notice Board: Categories (Announcement, Jobs, Rent/Flatmate, Buy & Sell, Lost & Found, Blood Donation, Help Needed). Title, Description, Category, Expiry Date.
5. Notifications: New post in locality, New notice in district.
Bottom Navigation: Feed, Notice Board, Create Post, Notifications, Profile.
Requirements: Clean architecture, Hilt, Error handling, Loading states, Offline caching, Dark mode, Production-ready code.

## Project Brief

# Project Brief: District One

A localized community platform that connects residents with their immediate locality and district. District One provides a streamlined experience for staying updated with local news, finding essential services, and receiving critical community notices.

## Features

1.  **Seamless Google Authentication**: Instant onboarding using Google Sign-In via Firebase and Credential Manager. The app automatically populates user profiles with name, email, and profile images for a frictionless start.
2.  **Conditional Profile Setup**: A smart onboarding flow that detects profile completion status. New users are guided through a one-time setup to select their State, District, and Locality (Ward/Mohalla/Village/Society) before accessing the main feed.
3.  **Community News Feed**: A real-time feed where residents can post updates, upload photos of local events, and interact through likes and comments. Includes a reporting system for community moderation.
4.  **Categorized Notice Board**: A dedicated space for essential local information including Jobs, Rent/Flatmate listings, Buy & Sell, and urgent community needs like Blood Donation or Help Needed.
5.  **Locality-Aware Notifications**: Intelligent push notifications that alert users to new posts in their specific locality or important notices at the district level.

## High-Level Technical Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose with Material Design 3 (M3)
*   **Navigation**: Jetpack Navigation 3 (State-driven navigation logic)
*   **Adaptive Strategy**: Compose Material Adaptive library for multi-pane and responsive layouts across all screen sizes.
*   **Architecture**: MVVM with Repository Pattern and Clean Architecture.
*   **Backend**: Firebase (Authentication, Firestore, Storage, FCM).
*   **Dependency Injection**: Hilt.
*   **Persistence**: Room Database for robust offline caching.
*   **Asynchronous Logic**: Kotlin Coroutines and Flow.
*   **Auth Integration**: Google Credential Manager.

## Implementation Steps

### Task_1_BaseSetup: Configure project infrastructure: Add Hilt and Firebase dependencies, initialize Room database, and set up the Material 3 theme with vibrant colors and Edge-to-Edge support.
- **Status:** COMPLETED
- **Updates:** Hilt and Firebase (Auth, Firestore, Storage, Messaging) integrated. Room database initialized with User and Post entities. Material 3 theme with vibrant colors and Edge-to-Edge support implemented. Adaptive app icon created. Project builds successfully. Note: Placeholder google-services.json added; user needs to replace it for actual Firebase functionality.
- **Acceptance Criteria:**
  - Project builds successfully
  - Hilt and Firebase are integrated
  - Base Material 3 theme is applied

### Task_2_Auth_Profile_Flow: Implement Phone OTP Authentication and User Profile setup. Use the expanded User model (State, District, Locality, Role, profileCompleted, fcmToken) and implement conditional navigation flow (Auth -> Profile Setup -> Main Feed). Profile picture upload is excluded.
- **Status:** COMPLETED
- **Updates:** Fixed critical auth blocker: Updated Resource.kt with an Idle state and re-initialized AuthViewModel states to Resource.Idle(). Buttons are now correctly enabled for user interaction. Build successful.
- **Acceptance Criteria:**
  - Phone OTP login works
  - User model correctly saved to Firestore
  - Conditional flow correctly routes new vs existing users

### Task_3_NewsFeed_Localized: Build the News Feed with localization-based filtering (Locality/District/State). Implement post creation with image uploads and Room-based offline caching for the feed.
- **Status:** COMPLETED
- **Updates:** News Feed implemented with localized filtering (Locality/District/State). Post creation with image uploads using Firebase Storage and Firestore. Offline caching implemented via Room database. UI follows Material 3 guidelines and supports cascading location scopes. Build successful with real Firebase configuration.
- **Acceptance Criteria:**
  - Feed displays localized content
  - Post creation with images works
  - Offline caching functional via Room

### Task_4_NoticeBoard_FCM: Develop the Categorized Notice Board with localized filtering and auto-expiry logic. Integrate Firebase Cloud Messaging (FCM) to trigger localized notifications.
- **Status:** COMPLETED
- **Updates:** Categorized Notice Board implemented with categories like Jobs, Blood Donation, Buy/Sell, etc. Auto-expiry logic integrated using Firestore timestamps. Localized filtering (Locality/District/State) applied. Firebase Cloud Messaging (FCM) integrated with location-based topic subscriptions (e.g., notices_district_name). Persistent Bottom Navigation Bar added. Build successful.
- **Acceptance Criteria:**
  - Notice board filtered by location
  - Notices expire correctly
  - FCM notifications received for localized updates

### Task_5_Refactor_Google_Auth: Refactor Authentication to Google Sign-In using Firebase and Credential Manager. Update the User model to include email, isActive, and profileImage (pre-filled from Google), and remove all Phone OTP logic and UI components.
- **Status:** COMPLETED
- **Updates:** Refactored Authentication from Phone OTP to Google Sign-In using Firebase and Credential Manager. Updated the User model with email, isActive, and profileImage. Removed all legacy Phone OTP screens, logic, and ViewModel states. Implementation includes session persistence and a pre-filled profile setup flow. Build successful. Note: User needs to update YOUR_WEB_CLIENT_ID in AuthRepositoryImpl.kt.
- **Acceptance Criteria:**
  - Google Sign-In integrated successfully
  - User model updated with email and profileImage
  - Phone OTP code and UI components removed
- **Duration:** N/A

### Task_6_Verify_Finalize: Update News Feed and Notice Board to ensure compatibility with the new User structure. Finalize the navigation flow (Splash -> Login -> Profile Setup -> Feed) and perform a final run to verify stability and Material 3 adaptive UI.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - News Feed and Notice Board work with new user model
  - Adaptive UI works across screen sizes
  - make sure all existing tests pass
  - build pass
  - app does not crash
- **StartTime:** 2026-06-29 23:54:11 IST

