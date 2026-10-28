---
name: voip-calling
description: Build your own incoming-call UI on Android's Telecom framework (self-managed ConnectionService, PhoneAccount, system call UI) on top of the Push Notifications SDK 2.0 in CallMode.MANUAL and the Calls SDK 5.1. Optional — the Push SDK's default AUTO mode already rings in every app state. Use for Bluetooth/car integration or the system call UI. Triggers on "VoIP", "voip calling", "ConnectionService", "PhoneAccount", "Telecom", "CallMode.MANUAL", "own incoming call UI".
inclusion: manual
---

# CometChat Calls SDK v5 — VoIP Calling (own incoming UI, optional)

> **On Calls SDK 5.1 or later, start with the `ringing-integration` skill instead.** The Push Notifications
> SDK 2.0 rings incoming calls itself in its default `CallMode.AUTO`: FCM, the ringing notification, the full-screen
> screen over the lock screen, answer, decline and missed calls, in every app state. That is what
> `samples/sample-app-ringing` uses, and it replaces the Telecom `ConnectionService` stack of Push SDK 1.x.
> This skill is the **optional** path for building the incoming side yourself on Android's Telecom framework, with
> the Push SDK in `CallMode.MANUAL`. No sample app for this approach is published in this repository.

The parts that do not change in this path:

- **Push tokens** are registered by the Push SDK: `CometChatPushNotifications.registerToken()` after login,
  `handleTokenRefresh()` from FCM, `unregisterToken()` before logout (`ringing-integration` §2). Never register
  tokens through the Chat SDK.
- **Signalling** is the Calls SDK 5.1: the caller rings with `CometChatCalls.initiateCall` and withdraws with
  `endCall` (`ringing-integration` §4–6). The callee answers by joining the session and declines with
  `CometChatCalls.endCall(sessionId, Constants.CALL_STATUS_REJECTED, …)`. There is no `CometChat.initiateCall`,
  `acceptCall`, `rejectCall` or `CometChat.CallListener` in this flow.

## Overview

In `MANUAL` the Push SDK parses, gates and forwards an incoming call to the Calls SDK, and does nothing else: it
never rings. The call surfaces on `CometChatCalls.IncomingCallListener`, whichever way it arrived (a push that
woke a killed process, or the websocket of a running app). From there the app rings through Telecom: a
self-managed `ConnectionService` shows the system call UI on the lock screen and works with Bluetooth and car
systems. When Telecom is unavailable it falls back to a high-priority full-screen notification.

## Prerequisites

- Calls SDK 5.1 + Push Notifications SDK 2.0, as in `ringing-integration`:
  `com.cometchat:calls-sdk-android:5.1.0`, `com.cometchat:push-notifications-android:2.0.0`,
  `com.cometchat:push-notifications-calls-android:2.0.0`. The Chat SDK is not needed for calls
- Firebase Cloud Messaging (FCM) configured with the `com.google.gms.google-services` plugin
- An FCM push provider on the dashboard (Notifications → Push) and its provider ID
- Android 8.0+ (API 26+), compileSdk 36 and Android Gradle Plugin 8.9.1+ (required by the Push SDK 2.0)

## Key Components

| Component | Purpose |
|-----------|---------|
| `VoIPApplication` | Configures the Push SDK in `MANUAL`, inits the Calls SDK, registers the `IncomingCallListener` |
| `RingingMessagingService` | Hands FCM messages and refreshed tokens to the Push SDK |
| `CallConnectionService` | Bridges app with Android Telecom, creates CallConnection instances |
| `PhoneAccountManager` | Registers app as self-managed VoIP calling provider |
| `CallConnection` | Represents individual call (answer/reject) in Telecom framework |
| `CallNotificationManager` | Routes calls to TelecomManager with fallback to high-priority notification |
| `CallActionReceiver` | Handles accept/decline from fallback notification |
| `CallConnectionHolder` | Singleton for cross-component access to active CallConnection |
| `CallData` | Parcelable call metadata passed between components |

## Incoming Call Path

| App State | Transport | Handler | UI |
|-----------|-----------|---------|-----|
| Any | FCM push (Push SDK) or websocket | `CometChatCalls.IncomingCallListener.onIncomingCallReceived` → `CallNotificationManager` → `TelecomManager` | System call UI |
| Any (fallback) | same | `CallNotificationManager` → high-priority notification | `IncomingCallActivity` via full-screen intent |

## Implementation

### 1. CallData Parcelable Model

