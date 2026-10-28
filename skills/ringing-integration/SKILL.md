---
name: ringing-integration
description: Implement 1:1 and group ringing with the Calls SDK 5.1 (initiateCall / endCall, prepared sessions) and the Push Notifications SDK 2.0 (FCM push, full-screen incoming call, answer/decline in every app state). Use when adding outgoing or incoming call screens, ringing a user or a group, cancelling, detecting declines, registering push tokens, or presenting an answered call. Triggers on "ringing", "incoming call", "outgoing call", "initiateCall", "endCall", "group call", "call back", "push notification call", "killed state", "not ringing".
inclusion: manual
---

# CometChat Calls SDK v5.1 — Ringing

## Overview

Ringing is split between two SDKs:

| Side | SDK | What it does |
|---|---|---|
| Caller | Calls SDK 5.1 | `initiateCall` creates the call, rings the receiver and **prepares the session** while it rings; `endCall` withdraws it |
| Callee | Push Notifications SDK 2.0 (`push-notifications-calls-android`) | Receives the FCM push, shows the ringing notification and a full-screen incoming-call screen, prepares the session, answers or declines. The app only shows its call screen |

The Chat SDK is not part of the call flow. The sample still uses it for the user and group lists.

There is no `CometChat.initiateCall` / `acceptCall` / `rejectCall` / `CometChat.CallListener` in this flow.

## Prerequisites

```kotlin
implementation("com.cometchat:calls-sdk-android:5.1.0")
implementation("com.cometchat:push-notifications-android:2.0.0")
implementation("com.cometchat:push-notifications-calls-android:2.0.0")
implementation("com.google.firebase:firebase-messaging:24.1.0")
```

- Build: **compileSdk 36** and **Android Gradle Plugin 8.9.1+**. The Push SDK brings `androidx.core` 1.18.0, whose AAR metadata sets `minCompileSdk=36` and `minAndroidGradlePluginVersion=8.9.1`. Use Kotlin 2.1: the Push SDK and `androidx.core` 1.18.0 depend on `kotlin-stdlib` 2.1.20, and the sample builds with AGP 8.11.2 and Kotlin 2.1.20
- Calls SDK initialized and the user logged in (`CometChatCalls.login`)
- An **FCM push provider** on the dashboard (Notifications → Push), using the same Firebase project as your `google-services.json`. Its **provider ID** goes in the push SDK configuration
- Runtime permissions **before** `initiateCall`: `RECORD_AUDIO`, plus `CAMERA` for video. The media session is prepared straight away
- `POST_NOTIFICATIONS` (Android 13+) and `USE_FULL_SCREEN_INTENT` in the manifest, so the call can ring over the lock screen

## 1. Configure the push SDK on every process start

FCM can start a killed process for a call, before any screen exists. Configure the SDK in `Application.onCreate`:

```kotlin
class RingingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CometChatPushNotifications.init(
            this,
            PNConfiguration.Builder(APP_ID, REGION)
                .setProviderId(PUSH_PROVIDER_ID)           // required to register tokens
                .setAutoRegisterToken(false)               // the app registers after login
                .setCallMode(CallMode.AUTO)                // the SDK rings, declines, records missed calls
                .setIncomingCallActivity(IncomingCallActivity::class.java)
                .setAnswerActivity(CallActivity::class.java)
                .setDefaultRingTimeoutMs(60_000L)          // longer than the caller's 45 s
                .build()
        )
    }
}
```

Hand FCM messages and tokens to the SDK:

```kotlin
class RingingMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        try {
            CometChatPushNotifications.handlePushNotification(this, message.data, message.sentTime)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to handle push", t)  // a bad push must not crash the next ring
        }
    }

    override fun onNewToken(token: String) = CometChatPushNotifications.handleTokenRefresh(token)
}
```

## 2. Tokens: login and logout

```kotlin
// After login (and on launch with a saved session):
CometChatPushNotifications.registerToken(object : PNCallback<Unit> {
    override fun onResult(result: PNResult<Unit>) { /* log result.errorOrNull() */ }
})

// Before logout — WAIT for it: the request is authenticated by the session you are ending.
CometChatPushNotifications.unregisterToken(object : PNCallback<Unit> {
    override fun onResult(result: PNResult<Unit>) {
        if (!result.isSuccess) {
            // Do not log out: offer Retry, or "Log out anyway" with a warning that this device may keep ringing.
            showUnregisterFailed(result.errorOrNull())
            return
        }
        CometChatCalls.logout(callsCallback)
        CometChat.logout(chatCallback)
    }
})
```

