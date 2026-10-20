package com.cometchat.samplecallsringing.data.repository

import android.os.Handler
import android.os.Looper
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.calls.model.CallUser
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException as ChatException
import com.cometchat.chat.models.User
import com.cometchat.samplecallsringing.utils.AppUtils
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object Repository {
    private val mainHandler = Handler(Looper.getMainLooper())

    // ── Chat SDK Login ──────────────────────────────────────────────────

    fun loginChatUser(
        uid: String,
        authKey: String,
        listener: CometChat.CallbackListener<User>
    ) {
        CometChat.login(uid, authKey, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                listener.onSuccess(user)
            }

            override fun onError(e: ChatException) {
                listener.onError(e)
            }
        })
    }

    // ── Calls SDK Login ─────────────────────────────────────────────────

    fun loginCallsUser(
        uid: String,
        authKey: String,
        listener: CometChatCalls.CallbackListener<CallUser>
    ) {
        CometChatCalls.login(uid, authKey, object : CometChatCalls.CallbackListener<CallUser>() {
            override fun onSuccess(user: CallUser) {
                listener.onSuccess(user)
            }

            override fun onError(e: CallsException) {
                listener.onError(e)
            }
        })
    }

    // ── Chat SDK Logout ─────────────────────────────────────────────────

    fun logoutChatUser(listener: CometChat.CallbackListener<String>) {
        CometChat.logout(object : CometChat.CallbackListener<String>() {
            override fun onSuccess(message: String) {
                listener.onSuccess(message)
            }

            override fun onError(e: ChatException) {
                listener.onError(e)
            }
        })
    }

    // ── Calls SDK Logout ────────────────────────────────────────────────

    fun logoutCallsUser(listener: CometChatCalls.CallbackListener<String>) {
        CometChatCalls.logout(object : CometChatCalls.CallbackListener<String>() {
            override fun onSuccess(message: String) {
                listener.onSuccess(message)
            }

            override fun onError(e: CallsException) {
                listener.onError(e)
            }
        })
    }

    // ── Fetch Sample Users (OkHttp) ─────────────────────────────────────

    fun fetchSampleUsers(listener: CometChatCalls.CallbackListener<List<SampleUser>>) {
        val request = Request.Builder()
            .url(AppUtils.SAMPLE_APP_USERS_URL)
            .get()
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                mainHandler.post {
                    listener.onError(CallsException("NETWORK_ERROR", e.message ?: "Network error"))
                }
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful && response.body != null) {
                    try {
                        val users = parseSampleUsers(response.body!!.string())
                        mainHandler.post { listener.onSuccess(users) }
                    } catch (e: Exception) {
                        mainHandler.post {
                            listener.onError(CallsException("PARSE_ERROR", e.message ?: "Parse error"))
                        }
                    }
                } else {
                    mainHandler.post {
                        listener.onError(CallsException("HTTP_ERROR", "HTTP ${response.code}"))
                    }
                }
            }
        })
    }

    private fun parseSampleUsers(json: String): List<SampleUser> {
        val users = mutableListOf<SampleUser>()
        try {
            val jsonObject = JSONObject(json)
            val usersArray = jsonObject.getJSONArray(AppUtils.KEY_USERS)
            for (i in 0 until usersArray.length()) {
                val userJson = usersArray.getJSONObject(i)
                users.add(
                    SampleUser(
                        uid = userJson.getString(AppUtils.KEY_UID),
                        name = userJson.getString(AppUtils.KEY_NAME),
                        avatar = userJson.optString(AppUtils.KEY_AVATAR, "")
                    )
                )
            }
        } catch (_: Exception) {
            // Return empty list on parse error
        }
        return users
    }

    // ── Call Operations (Calls SDK) ─────────────────────────────────────

    /**
     * Ends the call on the server with [status], a `Constants.CALL_STATUS_*` value such as
     * cancelled or ended. Calls are initiated with the Calls SDK, so they are ended with it too.
     */
    fun endCall(sessionId: String, status: String, listener: CometChatCalls.CallbackListener<Void>) {
        CometChatCalls.endCall(sessionId, status, object : CometChatCalls.CallbackListener<Void>() {
            override fun onSuccess(result: Void?) {
                listener.onSuccess(result)
            }

            override fun onError(e: CallsException) {
                listener.onError(e)
            }
        })
    }

    /**
     * The server's status for [sessionId] from recent call logs (e.g. rejected, busy, initiated), or
     * null when it is not listed yet or the lookup fails. Nothing pushes a decline to the caller, so
     * a ringing caller polls this.
     */
    fun fetchCallStatus(sessionId: String, listener: (String?) -> Unit) {
        val request = com.cometchat.calls.core.CallLogRequest.CallLogRequestBuilder()
            .setLimit(5)
            .build()

        request.fetchNext(object : CometChatCalls.CallbackListener<List<com.cometchat.calls.model.CallLog>>() {
            override fun onSuccess(callLogs: List<com.cometchat.calls.model.CallLog>) {
                val status = callLogs.firstOrNull { it.sessionID == sessionId }?.status
                mainHandler.post { listener(status) }
            }

            override fun onError(e: CallsException) {
                mainHandler.post { listener(null) }
            }
        })
    }
}

/**
 * Simple data class for sample users
 */
data class SampleUser(
    val uid: String,
    val name: String,
    val avatar: String
)
