package com.cometchat.samplecallsvoip.fcm

import android.util.Log
import com.cometchat.pushnotification.CometChatPushNotifications
import com.cometchat.pushnotification.helpers.CometChatPNHelper
import com.cometchat.pushnotification.models.PushPlatform
import com.cometchat.samplecallsvoip.utils.AppUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM Service that routes push notifications to CometChatPushNotifications.
 *
 * Strategy:
 * - Foreground: Do NOT pass call pushes to the library. The real-time CometChat.CallListener
 *   handles incoming calls in foreground (shows our custom IncomingCallActivity).
 * - Background/Killed: Pass all pushes to the library. It shows its built-in call screen
 *   and fires CallAnsweredHandler when the user accepts.
 */
class AppFCMService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AppFCMService"
        private const val TYPE_CALL = "call"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "Push notification received")

        val data = message.data

        // Check if this is a CometChat notification
        if (!CometChatPNHelper.isCometChatNotification(data)) {
            Log.d(TAG, "Not a CometChat notification, ignoring")
            return
        }

        // If the app is in foreground and this is a call push, skip it.
        // The real-time CometChat.CallListener will handle it instead.
        if (CometChatPushNotifications.isAppInForeground() && isCallNotification(data)) {
            Log.d(TAG, "App in foreground, skipping call push (handled by real-time listener)")
            return
        }

        // Pass to the library for background/killed handling
        CometChatPushNotifications.handlePushNotification(
            context = this,
            data = data
        )
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed")
        val providerId = AppUtils.getProviderId(this)
        CometChatPushNotifications.handleTokenRefresh(
            PushPlatform.FCM_ANDROID,
            token,
            providerId
        )
    }

    /**
     * Check if the push payload is a call notification.
     * CometChat call pushes typically have a "type" field indicating "call".
     */
    private fun isCallNotification(data: Map<String, String>): Boolean {
        val type = data["type"] ?: ""
        return type.contains(TYPE_CALL, ignoreCase = true)
    }
}
