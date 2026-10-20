package com.cometchat.samplecallsringing.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.cometchat.calls.constants.Constants
import com.cometchat.calls.core.CallSession
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.calls.listeners.ParticipantEventListener
import com.cometchat.calls.model.InitiateCallOptions
import com.cometchat.calls.model.InitiatedCall
import com.cometchat.calls.model.Participant
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.data.repository.Repository
import com.cometchat.samplecallsringing.databinding.ActivityOutgoingCallBinding
import com.cometchat.samplecallsringing.push.PushNotifications

/**
 * The caller's side while the callee's device rings, as in calls-core: the call is initiated with the
 * Calls SDK (which pushes the ring to the callee), and it joins the session once the callee answers.
 */
class OutgoingCallActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OutgoingCallActivity"
        const val EXTRA_RECEIVER_UID = "RECEIVER_UID"
        const val EXTRA_RECEIVER_NAME = "RECEIVER_NAME"
        const val EXTRA_RECEIVER_AVATAR = "RECEIVER_AVATAR"
        const val EXTRA_CALL_TYPE = "CALL_TYPE"

        /** Gives up when the callee's ring expires, so the callee never rings for a call already over. */
        private const val RING_TIMEOUT_MS = PushNotifications.RING_TIMEOUT_MS

        /** Decline and busy reach the server at once but nothing pushes them here, so the caller polls. */
        private const val STATUS_POLL_MS = 3000L
    }

    private lateinit var binding: ActivityOutgoingCallBinding
    private val handler = Handler(Looper.getMainLooper())
    private var callType: String = Constants.SESSION_TYPE_VIDEO
    private var sessionId: String? = null

    /** True from a successful initiate until the call is answered or ended. */
    private var ringing = false

    private val timeout = Runnable { endCall(Constants.CALL_STATUS_UNANSWERED, showMessage = null) }
    private val statusPoll = Runnable { pollServerStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOutgoingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val receiverUid = intent.getStringExtra(EXTRA_RECEIVER_UID) ?: run {
            finish()
            return
        }
        val receiverName = intent.getStringExtra(EXTRA_RECEIVER_NAME) ?: getString(R.string.unknown)
        val receiverAvatar = intent.getStringExtra(EXTRA_RECEIVER_AVATAR) ?: ""
        callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: Constants.SESSION_TYPE_VIDEO

        applyWindowInsets()
        setupUI(receiverName, receiverAvatar)
        setupCancelButton()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                endCall(Constants.CALL_STATUS_CANCELLED, showMessage = null)
            }
        })

        initiateCall(receiverUid)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUI(name: String, avatar: String) {
        binding.tvReceiverName.text = name
        if (avatar.isNotEmpty()) {
            Glide.with(this)
                .load(avatar)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.imgAvatar)
        }
    }

    private fun setupCancelButton() {
        binding.btnCancel.setOnClickListener {
            endCall(Constants.CALL_STATUS_CANCELLED, showMessage = null)
        }
    }

    private fun initiateCall(receiverUid: String) {
        val options = InitiateCallOptions().apply {
            type = callType
            receiverType = Constants.RECEIVER_TYPE_USER
        }
        Log.i(TAG, "initiating $callType call to $receiverUid")

        CometChatCalls.initiateCall(this, receiverUid, options, object : CometChatCalls.CallbackListener<InitiatedCall>() {
            override fun onSuccess(call: InitiatedCall) {
                val newSessionId = call.sessionId
                Log.i(TAG, "initiated session $newSessionId")
                if (isFinishing || isDestroyed) {
                    // Cancelled before the call existed: withdraw it so the callee does not ring for it.
                    Repository.endCall(newSessionId, Constants.CALL_STATUS_CANCELLED, quietly(newSessionId))
                    return
                }
                sessionId = newSessionId
                ringing = true
                listenForAnswer()
                handler.postDelayed(timeout, RING_TIMEOUT_MS)
                handler.postDelayed(statusPoll, STATUS_POLL_MS)
            }

            override fun onError(e: CallsException) {
                Log.e(TAG, "initiateCall failed: ${e.message}")
                Toast.makeText(this@OutgoingCallActivity, getString(R.string.failed_to_join_call, e.message), Toast.LENGTH_LONG).show()
                finish()
            }
        })
    }

    /** The callee's answer shows up on the prepared session as a participant joining. */
    private fun listenForAnswer() {
        CallSession.getInstance().addParticipantEventListener(this, object : ParticipantEventListener() {
            override fun onParticipantJoined(participant: Participant) {
                onAnswered()
            }
        })
    }

    private fun onAnswered() {
        if (!ringing) return
        val answeredSessionId = sessionId ?: return
        Log.i(TAG, "callee answered $answeredSessionId; joining")
        ringing = false
        handler.removeCallbacks(timeout)
        handler.removeCallbacks(statusPoll)
        startActivity(Intent(this, CallActivity::class.java).apply {
            putExtra(CallActivity.EXTRA_SESSION_ID, answeredSessionId)
            putExtra(CallActivity.EXTRA_CALL_TYPE, callType)
        })
        finish()
    }

    /**
     * Polls on a fixed interval while ringing, without waiting for the previous lookup: the call-log
     * lookup can take longer than the interval, and a lookup that fails or returns nothing final must
     * not stop the next one. Whichever lookup first reports a final status ends the call.
     */
    private fun pollServerStatus() {
        val pollSessionId = sessionId ?: return
        if (!ringing) return
        handler.postDelayed(statusPoll, STATUS_POLL_MS)
        Repository.fetchCallStatus(pollSessionId) { status ->
            if (!ringing) return@fetchCallStatus
            when (status?.lowercase()) {
                // Not listed yet, or the call is still being set up / already answered (joins on the event).
                null, Constants.CALL_STATUS_INITIATED, Constants.CALL_STATUS_ONGOING -> Unit
                Constants.CALL_STATUS_REJECTED -> endWithoutServer(R.string.call_rejected)
                Constants.CALL_STATUS_BUSY -> endWithoutServer(R.string.user_is_busy)
                else -> endWithoutServer(R.string.call_ended)
            }
        }
    }

    /** The server already recorded the outcome, so there is nothing to end; only leave the screen. */
    private fun endWithoutServer(messageRes: Int) {
        Log.i(TAG, "server says the call is over; leaving")
        ringing = false
        handler.removeCallbacksAndMessages(null)
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
        finish()
    }

    /** Ends our own ringing call on the server with [status], then leaves the screen. */
    private fun endCall(status: String, showMessage: Int?) {
        if (!ringing) {
            finish()
            return
        }
        ringing = false
        handler.removeCallbacksAndMessages(null)
        val endSessionId = sessionId
        if (endSessionId != null) {
            Repository.endCall(endSessionId, status, quietly(endSessionId))
        }
        showMessage?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        finish()
    }

    /** Logs a failed end; the screen has already left, so there is no one to show it to. */
    private fun quietly(endSessionId: String) = object : CometChatCalls.CallbackListener<Void>() {
        override fun onSuccess(result: Void?) {
            Log.i(TAG, "ended $endSessionId")
        }

        override fun onError(e: CallsException) {
            Log.e(TAG, "ending $endSessionId failed: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
