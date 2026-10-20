package com.cometchat.samplecallsringing.utils

/**
 * App credentials for the CometChat Calls SDK. Leave them empty to enter them on the first screen
 * instead, or fill them in to skip that screen.
 *
 * Get your credentials at: https://app.cometchat.com
 */
object AppConstants {
    const val APP_ID = "" // Your App ID
    const val REGION = ""  // us, eu, or in
    const val AUTH_KEY = "" // Your Auth Key

    /**
     * The FCM push provider set up for your app on the dashboard (Notifications → Push). Incoming
     * calls are pushed through it, so its Firebase project must be the one in google-services.json.
     */
    const val PUSH_PROVIDER_ID = ""
}
