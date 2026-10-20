package com.cometchat.samplecallsringing.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cometchat.pushnotification.CometChatPushNotifications

/**
 * Debug-only trigger for incoming calls, so the ring / full-screen / answer / decline / cancel /
 * timeout flow can be exercised without a real push. Debug builds only.
 *
 * Builds the same `type=call` data payload FCM delivers and hands it to
 * [CometChatPushNotifications.handlePushNotification], the entry point
 * [com.cometchat.samplecallsringing.push.RingingMessagingService] uses, so the real ring path runs.
 * `receiver` must be the logged-in uid, or the SDK drops the push as meant for someone else.
 *
 * ```
 * adb shell am broadcast -n com.cometchat.samplecallsringing/.debug.DebugIncomingCallReceiver \
 *     --es sessionId <session-id> --es senderName Andrew --es callType video --es receiver <your-uid>
 * adb shell am broadcast -n com.cometchat.samplecallsringing/.debug.DebugIncomingCallReceiver \
 *     --es sessionId <session-id> --es callAction cancelled --es receiver <your-uid>
 * ```
 */
class DebugIncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        fun extra(key: String, fallback: String) = intent.getStringExtra(key)?.takeIf { it.isNotBlank() } ?: fallback

        val sessionId = extra("sessionId", "debug-session")
        val callAction = extra("callAction", "initiated")
        // "Now" by default so the call is inside its ring window; pass an older value to test expiry.
        val initiatedAt = extra("initiatedAt", System.currentTimeMillis().toString())
        val sender = extra("sender", "debug-caller")
        val receiver = extra("receiver", "debug-receiver")
        val payload = mapOf(
            "type" to "call",
            "callAction" to callAction,
            "callType" to extra("callType", "audio"),
            "sessionId" to sessionId,
            "sender" to sender,
            "senderName" to extra("senderName", "Debug Caller"),
            "receiver" to receiver,
            "receiverType" to "user",
            "conversationId" to "${sender}_user_$receiver",
            "initiatedAt" to initiatedAt,
        )
        val handled = CometChatPushNotifications.handlePushNotification(
            context.applicationContext,
            payload,
            initiatedAt.toLongOrNull() ?: System.currentTimeMillis(),
        )
        Log.i(TAG, "injected $callAction for $sessionId (handled=$handled)")
    }

    companion object {
        private const val TAG = "DebugIncomingCall"
    }
}
