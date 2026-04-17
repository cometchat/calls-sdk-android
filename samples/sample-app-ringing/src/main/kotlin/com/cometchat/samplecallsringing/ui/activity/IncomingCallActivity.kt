package com.cometchat.samplecallsringing.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.core.Call
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.data.repository.Repository
import com.cometchat.samplecallsringing.databinding.ActivityIncomingCallBinding

class IncomingCallActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SESSION_ID = "SESSION_ID"
        const val EXTRA_CALLER_NAME = "CALLER_NAME"
        const val EXTRA_CALLER_AVATAR = "CALLER_AVATAR"
        const val EXTRA_CALL_TYPE = "CALL_TYPE"
        private const val LISTENER_ID = "incoming_call_listener"
    }

    private lateinit var binding: ActivityIncomingCallBinding
    private lateinit var sessionId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: run {
            finish()
            return
        }
        val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: getString(R.string.unknown)
        val callerAvatar = intent.getStringExtra(EXTRA_CALLER_AVATAR) ?: ""

        applyWindowInsets()
        setupUI(callerName, callerAvatar)
        setupButtons()
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
        binding.tvCallerName.text = name
        if (avatar.isNotEmpty()) {
            Glide.with(this)
                .load(avatar)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.imgAvatar)
        }
    }

    private fun setupButtons() {
        binding.btnAccept.setOnClickListener {
            Repository.acceptCall(sessionId, object : CometChat.CallbackListener<Call>() {
                override fun onSuccess(call: Call) {
                    runOnUiThread {
                        val intent = Intent(this@IncomingCallActivity, CallActivity::class.java)
                        intent.putExtra(CallActivity.EXTRA_SESSION_ID, call.sessionId)
                        intent.putExtra(CallActivity.EXTRA_CALL_TYPE, call.type)
                        startActivity(intent)
                        finish()
                    }
                }

                override fun onError(e: CometChatException) {
                    runOnUiThread {
                        Toast.makeText(this@IncomingCallActivity, e.message, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            })
        }

        binding.btnReject.setOnClickListener {
            Repository.rejectCall(sessionId, object : CometChat.CallbackListener<Call>() {
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
            override fun onIncomingCallCancelled(call: Call?) {
                runOnUiThread { finish() }
            }

            override fun onIncomingCallReceived(call: Call?) {}
            override fun onOutgoingCallAccepted(call: Call?) {}
            override fun onOutgoingCallRejected(call: Call?) {}
            override fun onCallEndedMessageReceived(call: Call?) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        CometChat.removeCallListener(LISTENER_ID)
    }
}
