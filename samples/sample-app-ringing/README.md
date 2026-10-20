# CometChat Calls SDK — Ringing Sample App (Android)

A sample Android app demonstrating the CometChat ringing flow with the **Calls SDK** (initiating calls and the media session), the **Push Notifications SDK** (ringing the callee through Firebase Cloud Messaging, even when the app is not running) and the **Chat SDK** (login and users). This app shows how to initiate, receive, accept, reject, cancel, and end calls with a complete ringing experience.

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 26+ (minSdk 26, targetSdk 35, compileSdk 36)
- A [CometChat](https://www.cometchat.com/) account with:
  - App ID
  - Auth Key
  - Region (US, EU, or IN)
  - An FCM push provider configured on the dashboard (Notifications → Push notifications). Its provider ID goes in `AppConstants.PUSH_PROVIDER_ID`.
- A [Firebase](https://console.firebase.google.com/) project that contains an Android app with the package name `com.cometchat.samplecallsringing`, using the same FCM project as the dashboard provider above. Download its `google-services.json`.

## Setup

1. Clone the repository and open the project in Android Studio.
2. Copy your `google-services.json` into `samples/sample-app-ringing/`. The file is git-ignored. Without it the app still builds and can place calls, but the device cannot be rung.
3. Set `PUSH_PROVIDER_ID` in `AppConstants.kt` to your dashboard push provider.
4. Sync Gradle to download dependencies.
5. Run the `sample-app-ringing` module on a device or emulator with Google Play services.
6. On first launch, enter your CometChat **App ID**, **Auth Key**, and select your **Region**.
   - Alternatively, set them in `AppConstants.kt` before building.
7. Log in with a sample user or enter a UID manually. Logging in registers this device's push token, so calls to that user ring here; logging out unregisters it.

## Features

- **Outgoing calls** — `CometChatCalls.initiateCall()` creates the call and the server pushes the ring to the callee. The outgoing screen joins the session when the callee answers, polls the call status for a decline or busy, and ends the call on cancel, back, or a 60-second timeout.
- **Incoming calls via push** — The Push Notifications SDK runs in `AUTO` mode: it parses the FCM payload, rings with a full-screen incoming-call screen (`IncomingCallActivity`) over the lock screen, handles decline, expiry and missed calls, and opens `CallActivity` with the answered call. Rings arrive even when the app has been swiped away.
- **Ring readiness** — The home screen checks, once per launch, whether anything would silence a ring (full-screen notifications revoked, incoming-call channel muted, Do Not Disturb) and offers the matching settings page.
- **User List** — Browse CometChat users with audio and video call buttons.
- **Call Session** — Full call UI powered by `CometChatCalls.joinSession()` with foreground service support.
- **Call Logs** — View call history with pull-to-refresh and auto-refresh on return.
- **Debug ring injection** — Debug builds include a broadcast receiver that feeds the same payload FCM would deliver into the push SDK, so the ring path can be exercised without a push (see below).

## Architecture

The app follows an **MVVM-lite** pattern:

- **Activities** with ViewBinding for UI
- **ViewModels** with LiveData for credentials and login flows
- **Repository** singleton wrapping both Chat SDK and Calls SDK operations
- **Fragments** for the tabbed Users and Call Logs screens
- **push/** — Push Notifications SDK configuration, token registration and the FCM messaging service

## Project Structure

```
sample-app-ringing/
├── src/main/kotlin/com/cometchat/samplecallsringing/
│   ├── RingingApplication.kt          # Configures the push SDK on every process start
│   ├── push/
│   │   ├── PushNotifications.kt       # Push SDK init (AUTO call mode), token register/unregister
│   │   └── RingingMessagingService.kt # FCM entry point: hands the payload to the push SDK
│   ├── data/repository/Repository.kt  # SDK call wrappers
│   ├── ui/activity/
│   │   ├── SplashActivity.kt          # SDK init + routing (registers the push token on a saved session)
│   │   ├── AppCredentialsActivity.kt  # Region + credentials input
│   │   ├── LoginActivity.kt           # Sample user / UID login
│   │   ├── HomeActivity.kt            # Tabbed home (Users + Call Logs)
│   │   ├── OutgoingCallActivity.kt    # Outgoing call: initiateCall, wait for the answer, cancel/timeout
│   │   ├── IncomingCallActivity.kt    # Ring screen launched by the push SDK; answer/decline through it
│   │   └── CallActivity.kt            # Active call session
│   ├── ui/adapters/                    # RecyclerView adapters
│   ├── ui/fragments/                   # Users & Call Logs fragments
│   ├── viewmodels/                     # Login & credentials ViewModels
│   └── utils/                          # Constants & SharedPreferences helpers
├── src/debug/.../debug/DebugIncomingCallReceiver.kt  # Debug-only ring injection (no FCM needed)
├── src/main/res/                       # Layouts, drawables, values
└── google-services.json                # Your Firebase config (git-ignored, not included)
```

## Testing a ring without FCM (debug builds)

Log in on the device, then inject an incoming call for the logged-in user. `receiver` must be that user's UID or the push SDK drops the push as meant for someone else. Use a real session ID (from a call another device just initiated) if you want answering to join an actual session.

```bash
adb shell am broadcast -n com.cometchat.samplecallsringing/.debug.DebugIncomingCallReceiver \
    --es sessionId <session-id> --es sender <caller-uid> --es senderName Andrew --es callType audio --es receiver <your-uid>
```

Cancel it the way the caller's cancel push would:

```bash
adb shell am broadcast -n com.cometchat.samplecallsringing/.debug.DebugIncomingCallReceiver \
    --es sessionId <session-id> --es callAction cancelled --es receiver <your-uid>
```

## Dependencies

- `com.cometchat:calls-sdk-android:5.1.0-beta.2` — Initiating and ending calls, call logs, the media session
- `com.cometchat:push-notifications-android:2.0.0-beta.3` — Push token registration, FCM payload routing
- `com.cometchat:push-notifications-calls-android:2.0.0-beta.3` — Ringing, answer/decline, missed calls
- `com.google.firebase:firebase-messaging:24.1.0` — Firebase Cloud Messaging
- `com.cometchat:chat-sdk-android:4.0.+` — Login and user management
- AndroidX, Material Components, Glide, OkHttp

The push SDKs are published to the CometChat `call-team` Cloudsmith repository, added in `settings.gradle.kts`.

## Build It with AI

This sample app was built using the agent skills in `skills/`. To recreate this exact functionality in your own project, copy the `skills/` folder and use this prompt with your AI coding assistant:

```text
Using the skills in skills/, build an Android app with CometChat Calls SDK v5, the CometChat
Push Notifications SDK and Chat SDK v4 that implements a full ringing flow. The app should:
let users enter App ID, Auth Key, and Region; log in to both SDKs and register the push
token; show a user list with audio/video call buttons; initiate calls with
CometChatCalls.initiateCall and join the session when the callee answers; ring incoming
calls through the push notifications SDK in AUTO call mode with a full-screen incoming
call screen and accept/decline; join the call session with foreground service support;
and display call logs with pull-to-refresh. Use setup for SDK init, ringing-integration for
the call flow, join-session and session-settings for the call session, event-listeners for
session and button events, background-handling for the foreground service, and call-logs
for call history.
```

**Skills used:** `setup`, `ringing-integration`, `join-session`, `session-settings`, `event-listeners`, `background-handling`, `call-logs`
