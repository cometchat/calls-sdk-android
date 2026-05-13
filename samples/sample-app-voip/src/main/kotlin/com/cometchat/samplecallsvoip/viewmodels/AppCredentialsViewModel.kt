package com.cometchat.samplecallsvoip.viewmodels

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cometchat.samplecallsvoip.VoIPApplication
import com.cometchat.samplecallsvoip.ui.activity.LoginActivity
import com.cometchat.samplecallsvoip.utils.AppUtils

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

        // Save credentials so Application can use them on next cold start
        AppUtils.saveCredentials(activity, appId, region, authKey)

        // Initialize all SDKs via the Application class
        (activity.application as VoIPApplication).initAllSdks(appId, region)

        // Navigate to login
        activity.startActivity(Intent(activity, LoginActivity::class.java))
        activity.finish()
    }
}
