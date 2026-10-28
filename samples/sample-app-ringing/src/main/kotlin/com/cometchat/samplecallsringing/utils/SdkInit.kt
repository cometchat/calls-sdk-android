package com.cometchat.samplecallsringing.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.cometchat.calls.core.CallAppSettings
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.chat.core.AppSettings
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException as ChatException
import com.cometchat.samplecallsringing.push.PushNotifications

/**
 * Initializes the push, Chat and Calls SDKs once per process.
 *
 * Started from [com.cometchat.samplecallsringing.RingingApplication]: Android can bring the app back
 * straight onto a screen after the process died (Home, a call), without the splash screen, and that
 * screen must find the SDKs ready. The splash screen waits on the same initialization.
 */
object SdkInit {

    private const val TAG = "SdkInit"

    private enum class State { IDLE, RUNNING, DONE, FAILED }

    private val main = Handler(Looper.getMainLooper())
    private var state = State.IDLE
    private var appId: String? = null
    private var error: String? = null
    private val waiting = mutableListOf<(String?) -> Unit>()

    /**
     * The saved or built-in credentials, or null when the app has none yet. A push provider ID is
     * required as well: registering the push token fails without it, so the app would never ring.
     */
    fun credentials(context: Context): Pair<String, String>? {
        if (AppConstants.APP_ID.isNotEmpty() && AppConstants.APP_ID != "YOUR_APP_ID" &&
            AppConstants.REGION.isNotEmpty() &&
            AppConstants.AUTH_KEY.isNotEmpty() && AppConstants.AUTH_KEY != "YOUR_AUTH_KEY" &&
            AppUtils.getPushProviderId(context).isNotBlank()
        ) {
            return AppConstants.APP_ID to AppConstants.REGION
        }
        if (!AppUtils.hasCredentials(context)) return null
        val appId = AppUtils.getAppId(context)
        if (appId.isNullOrEmpty()) return null
        return appId to (AppUtils.getRegion(context) ?: AppUtils.REGION_US)
    }

    /**
     * Initializes the SDKs for [appId] and calls [onDone] on the main thread with null on success or
     * the error message. A second call for the same app joins the first instead of starting again.
     */
    @Synchronized
    fun init(context: Context, appId: String, region: String, onDone: ((String?) -> Unit)? = null) {
        if (this.appId == appId) {
            when (state) {
                State.DONE -> { onDone?.let { main.post { it(null) } }; return }
                State.RUNNING -> { onDone?.let { waiting.add(it) }; return }
                else -> Unit // FAILED or IDLE: try again
            }
        }
        this.appId = appId
        state = State.RUNNING
        error = null
        onDone?.let { waiting.add(it) }

        val app = context.applicationContext
        PushNotifications.init(app, appId, region)
        val appSettings = AppSettings.AppSettingsBuilder()
            .subscribePresenceForAllUsers()
            .setRegion(region)
            .build()
        CometChat.init(app, appId, appSettings, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(s: String) {
                Log.d(TAG, "Chat SDK initialized")
                initCalls(app, appId, region)
            }

            override fun onError(e: ChatException) {
                finish(appId, "Chat SDK init failed: ${e.message}")
            }
        })
    }

    private fun initCalls(context: Context, appId: String, region: String) {
        val callAppSettings = CallAppSettings.CallAppSettingBuilder()
            .setAppId(appId)
            .setRegion(region)
            .build()
        CometChatCalls.init(context, callAppSettings, object : CometChatCalls.CallbackListener<String>() {
            override fun onSuccess(result: String) {
                Log.d(TAG, "Calls SDK initialized")
                finish(appId, null)
            }

            override fun onError(e: CallsException) {
                finish(appId, "Calls SDK init failed: ${e.message}")
            }
        })
    }

    @Synchronized
    private fun finish(appId: String, failure: String?) {
        if (this.appId != appId) return
        if (failure != null) Log.e(TAG, failure)
        state = if (failure == null) State.DONE else State.FAILED
        error = failure
        val callbacks = waiting.toList()
        waiting.clear()
        main.post { callbacks.forEach { it(failure) } }
    }
}
