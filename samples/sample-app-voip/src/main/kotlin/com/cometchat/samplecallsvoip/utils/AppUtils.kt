package com.cometchat.samplecallsvoip.utils

import android.content.Context

object AppUtils {

    private const val PREFS_NAME = "sample_calls_voip_prefs"
    private const val KEY_APP_ID = "app_id"
    private const val KEY_REGION = "region"
    private const val KEY_AUTH_KEY = "auth_key"
    private const val KEY_LOGGED_IN_UID = "logged_in_uid"
    private const val KEY_PROVIDER_ID = "provider_id"

    const val SAMPLE_APP_USERS_URL = "https://assets.cometchat.io/sampleapp/sampledata.json"
    const val KEY_USERS = "users"
    const val KEY_UID = "uid"
    const val KEY_NAME = "name"
    const val KEY_AVATAR = "avatar"

    const val REGION_US = "us"
    const val REGION_EU = "eu"
    const val REGION_IN = "in"

    fun saveCredentials(context: Context, appId: String, region: String, authKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_ID, appId)
            .putString(KEY_REGION, region)
            .putString(KEY_AUTH_KEY, authKey)
            .apply()
    }

    fun getAppId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_ID, null)
    }

    fun getRegion(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_REGION, null)
    }

    fun getAuthKey(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_AUTH_KEY, null)
    }

    fun saveLoggedInUid(context: Context, uid: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LOGGED_IN_UID, uid)
            .apply()
    }

    fun getLoggedInUid(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOGGED_IN_UID, null)
    }

    fun clearLoggedInUid(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LOGGED_IN_UID)
            .apply()
    }

    fun saveProviderId(context: Context, providerId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROVIDER_ID, providerId)
            .apply()
    }

    fun getProviderId(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROVIDER_ID, null) ?: AppConstants.PROVIDER_ID
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun hasCredentials(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_APP_ID, null).isNullOrEmpty() &&
                !prefs.getString(KEY_AUTH_KEY, null).isNullOrEmpty()
    }
}
