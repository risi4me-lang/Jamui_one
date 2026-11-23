# JamuiOne (District One)

JamuiOne (District One) is a community networking and local engagement Android application designed to connect individuals by hometown, locality, and district. It features community feeds, notice boards, local organizations, real-time Firebase services, and localized push notifications.

---

## Features & Architecture

### Authentication & Security
- **Sign-In Methods**: Email & Password (with email verification and password reset) and Google Sign-In (via Android Credential Manager and Firebase Auth). No phone OTP is used.
- **User Profiles**: Regional state, district, and locality settings, blood donor status, profession, company/college, bio, and profile completion tracking. Email addresses are kept private and read directly from Firebase Auth when needed.

### Firestore Collections & Data Model
- **`users`**: User profile documents (excluding email; user data is updated via merged writes to protect system-managed fields like role and verification).
- **`posts`**: Community posts with subcollections:
  - `likes`: Post reactions/likes per user.
  - `comments`: Comments on posts (supports threading and anonymization on account deletion).
- **`notices`**: Event bulletin board and notices with subcollections:
  - `rsvps`: User RSVPs for notices/events.
  - `helpful`: Helpful votes.
- **`reports`**: User reports for content moderation.
- **`announcements`**: System or general announcements.
- **`organizations`**: Local business, institution, and community organizations with subcollections:
  - `admins`: Organization administrators/owners (`OWNER`, etc.).
  - `followers`: Organization followers.
  - `announcements`: Organization-specific announcements.

### Admin Role
- **Admin Privileges**: Granted via the `role` field in Firestore user documents (`role: "admin"`), which is set manually directly in Firestore.

### Notifications & Push Messaging
- **Approach**: Firebase Cloud Messaging (FCM) topic subscriptions (`district_<state>_<district>` and `locality_<state>_<district>_<locality>` managed automatically via `TopicManager`) combined with in-app notifications and announcements. No cloud functions are currently used.

---

## How to Build and Run

1. **Prerequisites**:
   - Android Studio (Hedgehog or newer)
   - JDK 17 or later
   - Android SDK (compileSdk 37, minSdk 30)
   - A valid `google-services.json` file placed in the `app/` directory.

2. **Opening the Project**:
   - Clone the repository and open the root folder (`JamuiOne`) in Android Studio.
   - Let Gradle sync project dependencies.

3. **Building & Running**:
   - Connect an Android physical device or start an Android Emulator (API 30+).
   - Run the debug build using Gradle:
     ```bash
     ./gradlew assembleDebug
     ```
   - Or click **Run** ▶ in Android Studio to build, deploy, and launch the app.
