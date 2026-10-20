package com.cometchat.samplecallsringing.utils

/**
 * App credentials for CometChat Calls SDK.
 * Replace these placeholder values with your CometChat app credentials from the dashboard.
 *
 * Get your credentials at: https://app.cometchat.com
 */
object AppConstants {
    const val APP_ID = "" // Your App id
    const val REGION = ""  // us, eu, or in
    const val AUTH_KEY = "" // Your Auth Key

    /**
     * Push provider configured on the CometChat dashboard for this app (the same one calls-core uses).
     * Incoming calls are pushed to this provider's FCM project, so it must match google-services.json.
     */
    const val PUSH_PROVIDER_ID = "cometchatteammessanger"
}
