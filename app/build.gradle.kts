import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.subtrack"
    compileSdk = 36

    // Load API keys from api_keys.properties (gitignored, safe for secrets)
    val apiKeysFile = rootProject.file("api_keys.properties")
    val apiKeys = Properties()
    if (apiKeysFile.exists()) {
        apiKeysFile.inputStream().use { apiKeys.load(it) }
    }

    defaultConfig {
        applicationId = "com.example.subtrack"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject API keys into BuildConfig so Kotlin code can access them
        buildConfigField("String", "CLEARBIT_API_KEY", "\"${apiKeys.getProperty("CLEARBIT_API_KEY", "")}\"")
        buildConfigField("String", "EXCHANGE_RATE_API_KEY", "\"${apiKeys.getProperty("EXCHANGE_RATE_API_KEY", "")}\"")
        buildConfigField("String", "GMAIL_CLIENT_ID", "\"${apiKeys.getProperty("GMAIL_CLIENT_ID", "")}\"")
        buildConfigField("String", "LOGO_BASE_URL", "\"https://logo.clearbit.com\"")
        buildConfigField("String", "EXCHANGE_RATE_BASE_URL", "\"https://open.er-api.com/v6/latest/USD\"")
    }

    buildTypes {
        release {
            // Fix: Enable R8 minification and resource shrinking for release builds.
            // This obfuscates class names, shrinks the APK, and prevents easy decompilation.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("com.github.bumptech.glide:glide:4.16.0")

        // — WorkManager (background tasks) ———————————
        implementation("androidx.work:work-runtime-ktx:2.9.0")

        // — ViewModel + LiveData + Flow ———————————
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

        // — Navigation Component ———————————
        implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
        implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

        // — MPAndroidChart (graphs) ———————————
        implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

        // — ML Kit Text Recognition (OCR) ———————————
        implementation("com.google.mlkit:text-recognition:16.0.0")

        // — Material 3 ———————————
        implementation("com.google.android.material:material:1.11.0")

        // — AppCompat ———————————
        implementation("androidx.appcompat:appcompat:1.6.1")

        // — ConstraintLayout ———————————
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")

        // — Biometric ———————————
        implementation("androidx.biometric:biometric:1.1.0")

        // — Coroutines ———————————
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

        // — Firebase ———————————
        implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
        implementation("com.google.firebase:firebase-auth-ktx")
        implementation("com.google.firebase:firebase-firestore-ktx")

        // — Glide (Image Loading) — declared once above, no duplicate needed
}