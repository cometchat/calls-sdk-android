package com.cometchat.samplecalls.ui.activity

import android.os.Bundle
import android.util.Log
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.calls.core.CallSession
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException
import com.cometchat.calls.listeners.ButtonClickListener
import com.cometchat.calls.listeners.SessionStatusListener
import com.cometchat.calls.services.CometChatOngoingCallService
import com.cometchat.samplecalls.R

/**
 * CallActivity handles the active video call session.
 * Implements essential listeners for session management and user interactions.
 */
class CallActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CallActivity"
        const val EXTRA_SESSION_ID = "session_id"
    }

    private lateinit var callContainer: RelativeLayout
    private lateinit var sessionId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: run {
            Toast.makeText(this, R.string.invalid_session_id, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        applyWindowInsets()
        callContainer = findViewById(R.id.call_container)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                endCall()
            }
        })

        joinSession()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun joinSession() {
        val sessionSettings = CometChatCalls.SessionSettingsBuilder()
            .setTitle("CometChat Meeting")
            .startVideoPaused(false)
            .startAudioMuted(false)
            .build()

        CometChatCalls.joinSession(
            sessionId,
            sessionSettings,
            callContainer,
            object : CometChatCalls.CallbackListener<CallSession>() {
                override fun onSuccess(callSession: CallSession) {
                    Log.d(TAG, "Successfully joined session: $sessionId")
                    setupCallListeners(callSession)
                }

                override fun onError(e: CometChatException) {
                    Log.e(TAG, "Failed to join session: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(
                            this@CallActivity,
                            getString(R.string.failed_to_join_call, e.message),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }
        )
    }

    private fun setupCallListeners(callSession: CallSession) {
        // Session status listener - handles session lifecycle events
        callSession.addSessionStatusListener(this, object : SessionStatusListener() {
            override fun onSessionJoined() {
                Log.d(TAG, "Session joined")
                CometChatOngoingCallService.launch(this@CallActivity)
            }

            override fun onSessionLeft() {
                Log.d(TAG, "Session left")
                runOnUiThread {
                    CometChatOngoingCallService.abort(this@CallActivity)
                    finish()
                }
            }

            override fun onConnectionLost() {
                Log.w(TAG, "Connection lost")
                runOnUiThread {
                    Toast.makeText(this@CallActivity, R.string.connection_lost, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onConnectionRestored() {
                Log.d(TAG, "Connection restored")
                runOnUiThread {
                    Toast.makeText(this@CallActivity, R.string.connection_restored, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onSessionTimedOut() {
                Log.d(TAG, "Session timed out")
                runOnUiThread {
                    Toast.makeText(this@CallActivity, R.string.session_timed_out, Toast.LENGTH_SHORT).show()
                    CometChatOngoingCallService.abort(this@CallActivity)
                    finish()
                }
            }

            override fun onConnectionClosed() {
                Log.d(TAG, "Connection closed")
                runOnUiThread {
                    CometChatOngoingCallService.abort(this@CallActivity)
                    finish()
                }
            }
        })

        // Button click listener - handles leave button
        callSession.addButtonClickListener(this, object : ButtonClickListener() {
            override fun onLeaveSessionButtonClicked() {
                Log.d(TAG, "Leave button clicked")
                endCall()
            }
        })
    }

    private fun endCall() {
        val callSession = CallSession.getInstance()
        if (callSession.isSessionActive) {
            callSession.leaveSession()
        }
        CometChatOngoingCallService.abort(this)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        CometChatOngoingCallService.abort(this)
    }
}
