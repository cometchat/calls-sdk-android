package com.cometchat.samplecallsringing.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.calls.constants.Constants
import com.cometchat.calls.core.CallSession
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.calls.listeners.ButtonClickListener
import com.cometchat.calls.listeners.SessionStatusListener
import com.cometchat.calls.services.CometChatOngoingCallService
import com.cometchat.pushnotification.calls.CallBundles
import com.cometchat.pushnotification.calls.CometChatCallNotifications
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.calls.CallSettings
import com.cometchat.samplecallsringing.data.repository.Repository

/**
 * The call itself. Opened by [OutgoingCallActivity] when the callee answers, or by the push SDK
 * when this user answers an incoming call (the answered call is in [CallBundles]). Either way it
 * joins by session ID, which takes over the session prepared while it rang.
 */
class CallActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CallActivity"
        private const val EXTRA_SESSION_ID = "session_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_IS_VIDEO = "is_video"
        private const val EXTRA_IS_PEER_CALL = "is_peer_call"

        fun intent(context: Context, sessionId: String, title: String, isVideo: Boolean, isPeerCall: Boolean): Intent =
            Intent(context, CallActivity::class.java)
                .putExtra(EXTRA_SESSION_ID, sessionId)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_IS_VIDEO, isVideo)
                .putExtra(EXTRA_IS_PEER_CALL, isPeerCall)
    }

    private lateinit var callContainer: RelativeLayout
    private lateinit var sessionId: String
    private var title = ""
    private var isVideo = true
    private var isPeerCall = true
    private var ending = false
    /** Handing over to a fresh screen for a second call; this one must not stop the ongoing-call service. */
    private var switching = false

    /**
     * The caller asked for these before calling ([com.cometchat.samplecallsringing.calls.CallLauncher]);
     * a callee answering from the notification may never have been asked. Whatever the answer, the
     * call is joined: a denied microphone or camera only means this side is not heard or seen.
     */
    private val callPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (!isFinishing) joinSession()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        val answeredCall = CallBundles.getCall(intent)
        if (answeredCall != null) {
            sessionId = answeredCall.sessionId
            title = answeredCall.callerName ?: getString(R.string.unknown)
            isVideo = answeredCall.isVideo
            // False for a group call: the call carries on when someone leaves.
            isPeerCall = answeredCall.isPeerCall
            CometChatCallNotifications.onIncomingCallScreenClosed(sessionId)
        } else {
            sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: run {
                finish()
                return
            }
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
            isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, true)
            isPeerCall = intent.getBooleanExtra(EXTRA_IS_PEER_CALL, true)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        callContainer = findViewById(R.id.call_container)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Back must not hang up: a live call carries on in the background, as with Home, and
                // the ongoing-call notification or the app icon brings it back. The end button ends
                // it. With no session left (the SDK can leave a call it joined too late without telling
                // anyone), there is nothing to keep, so Back closes the screen.
                if (CallSession.getInstance().isSessionActive) moveTaskToBack(true) else close()
            }
        })

        // Before the join: the SDK can report it (and anything after) as soon as the join starts, and
        // adding a listener while it is announcing an event crashes the SDK's listener loop.
        listen(CallSession.getInstance())

        val missing = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (isVideo) add(Manifest.permission.CAMERA)
        }.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) joinSession() else callPermissions.launch(missing.toTypedArray())
    }

    /**
     * A second call answered while this one is up (End & accept on a call that rang over it): the
     * push SDK opens the answer activity with SINGLE_TOP, so the intent lands here. The SDK has
     * already left this call, so open a fresh screen that joins the new one and close this one.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val next = CallBundles.getCall(intent) ?: return
        if (::sessionId.isInitialized && next.sessionId == sessionId) return
        Log.d(TAG, "switching from ${if (::sessionId.isInitialized) sessionId else "-"} to ${next.sessionId}")
        switching = true
        startActivity(Intent(intent).setClass(this, CallActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun joinSession() {
        // The same settings the caller passed to initiateCall: a prepared session keeps those anyway.
        val settings = CallSettings.make(title, isVideo, isPeerCall)
        CometChatCalls.joinSession(sessionId, settings, callContainer, object : CometChatCalls.CallbackListener<CallSession>() {
            override fun onSuccess(callSession: CallSession) {
                Log.d(TAG, "joined $sessionId")
            }

            override fun onError(e: CallsException) {
                Log.e(TAG, "joining $sessionId failed: ${e.message}")
                // A half-joined session left behind keeps the SDK busy: the push SDK would take it for
                // an outgoing call of ours and reject the next incoming call as busy.
                CallSession.getInstance().leave()
                runOnUiThread {
                    Toast.makeText(this@CallActivity, getString(R.string.failed_to_join_call, e.message), Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        })
    }

    private fun listen(callSession: CallSession) {
        callSession.addSessionStatusListener(this, object : SessionStatusListener() {
            override fun onSessionJoined() {
                CometChatOngoingCallService.launch(this@CallActivity)
            }

            override fun onSessionLeft() = close()

            override fun onSessionTimedOut() = close()

            // The other side ended a 1:1 call, or the session went away.
            override fun onConnectionClosed() = close()

            override fun onConnectionLost() {
                Log.w(TAG, "connection lost")
            }
        })

        callSession.addButtonClickListener(this, object : ButtonClickListener() {
            override fun onLeaveSessionButtonClicked() {
                endCall()
            }
        })
    }

    private fun close() {
        runOnUiThread {
            if (!switching) CometChatOngoingCallService.abort(this)
            finish()
        }
    }

    /**
     * Leaves the call. A 1:1 call (`setPeerCall(true)`) ends for both sides, and is also marked
     * ended on the server; a group call carries on for whoever is left.
     */
    private fun endCall() {
        if (ending) return
        ending = true
        val session = CallSession.getInstance()
        if (session.isSessionActive) session.leave()

        if (isPeerCall) {
            Repository.endCall(sessionId, Constants.CALL_STATUS_ENDED, object : CometChatCalls.CallbackListener<Void>() {
                override fun onSuccess(result: Void?) {
                    Log.d(TAG, "ended $sessionId")
                }

                override fun onError(e: CallsException) {
                    // The far side may already have ended it.
                    Log.w(TAG, "ending $sessionId failed: ${e.message}")
                }
            })
        }
        CometChatOngoingCallService.abort(this)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!switching) CometChatOngoingCallService.abort(this)
        leaveIfAbandoned()
    }

    /**
     * The screen was closed for good with its session still live (nothing ended it): leave, so no
     * half-joined session is left behind. Not when the screen is only going to the background (Back,
     * Home) or being recreated, not mid-switch to a second call, and only for this screen's own session.
     */
    private fun leaveIfAbandoned() {
        if (!isFinishing || isChangingConfigurations || switching || !::sessionId.isInitialized) return
        val session = CallSession.getInstance()
        if (session.sessionId != sessionId || !session.isSessionActive) return
        Log.w(TAG, "call screen closed with $sessionId still live; leaving")
        session.leave()
    }
}
