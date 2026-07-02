# Authentication & Navigation Stabilization Plan

Focus on fixing the core auth and onboarding flow without adding new features.

## Proposed Changes

### Data Layer

#### [AuthRepositoryImpl.kt](file:///C:/Users/rupal/AndroidStudioProjects/JamuiOne/app/src/main/java/com/example/jamuione/data/repository/AuthRepositoryImpl.kt)
- Refactor `signInWithGoogle`, `signInWithEmail`, and `signUpWithEmail` to use `flow { ... }` instead of `callbackFlow`.
- Add `AUTH_DEBUG` logs for each step.
- Improve error handling for Google Sign-In.

### Domain Layer

#### [User.kt](file:///C:/Users/rupal/AndroidStudioProjects/JamuiOne/app/src/main/java/com/example/jamuione/domain/model/User.kt)
- Ensure all fields have default values to avoid Firestore mapping issues.

### UI Layer

#### [AuthViewModel.kt](file:///C:/Users/rupal/AndroidStudioProjects/JamuiOne/app/src/main/java/com/example/jamuione/ui/auth/AuthViewModel.kt)
- Centralize auth state handling in `handleAuthResource`.
- Add `AUTH_DEBUG` logs for state transitions.
- Ensure `createUserProfile` is only called if it's a new user (or idempotent).

#### [Navigation.kt](file:///C:/Users/rupal/AndroidStudioProjects/JamuiOne/app/src/main/java/com/example/jamuione/ui/Navigation.kt)
- Simplify `SplashScreen` to be the single source of truth for the initial flow.
- Remove redundant navigation logic from `LoginScreen`'s `LaunchedEffect`.
- Ensure Guest users are routed directly to `MainFeed`.

#### [ProfileSetupScreen.kt](file:///C:/Users/rupal/AndroidStudioProjects/JamuiOne/app/src/main/java/com/example/jamuione/ui/profile/ProfileSetupScreen.kt)
- Add a Logout button in the top bar to prevent being trapped.
- Disable Save button and show loading indicator during save operation.
- Improve error display via Snackbar.

#### [ProfileScreen.kt](file:///C:/Users/rupal/AndroidStudioProjects/JamuiOne/app/src/main/java/com/example/jamuione/ui/profile/ProfileScreen.kt)
- Implement guest mode view with "Sign In" and "Create Account" buttons.
- Display a clear message about guest limitations.

## Verification Plan

### Automated Tests
- N/A (Focus on manual verification for Auth flows).

### Manual Verification
1. **Fresh Install**: Open app -> Splash -> Login -> Guest Mode -> Feed.
2. **Email Sign Up**: Open app -> Splash -> Login -> Sign Up -> Profile Setup -> Save -> Main Feed.
3. **Google Sign-In**: Open app -> Splash -> Login -> Google Sign-In -> Profile Setup -> Save -> Main Feed.
4. **Existing User**: Open app -> Splash -> Main Feed.
5. **Incomplete Profile**: Open app -> Splash -> Profile Setup.
6. **Logout**: Main Feed -> Profile -> Logout -> Login.
7. **Trapped Recovery**: In Profile Setup -> Click Logout -> Login.
