package com.cometchat.samplecallsringing.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.chat.core.CometChat
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.push.PushNotifications
import com.cometchat.samplecallsringing.utils.SdkInit

/**
 * Splash screen: waits for the SDKs ([SdkInit], started by the application), then routes to the
 * appropriate screen based on credential and login state.
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app icon opened on top of a task that is already running (signed in, maybe in a call):
        // close, so that task's top screen comes back (a live call included) rather than a new Home.
        if (!isTaskRoot && intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            finish()
            return
        }
        setContentView(R.layout.activity_splash)

        val credentials = SdkInit.credentials(this) ?: run {
            navigateToAppCredentials()
            return
        }
        // Already started by the application; this waits for it (or starts it after new credentials).
        SdkInit.init(this, credentials.first, credentials.second) { error ->
            if (isFinishing || isDestroyed) return@init
            if (error == null) {
                navigateAfterInit()
            } else {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                navigateToAppCredentials()
            }
        }
    }

    private fun navigateAfterInit() {
        val destination = if (CometChat.getLoggedInUser() != null) {
            // A saved session: register this phone's push token, as LoginActivity does after a fresh login.
            PushNotifications.registerToken()
            HomeActivity::class.java
        } else {
            LoginActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }

    private fun navigateToAppCredentials() {
        startActivity(Intent(this, AppCredentialsActivity::class.java))
        finish()
    }
}
