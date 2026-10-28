# CometChat Calls SDK — Ringing Sample App (Android)

A sample Android app with 1:1 and group calls that ring on the other phone in every app state: in the foreground,
in the background, on the lock screen, and after the app was swiped away. The caller places and ends the call with
the **Calls SDK 5.1**. The callee's phone rings through Firebase Cloud Messaging and the **Push Notifications SDK
2.0**, which prepares the session while it rings so the call connects as soon as it is answered.

This README is also the integration guide for the Push Notifications SDK 2.0 on Android: [Add ringing to your
app](#add-ringing-to-your-app) walks through every step, with the code taken from this sample.

- [What the Push Notifications SDK does for calls](#what-the-push-notifications-sdk-does-for-calls)
- [Requirements](#requirements)
- [Run the sample](#run-the-sample)
- [Add ringing to your app](#add-ringing-to-your-app)
- [How the sample works](#how-the-sample-works)
- [Testing a ring without FCM (debug builds)](#testing-a-ring-without-fcm-debug-builds)
- [Troubleshooting](#troubleshooting)
- [Build It with AI](#build-it-with-ai)
- [Links](#links)

## What the Push Notifications SDK does for calls

The Push Notifications SDK 2.0 ships as two artifacts. `push-notifications-android` (core) registers the push token
and parses CometChat pushes. `push-notifications-calls-android` adds ringing. Adding the calls artifact is the
opt-in: there is no second initializer.

On the callee's phone, the SDK:

- **rings in every app state** with a call-style notification and a full-screen incoming-call screen over the lock
  screen, including when FCM starts a killed app;
- **prepares the call session while it rings** (Calls SDK `connect`), so an answered call only has to set up media
  and the call UI;
- **answers and declines into the Calls SDK**: Answer opens your call screen with the call, Decline ends the call
  as `rejected` through a queue that survives process death;
- **ends the ring for every other reason**: the caller cancelling, no answer, answered or declined on another
  device, logout. A missed-call notification is posted where it applies;
- **handles call waiting**: a call that arrives during a call rings with "Answering ends your current call"
  (Decline sends `busy`); a call that arrives while another one rings is rejected as `busy`;
- **registers the FCM token** for the logged-in user (`registerToken()` fetches the token itself);
- **reports ring readiness**: notifications off, the incoming-call channel muted, full-screen intents revoked, Do
  Not Disturb, or the microphone denied.

Two call modes:

- **`CallMode.AUTO`** (default, used by this sample): the SDK does all of the above. Your app supplies the
  activity that shows the answered call, and optionally its own incoming-call screen.
- **`CallMode.MANUAL`**: the SDK parses, checks and forwards the call to the Calls SDK, and never rings on its own.
  Your app presents the call, using the SDK's ringing steps one by one if it wants them.

The Chat SDK is optional. The call flow uses only the Calls SDK and the Push Notifications SDK; this sample adds the
Chat SDK for its user and group lists.

## Requirements

| | Requirement |
|---|---|
| Android | `minSdk` 26 with the calls artifact (the Calls SDK sets 26); 24 for the core artifact alone. The sample targets 35. |
| Build | `compileSdk` 36 and Android Gradle Plugin 8.9.1 or later. The Push Notifications SDK depends on `androidx.core` 1.18.0, whose AAR metadata requires both. |
| Kotlin | 2.1 or later. The Push Notifications SDK and `androidx.core` 1.18.0 depend on `kotlin-stdlib` 2.1.20. |
| Java | The SDKs target Java 11, so set `sourceCompatibility` / `targetCompatibility` to 11 or later (the sample uses 17). Gradle runs on JDK 17. |
| Android Studio | Narwhal (2025.1.1) or later to open the sample, which uses AGP 8.11.2 and Gradle 8.13. |
| Device | A phone or emulator with Google Play services. Use two devices to call one from the other. |
| CometChat | An app on the [CometChat Dashboard](https://app.cometchat.com/) with its **App ID**, **Region** (`us`, `eu` or `in`) and **Auth Key**, push notifications enabled, an **FCM provider**, and that provider's **Provider ID**. |
| Firebase | A [Firebase](https://console.firebase.google.com/) project with an Android app for your package (`com.cometchat.samplecallsringing` for the sample) and its `google-services.json`. It must be the same Firebase project as the dashboard's FCM provider. |

## Run the sample

1. Clone the repository and open it in Android Studio.
2. On the CometChat dashboard, enable push notifications and add an FCM provider (see
   [step 1](#1-set-up-push-on-the-dashboard)). Note its Provider ID.
3. Copy your `google-services.json` into `samples/sample-app-ringing/`. The file is git-ignored. It is required to
   build: the module applies the Google Services plugin, and the build fails without the file.
4. Run the `sample-app-ringing` module on two devices.
5. On first launch, enter your **App ID**, **Auth Key**, **Region** and **Push Provider ID** (all required: the push
   token cannot be registered without the provider ID). You can instead set them in `utils/AppConstants.kt` before
   building.
6. Log in as a different user on each device and call one from the other. Logging in registers this device's push
   token, so calls to that user ring here. Signing out removes it.
7. To prove the push path, swipe the app away on the callee before calling, then lock the phone. While the app is
   running, a ring can arrive another way, so only a killed app proves that the push works.

## Add ringing to your app

The steps below add the caller side (Calls SDK) and the callee side (Push Notifications SDK) to an existing app.
Each step points to the sample file it comes from.

### 1. Set up push on the dashboard

1. In the [CometChat Dashboard](https://app.cometchat.com/), open your app and go to **Notifications → Push**.
   Enable push notifications.
2. Add an **FCM** provider and upload a service account key of your Firebase project (Firebase console → Project
   settings → Service accounts → Generate new private key).
3. Note the provider's **Provider ID**. It goes into `PNConfiguration` in [step 4](#4-initialize-the-sdks).

The service account and your app's `google-services.json` must come from the same Firebase project. If they don't,
the token registers without an error and no push ever arrives.

### 2. Add the SDKs

Add the CometChat Maven repository in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://dl.cloudsmith.io/public/cometchat/cometchat/maven/") }
    }
}
```

Add the Google Services plugin in the root `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") version "8.11.2" apply false   // 8.9.1 or later
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false // 2.1 or later
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

Then, in the app module's `build.gradle.kts`, apply it and add the SDKs:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Calls: initiateCall / endCall, sessions and call logs
    implementation("com.cometchat:calls-sdk-android:5.1.0")
    // Push token registration and CometChat push parsing
    implementation("com.cometchat:push-notifications-android:2.0.0")
    // Ringing: the incoming-call notification and screen, answer and decline
    implementation("com.cometchat:push-notifications-calls-android:2.0.0")
    // Firebase Cloud Messaging. The Push Notifications SDK does not depend on it: your app owns Firebase.
    implementation("com.google.firebase:firebase-messaging:24.1.0")

    // Optional. The sample uses it for the user and group lists and to create a group to ring.
    implementation("com.cometchat:chat-sdk-android:4.2.0")
}
```

The Calls SDK needs Jetifier. In `gradle.properties`:

```properties
android.useAndroidX=true
android.enableJetifier=true
```

The sample also uses AndroidX, Material Components, Glide and OkHttp for its own screens; see
[`build.gradle.kts`](build.gradle.kts).

### 3. Update the manifest

The SDKs merge most of what ringing needs into your app's manifest:

| From | Merged for you |
|---|---|
| Calls SDK 5.1.0 | `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CAMERA`, `RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS`, `BLUETOOTH`, `WAKE_LOCK`, `MANAGE_OWN_CALLS`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CAMERA`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` |
| `push-notifications-android` | `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, and its internal notification receivers |
| `push-notifications-calls-android` | `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, `VIBRATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_PHONE_CALL`; the SDK's own incoming-call screen, the answer activity that asks for the microphone, the decline receiver, and the foreground service that keeps the process alive from the ring until your call screen joins |

Your app declares the rest. From the sample's [`AndroidManifest.xml`](src/main/AndroidManifest.xml):

```xml
<application ...>

    <!-- Your FCM service (step 5). The SDK never declares one: your app owns Firebase. -->
    <service
        android:name=".push.RingingMessagingService"
        android:exported="false">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT" />
        </intent-filter>
    </service>

    <!-- Keeps a joined call alive in the background with an ongoing-call notification. The Calls
         SDK does not declare it: without this entry CometChatOngoingCallService.launch() finds no
         service and the call drops when the app leaves the screen. -->
    <service
        android:name="com.cometchat.calls.services.CometChatOngoingCallService"
        android:foregroundServiceType="camera|microphone|mediaPlayback"
        android:exported="false" />

    <!-- The answered call (step 8). Opens over the lock screen. -->
    <activity
        android:name=".ui.activity.CallActivity"
        android:exported="false"
        android:showWhenLocked="true"
        android:turnScreenOn="true"
        android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation" />

    <!-- Only if you draw your own incoming-call screen (step 8). -->
    <activity
        android:name=".ui.activity.IncomingCallActivity"
        android:exported="false"
        android:launchMode="singleTop"
        android:showWhenLocked="true"
        android:turnScreenOn="true" />

</application>
```

The sample's manifest also lists the call permissions itself. That duplicates the merged entries and is harmless.

Runtime permissions:

- **`RECORD_AUDIO`**, plus **`CAMERA`** for video: request them before `initiateCall`, because the media session is
  prepared straight away (the sample's [`calls/CallLauncher.kt`](src/main/kotlin/com/cometchat/samplecallsringing/calls/CallLauncher.kt)).
  On the callee, the notification's Answer asks for them when they are missing (on by default,
  `setRequestCallPermissions`). An Accept button on a screen of your own does not, so ask in your call screen
  before joining, as the sample's `CallActivity` does. A denied microphone joins the call muted; it never fails it.
- **`POST_NOTIFICATIONS`** (Android 13+): `ensureRingReady` requests it when it is missing ([step 9](#9-check-ring-readiness)).
  The sample asks for it at Home together with the microphone and camera.
- **Full-screen intents** (Android 14+): `USE_FULL_SCREEN_INTENT` is merged, but the user or the system can revoke
  it. Without it the call still posts a notification, but no screen opens over the lock screen. Ring readiness
  reports it as `FSI_REVOKED`.

### 4. Initialize the SDKs

Initialize in `Application.onCreate`. FCM can start a killed process for a call before any activity exists, and
Android can restore the app straight onto a call screen; both must find the SDKs ready. From
[`push/PushNotifications.kt`](src/main/kotlin/com/cometchat/samplecallsringing/push/PushNotifications.kt) and
[`utils/SdkInit.kt`](src/main/kotlin/com/cometchat/samplecallsringing/utils/SdkInit.kt), trimmed:

```kotlin
import com.cometchat.calls.core.CallAppSettings
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException
import com.cometchat.pushnotification.CallMode
import com.cometchat.pushnotification.CometChatPushNotifications
import com.cometchat.pushnotification.PNConfiguration

class RingingApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Push Notifications SDK. The configuration is persisted, so a push that wakes a cold
        // process can ring even before this line runs again.
        CometChatPushNotifications.init(
            this,
            PNConfiguration.Builder("YOUR_APP_ID", "YOUR_REGION")
                .setProviderId("YOUR_PUSH_PROVIDER_ID")   // required to register the token
                .setAutoRegisterToken(false)               // the app registers after login (step 6)
                .setCallMode(CallMode.AUTO)                // the SDK rings, answers and declines
                .setAnswerActivity(CallActivity::class.java)              // required in AUTO
                .setIncomingCallActivity(IncomingCallActivity::class.java) // optional: your own ring screen
                .setDefaultRingTimeoutMs(60_000L)          // longer than the caller's 45 s (step 7)
                .build()
        )

        // Calls SDK.
        val callAppSettings = CallAppSettings.CallAppSettingBuilder()
            .setAppId("YOUR_APP_ID")
            .setRegion("YOUR_REGION")
            .build()
        CometChatCalls.init(this, callAppSettings, object : CometChatCalls.CallbackListener<String>() {
            override fun onSuccess(result: String) {}
            override fun onError(e: CometChatException) {
                Log.e(TAG, "Calls SDK init failed: ${e.message}")
            }
        })
    }
}
```

If your app uses the Chat SDK, initialize it here too. The sample initializes the Chat SDK first and the Calls SDK
from its success callback:

```kotlin
val appSettings = AppSettings.AppSettingsBuilder()
    .subscribePresenceForAllUsers()
    .setRegion("YOUR_REGION")
    .build()
CometChat.init(this, "YOUR_APP_ID", appSettings, object : CometChat.CallbackListener<String>() {
    override fun onSuccess(s: String) { /* CometChatCalls.init(...) */ }
    override fun onError(e: com.cometchat.chat.exceptions.CometChatException) { /* log */ }
})
```

`PNConfiguration.Builder` options used for calls:

| Option | Default | Notes |
|---|---|---|
| `setProviderId(id)` | none | The dashboard's FCM Provider ID. Without it `registerToken` fails with `PNError.Code.PROVIDER_REQUIRED`. |
| `setCallMode(mode)` | `CallMode.AUTO` | `MANUAL` only forwards the call ([step 8](#8-receive-a-call)). |
| `setAnswerActivity(activity)` | none | Your call screen, opened with the answered call. Required in `AUTO`. |
| `setIncomingCallActivity(activity)` | the SDK's screen | Replaces the SDK's full-screen ring screen with yours. |
| `setDefaultRingTimeoutMs(ms)` | 45 000 | How long a call rings when the push carries no timeout (held to 1 s – 10 min). |
| `setAutoRegisterToken(enabled)` | `true` | Registers the token on login and removes it on logout. The sample turns it off to control the order at logout ([step 6](#6-register-the-push-token-after-login-remove-it-before-logout)). |
| `setRequestCallPermissions(enabled)` | `true` | The notification's Answer asks for the microphone (and the camera for video) when missing. |
| `setShowMissedCallNotification(enabled)` | `true` | Missed-call notifications. |
| `setMissedCallActivity(activity)` | the launcher | Where a tapped missed-call notification goes. |
| `setIncomingCallTheme(theme)` | resources | Colours of the incoming-call screen and call notifications (`CometChatPNIncomingCallTheme`). |
| `overrideClientHost(host)` | production | Set it to the same host as your Chat and Calls SDKs if they use `overrideClientHost`; otherwise the token registers on the wrong host. |

A calls-only app can start the builder from the Calls SDK settings it already has, with
`PNConfiguration.from(callAppSettings)` (import `com.cometchat.pushnotification.calls.from`).

### 5. Forward FCM messages and tokens

Hand every FCM message and every new token to the SDK. From
[`push/RingingMessagingService.kt`](src/main/kotlin/com/cometchat/samplecallsringing/push/RingingMessagingService.kt):

```kotlin
import com.cometchat.pushnotification.CometChatPushNotifications
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RingingMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        try {
            // sentTime is the ring deadline's fallback: without it a push delivered late looks
            // fresh and rings for a call that is already over.
            val handled = CometChatPushNotifications.handlePushNotification(this, message.data, message.sentTime)
            if (!handled) {
                // Not a CometChat push: your own handling.
            }
        } catch (t: Throwable) {
            // A malformed push must never crash the service: the next call still has to ring.
            Log.e(TAG, "Failed to handle push", t)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CometChatPushNotifications.handleTokenRefresh(token)
    }
}
```

`handlePushNotification` returns `true` when the push was CometChat's. On a process that FCM just started, it
restores the configuration saved by `init`, so the call is not dropped. `handleTokenRefresh` re-registers the new
token when a user is logged in, or keeps it for the next login.

Declare the service in the manifest ([step 3](#3-update-the-manifest)).

### 6. Register the push token after login, remove it before logout

**Log in, then register.** Register after every login and on launch when a session was restored. With
`TokenRegistrar.AUTO` (the default) the token is registered under the Chat SDK's session when there is one and the
Calls SDK's otherwise, so a calls-only app needs no Chat SDK. The sample logs in to the Chat SDK and then the Calls
SDK, and registers once both succeeded ([`ui/activity/LoginActivity.kt`](src/main/kotlin/com/cometchat/samplecallsringing/ui/activity/LoginActivity.kt),
[`ui/activity/SplashActivity.kt`](src/main/kotlin/com/cometchat/samplecallsringing/ui/activity/SplashActivity.kt)):

```kotlin
import com.cometchat.calls.model.CallUser
import com.cometchat.pushnotification.PNCallback
import com.cometchat.pushnotification.PNResult

// Auth Key login is for development. In production, log in with an auth token from your server:
// CometChatCalls.login(authToken, listener).
CometChatCalls.login(uid, "YOUR_AUTH_KEY", object : CometChatCalls.CallbackListener<CallUser>() {
    override fun onSuccess(user: CallUser) {
        CometChatPushNotifications.registerToken(object : PNCallback<Unit> {
            override fun onResult(result: PNResult<Unit>) {
                if (!result.isSuccess) Log.e(TAG, "Registering the push token failed: ${result.errorOrNull()}")
            }
        })
    }

    override fun onError(e: CometChatException) { /* show the error */ }
})
```

**Unregister first, then log out.** The unregister request is authenticated by the session you are about to end.
Wait for its result and log out of the SDKs only if it succeeded. If you log out first, or log out after a failed
unregister, the request can no longer be retried and this device keeps ringing for the user who left. From
[`utils/SignOut.kt`](src/main/kotlin/com/cometchat/samplecallsringing/utils/SignOut.kt) and
[`push/PushNotifications.kt`](src/main/kotlin/com/cometchat/samplecallsringing/push/PushNotifications.kt):

```kotlin
CometChatPushNotifications.unregisterToken(object : PNCallback<Unit> {
    override fun onResult(result: PNResult<Unit>) {
        runOnUiThread {
            if (!result.isSuccess) {
                // Do not log out. Offer Retry, or "Sign out anyway" with a warning that this
                // device may keep ringing for this user.
                showUnregisterFailed(result.errorOrNull())
                return@runOnUiThread
            }
            CometChatCalls.logout(callsLogoutListener)   // then CometChat.logout(...) if you use the Chat SDK
        }
    }
})
```

The sample's Profile screen ([`ui/activity/ProfileActivity.kt`](src/main/kotlin/com/cometchat/samplecallsringing/ui/activity/ProfileActivity.kt))
offers **Retry**, **Sign out anyway** and **Cancel** when the unregister fails. A call that is ringing at logout
stops, with no decline and no missed call.

With `setAutoRegisterToken(true)` (the default) the SDK registers the token on login and removes it on logout by
itself, and you can skip both calls. The sample turns it off so that it can wait for the removal before logging out.

### 7. Place a call

The caller uses the Calls SDK only. `initiateCall` creates the call, rings the receiver (every member of a group)
and prepares the session while it rings. The sample's caller screen is
[`ui/activity/OutgoingCallActivity.kt`](src/main/kotlin/com/cometchat/samplecallsringing/ui/activity/OutgoingCallActivity.kt).

**Build the session settings once.** `initiateCall` freezes the settings on the session it prepares, and the
`joinSession` that follows ignores the ones it is given. Build both from one function, as
[`calls/CallSettings.kt`](src/main/kotlin/com/cometchat/samplecallsringing/calls/CallSettings.kt) does:

```kotlin
object CallSettings {
    fun make(title: String, isVideo: Boolean, isPeerCall: Boolean): SessionSettings =
        CometChatCalls.SessionSettingsBuilder()
            .setTitle(title)
            .setSessionType(if (isVideo) SessionType.VIDEO else SessionType.VOICE)
            .setLayout(if (isVideo) LayoutType.TILE else LayoutType.SPOTLIGHT)
            .setAudioMode(if (isVideo) AudioMode.SPEAKER else AudioMode.EARPIECE)
            .startVideoPaused(!isVideo)
            .startAudioMuted(false)
            .setPeerCall(isPeerCall)   // 1:1: ends for both when either leaves. Group: false
            .build()
}
```

**Listen, then initiate.** Register on `CallSession.getInstance()` before `initiateCall`, so no event from the
prepared session is missed:

```kotlin
val session = CallSession.getInstance()
session.addParticipantEventListener(this, object : ParticipantEventListener() {
    override fun onParticipantConnected(participant: Participant) {
        // The callee's device has the session: "Calling…" becomes "Ringing…".
    }
    override fun onParticipantJoined(participant: Participant) {
        // The callee answered: open the call screen (below).
    }
})
session.addSessionStatusListener(this, object : SessionStatusListener() {
    override fun onConnectionClosed() {
        // The prepared session went away while ringing. It carries no reason: read the call log (below).
    }
})

val options = InitiateCallOptions().apply {
    type = if (isVideo) Constants.SESSION_TYPE_VIDEO else Constants.SESSION_TYPE_AUDIO
    receiverType = if (isGroup) Constants.RECEIVER_TYPE_GROUP else Constants.RECEIVER_TYPE_USER
    sessionSettings = CallSettings.make(receiverName, isVideo, isPeerCall = !isGroup)
}
CometChatCalls.initiateCall(this, receiverId, options, object : CometChatCalls.CallbackListener<InitiatedCall>() {
    override fun onSuccess(call: InitiatedCall) {
        val sessionId = call.sessionId
        if (isFinishing || isDestroyed) {
            // The screen closed before the call existed: withdraw it so nobody rings for it.
            CometChatCalls.endCall(sessionId, Constants.CALL_STATUS_CANCELLED, endCallListener)
            CallSession.getInstance().leave()
            return
        }
        // Keep sessionId: endCall and joinSession use it. Show "Calling…",
        // start the 45 s ring timer and the 3 s decline poll.
    }

    override fun onError(e: CometChatException) {
        // Nothing exists on the server: show "Call failed".
    }
})
```

The status line goes **Calling…** (after `initiateCall`) → **Ringing…** (`onParticipantConnected`) → joined
(`onParticipantJoined`).

**Cancel and time out.** The end button, Back, or no answer within 45 seconds withdraws the call. That stops the
callee's phone ringing. The caller's 45 seconds are shorter than the callee's 60 seconds
(`setDefaultRingTimeoutMs`), so the callee never rings for a call nobody is waiting on.

```kotlin
// CALL_STATUS_UNANSWERED for a 1:1 timeout; CALL_STATUS_CANCELLED for a cancel, and for any group call.
CometChatCalls.endCall(sessionId, status, object : CometChatCalls.CallbackListener<Void>() {
    override fun onSuccess(result: Void?) {}
    override fun onError(e: CometChatException) { /* the far side may have ended it first */ }
})
CallSession.getInstance().leave()   // drop the prepared session
```

Before giving up on a timeout, the sample reads the call's status once: `ongoing` means the callee answered a
moment ago, so it joins instead.

**Detect a decline.** A decline reaches the server at once, but nothing pushes it to the caller. While the call
rings, read the call log every 3 seconds ([`data/repository/Repository.kt`](src/main/kotlin/com/cometchat/samplecallsringing/data/repository/Repository.kt)):

```kotlin
CallLogRequest.CallLogRequestBuilder().setLimit(5).build().fetchNext(
    object : CometChatCalls.CallbackListener<List<CallLog>>() {
        override fun onSuccess(logs: List<CallLog>) {
            when (logs.firstOrNull { it.sessionID == sessionId }?.status) {
                Constants.CALL_STATUS_REJECTED -> { /* "Declined" */ }
                Constants.CALL_STATUS_BUSY -> { /* "Busy" */ }
                Constants.CALL_STATUS_INITIATED, Constants.CALL_STATUS_ONGOING, null -> { /* still ringing */ }
                else -> { /* over for another reason: "Call ended" */ }
            }
        }
        override fun onError(e: CometChatException) { /* keep ringing: a failed lookup never ends a call */ }
    })
```

Run the same lookup when `onConnectionClosed` fires while ringing. If no callee device had connected yet, it was
your own connection that failed and the callee may still be ringing, so call `endCall` too.

The sample keeps all of these decisions in a small state machine,
[`calls/OutgoingCall.kt`](src/main/kotlin/com/cometchat/samplecallsringing/calls/OutgoingCall.kt)
(Initiating → Ringing → Joined / Ended(reason)), where every input is idempotent: duplicate SDK events and double
taps change nothing.

**Join the answered call.** On `onParticipantJoined`, open the call screen and join **by session ID** with the
same settings. That takes over the prepared session, so only the media and the call UI are left to set up. From
[`ui/activity/CallActivity.kt`](src/main/kotlin/com/cometchat/samplecallsringing/ui/activity/CallActivity.kt):

```kotlin
// Before joining: the SDK can report the join as soon as it starts.
CallSession.getInstance().addSessionStatusListener(this, object : SessionStatusListener() {
    override fun onSessionJoined() = CometChatOngoingCallService.launch(this@CallActivity)
    override fun onSessionLeft() = close()
    override fun onConnectionClosed() = close()   // the other side ended a 1:1 call
})

CometChatCalls.joinSession(sessionId, CallSettings.make(title, isVideo, isPeerCall), callContainer,
    object : CometChatCalls.CallbackListener<CallSession>() {
        override fun onSuccess(session: CallSession) {}
        override fun onError(e: CometChatException) {
            // Leave a half-joined session, or the next incoming call is rejected as busy.
            CallSession.getInstance().leave()
            // show the error and close the screen
        }
    })
```

`callContainer` is the `RelativeLayout` the call UI is drawn into. `close()` calls
`CometChatOngoingCallService.abort(this)` and finishes the screen.

**Leave the call** with `CallSession.getInstance().leave()`. A 1:1 call (`setPeerCall(true)`) ends for both sides;
the sample also marks it ended on the server with `endCall`. A group call carries on for whoever is left.

### 8. Receive a call

**`AUTO` mode.** With the configuration from step 4 and the FCM service from step 5, the SDK rings for you: the
notification with Answer and Decline, the full-screen screen over the lock screen, the prepared session, decline,
expiry, missed calls, the caller cancelling and call waiting. Your app only shows the answered call.

**The answered call.** Answering, from the notification or from your own screen, opens your answer activity with
the call in `CallBundles`. Read it and join by session ID as in step 7. From
[`ui/activity/CallActivity.kt`](src/main/kotlin/com/cometchat/samplecallsringing/ui/activity/CallActivity.kt):

```kotlin
import com.cometchat.pushnotification.calls.CallBundles
import com.cometchat.pushnotification.calls.CometChatCallNotifications

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_call)

    val answeredCall = CallBundles.getCall(intent)
    if (answeredCall != null) {
        sessionId = answeredCall.sessionId
        title = answeredCall.callerName ?: getString(R.string.unknown)
        isVideo = answeredCall.isVideo
        isPeerCall = answeredCall.isPeerCall   // false for a group call
        CometChatCallNotifications.onIncomingCallScreenClosed(sessionId)
    } else {
        // An outgoing call: the extras OutgoingCallActivity passed.
    }

    // Add the session listeners (step 7), ask for RECORD_AUDIO (and CAMERA for video) if missing,
    // then join: CometChatCalls.joinSession(sessionId, CallSettings.make(title, isVideo, isPeerCall), ...)
}
```

`IncomingCall` also carries `callerUid`, `callerAvatarUrl`, `callType`, `receiverType`, `receiverUid` and
`ringTimeoutMs`.

**Call waiting.** When the user answers a second call during a call ("End & accept"), the SDK leaves the live call
and opens the answer activity again. If that activity is on top, the new call arrives in `onNewIntent`. The sample's
`CallActivity.onNewIntent` reads it with `CallBundles.getCall(intent)` and starts a fresh `CallActivity` for it.

**Your own incoming-call screen (optional).** Set `setIncomingCallActivity(...)`. The SDK still owns the ringtone,
the prepared session, expiry, the keep-alive and the decline queue. Tell the SDK when your screen opens and closes,
so it can close it when the ring ends elsewhere. From
[`ui/activity/IncomingCallActivity.kt`](src/main/kotlin/com/cometchat/samplecallsringing/ui/activity/IncomingCallActivity.kt):

```kotlin
class IncomingCallActivity : AppCompatActivity() {
    private lateinit var sessionId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(...)
        val call = CallBundles.getCall(intent) ?: run { finish(); return }
        sessionId = call.sessionId
        // call.callerName, call.callerAvatarUrl, call.isVideo, call.isPeerCall (false = group call)

        // The ring ended for any reason: cancelled, timed out, answered or declined elsewhere.
        CometChatCallNotifications.onIncomingCallScreenOpened(sessionId,
            CometChatCallNotifications.RingEndListener { runOnUiThread { finish() } })

        acceptButton.setOnClickListener { CometChatCallNotifications.answer(this, sessionId); finish() }
        declineButton.setOnClickListener { CometChatCallNotifications.decline(this, sessionId); finish() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::sessionId.isInitialized) CometChatCallNotifications.onIncomingCallScreenClosed(sessionId)
    }
}
```

Without `onIncomingCallScreenOpened`, a cancelled call leaves your screen on the lock screen with an Answer button
for a call that no longer exists. If one activity is both your incoming-call screen and your answer activity, finish
it only when the `RingEndListener` reason is not `CometChatCallNotifications.RING_END_ANSWERED`.

**Knowing about a call in your own code.** The Calls SDK reports every incoming call, whether it arrived by push or
over the websocket, in `AUTO` and `MANUAL` alike:

```kotlin
CometChatCalls.addIncomingCallListener("my-listener-id", object : CometChatCalls.IncomingCallListener() {
    override fun onIncomingCallReceived(call: InitiatedCall) { /* ringing for this user */ }
    override fun onIncomingCallEnded(call: InitiatedCall, reason: String) { /* cancelled, rejected, busy, ended, unanswered; ongoing = answered elsewhere */ }
})
// CometChatCalls.removeIncomingCallListener("my-listener-id") when you no longer need it.
```

The id is a key: adding the same id again replaces the listener.

**`MANUAL` mode** (`setCallMode(CallMode.MANUAL)`). The SDK parses and forwards the call and never rings. Handle it
in `IncomingCallListener` and use the steps you want from `CometChatCallNotifications.Calls`:
`showIncomingNotification(context, call)`, `verify(context, call)` (prepares the session and confirms the call is
still live), `keepAlive(context, call)`, `stopRinging(context, sessionId)` and `showMissedCall(context, call)`, where
`call` is an `IncomingCall`. The [`voip-calling`](../../skills/voip-calling/SKILL.md)
skill builds a Telecom-based incoming UI on `MANUAL`.

### 9. Check ring readiness

Some settings silence a call without an error anywhere. `ensureRingReady` requests the notification permission when
it is missing, and otherwise names what is wrong. Call it once from a call-related screen, not from `onResume`. From
[`ui/activity/HomeActivity.kt`](src/main/kotlin/com/cometchat/samplecallsringing/ui/activity/HomeActivity.kt):

```kotlin
import com.cometchat.pushnotification.RingReadiness

CometChatPushNotifications.ensureRingReady(this, object : PNCallback<RingReadiness> {
    override fun onResult(result: PNResult<RingReadiness>) {
        val readiness = result.valueOrNull() ?: return
        // READY needs nothing; for NOTIFICATIONS_OFF the SDK asks for the permission itself.
        if (readiness == RingReadiness.READY || readiness == RingReadiness.NOTIFICATIONS_OFF) return
        runOnUiThread {
            val settings: Intent? = CometChatPushNotifications.settingsIntentFor(this@HomeActivity, readiness)
            // Explain the problem; if (settings != null), offer startActivity(settings).
        }
    }
})
```

| `RingReadiness` | Meaning | `settingsIntentFor` opens |
|---|---|---|
| `READY` | Nothing in the way. | `null` |
| `NOTIFICATIONS_OFF` | Notifications are off for the app: nothing can ring. | The app's notification settings |
| `CHANNEL_MUTED` | The user lowered the incoming-call channel. | That channel's settings |
| `FSI_REVOKED` | Android 14+: full-screen intents revoked, so no ring screen over the lock screen. | The full-screen intent setting (`null` below Android 14) |
| `DND_BLOCKING` | Do Not Disturb is filtering the call. | Do Not Disturb settings |
| `MICROPHONE_DENIED` | The microphone was refused when answering: calls still ring, but join muted. | The app's details page |

`settingsIntentFor` returns `Intent?`; check for `null` before starting it. The sample shows a dialog for
`FSI_REVOKED`, `CHANNEL_MUTED` and `DND_BLOCKING`, and does not prompt for `MICROPHONE_DENIED`. For a bug report,
`CometChatPushNotifications.getDiagnostics(context)` returns a redacted snapshot (no token, auth value or payload).

### 10. Group calls

- Ring a group with `receiverType = Constants.RECEIVER_TYPE_GROUP`, the group's GUID as the receiver, and
  `setPeerCall(false)`. Without `setPeerCall(false)` the session ends when the first member leaves.
- Every member's devices ring. The first to answer is the caller's `onParticipantJoined`, and the caller joins;
  other members can answer and join while the call lasts. The caller's status line counts the members whose devices are ringing.
- A member who declines stops the ring on that device only. Nothing is sent, because `endCall` is call-wide and
  would end the call for everyone, so the others keep ringing and the caller's poll does not see a decline.
- A member who does not answer gets a missed call; the call goes on for the others.
- The caller withdraws a group call with `CALL_STATUS_CANCELLED`, also on its ring timeout: "unanswered" is each
  member's own outcome.
- The sample's **New group call** screen ([`ui/activity/NewGroupCallActivity.kt`](src/main/kotlin/com/cometchat/samplecallsringing/ui/activity/NewGroupCallActivity.kt))
  creates a private group with the Chat SDK and rings it. It limits the group to 20 members, because every
  member's phone rings.

## How the sample works

### Screens

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

### How a call works

**Placing a call** (`OutgoingCallActivity`)

1. `CometChatCalls.initiateCall()` creates the call and rings the callee (every member, when `receiverType` is
   group), and starts preparing the session with the settings from `calls/CallSettings.kt`.
2. `onParticipantConnected`: the callee's phone has the session, and "Calling…" becomes "Ringing…".
3. `onParticipantJoined`: the callee answered. `CallActivity` joins the prepared session by its ID.
4. The end button, Back, or no answer after 45 seconds withdraws the call with `CometChatCalls.endCall()`, which
   stops the callee's phone ringing.
5. While the call rings, the app reads the call log every 3 seconds and shows "Declined" or "Busy".

`calls/OutgoingCall.kt` makes every decision. It is a small state machine with no Android or SDK code in it.

**Receiving a call** (`push/`, `IncomingCallActivity`, `CallActivity`)

1. `RingingApplication` initializes the SDKs on every process start, with the Push Notifications SDK in `AUTO` call
   mode.
2. `RingingMessagingService` hands each FCM message to the SDK. The SDK shows the ringing notification and the
   full-screen `IncomingCallActivity`, prepares the session while it rings, and handles decline, expiry, missed
   calls and the caller cancelling.
3. When the user answers, the SDK opens `CallActivity` with the answered call in `CallBundles`. It joins by session
   ID.
4. On the home screen, the app checks ring readiness and offers the matching settings page.

**Signing out** waits for `CometChatPushNotifications.unregisterToken()` and logs out of the SDKs only if it
succeeded. If it fails, the app offers **Retry** or **Sign out anyway** (with a warning that this device may keep
ringing).

### Project Structure

```
sample-app-ringing/
├── src/main/kotlin/com/cometchat/samplecallsringing/
│   ├── RingingApplication.kt            # Appearance + SDK init on every process start
│   ├── calls/
│   │   ├── OutgoingCall.kt              # Caller state machine: Initiating → Ringing → Joined / Ended
│   │   ├── CallSettings.kt              # Session settings shared by initiateCall and joinSession
│   │   ├── CallLauncher.kt              # Permissions + double-tap guard, then the outgoing screen
│   │   ├── CallTarget.kt                # A user or a group to ring
│   │   └── CallLogDisplay.kt            # How a call log reads: kind, status, who, dates, duration
│   ├── push/
│   │   ├── PushNotifications.kt         # Push SDK configuration (AUTO call mode), token register/unregister
│   │   └── RingingMessagingService.kt   # FCM entry point
│   ├── data/repository/Repository.kt    # Login/logout, sample users, endCall, call status lookup
│   ├── ui/activity/
│   │   ├── SplashActivity.kt            # Waits for SDK init, registers the token for a saved session
│   │   ├── AppCredentialsActivity.kt    # Region, credentials and push provider ID
│   │   ├── LoginActivity.kt             # Sample user / UID login, then registerToken
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
│   └── utils/
│       ├── SdkInit.kt                   # Push, Chat and Calls SDK init, once per process
│       ├── SignOut.kt                   # Unregister the token, then log out
│       └── ...                          # Constants, preferences, appearance
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

The receiver builds the same data payload FCM delivers and passes it to `handlePushNotification`, so the real ring
path runs. It tests the ring, not the push: only a real call to a killed app proves that FCM delivery works.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| No ring at all | **Provider ID** missing or wrong: `registerToken` fails (`PNError.Code.PROVIDER_REQUIRED` when it is missing). Check the log for the `registerToken` result and compare the ID with the dashboard. |
| No ring, token registered fine | **Firebase project mismatch**: the dashboard's FCM service account and your `google-services.json` are from different Firebase projects. Use one project for both. |
| No ring for one user | **Token not registered**: with `setAutoRegisterToken(false)`, call `registerToken()` after every login and on launch with a saved session. |
| No ring when the app was swiped away or the phone was idle | **Battery optimisation** or the manufacturer's background restrictions stop FCM from starting the app, and a force-stopped app gets no pushes until it is opened again. Set the app's battery usage to unrestricted on those devices. |
| Notification but no screen over the lock screen | **Full-screen intents revoked** (Android 14+). `ensureRingReady` reports `FSI_REVOKED`; open `settingsIntentFor(context, RingReadiness.FSI_REVOKED)`. |
| Silent ring, or none | **Do Not Disturb** (`DND_BLOCKING`), the **incoming-call channel muted** (`CHANNEL_MUTED`), or **notifications off** (`NOTIFICATIONS_OFF`). See [step 9](#9-check-ring-readiness). |
| Rings for a call that is already over | Pass `message.sentTime` to `handlePushNotification`. |
| Build fails: `google-services.json` is missing | The Google Services plugin needs the file in the app module (`samples/sample-app-ringing/` for the sample). Download it from your Firebase project. |
| Build fails on the AAR metadata check for `androidx.core` 1.18.0 | Set `compileSdk = 36` and use Android Gradle Plugin 8.9.1 or later. |
| Build fails: Kotlin metadata or `kotlin-stdlib` version error | Use Kotlin 2.1 or later. |
| Call joins muted | The microphone was denied. `ensureRingReady` reports `MICROPHONE_DENIED`; `settingsIntentFor` opens the app's details page so the user can grant it. |
| The call drops when the app goes to the background | `CometChatOngoingCallService` is not declared in your manifest, or not started on `onSessionJoined` ([step 3](#3-update-the-manifest)). |
| Still ringing after logout | The token was not removed: `unregisterToken` failed, or the app logged out before it finished. Unregister first and log out only on success ([step 6](#6-register-the-push-token-after-login-remove-it-before-logout)). |
| The next incoming call is rejected as busy | A half-joined session was left behind. Call `CallSession.getInstance().leave()` when `joinSession` fails or the call screen closes with its session still live. |

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

## Links

- [Calls SDK for Android documentation](https://www.cometchat.com/docs/calls/android/overview)
- [Android push notifications](https://www.cometchat.com/docs/notifications/android-push-notifications)
- [VoIP calling on Android](https://www.cometchat.com/docs/calls/android/voip-calling)
- [Key concepts](https://www.cometchat.com/docs/fundamentals/key-concepts)
- Skills: [`ringing-integration`](../../skills/ringing-integration/SKILL.md) (this flow),
  [`voip-calling`](../../skills/voip-calling/SKILL.md) (`MANUAL` mode with Telecom),
  [`setup`](../../skills/setup/SKILL.md), [`session-settings`](../../skills/session-settings/SKILL.md),
  [`event-listeners`](../../skills/event-listeners/SKILL.md), [`call-logs`](../../skills/call-logs/SKILL.md)
- [Support](https://help.cometchat.com/hc/en-us)
