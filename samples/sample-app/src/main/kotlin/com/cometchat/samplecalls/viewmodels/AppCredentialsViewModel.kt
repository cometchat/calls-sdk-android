package com.cometchat.samplecalls.viewmodels

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cometchat.calls.core.CallAppSettings
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException
import com.cometchat.samplecalls.ui.activity.LoginActivity
import com.cometchat.samplecalls.utils.AppUtils

class AppCredentialsViewModel : ViewModel() {

    val selectedRegion: MutableLiveData<String> = MutableLiveData()
    private val _initStatus = MutableLiveData<Boolean>()
    val initStatus: LiveData<Boolean> = _initStatus
    private val _error = MutableLiveData<CometChatException>()
    val error: LiveData<CometChatException> = _error

    fun setSelectedRegion(region: String) {
        selectedRegion.value = region
    }

    fun initCallsSDK(activity: Activity, appId: String, authKey: String) {
        // Save credentials
        val region = selectedRegion.value ?: AppUtils.REGION_US
        AppUtils.saveCredentials(activity, appId, region, authKey)

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

                override fun onError(e: CometChatException) {
                    _error.postValue(e)
                }
            }
        )
    }
}
