<div align="center">

# SubTrack

### Native Android Subscription Manager and Expense Tracking System

SubTrack is a production-ready, native Android application engineered to help users track, manage, and optimize recurring subscriptions. Built with Kotlin and modern Android Jetpack architecture, SubTrack features smart on-device OCR receipt recognition, detailed expenditure analytics, dynamic multi-currency conversions, and real-time cloud synchronization backed by Firebase.

<br/>

[![Download](https://img.shields.io/badge/Download-APK%20Release-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/KrishnaP1504/SubTrack/releases/tag/SubTrack)
[![Platform](https://img.shields.io/badge/Platform-Android%2011+%20(API%2030+)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM-007ACC?style=flat-square)](https://developer.android.com/topic/architecture)
[![Backend](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=black)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-MIT-2ea44f?style=flat-square)](LICENSE)

</div>

---

## Key Features

- **Centralized Dashboard**: Real-time overview of active subscriptions displaying merchant logos, billing cycles, monthly outflows, and renewal countdowns.
- **Smart OCR Scanner**: On-device text recognition powered by Google ML Kit. Snap or upload a receipt to automatically parse the merchant name, renewal date, and billing amount.
- **Spending Analytics & Insights**: Interactive pie and bar charts powered by MPAndroidChart showcasing category-wise expenditure, annual burn rate, and daily averages.
- **Multi-Currency Engine**: Automated exchange rate conversions powered by the Open Exchange Rates API, enabling users to track international services in their preferred base currency.
- **Proactive Renewal Reminders**: Android WorkManager background jobs schedule automated reminder notifications 24 hours prior to billing events, resilient across device reboots via `BOOT_COMPLETED` receivers.
- **Biometric Security**: Hardware-backed biometric authentication (fingerprint and face recognition) using AndroidX BiometricPrompt to safeguard financial records.
- **Real-Time Cloud Synchronization**: Offline-first Room database synchronized seamlessly with Cloud Firestore using Kotlin `callbackFlow` real-time listeners.
- **Material Design 3 Theming**: Complete Material 3 UI design supporting system-default, light, and dark modes.
- **Automatic Merchant Branding**: Instant company logo retrieval via the Clearbit Logo API paired with Glide for high-performance image caching.

---

## Architecture and Design

SubTrack follows the recommended Android Architecture Guidelines with a strict unidirectional data flow and clean separation of concerns:

- **UI Layer**: Fragment-based navigation coordinated by the Single Activity architecture (`MainActivity`) with ViewBinding and Jetpack Navigation Component (SafeArgs).
- **Presentation Layer**: Android Jetpack `ViewModel` instances maintaining UI state and exposing reactive streams via `StateFlow` and `LiveData`.
- **Domain & Repository Layer**: Unified repository arbitrating between local data caching and remote cloud sync.
- **Local Persistence**: Room SQLite database providing immediate offline responsiveness.
- **Remote Backend**: Firebase Authentication managing identity and Cloud Firestore providing cloud backup and cross-device sync.
- **Background Tasks**: Android `WorkManager` scheduling non-deferrable background reminder workers.

---

## Tech Stack

| Layer | Technology |
|:---|:---|
| **Programming Language** | Kotlin |
| **UI Framework** | XML Layouts, ViewBinding, Material Design 3 |
| **Architecture Pattern** | MVVM (Model-View-ViewModel) |
| **Concurrency & Async** | Kotlin Coroutines, StateFlow, callbackFlow, LiveData |
| **Local Database** | Room (SQLite ORM) |
| **Cloud Backend** | Firebase Authentication, Cloud Firestore |
| **Background Scheduling** | Android WorkManager |
| **Machine Learning / OCR**| Google ML Kit Text Recognition |
| **Visualizations** | MPAndroidChart (Pie and Bar Charts) |
| **Image Loading** | Glide + Clearbit Logo API |
| **Security & Auth** | AndroidX BiometricPrompt, Secure SharedPreferences |
| **Currency API** | Open Exchange Rates API |

---

## Project Structure

```
app/src/main/java/com/example/subtrack/
├── MainActivity.kt               # Single-activity host with biometric authentication
├── SubTrackApplication.kt        # Application class initialising Room DB and repository
│
├── data/
│   ├── local/
│   │   ├── entity/               # Room database entities (Subscription)
│   │   └── dao/                  # Room Data Access Objects
│   └── repository/               # Repository layer coordinating Room and Firestore
│
├── ui/
│   ├── fragment/
│   │   ├── WelcomeFragment.kt    # Onboarding and splash screen
│   │   ├── LoginFragment.kt      # Firebase email and password sign-in
│   │   ├── RegisterFragment.kt   # Account registration flow
│   │   ├── DashboardFragment.kt  # Main subscription feed and stats
│   │   ├── AddEditFragment.kt    # Subscription entry and editing form
│   │   ├── SubscriptionDetailFragment.kt # Detailed subscription drill-down
│   │   ├── ScannerFragment.kt    # OCR camera and gallery receipt scanner
│   │   ├── AnalyticsFragment.kt  # Spending breakdown and charts
│   │   ├── AccountFragment.kt    # User profile, theme, and security settings
│   │   └── EmailSyncFragment.kt  # Invoice and subscription import helper
│   ├── adapter/                  # RecyclerView adapters
│   ├── viewmodel/                # Architecture ViewModels per screen
│   └── theme/                    # Material 3 styling and color utilities
│
├── util/
│   └── CurrencyUtils.kt          # Exchange rate calculations and formatting
│
└── worker/
    └── RenewalReminderWorker.kt  # WorkManager notification scheduling worker
```

---

## Getting Started

### Prerequisites

- Android Studio (Hedgehog 2023.1.1 or newer recommended)
- Java Development Kit (JDK 11 or JDK 17)
- Android SDK with Platform 34 (Minimum SDK: API 30 / Android 11)
- A Firebase project with **Authentication** and **Cloud Firestore** activated

### 1. Clone the Repository

```bash
git clone https://github.com/KrishnaP1504/SubTrack.git
cd SubTrack
```

### 2. Configure API Keys

Create a file named `api_keys.properties` in the root project directory:

```properties
CLEARBIT_API_KEY=your_clearbit_api_key
EXCHANGE_RATE_API_KEY=your_exchange_rate_api_key
GMAIL_CLIENT_ID=your_gmail_oauth_client_id
```

### 3. Add Firebase Configuration

1. In the [Firebase Console](https://console.firebase.google.com/), register your Android application with package name `com.example.subtrack`.
2. Download the resulting `google-services.json` file.
3. Place `google-services.json` inside the `app/` directory (`app/google-services.json`).

### 4. Build and Run

Assemble the debug APK via Gradle:

```bash
./gradlew assembleDebug
```

Alternatively, open the project in Android Studio and run the `app` configuration on an emulator or physical device.

---

## Security Architecture

- **Credential Isolation**: All sensitive external API credentials reside in `api_keys.properties`, excluded from source control.
- **Biometric Gate**: Biometric challenge protects the activity lifecycle on app launch, terminating the task on unauthorized access.
- **Cloud Security**: Cloud Firestore security rules restrict read and write access strictly to authenticated users matching document UIDs.
- **Release Hardening**: Release builds enforce R8 code obfuscation, dead code stripping, and resource shrinking.

---

## Download

Pre-built production binaries are published under the repository releases:

- [Download SubTrack APK](https://github.com/KrishnaP1504/SubTrack/releases/tag/SubTrack)

---

## Author

**Krishnakumar Pipaliya**  
Mobile Application Developer specializing in Android (Kotlin) and Flutter

- Portfolio: [krishnapipaliya.in](https://krishnapipaliya.in)
- LinkedIn: [linkedin.com/in/krishn-pipaliya1504](https://www.linkedin.com/in/krishn-pipaliya1504/)
- GitHub: [github.com/KrishnaP1504](https://github.com/KrishnaP1504)
- Email: [pipaliyakrishna1504@gmail.com](mailto:pipaliyakrishna1504@gmail.com)

---

## License

Distributed under the MIT License. See [LICENSE](LICENSE) for full details.
