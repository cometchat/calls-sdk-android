package com.cometchat.samplecallsringing.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
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
                endCall()
            }
        })

        joinSession()
    }

    private fun joinSession() {
        // The same settings the caller passed to initiateCall: a prepared session keeps those anyway.
        val settings = CallSettings.make(title, isVideo, isPeerCall)
        CometChatCalls.joinSession(sessionId, settings, callContainer, object : CometChatCalls.CallbackListener<CallSession>() {
            override fun onSuccess(callSession: CallSession) {
                Log.d(TAG, "joined $sessionId")
                listen(callSession)
            }

            override fun onError(e: CallsException) {
                Log.e(TAG, "joining $sessionId failed: ${e.message}")
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
            CometChatOngoingCallService.abort(this)
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
        CometChatOngoingCallService.abort(this)
    }
}
