---
name: background-handling
description: Keep calls alive in background using CometChatOngoingCallService foreground service. Use when handling home button press during calls, ongoing call notifications, or preventing call termination. Triggers on "background handling", "foreground service", "ongoing call", "CometChatOngoingCallService", "keep call alive".
inclusion: manual
---

# CometChat Calls SDK v5 — Background Handling

## Overview

`CometChatOngoingCallService` is a foreground service that keeps calls alive when users press HOME or switch apps. Shows an ongoing notification with tap-to-return and hangup actions.

## Prerequisites

- Active call session
- Foreground service permissions in manifest

## Key Imports

```kotlin
import com.cometchat.calls.services.CometChatOngoingCallService
import com.cometchat.calls.utils.OngoingNotification
import com.cometchat.calls.constants.Constants
import com.cometchat.calls.core.CallSession
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException
import com.cometchat.calls.listeners.SessionStatusListener
import com.cometchat.calls.listeners.ButtonClickListener
```

## Implementation

### 1. Manifest Permissions

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Declare the service too. The Calls SDK 5.1 manifest does not declare `CometChatOngoingCallService`:

```xml
<!-- Keeps a joined call alive in the background with an ongoing-call notification. The Calls
     SDK does not declare it: without this entry CometChatOngoingCallService.launch() finds no
     service and the call drops when the app leaves the screen. -->
<service
    android:name="com.cometchat.calls.services.CometChatOngoingCallService"
    android:foregroundServiceType="camera|microphone|mediaPlayback"
    android:exported="false" />
```

### 2. Start/Stop Service

```kotlin
callSession.addSessionStatusListener(this, object : SessionStatusListener() {
    override fun onSessionJoined() {
        CometChatOngoingCallService.launch(this@CallActivity)
    }
    override fun onSessionLeft() {
        CometChatOngoingCallService.abort(this@CallActivity)
        finish()
    }
    override fun onConnectionClosed() {
        CometChatOngoingCallService.abort(this@CallActivity)
        finish()
    }
    override fun onSessionTimedOut() {
        CometChatOngoingCallService.abort(this@CallActivity)
        finish()
    }
    override fun onConnectionLost() {}
    override fun onConnectionRestored() {}
})
```

### 3. Handle Leave Button

```kotlin
callSession.addButtonClickListener(this, object : ButtonClickListener() {
    override fun onLeaveSessionButtonClicked() {
        endCall()
    }
})
```

### 4. Handle Remote Call End

The Calls SDK 5.1 reports the far side ending the call on the session itself; no Chat SDK listener is involved.
Build a 1:1 call with `setPeerCall(true)` (on both sides) and it ends for both when either leaves: the side that
stayed gets `onConnectionClosed` (or `onSessionLeft`), which the listener from step 2 already handles. A group call
(`setPeerCall(false)`) carries on for whoever is left.

When hanging up a 1:1 call yourself, leave and also mark it ended on the server, as the sample's `CallActivity` does:

```kotlin
// sessionId and isPeerCall: the call this screen joined
private fun endCall() {
    val session = CallSession.getInstance()
    if (session.isSessionActive) session.leave()
    if (isPeerCall) {
        CometChatCalls.endCall(sessionId, Constants.CALL_STATUS_ENDED, object : CometChatCalls.CallbackListener<Void>() {
            override fun onSuccess(result: Void?) {}
            override fun onError(e: CometChatException) { /* the far side may already have ended it */ }
        })
    }
    CometChatOngoingCallService.abort(this)
    finish()
}
```

Before the call is answered there is no session to watch. With the Push Notifications SDK 2.0 the ringing screen
learns the ring ended (cancelled, timed out, answered or declined on another device) from
`CometChatCallNotifications.onIncomingCallScreenOpened(sessionId, RingEndListener)` (`ringing-integration` §7).

### 5. Always Stop in onDestroy

```kotlin
override fun onDestroy() {
    super.onDestroy()
    CometChatOngoingCallService.abort(this)
}
```

The session listeners are lifecycle-aware (`addSessionStatusListener(owner, …)`) and need no removal.

### 6. Custom Notification (Optional)

```kotlin
private fun buildCustomNotification(): Notification {
    val channelId = "CometChat_Call_Ongoing_Conference"  // must use this ID
    val intent = Intent(this, CallActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    return NotificationCompat.Builder(this, channelId)
        .setSmallIcon(R.drawable.ic_call)
        .setContentTitle("Ongoing Call")
        .setContentText("Tap to return to your call")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .build()
}

// Set before launching service
OngoingNotification.buildOngoingConferenceNotification(buildCustomNotification())
CometChatOngoingCallService.launch(this)
```

### 7. VoIP Integration — Additional Cleanup (Optional)

When you build your own incoming UI on Telecom (the optional `voip-calling` skill: ConnectionService + TelecomManager, Push SDK in `CallMode.MANUAL`), also disconnect the `CallConnection` and cancel any VoIP notifications on every terminal state:

```kotlin
// In each terminal callback (onSessionLeft, onSessionTimedOut, onConnectionClosed):
CometChatOngoingCallService.abort(this@CallActivity)
CallConnectionHolder.endCall()
CallNotificationManager.cancelNotification(this@CallActivity)
finish()
```

## API Reference

| Method | Description |
|--------|-------------|
| `CometChatOngoingCallService.launch(context)` | Start foreground service |
| `CometChatOngoingCallService.abort(context)` | Stop foreground service |
| `OngoingNotification.buildOngoingConferenceNotification(notification)` | Set custom notification (call before `launch()`) |

## Gotchas

- The notification channel ID **must** be `"CometChat_Call_Ongoing_Conference"`
- Without the `<service>` entry in your manifest, `launch()` finds no service and the call drops in the background
- Always call `abort()` in `onDestroy()` to prevent leaked services
- A remote hang-up only ends your side of a 1:1 call if both sides joined with `setPeerCall(true)`
- This is for keeping **active** calls alive — for receiving calls when app is killed, use VoIP Calling
- Call `launch()` only after `onSessionJoined()` — not before joining
- Custom notification must be set **before** calling `launch()`
- When using your own Telecom incoming UI (ConnectionService), also call `CallConnectionHolder.endCall()` in every terminal state to properly disconnect the Telecom connection

## Sample App Reference

- `sample-app-ringing` — `AndroidManifest.xml` — the `CometChatOngoingCallService` declaration
- `sample-app-ringing` — `CallActivity.kt` — basic foreground service pattern with `launch()` in `onSessionJoined()`, `abort()` in `onSessionLeft()` / `onConnectionClosed()` and `onDestroy()`, and `endCall()` for a 1:1 hang-up
