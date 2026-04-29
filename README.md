<div align="center">

# 📱 SubTrack

### **Never lose track of a subscription again.**

SubTrack is a sleek, secure Android app for tracking all your recurring subscriptions — with smart OCR scanning, spending analytics, and multi-currency support — all backed by Firebase.

<br/>

[![Download on GitHub](https://img.shields.io/badge/Download-GitHub%20Releases-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/KrishnaP1504/SubTrack/releases/tag/SubTrack)
&nbsp;
[![Android](https://img.shields.io/badge/Platform-Android%2011+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
&nbsp;
[![Kotlin](https://img.shields.io/badge/Built%20with-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)

</div>

---

## ✨ Features

| Feature | Description |
|---|---|
| 📋 **Dashboard** | View all active subscriptions at a glance with their logos, costs, and renewal dates |
| 📸 **OCR Scanner** | Snap or upload a receipt image — ML Kit auto-detects the service name & price |
| 📊 **Analytics** | Pie & bar charts showing monthly spend by category, annual burn rate & daily average |
| 🌍 **Multi-Currency** | Live exchange rate conversion — pick any currency as your default |
| 🔐 **Biometric Lock** | Fingerprint / face unlock keeps your financial data private |
| 🔥 **Firebase Sync** | Authentication & Firestore cloud backup so your data is safe across devices |
| 🎨 **Dark / Light Theme** | Full Material Design 3 theming with system-default, light, and dark modes |
| 🔔 **Renewal Reminders** | WorkManager background jobs fire timely notifications before a subscription renews |
| 🏢 **Auto Logos** | Company logos fetched automatically via Clearbit Logo API using Glide |
| 🔒 **Password Reset** | In-app Firebase re-authentication password change flow |

---

## 📲 Download

<a href="https://play.google.com/store/apps/details?id=com.example.subtrack">
  <img alt="Get it on Google Play" src="https://upload.wikimedia.org/wikipedia/commons/7/78/Google_Play_Store_badge_EN.svg" width="200"/>
</a>

> **Minimum Android version:** Android 11 (API 30)

---

## 🛠️ Tech Stack

```
Language        Kotlin
UI Framework    XML Layouts + ViewBinding + Material Design 3
Navigation      Jetpack Navigation Component (SafeArgs)
Architecture    MVVM — ViewModel + LiveData + StateFlow
Database        Room (local) + Cloud Firestore (remote)
Auth            Firebase Authentication
Charts          MPAndroidChart (Pie & Bar)
OCR             Google ML Kit Text Recognition
Images          Glide + Clearbit Logo API
Background      WorkManager (renewal notifications)
Security        AndroidX Biometric API
Currency        Open Exchange Rates API
```

---

## 🏗️ Project Structure

```
app/src/main/java/com/example/subtrack/
├── MainActivity.kt               # Single-activity host with biometric gate
├── SubTrackApplication.kt        # Application class — initialises Room DB & repo
│
├── data/
│   ├── local/
│   │   ├── entity/               # Room entities (Subscription)
│   │   └── dao/                  # Room DAOs
│   └── repository/               # Repository layer (local ↔ Firestore sync)
│
├── ui/
│   ├── fragment/
│   │   ├── WelcomeFragment.kt    # Onboarding / splash
│   │   ├── LoginFragment.kt      # Email + password sign-in
│   │   ├── RegisterFragment.kt   # Account creation
│   │   ├── DashboardFragment.kt  # Subscription list
│   │   ├── AddEditFragment.kt    # Add / edit a subscription
│   │   ├── SubscriptionDetailFragment.kt
│   │   ├── ScannerFragment.kt    # OCR receipt scanner
│   │   ├── AnalyticsFragment.kt  # Charts & spend summary
│   │   ├── AccountFragment.kt    # Profile, theme, currency, biometric
│   │   └── EmailSyncFragment.kt  # Email-based sync helper
│   ├── adapter/                  # RecyclerView adapters
│   ├── viewmodel/                # ViewModels per screen
│   └── theme/                    # Material theme helpers
│
├── util/
│   └── CurrencyUtils.kt          # Currency conversion & formatting
│
└── worker/
    └── RenewalReminderWorker.kt  # WorkManager background reminder job
```

---

## 🚀 Getting Started (Local Build)

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 11+
- A Firebase project with **Authentication** and **Firestore** enabled

### 1. Clone the repository
```bash
git clone https://github.com/KrishnaP1504/SubTrack.git
cd SubTrack
```

### 2. Configure API keys

Create a file called `api_keys.properties` in the **root** of the project (it is git-ignored):

```properties
CLEARBIT_API_KEY=your_clearbit_api_key
EXCHANGE_RATE_API_KEY=your_exchange_rate_api_key
GMAIL_CLIENT_ID=your_gmail_oauth_client_id
```

### 3. Add Firebase config

Place your `google-services.json` file in the `app/` directory.  
*(Download it from the Firebase Console → Project Settings → Your Apps)*

### 4. Build & run

```bash
./gradlew assembleDebug
```

Or simply press **Run ▶** in Android Studio.

---

## 📸 Screenshots

> *(Coming soon)*

---

## 🔐 Security Notes

- API keys are stored in `api_keys.properties` (excluded from version control via `.gitignore`)
- Firebase re-authentication is required before changing passwords
- Biometric lock covers the entire app on launch — failed attempts close the app
- Release builds have R8 minification + resource shrinking enabled

---

## 🤝 Contributing

Pull requests are welcome! Please open an issue first to discuss what you would like to change.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

<div align="center">
Made with ❤️ by <a href="https://github.com/KrishnaP1504">Krishna Pipaliya</a>
</div>
