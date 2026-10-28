package com.cometchat.samplecallsringing.push

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.cometchat.pushnotification.CallMode
import com.cometchat.pushnotification.CometChatPushNotifications
import com.cometchat.pushnotification.PNCallback
import com.cometchat.pushnotification.PNConfiguration
import com.cometchat.pushnotification.PNResult
import com.cometchat.samplecallsringing.ui.activity.CallActivity
import com.cometchat.samplecallsringing.ui.activity.IncomingCallActivity
import com.cometchat.samplecallsringing.calls.OutgoingCall
import com.cometchat.samplecallsringing.utils.AppUtils

/**
 * Incoming calls ring through the push notifications SDK in AUTO mode: the SDK owns the FCM
 * payload, the ringing notification and full-screen screen, decline, missed calls and the caller
 * cancelling. The push token is registered by the app after login ([registerToken]) rather than by
 * the SDK, so it is only registered for a signed-in user.
 */
object PushNotifications {

    private const val TAG = "PushNotifications"
    private val mainHandler = Handler(Looper.getMainLooper())

    /** How long an incoming call rings. The caller gives up first ([OutgoingCall.RING_TIMEOUT_MS]). */
    const val RING_TIMEOUT_MS = 60_000L

    /**
     * Configures the push SDK. Runs on every process start and after credentials are saved; the SDK
     * persists the configuration so a push that wakes a cold process can still ring.
     */
    fun init(context: Context, appId: String, region: String) {
        val providerId = AppUtils.getPushProviderId(context)
        // SdkInit.credentials() sends the app to the credentials screen without one, so this is a fallback.
        if (providerId.isEmpty()) {
            Log.e(TAG, "No push provider ID: registering the push token will fail. Set it in AppConstants or on the credentials screen.")
        }
        CometChatPushNotifications.init(
            context.applicationContext,
            PNConfiguration.Builder(appId, region)
                .setProviderId(providerId)
                .setAutoRegisterToken(false)
                .setCallMode(CallMode.AUTO)
                // Answering re-launches the call screen with the answered call (CallBundles).
                .setAnswerActivity(CallActivity::class.java)
                .setIncomingCallActivity(IncomingCallActivity::class.java)
                .setDefaultRingTimeoutMs(RING_TIMEOUT_MS)
                .build()
        )
    }

    fun registerToken() {
        CometChatPushNotifications.registerToken(object : PNCallback<Unit> {
            override fun onResult(result: PNResult<Unit>) {
                if (result.isSuccess) {
                    Log.i(TAG, "Push token registered")
                } else {
                    Log.e(TAG, "Registering the push token failed: ${result.errorOrNull()}")
                }
            }
        })
    }

    /**
     * Stops this phone ringing for the signed-in user, then calls [onDone] on the main thread with
     * whether it worked. Call it before logging out and log out only on success: the request is
     * authenticated by the session being logged out, so after a logout it can no longer be retried.
     */
    fun unregisterToken(onDone: (success: Boolean) -> Unit) {
        CometChatPushNotifications.unregisterToken(object : PNCallback<Unit> {
            override fun onResult(result: PNResult<Unit>) {
                if (!result.isSuccess) {
                    Log.e(TAG, "Unregistering the push token failed: ${result.errorOrNull()}")
                }
                mainHandler.post { onDone(result.isSuccess) }
            }
        })
    }

    /** FCM handed over a refreshed token. */
    fun handleTokenRefresh(token: String) {
        CometChatPushNotifications.handleTokenRefresh(token)
    }
}