```kotlin
import android.os.Parcelable
import com.cometchat.calls.constants.Constants
import com.cometchat.calls.model.InitiatedCall
import kotlinx.parcelize.Parcelize

@Parcelize
data class CallData(
    val sessionId: String,
    val callerName: String,
    val callerUid: String,
    val callType: String,       // Constants.SESSION_TYPE_AUDIO or SESSION_TYPE_VIDEO
    val callerAvatar: String?,
    val isGroup: Boolean        // a group call is not a peer call: setPeerCall(false)
) : Parcelable {
    companion object {
        fun from(call: InitiatedCall): CallData? {
            val sessionId = call.sessionId ?: return null
            return CallData(
                sessionId = sessionId,
                callerName = call.senderName ?: "",
                callerUid = call.sender ?: "",
                callType = call.type ?: Constants.SESSION_TYPE_AUDIO,
                callerAvatar = call.senderAvatar,
                isGroup = call.receiverType == Constants.RECEIVER_TYPE_GROUP
            )
        }
    }
}
```

### 2. VoIPApplication (Push SDK in MANUAL + IncomingCallListener)

FCM can start a killed process for a call, and the Push SDK forwards it at once, so configure everything in
`Application.onCreate`:

```kotlin
class VoIPApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PhoneAccountManager.register(this)

        CometChatPushNotifications.init(
            this,
            PNConfiguration.Builder(APP_ID, REGION)
                .setProviderId(PUSH_PROVIDER_ID)   // required: token registration fails without it
                .setAutoRegisterToken(false)       // registerToken() after login
                .setCallMode(CallMode.MANUAL)      // forward only: the app rings (no answer activity needed)
                .build()
        )
        // Init the Calls SDK here too (CometChatCalls.init), so answering from a killed state can join.

        CometChatCalls.addIncomingCallListener(LISTENER_ID, object : CometChatCalls.IncomingCallListener() {
            override fun onIncomingCallReceived(call: InitiatedCall) {
                val data = CallData.from(call) ?: return
                CallNotificationManager.showIncomingCall(this@VoIPApplication, data)
            }

            override fun onIncomingCallEnded(call: InitiatedCall, reason: String) {
                // Cancelled, unanswered, or answered/declined on another device ("ongoing" = answered elsewhere).
                CallConnectionHolder.ringEnded()
                CallNotificationManager.cancelNotification(this@VoIPApplication)
            }
        })
    }

    companion object {
        private const val LISTENER_ID = "voip_incoming_calls"
    }
}
```

Both callbacks arrive on the main thread.

### 3. FCM Service

```kotlin
class RingingMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            // In MANUAL this parses and forwards the call to the IncomingCallListener; it never rings.
            CometChatPushNotifications.handlePushNotification(this, message.data, message.sentTime)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to handle push", t)  // a bad push must not crash the next ring
        }
    }

    // The Push SDK owns the token; it re-registers it for the signed-in user.
    override fun onNewToken(token: String) = CometChatPushNotifications.handleTokenRefresh(token)

    companion object {
        private const val TAG = "RingingMessagingService"
    }
}
```

### 4. Register PhoneAccount

```kotlin
object PhoneAccountManager {
    private const val ACCOUNT_ID = "cometchat_voip_account"
    private const val ACCOUNT_LABEL = "CometChat Calls VoIP"

    fun register(context: Context) {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val handle = getPhoneAccountHandle(context)
        val phoneAccount = PhoneAccount.builder(handle, ACCOUNT_LABEL)
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER or PhoneAccount.CAPABILITY_SELF_MANAGED)
            .build()
        telecomManager.registerPhoneAccount(phoneAccount)
    }

    fun getPhoneAccountHandle(context: Context): PhoneAccountHandle {
        val componentName = ComponentName(context, CallConnectionService::class.java)
        return PhoneAccountHandle(componentName, ACCOUNT_ID)
    }

    fun isPhoneAccountEnabled(context: Context): Boolean {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val account = telecomManager.getPhoneAccount(getPhoneAccountHandle(context))
        return account?.isEnabled == true
    }

    fun openPhoneAccountSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)
        } else {
            Intent("android.telecom.action.CHANGE_PHONE_ACCOUNTS")
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
// Call PhoneAccountManager.register() in Application.onCreate() or SplashActivity
```

### 5. CallNotificationManager (TelecomManager + Fallback)

