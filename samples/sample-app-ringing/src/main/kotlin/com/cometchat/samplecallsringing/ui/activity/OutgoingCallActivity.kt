package com.cometchat.samplecallsringing.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.calls.constants.Constants
import com.cometchat.calls.core.CallSession
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.calls.listeners.ParticipantEventListener
import com.cometchat.calls.listeners.SessionStatusListener
import com.cometchat.calls.model.InitiateCallOptions
import com.cometchat.calls.model.InitiatedCall
import com.cometchat.calls.model.Participant
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.calls.CallSettings
import com.cometchat.samplecallsringing.calls.CallTarget
import com.cometchat.samplecallsringing.calls.OutgoingCall
import com.cometchat.samplecallsringing.data.repository.Repository
import com.cometchat.samplecallsringing.databinding.ActivityOutgoingCallBinding

/**
 * The caller's screen: "Calling… / Ringing…" until the callee answers, then the call screen.
 *
 * `initiateCall` (Calls SDK) creates the call, rings the callee (every member of a group) and
 * prepares the session while it rings. Every decision is made by [OutgoingCall]; this activity
 * feeds it SDK events and renders what it says.
 */
class OutgoingCallActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OutgoingCallActivity"
        private const val EXTRA_RECEIVER_ID = "RECEIVER_ID"
        private const val EXTRA_RECEIVER_NAME = "RECEIVER_NAME"
        private const val EXTRA_RECEIVER_AVATAR = "RECEIVER_AVATAR"
        private const val EXTRA_IS_GROUP = "IS_GROUP"
        private const val EXTRA_IS_VIDEO = "IS_VIDEO"

        /** Decline and busy reach the server at once but nothing pushes them here, so the caller polls. */
        private const val STATUS_POLL_MS = 3000L

        /** How long a close waits for the server to say why before calling it "Call ended". */
        private const val SERVER_STATUS_TIMEOUT_MS = 1500L

        /** How long "No answer", "Declined" and the like stay on screen. */
        private const val END_TEXT_DELAY_MS = 1500L

        fun intent(context: Context, target: CallTarget, isVideo: Boolean): Intent =
            Intent(context, OutgoingCallActivity::class.java)
                .putExtra(EXTRA_RECEIVER_ID, target.id)
                .putExtra(EXTRA_RECEIVER_NAME, target.name)
                .putExtra(EXTRA_RECEIVER_AVATAR, target.avatar)
                .putExtra(EXTRA_IS_GROUP, target.isGroup)
                .putExtra(EXTRA_IS_VIDEO, isVideo)
    }

    private lateinit var binding: ActivityOutgoingCallBinding
    private lateinit var call: OutgoingCall
    private val handler = Handler(Looper.getMainLooper())
    private var receiverName = ""
    private var isVideo = true

    private val timeout = Runnable { onRingTimeout() }
    private val statusPoll = Runnable { pollServerStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOutgoingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val receiverId = intent.getStringExtra(EXTRA_RECEIVER_ID) ?: run {
            finish()
            return
        }
        receiverName = intent.getStringExtra(EXTRA_RECEIVER_NAME) ?: receiverId
        isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, true)
        val isGroup = intent.getBooleanExtra(EXTRA_IS_GROUP, false)

        call = OutgoingCall(isGroup)
        call.onTransition = { from, to -> Log.i(TAG, "$from -> $to") }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, bars.bottom)
            insets
        }
        binding.tvHeader.setText(
            when {
                isGroup && isVideo -> R.string.header_group_video_call
                isGroup -> R.string.header_group_voice_call
                isVideo -> R.string.header_video_call
                else -> R.string.header_voice_call
            }
        )
        binding.imgAvatar.set(receiverName, intent.getStringExtra(EXTRA_RECEIVER_AVATAR), initialsCount = if (isGroup) 2 else 1)
        binding.tvReceiverName.text = receiverName
        binding.btnCancel.setOnClickListener { giveUp(OutgoingCall.EndReason.CANCELLED) }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Before the call exists there is nothing to cancel; initiateCall's callback withdraws it.
                if (!giveUp(OutgoingCall.EndReason.CANCELLED) && !call.isRinging) finish()
            }
        })

        listenToPreparedSession()
        render()
        initiateCall(receiverId, isGroup)
    }

    /** Registered before `initiateCall`, so no event from the prepared session is missed. */
    private fun listenToPreparedSession() {
        val session = CallSession.getInstance()
        session.addParticipantEventListener(this, object : ParticipantEventListener() {
            override fun onParticipantConnected(participant: Participant) {
                // A callee's device has the session: "Calling…" becomes "Ringing…".
                runOnUiThread { if (call.participantConnected(participant.uid)) render() }
            }

            override fun onParticipantJoined(participant: Participant) {
                runOnUiThread { onAnswered() }
            }
        })
        session.addSessionStatusListener(this, object : SessionStatusListener() {
            override fun onConnectionClosed() {
                runOnUiThread { onPreparedSessionClosed() }
            }
        })
    }

    private fun initiateCall(receiverId: String, isGroup: Boolean) {
        val options = InitiateCallOptions().apply {
            type = if (isVideo) Constants.SESSION_TYPE_VIDEO else Constants.SESSION_TYPE_AUDIO
            receiverType = if (isGroup) Constants.RECEIVER_TYPE_GROUP else Constants.RECEIVER_TYPE_USER
            // Frozen on the prepared session; CallActivity builds the same ones to join it.
            sessionSettings = CallSettings.make(receiverName, isVideo, isPeerCall = !isGroup)
        }
        Log.i(TAG, "initiating ${options.type} ${options.receiverType} call to $receiverId")

        CometChatCalls.initiateCall(this, receiverId, options, object : CometChatCalls.CallbackListener<InitiatedCall>() {
            override fun onSuccess(initiated: InitiatedCall) {
                val sessionId = initiated.sessionId
                if (isFinishing || isDestroyed) {
                    // Left before the call existed: withdraw it so nobody rings for it.
                    endOnServer(sessionId, Constants.CALL_STATUS_CANCELLED)
                    CallSession.getInstance().leave()
                    return
                }
                if (!call.initiated(sessionId)) return
                handler.postDelayed(timeout, OutgoingCall.RING_TIMEOUT_MS)
                handler.postDelayed(statusPoll, STATUS_POLL_MS)
                render()
            }

            override fun onError(e: CallsException) {
                Log.e(TAG, "initiateCall failed: ${e.code} ${e.message}")
                if (isFinishing || isDestroyed || !call.initiateFailed()) return
                render()
                finishAfterEndText()
            }
        })
    }

    /** The callee answered: the call screen joins the session `initiateCall` prepared. */
    private fun onAnswered() {
        if (!call.participantJoined()) return
        handler.removeCallbacksAndMessages(null)
        val sessionId = call.sessionId ?: return
        startActivity(CallActivity.intent(this, sessionId, receiverName, isVideo, isPeerCall = !call.isGroup))
        finish()
    }

    /**
     * The ring timer expired. The callee may have answered moments ago, before the join reached this
     * side: giving up then would leave the session and end a call the callee is already in. So ask
     * the server first; `ongoing` means answered, anything else (or no reply in time) is no answer.
     */
    private fun onRingTimeout() {
        val sessionId = call.sessionId ?: return
        if (!call.isRinging) return
        var decided = false
        val decide = { status: String? ->
            if (!decided && !isDestroyed) {
                decided = true
                if (status?.lowercase() == Constants.CALL_STATUS_ONGOING) {
                    Log.i(TAG, "ring timer expired, but $sessionId is ongoing: answered, joining")
                    onAnswered()
                } else {
                    giveUp(OutgoingCall.EndReason.TIMEOUT)
                }
            }
        }
        handler.postDelayed({ decide(null) }, SERVER_STATUS_TIMEOUT_MS)
        Repository.fetchCallStatus(sessionId) { decide(it) }
    }

    /**
     * The end button or the ring timer. Ends the call on the server, which stops the callee's
     * phone ringing, and leaves the prepared session. Returns false when there was nothing to end.
     */
    private fun giveUp(reason: OutgoingCall.EndReason): Boolean {
        val changed = if (reason == OutgoingCall.EndReason.TIMEOUT) call.timeout() else call.cancel()
        if (!changed) return false
        handler.removeCallbacksAndMessages(null)
        call.sessionId?.let { endOnServer(it, call.endCallStatusFor(reason)) }
        CallSession.getInstance().leave()
        render()
        if (reason == OutgoingCall.EndReason.TIMEOUT) finishAfterEndText() else finish()
        return true
    }

    /**
     * Polls while ringing, on a fixed interval: a lookup can take longer than the interval, and one
     * that fails must not stop the next.
     */
    private fun pollServerStatus() {
        val sessionId = call.sessionId ?: return
        if (!call.isRinging) return
        handler.postDelayed(statusPoll, STATUS_POLL_MS)
        Repository.fetchCallStatus(sessionId) { status ->
            if (isDestroyed || !call.serverEnded(status)) return@fetchCallStatus
            handler.removeCallbacksAndMessages(null)
            CallSession.getInstance().leave()
            render()
            finishAfterEndText()
        }
    }

    /** The prepared session went away while ringing. The close carries no reason, so ask the server. */
    private fun onPreparedSessionClosed() {
        val sessionId = call.sessionId ?: return
        if (!call.isRinging) return
        var delivered = false
        val apply = { status: String? ->
            if (!delivered) {
                delivered = true
                applyClose(status)
            }
        }
        handler.postDelayed({ apply(null) }, SERVER_STATUS_TIMEOUT_MS)
        Repository.fetchCallStatus(sessionId) { apply(it) }
    }

    private fun applyClose(serverStatus: String?) {
        if (isDestroyed || !call.closedWithServerStatus(serverStatus)) return
        handler.removeCallbacks(timeout)
        handler.removeCallbacks(statusPoll)
        // Our own connection failed before any callee connected: the callee may still be ringing.
        if (call.needsServerEnd) {
            call.sessionId?.let { endOnServer(it, call.endCallStatusFor(OutgoingCall.EndReason.LOST)) }
        }
        render()
        finishAfterEndText()
    }

    private fun render() {
        binding.btnCancel.isEnabled = call.canCancel
        binding.btnCancel.alpha = if (call.canCancel) 1f else 0.4f
        val status = call.statusText
        binding.tvCalling.text = when {
            status == OutgoingCall.StatusText.RINGING && call.isGroup && call.connectedCount > 0 ->
                resources.getQuantityString(R.plurals.group_ringing, call.connectedCount, call.connectedCount)
            else -> when (status) {
                OutgoingCall.StatusText.CALLING -> getString(R.string.calling)
                OutgoingCall.StatusText.RINGING -> getString(R.string.ringing)
                OutgoingCall.StatusText.NO_ANSWER -> getString(R.string.outgoing_no_answer)
                OutgoingCall.StatusText.FAILED -> getString(R.string.outgoing_failed)
                OutgoingCall.StatusText.ENDED -> getString(R.string.call_ended)
                OutgoingCall.StatusText.DECLINED -> getString(R.string.outgoing_declined)
                OutgoingCall.StatusText.BUSY -> getString(R.string.outgoing_busy)
                OutgoingCall.StatusText.NONE -> binding.tvCalling.text
            }
        }
    }

    private fun finishAfterEndText() {
        handler.postDelayed({ finish() }, END_TEXT_DELAY_MS)
    }

    /** Fire-and-forget: leaving the screen must not wait on the network. */
    private fun endOnServer(sessionId: String, status: String) {
        Repository.endCall(sessionId, status, object : CometChatCalls.CallbackListener<Void>() {
            override fun onSuccess(result: Void?) {
                Log.i(TAG, "ended $sessionId as $status")
            }

            override fun onError(e: CallsException) {
                // Losing a race with the far side ending it first is harmless.
                Log.w(TAG, "ending $sessionId failed: ${e.message}")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        // Whatever took the screen down, the callee must not be left ringing.
        if (::call.isInitialized && call.isRinging) giveUp(OutgoingCall.EndReason.CANCELLED)
    }
}
