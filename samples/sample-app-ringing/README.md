# CometChat Calls SDK — Ringing Sample App (Android)

A sample Android app showing 1:1 and group ringing calls with the **Calls SDK** and the **Push Notifications SDK**.
The caller places and ends the call with the Calls SDK. The callee's phone rings through Firebase Cloud Messaging
in the foreground, in the background, locked, or after the app was swiped away. The session is prepared while it
rings, so the call connects as soon as it is answered.

## Prerequisites

- Android Studio Narwhal (2025.1.1) or later (the project uses Android Gradle Plugin 8.11)
- Android SDK 26+ (minSdk 26, targetSdk 35, compileSdk 36)
- A device or emulator with Google Play services
- A [CometChat](https://www.cometchat.com/) app with:
  - App ID, Auth Key and Region (US, EU or IN)
  - An **FCM push provider** (dashboard → Notifications → Push), and its provider ID
- A [Firebase](https://console.firebase.google.com/) project with an Android app for the package
  `com.cometchat.samplecallsringing`. It must be the same Firebase project as the dashboard provider. Download its
  `google-services.json`.

## Setup

1. Clone the repository and open it in Android Studio.
2. Copy your `google-services.json` into `samples/sample-app-ringing/`. The file is git-ignored. It is required to
   build: the module applies the Google Services plugin, and the build fails without the file.
3. Run the `sample-app-ringing` module on two devices.
4. On first launch, enter your **App ID**, **Auth Key**, **Region** and **Push Provider ID** (all required: the
   push token cannot be registered without the provider ID). You can instead set them in `utils/AppConstants.kt`
   before building.
5. Log in as a different user on each device and call one from the other. Logging in registers this device's push
   token, so calls to that user ring here. Signing out removes it.

## Screens

- **Users**: everyone in the app, with presence. The phone and video buttons ring that person.
- **Groups**: the groups you have joined. The buttons ring every member. Whoever answers joins the same session,
  and the call carries on when one of them leaves.
  - **+** opens **New group call**: pick up to 20 people and, optionally, name the group. The app creates a private
    group with them and rings it.
- **History**: your call logs, with who, voice/video/group, the outcome, when and how long.
  - Tap a call for its details, its participants, and to call back or ring the group again.
  - The list reloads each time you open the tab. Pull to refresh.
- **Profile**: tap your avatar in the top-right corner. It shows:
  - your UID, with a copy button,
  - the System / Light / Dark appearance,
  - the app's credentials and SDK versions,
  - **Sign out**.

The call screens stay dark in both themes because they sit over video.

## How a call works

**Placing a call** (`OutgoingCallActivity`)

1. `CometChatCalls.initiateCall()` creates the call and rings the callee: every member, when `receiverType` is
   group. It also starts preparing the session. The session settings are frozen at this point, so the call screen
   builds the same ones (`calls/CallSettings.kt`).
2. `onParticipantConnected`: the callee's phone has the session, and "Calling…" becomes "Ringing…".
3. `onParticipantJoined`: the callee answered. `CallActivity` joins the prepared session by its ID.
4. The end button, back, or no answer after 45 seconds withdraws the call with `CometChatCalls.endCall()`. That
   stops the callee's phone ringing. The callee rings for 60 seconds, so the caller always gives up first.
5. A decline is not pushed to the caller. While a call rings, the app checks the server's call log every
   3 seconds and shows "Declined" or "Busy".

Every decision is made by `calls/OutgoingCall.kt`, a small state machine with no Android or SDK code in it.
Duplicate events and double taps change nothing.

**Receiving a call** (`push/`, `IncomingCallActivity`, `CallActivity`)

1. `RingingApplication` configures the Push Notifications SDK on every process start, in `AUTO` call mode. FCM can
   start a killed app for a call before any screen exists.
2. `RingingMessagingService` hands each FCM message to the SDK. The SDK then:
   - shows the ringing notification and the full-screen `IncomingCallActivity` over the lock screen,
   - prepares the session while it rings,
   - handles decline, expiry, missed calls and the caller cancelling.
3. When the user answers, the SDK opens `CallActivity` with the answered call in `CallBundles`. It joins by session
   ID, so only the media and the call UI are left to set up.
4. On the home screen, the app checks whether anything would silence a ring and offers the matching settings page:
   full-screen notifications revoked, the incoming-call channel muted, or Do Not Disturb. The SDK can also report
   `RingReadiness.MICROPHONE_DENIED` (the microphone was refused when answering, so calls still ring but join
   muted); the sample does not prompt for that one.

**Signing out** waits for `CometChatPushNotifications.unregisterToken()` and logs out of the SDKs only if it
succeeded. The request is authenticated by the session being logged out, and without it this device keeps ringing
for the user who left. If it fails, the app offers **Retry** or **Sign out anyway** (with that warning).

## Project Structure

```
sample-app-ringing/
├── src/main/kotlin/com/cometchat/samplecallsringing/
│   ├── RingingApplication.kt            # Appearance + push SDK setup on every process start
│   ├── calls/
│   │   ├── OutgoingCall.kt              # Outgoing call state machine
│   │   ├── CallSettings.kt              # Session settings shared by initiateCall and joinSession
│   │   ├── CallLauncher.kt              # Permissions + double-tap guard, then the outgoing screen
│   │   ├── CallTarget.kt                # A user or a group to ring
│   │   └── CallLogDisplay.kt            # How a call log reads: kind, status, who, dates, duration
│   ├── push/
│   │   ├── PushNotifications.kt         # Push SDK configuration (AUTO call mode), token register/unregister
│   │   └── RingingMessagingService.kt   # FCM entry point
│   ├── data/repository/Repository.kt    # Login/logout, sample users, endCall, call status lookup
│   ├── ui/activity/
│   │   ├── SplashActivity.kt            # SDK init + routing
│   │   ├── AppCredentialsActivity.kt    # Region, credentials and push provider ID
│   │   ├── LoginActivity.kt             # Sample user / UID login
│   │   ├── HomeActivity.kt              # Users / Groups / History tabs, ring readiness
│   │   ├── ProfileActivity.kt           # Profile, appearance, app info, sign out
│   │   ├── NewGroupCallActivity.kt      # Create a group from picked members and ring it
│   │   ├── CallDetailsActivity.kt       # One call from History, call back
│   │   ├── OutgoingCallActivity.kt      # "Calling… / Ringing…", cancel, timeout, decline
│   │   ├── IncomingCallActivity.kt      # Ring screen opened by the push SDK
│   │   └── CallActivity.kt              # The call, for outgoing and answered calls
│   ├── ui/fragments/                    # Users, Groups and History tabs; search debounce
│   ├── ui/adapters/                     # RecyclerView adapters
│   ├── ui/views/AvatarView.kt           # Avatar with initials fallback and presence dot
│   ├── viewmodels/                      # Login and credentials
│   └── utils/                           # Constants, preferences, appearance
├── src/main/res/values/colors.xml       # Light colour tokens (values-night/ holds the dark ones)
├── src/debug/.../DebugIncomingCallReceiver.kt  # Debug-only ring injection (no FCM needed)
└── google-services.json                 # Your Firebase config (git-ignored, not included)
```

## Testing a ring without FCM (debug builds)

Log in on the device, then inject an incoming call for the logged-in user. `receiver` must be that user's UID, or
the push SDK drops the push as meant for someone else. To have the answer join a real session, use the session ID
of a call another device just initiated.

```bash
adb shell am broadcast -n com.cometchat.samplecallsringing/.debug.DebugIncomingCallReceiver \
    --es sessionId <session-id> --es sender <caller-uid> --es senderName Andrew --es callType audio --es receiver <your-uid>
```

Cancel it the way the caller's cancel push would:

```bash
adb shell am broadcast -n com.cometchat.samplecallsringing/.debug.DebugIncomingCallReceiver \
    --es sessionId <session-id> --es callAction cancelled --es receiver <your-uid>
```

## Add ringing to your app

The SDKs are on the CometChat Maven repository. Add it in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://dl.cloudsmith.io/public/cometchat/cometchat/maven/") }
    }
}
```

Apply the Google Services plugin (`id("com.google.gms.google-services") version "4.4.2"` in the root
`build.gradle.kts`, and `id("com.google.gms.google-services")` in the app module), then add the SDKs to the app module:

```kotlin
dependencies {
    implementation("com.cometchat:calls-sdk-android:5.1.0")
    implementation("com.cometchat:push-notifications-android:2.0.0")
    implementation("com.cometchat:push-notifications-calls-android:2.0.0")
    implementation("com.google.firebase:firebase-messaging:24.1.0")
}
```

Your app needs `minSdk` 26, `compileSdk` 36 and Android Gradle Plugin 8.9.1 or later. The Chat SDK is optional: this
sample uses it only for the user and group lists. For the integration steps (configuring the push SDK, registering
the token, placing and answering calls), follow the [`ringing-integration`](../../skills/ringing-integration/SKILL.md)
skill or this app's code.

## Dependencies

- `com.cometchat:calls-sdk-android` 5.1: calls, sessions and call logs
- `com.cometchat:push-notifications-android` and `com.cometchat:push-notifications-calls-android` 2.0: push
  tokens, FCM payloads, ringing, answer and decline
- `com.google.firebase:firebase-messaging`: Firebase Cloud Messaging
- `com.cometchat:chat-sdk-android` 4.2: login, users, groups and group creation
- AndroidX, Material Components, Glide, OkHttp

## Build It with AI

The skills in the repository's [`skills/`](../../skills) folder cover this app. Copy that folder into your project
and try:

```text
Using the skills in skills/, build an Android app with the CometChat Calls SDK 5.1 and the Push Notifications
SDK 2.0 that rings 1:1 and group calls. Configure the push SDK in Application.onCreate in AUTO call mode with my
FCM provider ID, register the push token after login and unregister it before logout. Place calls with
CometChatCalls.initiateCall (receiverType user or group), show "Calling… / Ringing…" until onParticipantJoined,
then join the prepared session by session ID. End the call with endCall after 45 seconds without an answer, poll
the call log for a decline, and open the call screen from the push SDK's answer activity. List call history with
CallLogRequest.
```

**Skills used:** `setup`, `ringing-integration`, `session-settings`, `event-listeners`, `background-handling`, `call-logs`