```kotlin
object CallNotificationManager {
    const val NOTIFICATION_ID = 1001
    const val CHANNEL_ID = "voip_call_channel"
    const val ACTION_ACCEPT = "ACTION_ACCEPT"
    const val ACTION_DECLINE = "ACTION_DECLINE"
    const val EXTRA_CALL_DATA = "call_data"

    fun showIncomingCall(context: Context, callData: CallData) {
        // Check PhoneAccount first — if not enabled, go straight to fallback
        if (!PhoneAccountManager.isPhoneAccountEnabled(context)) {
            showFallbackNotification(context, callData)
            return
        }
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val extras = Bundle().apply { putParcelable("call_data", callData) }
            telecomManager.addNewIncomingCall(PhoneAccountManager.getPhoneAccountHandle(context), extras)
        } catch (e: SecurityException) {
            showFallbackNotification(context, callData)
        }
    }

    fun cancelNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
    }

    private fun showFallbackNotification(context: Context, callData: CallData) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH)
        )

        // Full-screen intent → IncomingCallActivity
        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java).apply {
            putExtra(EXTRA_CALL_DATA, callData)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPI = PendingIntent.getActivity(context, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Accept/Decline actions → CallActionReceiver
        val acceptPI = PendingIntent.getBroadcast(context, 1,
            Intent(context, CallActionReceiver::class.java).apply {
                action = ACTION_ACCEPT; putExtra(EXTRA_CALL_DATA, callData)
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val declinePI = PendingIntent.getBroadcast(context, 2,
            Intent(context, CallActionReceiver::class.java).apply {
                action = ACTION_DECLINE; putExtra(EXTRA_CALL_DATA, callData)
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cometchat_logo)
            .setContentTitle("${if (callData.callType == Constants.SESSION_TYPE_AUDIO) "Voice" else "Video"} Call")
            .setContentText("${callData.callerName.ifEmpty { "Unknown" }} is calling…")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPI, true)
            .addAction(0, "Accept", acceptPI)
            .addAction(0, "Decline", declinePI)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }
}
```

### 6. ConnectionService

```kotlin
class CallConnectionService : ConnectionService() {
    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val callData: CallData? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            request?.extras?.getParcelable("call_data", CallData::class.java)
        } else {
            @Suppress("DEPRECATION")
            request?.extras?.getParcelable("call_data")
        }
        val connection = CallConnection(applicationContext, callData)
        CallConnectionHolder.set(connection)
        return connection
    }
}
```

### 7. CallConnection (Answer/Reject)

Answering is joining the session: launch the call screen, which calls `CometChatCalls.joinSession` (see Call
Session Integration). Declining is `CometChatCalls.endCall` with `CALL_STATUS_REJECTED`.

```kotlin
class CallConnection(
    private val context: Context,
    private val callData: CallData?
) : Connection() {

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_CALL_TYPE = "call_type"
        const val EXTRA_IS_GROUP = "is_group"

        /** Declines on the server: the caller's screen and the call log both see "rejected". */
        fun decline(sessionId: String) {
            CometChatCalls.endCall(sessionId, Constants.CALL_STATUS_REJECTED,
                object : CometChatCalls.CallbackListener<Void>() {
                    override fun onSuccess(result: Void?) {}
                    override fun onError(e: CometChatException) { Log.w("CallConnection", "decline failed: ${e.message}") }
                })
        }

        fun launchCallActivity(context: Context, callData: CallData) {
            val intent = Intent(context, CallActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, callData.sessionId)
                putExtra(EXTRA_CALL_TYPE, callData.callType)
                putExtra(EXTRA_IS_GROUP, callData.isGroup)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    init {
        connectionProperties = PROPERTY_SELF_MANAGED
        connectionCapabilities = CAPABILITY_HOLD or CAPABILITY_SUPPORT_HOLD
        audioModeIsVoip = true
        callData?.let {
            setCallerDisplayName(it.callerName, TelecomManager.PRESENTATION_ALLOWED)
            setAddress(Uri.fromParts("tel", it.callerUid, null), TelecomManager.PRESENTATION_ALLOWED)
        }
        setInitializing()
        setRinging()
    }

    override fun onAnswer() {
        val data = callData ?: run {
            setDisconnected(DisconnectCause(DisconnectCause.ERROR)); destroy(); return
        }
        setActive()
        launchCallActivity(context, data)
        CallNotificationManager.cancelNotification(context)
    }

    override fun onReject() {
        callData?.sessionId?.let { decline(it) }
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
        CallNotificationManager.cancelNotification(context)
    }

    override fun onDisconnect() {
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }
}
```

### 8. CallActionReceiver (Fallback Notification Actions)

```kotlin
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val callData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(CallNotificationManager.EXTRA_CALL_DATA, CallData::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(CallNotificationManager.EXTRA_CALL_DATA)
        } ?: return

        when (intent.action) {
            CallNotificationManager.ACTION_ACCEPT -> CallConnection.launchCallActivity(context, callData)
            CallNotificationManager.ACTION_DECLINE -> CallConnection.decline(callData.sessionId)
        }
        CallNotificationManager.cancelNotification(context)
    }
}
```

### 9. CallConnectionHolder

