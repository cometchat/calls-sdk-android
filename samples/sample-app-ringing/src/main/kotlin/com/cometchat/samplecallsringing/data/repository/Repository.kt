package com.cometchat.samplecallsringing.data.repository

import android.os.Handler
import android.os.Looper
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.calls.model.CallUser
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.exceptions.CometChatException as ChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.core.Call
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

    // ── Fetch CometChat Users ───────────────────────────────────────────

    private var usersRequest: UsersRequest? = null

    fun fetchUsers(listener: CometChat.CallbackListener<List<User>>) {
        if (usersRequest == null) {
            usersRequest = UsersRequest.UsersRequestBuilder()
                .setLimit(30)
                .build()
        }

        usersRequest!!.fetchNext(object : CometChat.CallbackListener<List<User>>() {
            override fun onSuccess(users: List<User>) {
                listener.onSuccess(users)
            }

            override fun onError(e: ChatException) {
                listener.onError(e)
            }
        })
    }

    fun resetUsersRequest() {
        usersRequest = null
    }

    fun searchUsers(keyword: String, listener: CometChat.CallbackListener<List<User>>) {
        val request = UsersRequest.UsersRequestBuilder()
            .setLimit(30)
            .setSearchKeyword(keyword)
            .build()

        request.fetchNext(object : CometChat.CallbackListener<List<User>>() {
            override fun onSuccess(users: List<User>) {
                listener.onSuccess(users)
            }

            override fun onError(e: ChatException) {
                listener.onError(e)
            }
        })
    }

    // ── Fetch Call Logs ─────────────────────────────────────────────────

    fun fetchCallLogs(listener: CometChat.CallbackListener<List<BaseMessage>>) {
        val messagesRequest = MessagesRequest.MessagesRequestBuilder()
            .setCategory("call")
            .setLimit(30)
            .build()

        messagesRequest.fetchPrevious(object : CometChat.CallbackListener<List<BaseMessage>>() {
            override fun onSuccess(messages: List<BaseMessage>) {
                listener.onSuccess(messages)
            }

            override fun onError(e: ChatException) {
                listener.onError(e)
            }
        })
    }

    // ── Call Operations ─────────────────────────────────────────────────

    fun initiateCall(call: Call, listener: CometChat.CallbackListener<Call>) {
        CometChat.initiateCall(call, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(outgoingCall: Call) {
                listener.onSuccess(outgoingCall)
            }

            override fun onError(e: ChatException) {
                listener.onError(e)
            }
        })
    }

    fun acceptCall(sessionId: String, listener: CometChat.CallbackListener<Call>) {
        CometChat.acceptCall(sessionId, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(call: Call) {
                listener.onSuccess(call)
            }

            override fun onError(e: ChatException) {
                listener.onError(e)
            }
        })
    }

    fun rejectCall(sessionId: String, listener: CometChat.CallbackListener<Call>) {
        CometChat.rejectCall(
            sessionId,
            CometChatConstants.CALL_STATUS_REJECTED,
            object : CometChat.CallbackListener<Call>() {
                override fun onSuccess(call: Call) {
                    listener.onSuccess(call)
                }

                override fun onError(e: ChatException) {
                    listener.onError(e)
                }
            }
        )
    }

    fun cancelCall(sessionId: String, listener: CometChat.CallbackListener<Call>) {
        CometChat.rejectCall(
            sessionId,
            CometChatConstants.CALL_STATUS_CANCELLED,
            object : CometChat.CallbackListener<Call>() {
                override fun onSuccess(call: Call) {
                    listener.onSuccess(call)
                }

                override fun onError(e: ChatException) {
                    listener.onError(e)
                }
            }
        )
    }

    fun endCall(sessionId: String, listener: CometChat.CallbackListener<Call>) {
        CometChat.endCall(sessionId, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(call: Call) {
                listener.onSuccess(call)
            }

            override fun onError(e: ChatException) {
                listener.onError(e)
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