If you don't wait, or log out after a failed unregister, the device keeps ringing for the user who left: once logged out the request can no longer be retried. The sample's `ProfileActivity` offers Retry / Sign out anyway.

## 3. Session settings: build them once

`initiateCall` freezes the settings on the session it prepares, and the `joinSession` that follows **ignores** the ones it is given. Build both from one function:

```kotlin
object CallSettings {
    fun make(title: String, isVideo: Boolean, isPeerCall: Boolean): SessionSettings =
        CometChatCalls.SessionSettingsBuilder()
            .setTitle(title)
            .setSessionType(if (isVideo) SessionType.VIDEO else SessionType.VOICE)
            .startVideoPaused(!isVideo)
            .startAudioMuted(false)
            .setPeerCall(isPeerCall)   // 1:1: ends for both when either leaves; group: false
            .build()
}
```

## 4. Place the call (caller)

Register on `CallSession.getInstance()` **before** `initiateCall`, so no event is missed:

```kotlin
val session = CallSession.getInstance()
session.addParticipantEventListener(this, object : ParticipantEventListener() {
    override fun onParticipantConnected(participant: Participant) { /* callee's device has the session: "Ringing…" */ }
    override fun onParticipantJoined(participant: Participant) { /* answered: open the call screen */ }
})
session.addSessionStatusListener(this, object : SessionStatusListener() {
    override fun onConnectionClosed() { /* the prepared session went away: ask the server why (step 6) */ }
})

val options = InitiateCallOptions().apply {
    type = if (isVideo) Constants.SESSION_TYPE_VIDEO else Constants.SESSION_TYPE_AUDIO
    receiverType = if (isGroup) Constants.RECEIVER_TYPE_GROUP else Constants.RECEIVER_TYPE_USER
    sessionSettings = CallSettings.make(name, isVideo, isPeerCall = !isGroup)
}
CometChatCalls.initiateCall(context, uidOrGuid, options, object : CometChatCalls.CallbackListener<InitiatedCall>() {
    override fun onSuccess(call: InitiatedCall) {
        // Keep call.sessionId: endCall and joinSession use it. Show "Calling…", start the
        // 45 s ring timer and the decline poll (step 6).
    }
    override fun onError(e: CometChatException) { /* nothing exists on the server: "Call failed" */ }
})
```

A group receiver rings every member. The first to answer joins, and others can join while the call lasts.

If the screen closes before `onSuccess` arrives, withdraw the call there: `endCall(sessionId, CALL_STATUS_CANCELLED)`.

## 5. Join the accepted call

Join **by session ID**. That takes over the prepared session, so only the media and the UI are left to set up:

```kotlin
CometChatCalls.joinSession(sessionId, CallSettings.make(name, isVideo, !isGroup), container,
    object : CometChatCalls.CallbackListener<CallSession>() {
        override fun onSuccess(session: CallSession) { /* add listeners; start CometChatOngoingCallService */ }
        override fun onError(e: CometChatException) { /* show the error, close the screen */ }
    })
```

The Calls SDK 5.1 manifest does not declare `CometChatOngoingCallService` (it does merge the `FOREGROUND_SERVICE_CAMERA`, `_MICROPHONE` and `_MEDIA_PLAYBACK` permissions). Declare the service in your app's manifest, as the sample does:

```xml
<!-- Keeps a joined call alive in the background with an ongoing-call notification. The Calls
     SDK does not declare it: without this entry CometChatOngoingCallService.launch() finds no
     service and the call drops when the app leaves the screen. -->
<service
    android:name="com.cometchat.calls.services.CometChatOngoingCallService"
    android:foregroundServiceType="camera|microphone|mediaPlayback"
    android:exported="false" />
```

## 6. Cancel, timeout and decline

```kotlin
// End button, back, or no answer after 45 s (shorter than the callee's 60 s ring):
CometChatCalls.endCall(sessionId, status, callback)   // UNANSWERED on a 1:1 timeout, otherwise CANCELLED
CallSession.getInstance().leave()                     // drop the prepared session
```

On a group call, a timeout is `CALL_STATUS_CANCELLED` too: "unanswered" describes one member.

