package com.cometchat.samplecallsringing.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException as ChatException
import com.cometchat.samplecallsringing.data.repository.Repository
import com.cometchat.samplecallsringing.push.PushNotifications

/**
 * Signs the user out of the push, Calls and Chat SDKs. App-scoped rather than owned by a screen:
 * once this phone's push token is gone the logout must finish even if the screen that started it
 * is closed or recreated, or the user stays signed in on a phone that no longer rings. Callbacks
 * are for UI only and run on the main thread.
 */
object SignOut {

    private const val TAG = "SignOut"
    private val main = Handler(Looper.getMainLooper())

    /**
     * Removes this phone's push token first, and logs out only once that worked: the request is
     * authenticated by the session being logged out, and without it this phone keeps ringing for
     * the user who left. On failure nothing is logged out and [onUnregisterFailed] is called, so
     * the user can retry or [logout] anyway.
     */
    fun unregisterThenLogout(
        context: Context,
        onUnregisterFailed: () -> Unit,
        onDone: ((error: ChatException?) -> Unit)? = null
    ) {
        val appContext = context.applicationContext
        PushNotifications.unregisterToken { success ->
            if (success) logout(appContext, onDone) else onUnregisterFailed()
        }
    }

    /** Logs out of the Calls SDK, then the Chat SDK; [onDone] gets the Chat logout's error, or null. */
    fun logout(context: Context, onDone: ((error: ChatException?) -> Unit)? = null) {
        val appContext = context.applicationContext
        Repository.logoutCallsUser(object : CometChatCalls.CallbackListener<String>() {
            override fun onSuccess(message: String) = logoutChat(appContext, onDone)

            override fun onError(e: CallsException) {
                // The Chat session still has to go, or the next launch signs this user back in.
                Log.e(TAG, "Calls SDK logout failed: ${e.message}")
                logoutChat(appContext, onDone)
            }
        })
    }

    private fun logoutChat(context: Context, onDone: ((ChatException?) -> Unit)?) {
        Repository.logoutChatUser(object : CometChat.CallbackListener<String>() {
            override fun onSuccess(message: String) {
                Log.d(TAG, "Signed out")
                AppUtils.clearLoggedInUid(context)
                main.post { onDone?.invoke(null) }
            }

            override fun onError(e: ChatException) {
                Log.e(TAG, "Chat SDK logout failed: ${e.message}")
                main.post { onDone?.invoke(e) }
            }
        })
    }
}
