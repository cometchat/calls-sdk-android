package com.cometchat.samplecallsringing.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException as ChatException
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.RingingApplication
import com.cometchat.samplecallsringing.data.repository.Repository
import com.cometchat.samplecallsringing.databinding.ActivityUserDetailBinding
import com.cometchat.samplecallsringing.utils.AppUtils

class UserDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "UserDetailActivity"
    }

    private lateinit var binding: ActivityUserDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupToolbar()
        loadUserInfo()
        setupLogout()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadUserInfo() {
        val user = CometChat.getLoggedInUser() ?: return

        if (!user.avatar.isNullOrEmpty()) {
            Glide.with(this)
                .load(user.avatar)
                .circleCrop()
                .placeholder(R.drawable.ic_default_avatar)
                .into(binding.ivAvatar)
        }

        binding.tvName.text = user.name
        binding.tvUid.text = getString(R.string.uid_label, user.uid)

        val isOnline = user.status == "online"
        binding.tvStatus.text = if (isOnline) getString(R.string.status_online) else getString(R.string.status_offline)
        binding.tvStatus.setTextColor(
            ContextCompat.getColor(this, if (isOnline) R.color.call_accept else R.color.text_secondary)
        )
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            binding.btnLogout.isEnabled = false
            Repository.logoutChatUser(object : CometChat.CallbackListener<String>() {
                override fun onSuccess(s: String) {
                    Repository.logoutCallsUser(object : CometChatCalls.CallbackListener<String>() {
                        override fun onSuccess(message: String) {
                            Log.d(TAG, "Logout successful")
                            (application as RingingApplication).removeCallListener()
                            AppUtils.clearLoggedInUid(this@UserDetailActivity)
                            runOnUiThread {
                                val intent = Intent(this@UserDetailActivity, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            }
                        }

                        override fun onError(e: CallsException) {
                            Log.e(TAG, "Calls SDK logout failed: ${e.message}")
                            runOnUiThread {
                                binding.btnLogout.isEnabled = true
                                Toast.makeText(this@UserDetailActivity, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }

                override fun onError(e: ChatException) {
                    Log.e(TAG, "Chat SDK logout failed: ${e.message}")
                    runOnUiThread {
                        binding.btnLogout.isEnabled = true
                        Toast.makeText(this@UserDetailActivity, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }
}
