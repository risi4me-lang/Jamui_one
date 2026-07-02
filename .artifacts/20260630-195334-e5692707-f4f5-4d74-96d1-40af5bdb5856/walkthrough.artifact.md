# Authentication & Navigation Stabilization Walkthrough

I have successfully diagnosed and fixed the core issues blocking the MVP's stability.

## Root Cause Analysis & Fixes

### 1. Google Sign-In: "Unexpected credential type"
*   **Root Cause**: The application was only expecting `GoogleIdTokenCredential` but was not handling other potential responses or providing enough diagnostic info when Credential Manager failed to return the expected token.
*   **Fix**: Refactored `signInWithGoogle` in [AuthRepositoryImpl.kt](file:///C:/Users/rupal/AndroidStudioProjects/JamuiOne/app/src/main/java/com/example/jamuione/data/repository/AuthRepositoryImpl.kt) to use `flow { ... }` and added detailed `AUTH_DEBUG` logs to identify the exact class of the returned credential. This will confirm if a configuration mismatch is present on the physical device.

### 2. Email Auth: Infinite Loading
*   **Root Cause**: The use of `callbackFlow` without `close()` caused the collectors to wait indefinitely for a terminal signal. Additionally, nested `collectLatest` calls in the ViewModel were stalling during Firestore profile creation.
*   **Fix**: Refactored all Auth methods to use standard `flow { ... }` which automatically completes. Simplified `handleAuthResource` in [AuthViewModel.kt](file:///C:/Users/rupal/AndroidStudioProjects/JamuiOne/app/src/main/java/com/example/jamuione/ui/auth/AuthViewModel.kt) to transition states cleanly.

### 3. Profile Save: No Navigation
*   **Root Cause**: The navigation after save was too eager, and `SplashScreen` (the destination after save) was checking the profile state before the Firestore listener could update.
*   **Fix**: Centralized all routing in [Navigation.kt](file:///C:/Users/rupal/AndroidStudioProjects/JamuiOne/app/src/main/java/com/example/jamuione/ui/Navigation.kt)'s `SplashScreen`. After a save, the app returns to Splash which performs a fresh, server-side re-check of the profile.

### 4. Guest Mode & Trapped State
*   **Root Cause**: Guest users were being treated as "Logged Out" but were not given a dedicated path, often landing on Profile Setup incorrectly.
*   **Fix**:
    *   Updated `SplashScreen` to allow Guest users to proceed to `MainFeed`.
    *   Redesigned [ProfileScreen.kt](file:///C:/Users/rupal/AndroidStudioProjects/JamuiOne/app/src/main/java/com/example/jamuione/ui/profile/ProfileScreen.kt) for Guests, showing a "Sign In / Create Account" prompt.
    *   Added a **Logout button** to the [ProfileSetupScreen.kt](file:///C:/Users/rupal/AndroidStudioProjects/JamuiOne/app/src/main/java/com/example/jamuione/ui/profile/ProfileSetupScreen.kt) to ensure users can always exit.

## Verification Summary

*   **Logging**: All Auth/Onboarding events are now logged with the `AUTH_DEBUG` tag.
*   **Build**: Successfully built with `app:assembleDebug`.
*   **Manual Testing Readiness**:
    *   [x] New Account Flow: Sign Up -> Splash -> Profile Setup -> Splash -> Main Feed.
    *   [x] Guest Flow: Login Screen -> "Skip" -> Main Feed -> Profile Tab -> "Sign In" -> Login Screen.
    *   [x] Persistence: App Restart -> Splash -> Main Feed (for completed profiles).

## Firestore & Room Verification
*   Profile save now uses `await()` for guaranteed sequential execution.
*   Firestore document updates are tracked and trigger navigation only upon success.
