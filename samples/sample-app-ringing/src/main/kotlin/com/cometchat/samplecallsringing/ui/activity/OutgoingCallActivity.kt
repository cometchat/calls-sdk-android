package com.cometchat.samplecallsringing.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.core.Call
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.data.repository.Repository
import com.cometchat.samplecallsringing.databinding.ActivityOutgoingCallBinding

class OutgoingCallActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SESSION_ID = "SESSION_ID"
        const val EXTRA_RECEIVER_NAME = "RECEIVER_NAME"
        const val EXTRA_RECEIVER_AVATAR = "RECEIVER_AVATAR"
        const val EXTRA_CALL_TYPE = "CALL_TYPE"
        private const val LISTENER_ID = "outgoing_call_listener"
    }

    private lateinit var binding: ActivityOutgoingCallBinding
    private lateinit var sessionId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOutgoingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: run {
            finish()
            return
        }
        val receiverName = intent.getStringExtra(EXTRA_RECEIVER_NAME) ?: getString(R.string.unknown)
        val receiverAvatar = intent.getStringExtra(EXTRA_RECEIVER_AVATAR) ?: ""

        applyWindowInsets()
        setupUI(receiverName, receiverAvatar)
        setupCancelButton()
        registerCallListener()
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
            Repository.cancelCall(sessionId, object : CometChat.CallbackListener<Call>() {
                override fun onSuccess(call: Call) {
                    finish()
                }

                override fun onError(e: CometChatException) {
                    finish()
                }
            })
        }
    }

    private fun registerCallListener() {
        CometChat.addCallListener(LISTENER_ID, object : CometChat.CallListener() {
            override fun onOutgoingCallAccepted(call: Call) {
                runOnUiThread {
                    val intent = Intent(this@OutgoingCallActivity, CallActivity::class.java)
                    intent.putExtra(CallActivity.EXTRA_SESSION_ID, call.sessionId)
                    intent.putExtra(CallActivity.EXTRA_CALL_TYPE, call.type)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onOutgoingCallRejected(call: Call) {
                runOnUiThread {
                    val status = call.callStatus
                    if (status == CometChatConstants.CALL_STATUS_BUSY) {
                        Toast.makeText(this@OutgoingCallActivity, R.string.user_is_busy, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@OutgoingCallActivity, R.string.call_rejected, Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
            }

            override fun onIncomingCallReceived(call: Call?) {}
            override fun onIncomingCallCancelled(call: Call?) {}
            override fun onCallEndedMessageReceived(call: Call?) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        CometChat.removeCallListener(LISTENER_ID)
    }
}
