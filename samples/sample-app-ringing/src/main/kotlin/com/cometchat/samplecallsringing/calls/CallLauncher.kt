package com.cometchat.samplecallsringing.calls

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.ui.activity.OutgoingCallActivity

/**
 * Starts an outgoing call from any screen. `initiateCall` prepares the media session straight
 * away, so the microphone (and the camera for video) must be granted first.
 */
object CallLauncher {

    private const val PERMISSION_REQUEST_CODE = 2001
    private const val DOUBLE_TAP_WINDOW_MS = 1000L
    private var lastStartAt = 0L

    fun start(activity: Activity, target: CallTarget, isVideo: Boolean) {
        if (!ensurePermissions(activity, isVideo)) return

        // Two taps in a row would place two calls.
        val now = SystemClock.elapsedRealtime()
        if (now - lastStartAt < DOUBLE_TAP_WINDOW_MS) return
        lastStartAt = now

        activity.startActivity(OutgoingCallActivity.intent(activity, target, isVideo))
    }

    /** True when the call can be placed; otherwise asks for what is missing and returns false. */
    fun ensurePermissions(activity: Activity, isVideo: Boolean): Boolean {
        val missing = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (isVideo) add(Manifest.permission.CAMERA)
        }.filter { ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) return true
        val message = if (Manifest.permission.RECORD_AUDIO in missing) R.string.microphone_required else R.string.camera_required
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        ActivityCompat.requestPermissions(activity, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        return false
    }
}
