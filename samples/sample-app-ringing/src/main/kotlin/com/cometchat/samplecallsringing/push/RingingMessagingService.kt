package com.cometchat.samplecallsringing.push

import android.util.Log
import com.cometchat.pushnotification.CometChatPushNotifications
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM entry point. Runs in the app process, started by Google Play services when the app is not
 * already running, so an incoming call can ring even after the app has been swiped away.
 *
 * Only data-only messages reach [onMessageReceived] in every app state; the push SDK rings from the
 * data payload. The data values carry the caller's name and avatar, so only the keys are logged.
 */
class RingingMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "RingingMessagingService"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "onMessageReceived keys=${message.data.keys}")
        try {
            // sentTime is the ring deadline's fallback when the payload has no initiatedAt: without it a
            // push delivered late looks fresh and rings for a call that is already over.
            CometChatPushNotifications.handlePushNotification(this, message.data, message.sentTime)
        } catch (t: Throwable) {
            // A malformed push must never crash the service: the next call still has to ring.
            Log.e(TAG, "Failed to handle push", t)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Received refreshed FCM token")
        PushNotifications.handleTokenRefresh(token)
    }
}
