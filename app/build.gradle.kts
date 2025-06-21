import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services") // Na końcu, jak zaleca Google
}

// Wczytaj dane z local.properties
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        load(localPropsFile.inputStream())
    }
}

val appID = localProperties["APP_ID"]
val appSign = localProperties["APP_SIGN"]
val apiKey = localProperties["GEMINI_API_KEY"]

android {
    namespace = "com.example.e_clinic"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.e_clinic"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "APP_ID", "\"$appID\"")
        buildConfigField("String", "APP_SIGN", "\"$appSign\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
        dataBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    // AndroidX + Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigation
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose)

    // UI + Layout
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.11.0")) // najnowsza na 2025
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.zxing:core:3.5.1")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Credentials API
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // ZEGOCLOUD In-App Chat
    implementation("com.github.ZEGOCLOUD:zego_inapp_chat_uikit_android:+") {
        exclude(group = "com.github.ZEGOCLOUD", module = "zego_uikit_signaling_plugin_android")
    }

    implementation ("com.github.Zavhorodnii01:zego_uikit_signaling_plugin_android:v1.0.0")

    // Accompanist (Zaktualizowana wersja)
    implementation("com.google.accompanist:accompanist-pager:0.33.2-alpha")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.33.2-alpha")

    // DateTime Dialogs
    implementation("io.github.vanpra.compose-material-dialogs:datetime:0.9.0")
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.androidx.datastore.core.android)
    implementation(libs.generativeai)
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation(libs.androidx.media3.common.ktx)

    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.security:security-crypto:1.0.0")
    //implementation("androidx.compose.material:material-icons-extended:<version>")
    implementation(libs.firebase.messaging.ktx)
    //implementation("androidx.biometric:biometric-ktx:1.0.1")

    // Testy
    testImplementation(libs.junit)
    implementation("com.github.yalantis:ucrop:2.2.8")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    //extended icons
    implementation("androidx.compose.material:material-icons-extended:1.6.1")


    implementation("com.google.firebase:firebase-messaging:24.0.0") // Check for latest version

    // For custom notifications:
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core:1.12.0")

    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    // If using ViewModel with SavedState:
    implementation ("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.6.2")



    //implementation("com.google.ai.client:generativeai:0.3.1") // проверь последнюю доступную версию



}
