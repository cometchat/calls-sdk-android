package com.cometchat.samplecallsringing.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.pushnotification.calls.CallBundles
import com.cometchat.pushnotification.calls.CometChatCallNotifications
import com.cometchat.pushnotification.calls.IncomingCall
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.databinding.ActivityIncomingCallBinding

/**
 * The callee's ring screen. The push notifications SDK launches it (see
 * [com.cometchat.samplecallsringing.push.PushNotifications]) with the call in [CallBundles]. Accept and
 * decline go through the SDK, which records the answer and stops the ring; answering then opens
 * [CallActivity] with the answered call.
 */
class IncomingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingCallBinding
    private lateinit var sessionId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val call = CallBundles.getCall(intent) ?: run {
            finish()
            return
        }
        sessionId = call.sessionId

        applyWindowInsets()
        setupUI(call)
        setupButtons()

        // The SDK reports the ring ending for every reason: cancelled, timed out, declined on another
        // device, or answered (here or from the notification). Whichever it is, this screen is done.
        CometChatCallNotifications.onIncomingCallScreenOpened(sessionId, CometChatCallNotifications.RingEndListener {
            runOnUiThread { finish() }
        })
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUI(call: IncomingCall) {
        val name = call.callerName ?: getString(R.string.unknown)
        binding.tvCallerName.text = name
        binding.imgAvatar.set(name, call.callerAvatarUrl)
        // A group call is not a peer call: the call carries on when someone leaves.
        val isGroup = !call.isPeerCall
        binding.tvIncomingCall.setText(
            when {
                isGroup && call.isVideo -> R.string.incoming_group_video_call
                isGroup -> R.string.incoming_group_voice_call
                call.isVideo -> R.string.incoming_video_call
                else -> R.string.incoming_voice_call
            }
        )
    }

    private fun setupButtons() {
        binding.btnAccept.setOnClickListener {
            CometChatCallNotifications.answer(this, sessionId)
            finish()
        }

        binding.btnReject.setOnClickListener {
            CometChatCallNotifications.decline(this, sessionId)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::sessionId.isInitialized) {
            CometChatCallNotifications.onIncomingCallScreenClosed(sessionId)
        }
    }
}
