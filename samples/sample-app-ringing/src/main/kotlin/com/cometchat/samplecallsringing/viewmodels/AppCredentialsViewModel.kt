package com.cometchat.samplecallsringing.viewmodels

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cometchat.calls.core.CallAppSettings
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.chat.core.AppSettings
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException as ChatException
import com.cometchat.samplecallsringing.ui.activity.LoginActivity
import com.cometchat.samplecallsringing.utils.AppUtils

class AppCredentialsViewModel : ViewModel() {

    val selectedRegion: MutableLiveData<String> = MutableLiveData()
    private val _initStatus = MutableLiveData<Boolean>()
    val initStatus: LiveData<Boolean> = _initStatus
    private val _error = MutableLiveData<Exception>()
    val error: LiveData<Exception> = _error

    fun setSelectedRegion(region: String) {
        selectedRegion.value = region
    }

    fun initSDKs(activity: Activity, appId: String, authKey: String) {
        val region = selectedRegion.value ?: AppUtils.REGION_US
        AppUtils.saveCredentials(activity, appId, region, authKey)

        // 1. Init Chat SDK
        val appSettings = AppSettings.AppSettingsBuilder()
            .subscribePresenceForAllUsers()
            .setRegion(region)
            .build()

        CometChat.init(activity.applicationContext, appId, appSettings, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(s: String) {
                // 2. Init Calls SDK
                val callAppSettings = CallAppSettings.CallAppSettingBuilder()
                    .setAppId(appId)
                    .setRegion(region)
                    .build()

                CometChatCalls.init(
                    activity.applicationContext,
                    callAppSettings,
                    object : CometChatCalls.CallbackListener<String>() {
                        override fun onSuccess(result: String) {
                            activity.startActivity(Intent(activity, LoginActivity::class.java))
                            activity.finish()
                        }

                        override fun onError(e: CallsException) {
                            _error.postValue(e)
                        }
                    }
                )
            }

            override fun onError(e: ChatException) {
                _error.postValue(e)
            }
        })
    }
}
