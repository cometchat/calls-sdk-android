plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.cometchat.samplecallsvoip"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cometchat.samplecallsvoip"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Network
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // CometChat Chat SDK (signaling, users, call logs)
    implementation("com.cometchat:chat-sdk-android:4.0.+")

    // CometChat Calls SDK (session management)
    implementation("com.cometchat:calls-sdk-android:5.0.0")

    // CometChat Push Notifications library (VoIP + chat notifications)
    implementation("com.cometchat:push-notifications-android:1.0.0-alpha.1")

    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging:24.1.0")
}

// Force compatible versions of androidx.core to work with AGP 8.7.3
// (push-notifications-android transitively pulls in core-ktx:1.18.0 which requires AGP 8.9.1)
configurations.all {
    resolutionStrategy {
        force("androidx.core:core-ktx:1.15.0")
        force("androidx.core:core:1.15.0")
    }
}