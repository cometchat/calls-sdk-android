<p align="center">
  <img alt="CometChat" src="https://assets.cometchat.io/website/images/logos/banner.png">
</p>

# CometChat Android Calls SDK

The CometChat Calls SDK enables real-time voice and video calling capabilities in your Android application. Built on top of WebRTC, it provides a complete calling solution with built-in UI components and extensive customization options.

<p align="center">
  <img src="./screenshots/showcase-1.png" alt="Mobile Screenshot 1" width="30%">&nbsp;&nbsp;
  <img src="./screenshots/showcase-2.png" alt="Mobile Screenshot 2" width="30%">&nbsp;&nbsp;
  <img src="./screenshots/showcase-3.png" alt="Mobile Screenshot 3" width="30%">
</p>

---

## Getting Started

To set up the CometChat Calls SDK and utilize CometChat for your calling functionality, you'll need to follow these steps:

1. Registration: Go to the [CometChat Dashboard](https://app.cometchat.com/) and sign up for an account.
2. After registering, log into your CometChat account and create a new app. Once created, CometChat will generate an Auth Key and App ID for you. Keep these credentials secure as you'll need them later.
3. Check the [Key Concepts](https://www.cometchat.com/docs/fundamentals/key-concepts) to understand the basic components of CometChat.

## 📦 Installation

Add the repository and dependency to your project:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://dl.cloudsmith.io/public/cometchat/cometchat/maven/") }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.cometchat:calls-sdk-android:5.0.0-beta.2")
}
```

For the full setup guide, refer to our [official documentation](https://www.cometchat.com/docs/calls/android/overview).

## 🚀 Explore the Sample Apps

Dive straight into our sample apps to see the CometChat Calls SDK in action.

| Sample App | Description |
|------------|-------------|
| [Standalone Calling](samples/sample-app#readme) | Join calls by session ID — no Chat SDK required |
| [Ringing](samples/sample-app-ringing#readme) | 1:1 calls with Chat SDK signaling (incoming/outgoing call screens, call logs) |

---

## Help and Support

For issues running the project or integrating with our UI Kits, consult our [documentation](https://www.cometchat.com/docs) or create a [support ticket](https://help.cometchat.com/hc/en-us) or seek real-time support via the [CometChat Dashboard](https://app.cometchat.com/).