A decline reaches the server at once, but nothing pushes it to the caller. While ringing, poll the call log every 3 seconds:

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

Run the same lookup when `onConnectionClosed` fires while ringing.
- If no callee device had connected yet, your own connection failed. The callee may still be ringing, so call `endCall` too.

Keep these decisions in a small state machine (Initiating → Ringing → Joined / Ended(reason)), where every input is idempotent. Duplicate SDK events and double taps then change nothing.

## 7. Incoming calls (callee)

In `AUTO` mode the SDK shows the ringing notification and opens your incoming-call activity with the call:

```kotlin
class IncomingCallActivity : AppCompatActivity() {
    private lateinit var sessionId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val call = CallBundles.getCall(intent) ?: return finish()
        sessionId = call.sessionId
        // call.callerName, call.callerAvatarUrl, call.isVideo, call.isPeerCall (false = group call)

        // Ring ended for any reason: cancelled, timed out, answered or declined elsewhere.
        CometChatCallNotifications.onIncomingCallScreenOpened(call.sessionId,
            CometChatCallNotifications.RingEndListener { runOnUiThread { finish() } })

        acceptButton.setOnClickListener { CometChatCallNotifications.answer(this, call.sessionId); finish() }
        declineButton.setOnClickListener { CometChatCallNotifications.decline(this, call.sessionId); finish() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::sessionId.isInitialized) {
            CometChatCallNotifications.onIncomingCallScreenClosed(sessionId)
        }
    }
}
```

The answer, from this screen or from the notification, opens the answer activity with the call in `CallBundles`. Join it by `call.sessionId` with `isPeerCall = call.isPeerCall` (step 5).

Declare both activities with `showWhenLocked="true"` and `turnScreenOn="true"`.

## 8. Ring readiness

Some settings silence a ring and nothing reports an error:
- full-screen notifications revoked (Android 14+),
- the incoming-call channel muted,
- Do Not Disturb.

One more spoils the answer instead: the microphone was refused when answering (`MICROPHONE_DENIED`). Calls still ring, but an answered call joins muted until it is granted in the app's settings.

The SDK can tell you which one applies:

```kotlin
CometChatPushNotifications.ensureRingReady(activity, object : PNCallback<RingReadiness> {
    override fun onResult(result: PNResult<RingReadiness>) {
        val readiness = result.valueOrNull() ?: return
        if (readiness != RingReadiness.READY && readiness != RingReadiness.NOTIFICATIONS_OFF) {
            // FSI_REVOKED, CHANNEL_MUTED, DND_BLOCKING or MICROPHONE_DENIED
            val settings = CometChatPushNotifications.settingsIntentFor(activity, readiness)  // Intent?
            // explain; if (settings != null), offer startActivity(settings)
        }
    }
})
```

## 9. Leave a call

```kotlin
CallSession.getInstance().leave()
```

A 1:1 call (`setPeerCall(true)`) ends for both sides. A group call carries on for whoever is left. Take the screen down on `onSessionLeft` / `onConnectionClosed`.

## Gotchas

- **Settings passed to join are ignored for a prepared session.** Build them with the same function on both sides.
- **Microphone (and camera) before `initiateCall`.** Preparing creates the media session.
- **Group calls:** use `receiverType` group plus `setPeerCall(false)`. Without that, the session ends when the first member leaves.
- **Don't wait for a push to learn about a decline:** poll the call log while ringing.
- **Caller timeout shorter than the callee's ring** (45 s vs 60 s), so the callee never rings for a call nobody is waiting on.
- **Unregister the token before logout, wait for it to finish, and don't log out if it failed** (offer a retry).
- **Only a killed app proves the push works.** When the app is open, the ring can still arrive another way.

## Sample App Reference

`samples/sample-app-ringing`:

- `calls/OutgoingCall.kt` — caller state machine
- `ui/activity/OutgoingCallActivity.kt` — `initiateCall`, listeners, cancel / timeout, decline polling
- `calls/CallSettings.kt` — the shared session settings
- `ui/activity/CallActivity.kt` — joins by session ID, for outgoing and answered calls
- `ui/activity/IncomingCallActivity.kt` — the ring screen
- `push/PushNotifications.kt`, `push/RingingMessagingService.kt` — push SDK setup, tokens, FCM
- `ui/activity/NewGroupCallActivity.kt` — creating a group from picked members and ringing it
