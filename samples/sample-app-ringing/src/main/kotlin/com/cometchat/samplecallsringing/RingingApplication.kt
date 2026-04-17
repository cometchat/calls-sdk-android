package com.cometchat.samplecallsringing

import android.app.Application
import android.content.Intent
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.Call
import com.cometchat.samplecallsringing.ui.activity.IncomingCallActivity

class RingingApplication : Application() {

    companion object {
        private const val LISTENER_ID = "global_call_listener"
    }

    override fun onCreate() {
        super.onCreate()
    }

    fun registerCallListener() {
        CometChat.addCallListener(LISTENER_ID, object : CometChat.CallListener() {
            override fun onIncomingCallReceived(call: Call) {
                val intent = Intent(this@RingingApplication, IncomingCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(IncomingCallActivity.EXTRA_SESSION_ID, call.sessionId)
                    putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, call.sender?.name ?: "Unknown")
                    putExtra(IncomingCallActivity.EXTRA_CALLER_AVATAR, call.sender?.avatar ?: "")
                    putExtra(IncomingCallActivity.EXTRA_CALL_TYPE, call.type)
                }
                startActivity(intent)
            }

            override fun onOutgoingCallAccepted(call: Call?) {}
            override fun onOutgoingCallRejected(call: Call?) {}
            override fun onIncomingCallCancelled(call: Call?) {}
            override fun onCallEndedMessageReceived(call: Call?) {}
        })
    }

    fun removeCallListener() {
        CometChat.removeCallListener(LISTENER_ID)
    }
}
