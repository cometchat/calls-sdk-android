package com.cometchat.samplecallsvoip

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cometchat.calls.core.CallAppSettings
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.chat.core.AppSettings
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.Call
import com.cometchat.chat.exceptions.CometChatException as ChatException
import com.cometchat.pushnotification.CometChatPushNotifications
import com.cometchat.pushnotification.PNConfiguration
import com.cometchat.pushnotification.listeners.CallAnsweredHandler
import com.cometchat.pushnotification.models.PNCallInfo
import com.cometchat.samplecallsvoip.ui.activity.CallActivity
import com.cometchat.samplecallsvoip.ui.activity.IncomingCallActivity
import com.cometchat.samplecallsvoip.utils.AppConstants
import com.cometchat.samplecallsvoip.utils.AppUtils

class VoIPApplication : Application() {

    companion object {
        private const val TAG = "VoIPApplication"
        private const val CALL_LISTENER_ID = "global_call_listener"
    }

    override fun onCreate() {
        super.onCreate()
        // All SDK initialization happens here — once, before anything else.
        initAllSdks()
    }

    /**
     * Initialize all SDKs (Chat SDK, Calls SDK, Push Notifications) if credentials are available.
     * Called from Application.onCreate() so everything is ready before any FCM or Activity runs.
     */
    fun initAllSdks() {
        val appId: String
        val region: String

        if (AppConstants.APP_ID.isNotEmpty() && AppConstants.APP_ID != "YOUR_APP_ID") {
            appId = AppConstants.APP_ID
            region = AppConstants.REGION
        } else if (AppUtils.hasCredentials(this)) {
            appId = AppUtils.getAppId(this) ?: return
            region = AppUtils.getRegion(this) ?: AppUtils.REGION_US
        } else {
            return
        }

        initAllSdks(appId, region)
    }

    /**
     * Initialize all SDKs with explicit appId and region.
     * Called from AppCredentialsViewModel after user enters new credentials.
     */
    fun initAllSdks(appId: String, region: String) {
        // 1. Chat SDK
        val appSettings = AppSettings.AppSettingsBuilder()
            .subscribePresenceForAllUsers()
            .setRegion(region)
            .build()

        CometChat.init(this, appId, appSettings, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(s: String) {
                Log.d(TAG, "Chat SDK initialized")

                // 2. Calls SDK
                val callAppSettings = CallAppSettings.CallAppSettingBuilder()
                    .setAppId(appId)
                    .setRegion(region)
                    .build()

                CometChatCalls.init(this@VoIPApplication, callAppSettings, object : CometChatCalls.CallbackListener<String>() {
                    override fun onSuccess(result: String) {
                        Log.d(TAG, "Calls SDK initialized")
                    }

                    override fun onError(e: CallsException) {
                        Log.e(TAG, "Calls SDK init failed: ${e.message}")
                    }
                })
            }

            override fun onError(e: ChatException) {
                Log.e(TAG, "Chat SDK init failed: ${e.message}")
            }
        })

        // 3. Push Notifications (doesn't depend on Chat SDK callback — has its own init)
        val config = PNConfiguration.Builder(appId, region)
            .setNotificationSmallIcon(R.drawable.ic_launcher_foreground)
            .setVoIPEnabled(true)
            .setInlineReplyEnabled(true)
            .build()

        CometChatPushNotifications.init(this, config)
        Log.d(TAG, "Push Notifications initialized")

        // Set up the call answered handler (for background/killed state)
        setupCallAnsweredHandler()
    }

    /**
     * Register the real-time CometChat CallListener for foreground incoming calls.
     * This is the ONLY path for foreground calls — same behavior as sample-app-ringing.
     */
    fun registerCallListener() {
        CometChat.addCallListener(CALL_LISTENER_ID, object : CometChat.CallListener() {
            override fun onIncomingCallReceived(call: Call) {
                Log.d(TAG, "Foreground incoming call, sessionId: ${call.sessionId}")
                val intent = Intent(this@VoIPApplication, IncomingCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(IncomingCallActivity.EXTRA_SESSION_ID, call.sessionId)
                    putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, call.sender?.name ?: "Unknown")
                    putExtra(IncomingCallActivity.EXTRA_CALLER_AVATAR, call.sender?.avatar ?: "")
                    putExtra(IncomingCallActivity.EXTRA_CALL_TYPE, call.type)
                }
                startActivity(intent)
            }

            override fun onOutgoingCallAccepted(call: Call?) {}
            override fun onOutgoingCallRejected(call: Call?) {}
            override fun onIncomingCallCancelled(call: Call?) {}
            override fun onCallEndedMessageReceived(call: Call?) {}
        })
    }

    fun removeCallListener() {
        CometChat.removeCallListener(CALL_LISTENER_ID)
    }

    /**
     * Handler for when user accepts a call from the library's built-in call screen
     * (background/killed state only).
     *
     * NOTE: We use the Application context (this@VoIPApplication) for startActivity
     * instead of the provided context. The provided context comes from CallRingingActivity
     * which is finishing/destroyed. On Xiaomi/MIUI/HyperOS, starting an activity from a
     * dying Activity context is silently blocked by their background-activity-launch restriction.
     * Using Application context with FLAG_ACTIVITY_NEW_TASK works reliably across all OEMs.
     */
    private fun setupCallAnsweredHandler() {
        CometChatPushNotifications.setOnCallAnsweredHandler(object : CallAnsweredHandler {
            override fun onCallAnswered(context: Context, callInfo: PNCallInfo) {
                val sessionId = callInfo.sessionId ?: return
                Log.d(TAG, "Call answered from push, sessionId: $sessionId")

                // Accept the call on the server first, then launch CallActivity
                CometChat.acceptCall(sessionId, object : CometChat.CallbackListener<Call>() {
                    override fun onSuccess(call: Call?) {
                        Log.d(TAG, "Call accepted on server")
                        val intent = Intent(this@VoIPApplication, CallActivity::class.java).apply {
                            putExtra(CallActivity.EXTRA_SESSION_ID, sessionId)
                            putExtra(CallActivity.EXTRA_CALL_TYPE, call?.type ?: "video")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        this@VoIPApplication.startActivity(intent)
                    }

                    override fun onError(e: ChatException?) {
                        Log.e(TAG, "Failed to accept call: ${e?.message}")
                    }
                })
            }
        })
    }
}
