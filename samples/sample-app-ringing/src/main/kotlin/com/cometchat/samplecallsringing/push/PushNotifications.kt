package com.cometchat.samplecallsringing.push

import android.content.Context
import android.util.Log
import com.cometchat.pushnotification.CallMode
import com.cometchat.pushnotification.CometChatPushNotifications
import com.cometchat.pushnotification.PNCallback
import com.cometchat.pushnotification.PNConfiguration
import com.cometchat.pushnotification.PNResult
import com.cometchat.samplecallsringing.ui.activity.CallActivity
import com.cometchat.samplecallsringing.ui.activity.IncomingCallActivity
import com.cometchat.samplecallsringing.utils.AppConstants

/**
 * Incoming calls ring through the push notifications SDK in AUTO mode, as in calls-core: the SDK
 * owns the FCM payload, the ringing notification and full-screen screen, decline and missed calls.
 * The push token is registered by the app after login ([registerToken]) rather than by the SDK, so
 * it is only registered for a signed-in user.
 */
object PushNotifications {

    private const val TAG = "PushNotifications"

    /** How long an incoming call rings. The caller gives up first (OutgoingCallActivity.RING_TIMEOUT_MS). */
    const val RING_TIMEOUT_MS = 60_000L

    /**
     * Configures the push SDK. Runs on every process start and after credentials are saved; the SDK
     * persists the configuration so a push that wakes a cold process can still ring.
     */
    fun init(context: Context, appId: String, region: String) {
        CometChatPushNotifications.init(
            context.applicationContext,
            PNConfiguration.Builder(appId, region)
                .setProviderId(AppConstants.PUSH_PROVIDER_ID)
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

    fun unregisterToken() {
        CometChatPushNotifications.unregisterToken(object : PNCallback<Unit> {
            override fun onResult(result: PNResult<Unit>) {
                if (!result.isSuccess) {
                    Log.e(TAG, "Unregistering the push token failed: ${result.errorOrNull()}")
                }
            }
        })
    }

    /** FCM handed over a refreshed token. */
    fun handleTokenRefresh(token: String) {
        CometChatPushNotifications.handleTokenRefresh(token)
    }
}