```kotlin
object CallConnectionHolder {
    private var activeConnection: Connection? = null

    fun set(connection: Connection) { activeConnection = connection }
    fun get(): Connection? = activeConnection
    fun clear() { activeConnection = null }

    /** Ends the active call by calling onDisconnect() and clears the reference. */
    fun endCall() {
        activeConnection?.onDisconnect()
        clear()
    }

    /** The ring ended elsewhere (cancelled, unanswered, answered on another device): drop the system call UI. */
    fun ringEnded() {
        val connection = activeConnection ?: return
        if (connection.state != Connection.STATE_RINGING) return
        connection.setDisconnected(DisconnectCause(DisconnectCause.MISSED))
        connection.destroy()
        clear()
    }
}
```

### 10. Manifest

```xml
<!-- Permissions -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.MANAGE_OWN_CALLS" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />

<!-- VoIP ConnectionService -->
<service android:name=".voip.CallConnectionService"
    android:exported="true"
    android:permission="android.permission.BIND_TELECOM_CONNECTION_SERVICE">
    <intent-filter>
        <action android:name="android.telecom.ConnectionService" />
    </intent-filter>
</service>

<!-- FCM Service -->
<service android:name=".voip.RingingMessagingService" android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>

<!-- Fallback notification action receiver -->
<receiver android:name=".voip.CallActionReceiver" android:exported="false" />
```

## Call Session Integration

When a call is answered (via ConnectionService or fallback notification), `CallActivity` joins the session by ID.
Joining is the answer: it takes over the session the caller's `initiateCall` prepared, whose settings win over
the ones passed here, so build both sides' settings with one function (`ringing-integration` §3):

```kotlin
val isVoiceOnly = callType == Constants.SESSION_TYPE_AUDIO
val settings = CometChatCalls.SessionSettingsBuilder()
    .setTitle("CometChat Call")
    .setSessionType(if (isVoiceOnly) SessionType.VOICE else SessionType.VIDEO)
    .setLayout(if (isVoiceOnly) LayoutType.SPOTLIGHT else LayoutType.TILE)
    .setAudioMode(if (isVoiceOnly) AudioMode.EARPIECE else AudioMode.SPEAKER)
    .startVideoPaused(isVoiceOnly)
    .startAudioMuted(false)
    .setPeerCall(!isGroup)   // 1:1: ends for both sides when either leaves
    .build()

CometChatCalls.joinSession(sessionId, settings, callContainer, listener)

// On session joined → start foreground service
CometChatOngoingCallService.launch(activity)

// Hang up: leave; a 1:1 call is also marked ended on the server
CallSession.getInstance().leave()
if (!isGroup) CometChatCalls.endCall(sessionId, Constants.CALL_STATUS_ENDED, endCallback)

// On session end (onSessionLeft / onConnectionClosed / onSessionTimedOut) → clean up
CometChatOngoingCallService.abort(activity)
CallConnectionHolder.endCall()
CallNotificationManager.cancelNotification(activity)
```

## Gotchas

- `CAPABILITY_SELF_MANAGED` is required for VoIP apps that manage their own UI
- Some devices require users to manually enable the PhoneAccount in Settings — use `PhoneAccountManager.isPhoneAccountEnabled()` to check and `openPhoneAccountSettings()` to redirect
- One incoming path: `CometChatCalls.IncomingCallListener` fires for a push and for the websocket alike, so don't also ring from the FCM service
- Register the `IncomingCallListener` in `Application.onCreate`: a push that starts a killed process is forwarded at once
- `MANUAL` never rings for you. If neither Telecom nor the fallback notification shows the call, nothing does
- Tokens: `registerToken()` / `unregisterToken()` / `handleTokenRefresh()` on the Push SDK only, never through the Chat SDK
- `CallData` must implement `Parcelable` to pass between components (Services, Activities, BroadcastReceivers)
- Always register PhoneAccount in `Application.onCreate()` or `SplashActivity` before any calls arrive
- On API 33+ use the typed `getParcelable(key, Class)` overload in `ConnectionService.onCreateIncomingConnection()`
- Always cancel the fallback notification on terminal call states (answered, rejected, cancelled) via `CallNotificationManager.cancelNotification()`
- Always disconnect the `CallConnection` via `CallConnectionHolder.endCall()` when the call session ends
- The fallback notification uses `PRIORITY_MAX`, `CATEGORY_CALL`, `ongoing=true`, and a full-screen intent for lock screen display
- This builds on top of the basic ringing integration (caller side, settings, decline polling) — understand that first

## Sample App Reference

No VoIP sample app is published in this repository. For incoming calls without building the Telecom side yourself,
see `samples/sample-app-ringing` and the `ringing-integration` skill.
