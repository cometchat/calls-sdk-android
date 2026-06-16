# CometChat Calls SDK — Ringing Sample App (Android)

A sample Android app demonstrating the CometChat ringing flow using both the **Chat SDK** (for call signaling) and the **Calls SDK** (for session management). This app shows how to initiate, receive, accept, reject, and end calls with a complete ringing experience.

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 26+ (minSdk 26, targetSdk 35)
- A [CometChat](https://www.cometchat.com/) account with:
  - App ID
  - Auth Key
  - Region (US, EU, or IN)

## Setup

1. Clone the repository and open the project in Android Studio.
2. Sync Gradle to download dependencies.
3. Run the `sample-app-ringing` module on a device or emulator.
4. On first launch, enter your CometChat **App ID**, **Auth Key**, and select your **Region**.
   - Alternatively, set them in `AppConstants.kt` before building.
5. Log in with a sample user or enter a UID manually.

## Features

- **Dual SDK Integration** — Chat SDK handles call signaling (initiate, accept, reject, cancel, end); Calls SDK handles the media session.
- **User List** — Browse CometChat users with audio and video call buttons.
- **Outgoing Call Screen** — Shows receiver info with a cancel button. Listens for accepted/rejected/busy events.
- **Incoming Call Screen** — Shows caller info with accept and reject buttons. Auto-dismisses if the caller cancels.
- **Call Session** — Full call UI powered by `CometChatCalls.joinSession()` with foreground notification support.
- **Global Incoming Call Listener** — Receives incoming calls from any screen via the Application class.
- **Call Logs** — View call history with pull-to-refresh and auto-refresh on return.

## Architecture

The app follows an **MVVM-lite** pattern:

- **Activities** with ViewBinding for UI
- **ViewModels** with LiveData for credentials and login flows
- **Repository** singleton wrapping both Chat SDK and Calls SDK operations
- **Fragments** for the tabbed Users and Call Logs screens

## Project Structure

```
sample-app-ringing/
├── src/main/kotlin/com/cometchat/samplecallsringing/
│   ├── RingingApplication.kt          # Global incoming call listener
│   ├── data/repository/Repository.kt  # SDK call wrappers
│   ├── ui/activity/
│   │   ├── SplashActivity.kt          # SDK init + routing
│   │   ├── AppCredentialsActivity.kt  # Region + credentials input
│   │   ├── LoginActivity.kt           # Sample user / UID login
│   │   ├── HomeActivity.kt            # Tabbed home (Users + Call Logs)
│   │   ├── OutgoingCallActivity.kt    # Outgoing call UI
│   │   ├── IncomingCallActivity.kt    # Incoming call UI
│   │   └── CallActivity.kt            # Active call session
│   ├── ui/adapters/                    # RecyclerView adapters
│   ├── ui/fragments/                   # Users & Call Logs fragments
│   ├── viewmodels/                     # Login & credentials ViewModels
│   └── utils/                          # Constants & SharedPreferences helpers
└── src/main/res/                       # Layouts, drawables, values
```

## Dependencies

- `com.cometchat:chat-sdk-android:4.0.+` — Call signaling, user management, call logs
- `com.cometchat:calls-sdk-android:5.0.1` — Call session management
- AndroidX, Material Components, Glide, OkHttp

## Build It with AI

This sample app was built using the agent skills in `skills/`. To recreate this exact functionality in your own project, copy the `skills/` folder and use this prompt with your AI coding assistant:

```text
Using the skills in skills/, build an Android app with CometChat Calls SDK v5 and Chat SDK v4
that implements a full ringing flow. The app should: let users enter App ID, Auth Key, and
Region; log in to both SDKs; show a user list with audio/video call buttons; initiate calls
via Chat SDK signaling; show incoming/outgoing call screens with accept/reject/cancel; join
the call session on accept with foreground service support; display call logs with
pull-to-refresh; and register a global incoming call listener in the Application class. Use
setup for SDK init, ringing-integration for the call signaling flow, join-session and
session-settings for the call session, event-listeners for session and button events,
background-handling for the foreground service, and call-logs for call history.
```

**Skills used:** `setup`, `ringing-integration`, `join-session`, `session-settings`, `event-listeners`, `background-handling`, `call-logs`
