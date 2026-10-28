plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Firebase Cloud Messaging for incoming-call pushes. Needs a local google-services.json (git-ignored).
    id("com.google.gms.google-services")
}

// The push SDK has no runtime version constant, so the profile screen reads it from BuildConfig.
val pushSdkVersion = "2.0.0"

android {
    namespace = "com.cometchat.samplecallsringing"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cometchat.samplecallsringing"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "PUSH_SDK_VERSION", "\"$pushSdkVersion\"")
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
        buildConfig = true
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.13.0")
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

    // CometChat Chat SDK: the user and group lists, and creating a group to ring
    implementation("com.cometchat:chat-sdk-android:4.2.0")

    // CometChat Calls SDK: initiateCall / endCall, sessions and call logs
    implementation("com.cometchat:calls-sdk-android:5.1.0")

    // Push Notifications SDK: rings incoming calls delivered through FCM, in every app state
    implementation("com.cometchat:push-notifications-android:$pushSdkVersion")
    implementation("com.cometchat:push-notifications-calls-android:$pushSdkVersion")
    implementation("com.google.firebase:firebase-messaging:24.1.0")
}
