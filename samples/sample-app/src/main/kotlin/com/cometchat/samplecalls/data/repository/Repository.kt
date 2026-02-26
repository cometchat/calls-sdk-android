package com.cometchat.samplecalls.data.repository

import android.os.Handler
import android.os.Looper
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException
import com.cometchat.calls.model.CallUser
import com.cometchat.samplecalls.utils.AppUtils
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object Repository {
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Login user with UID and API Key
     */
    fun loginUser(
        uid: String,
        apiKey: String,
        listener: CometChatCalls.CallbackListener<CallUser>
    ) {
        CometChatCalls.login(uid, apiKey, object : CometChatCalls.CallbackListener<CallUser>() {
            override fun onSuccess(user: CallUser) {
                listener.onSuccess(user)
            }

            override fun onError(e: CometChatException) {
                listener.onError(e)
            }
        })
    }

    /**
     * Logout current user
     */
    fun logout(listener: CometChatCalls.CallbackListener<String>) {
        CometChatCalls.logout(object : CometChatCalls.CallbackListener<String>() {
            override fun onSuccess(message: String) {
                listener.onSuccess(message)
            }

            override fun onError(e: CometChatException) {
                listener.onError(e)
            }
        })
    }

    /**
     * Fetch sample users from CometChat sample data
     */
    fun fetchSampleUsers(listener: CometChatCalls.CallbackListener<List<SampleUser>>) {
        val request = Request.Builder()
            .url(AppUtils.SAMPLE_APP_USERS_URL)
            .get()
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                mainHandler.post {
                    listener.onError(CometChatException("NETWORK_ERROR", e.message ?: "Network error"))
                }
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful && response.body != null) {
                    try {
                        val users = parseSampleUsers(response.body!!.string())
                        mainHandler.post { listener.onSuccess(users) }
                    } catch (e: Exception) {
                        mainHandler.post {
                            listener.onError(CometChatException("PARSE_ERROR", e.message ?: "Parse error"))
                        }
                    }
                } else {
                    mainHandler.post {
                        listener.onError(CometChatException("HTTP_ERROR", "HTTP ${response.code}"))
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
        } catch (e: Exception) {
            // Return empty list on parse error
        }
        return users
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
