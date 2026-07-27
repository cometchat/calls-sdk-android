package com.cometchat.samplecallsvoip.ui.activity

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
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.calls.listeners.ButtonClickListener
import com.cometchat.calls.listeners.SessionStatusListener
import com.cometchat.calls.model.AudioMode
import com.cometchat.calls.model.LayoutType
import com.cometchat.calls.model.SessionType
import com.cometchat.calls.services.CometChatOngoingCallService
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException as ChatException
import com.cometchat.chat.core.Call
import com.cometchat.samplecallsvoip.R
import com.cometchat.samplecallsvoip.data.repository.Repository

class CallActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CallActivity"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_CALL_TYPE = "call_type"
        private const val LISTENER_ID = "call_activity_listener"
    }

    private lateinit var callContainer: RelativeLayout
    private lateinit var sessionId: String
    private var callType: String = CometChatConstants.CALL_TYPE_VIDEO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: run {
            finish()
            return
        }
        callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: CometChatConstants.CALL_TYPE_VIDEO

        applyWindowInsets()
        callContainer = findViewById(R.id.call_container)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                endCall()
            }
        })

        registerChatCallListener()
        joinSession()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun registerChatCallListener() {
        CometChat.addCallListener(LISTENER_ID, object : CometChat.CallListener() {
            override fun onCallEndedMessageReceived(call: Call?) {
                Log.d(TAG, "Call ended by other party")
                val session = CallSession.getInstance()
                if (session.isSessionActive) {
                    session.leaveSession()
                }
                CometChatOngoingCallService.abort(this@CallActivity)
                runOnUiThread { finish() }
            }

            override fun onIncomingCallReceived(call: Call?) {}
            override fun onOutgoingCallAccepted(call: Call?) {}
            override fun onOutgoingCallRejected(call: Call?) {}
            override fun onIncomingCallCancelled(call: Call?) {}
        })
    }

    private fun joinSession() {
        val isVoiceOnly = callType == CometChatConstants.CALL_TYPE_AUDIO
        val sessionSettings = CometChatCalls.SessionSettingsBuilder()
            .setTitle("CometChat Call")
            .setSessionType(if (isVoiceOnly) SessionType.VOICE else SessionType.VIDEO)
            .setLayout(if (isVoiceOnly) LayoutType.SPOTLIGHT else LayoutType.TILE)
            .setAudioMode(if (isVoiceOnly) AudioMode.EARPIECE else AudioMode.SPEAKER)
            .startVideoPaused(isVoiceOnly)
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

                override fun onError(e: CallsException) {
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
        callSession.addSessionStatusListener(this, object : SessionStatusListener() {
            override fun onSessionJoined() {
                Log.d(TAG, "Session joined")
                CometChatOngoingCallService.launch(this@CallActivity)
            }

            override fun onSessionLeft() {
                Log.d(TAG, "Session left")
                CometChat.clearActiveCall()
                runOnUiThread {
                    CometChatOngoingCallService.abort(this@CallActivity)
                    finish()
                }
            }

            override fun onConnectionLost() {
                Log.w(TAG, "Connection lost")
            }

            override fun onConnectionRestored() {
                Log.d(TAG, "Connection restored")
            }

            override fun onSessionTimedOut() {
                Log.d(TAG, "Session timed out")
                CometChat.clearActiveCall()
                runOnUiThread {
                    CometChatOngoingCallService.abort(this@CallActivity)
                    finish()
                }
            }

            override fun onConnectionClosed() {
                Log.d(TAG, "Connection closed")
                CometChat.clearActiveCall()
                runOnUiThread {
                    CometChatOngoingCallService.abort(this@CallActivity)
                    finish()
                }
            }
        })

        callSession.addButtonClickListener(this, object : ButtonClickListener() {
            override fun onLeaveSessionButtonClicked() {
                Log.d(TAG, "Leave button clicked")
                endCall()
            }
        })
    }

    private fun endCall() {
        val session = CallSession.getInstance()
        if (session.isSessionActive) {
            session.leaveSession()
        }

        CometChat.clearActiveCall()

        Repository.endCall(sessionId, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(call: Call?) {
                Log.d(TAG, "Call ended successfully")
            }

            override fun onError(e: ChatException) {
                Log.e(TAG, "Failed to end call: ${e.message}")
            }
        })

        CometChatOngoingCallService.abort(this)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        CometChat.removeCallListener(LISTENER_ID)
        CometChatOngoingCallService.abort(this)
    }
}
