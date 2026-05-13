package com.cometchat.samplecallsvoip.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.chat.core.CometChat
import com.cometchat.samplecallsvoip.R
import com.cometchat.samplecallsvoip.VoIPApplication
import com.cometchat.samplecallsvoip.utils.AppConstants
import com.cometchat.samplecallsvoip.utils.AppUtils

/**
 * Splash screen that routes to the appropriate screen based on credential and login state.
 * All SDK initialization is done in VoIPApplication.onCreate().
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val hasCredentials = hasValidAppConstants() || AppUtils.hasCredentials(this)

        if (!hasCredentials) {
            navigateToAppCredentials()
            return
        }

        // SDKs are already initialized in Application.onCreate()
        if (CometChat.getLoggedInUser() != null) {
            // User already logged in — register foreground call listener and go home
            (application as VoIPApplication).registerCallListener()
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }

    private fun navigateToAppCredentials() {
        startActivity(Intent(this, AppCredentialsActivity::class.java))
        finish()
    }

    private fun hasValidAppConstants(): Boolean {
        return AppConstants.APP_ID.isNotEmpty() &&
                AppConstants.APP_ID != "YOUR_APP_ID" &&
                AppConstants.REGION.isNotEmpty() &&
                AppConstants.AUTH_KEY.isNotEmpty() &&
                AppConstants.AUTH_KEY != "YOUR_AUTH_KEY"
    }
}
