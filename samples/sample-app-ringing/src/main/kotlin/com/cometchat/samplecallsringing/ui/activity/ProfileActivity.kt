package com.cometchat.samplecallsringing.ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.calls.utils.CometChatUtils
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException as ChatException
import com.cometchat.samplecallsringing.BuildConfig
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.data.repository.Repository
import com.cometchat.samplecallsringing.databinding.ActivityProfileBinding
import com.cometchat.samplecallsringing.databinding.ItemInfoRowBinding
import com.cometchat.samplecallsringing.push.PushNotifications
import com.cometchat.samplecallsringing.utils.AppConstants
import com.cometchat.samplecallsringing.utils.AppUtils
import com.cometchat.samplecallsringing.utils.Appearance

/** Who is signed in, the appearance, the app's credentials and SDK versions, and signing out. */
class ProfileActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ProfileActivity"
    }

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.toolbar.toolbar.setTitle(R.string.profile)
        binding.toolbar.toolbar.setNavigationOnClickListener { finish() }

        bindUser()
        bindAppearance()
        bindAppInfo()
        binding.btnLogout.setOnClickListener { confirmSignOut() }
    }

    private fun bindUser() {
        val user = CometChat.getLoggedInUser() ?: return
        val name = user.name ?: user.uid
        val online = user.status == CometChatConstants.USER_STATUS_ONLINE
        binding.ivAvatar.set(name, user.avatar)
        binding.tvName.text = name
        binding.tvStatus.setText(if (online) R.string.status_online else R.string.status_offline)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, if (online) R.color.status_online else R.color.text_secondary))
        binding.tvUid.text = user.uid
        binding.btnCopyUid.setOnClickListener {
            val clipboard = getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.uid), user.uid))
            // Android 13+ confirms a copy itself.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(this, R.string.uid_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindAppearance() {
        val buttons = mapOf(
            Appearance.SYSTEM to R.id.btn_appearance_system,
            Appearance.LIGHT to R.id.btn_appearance_light,
            Appearance.DARK to R.id.btn_appearance_dark
        )
        binding.groupAppearance.check(buttons.getValue(Appearance.current(this)))
        binding.groupAppearance.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val chosen = buttons.entries.first { it.value == checkedId }.key
            if (chosen != Appearance.current(this)) Appearance.set(this, chosen)
        }
    }

    private fun bindAppInfo() {
        val notSet = getString(R.string.not_set)
        val rows = listOf(
            getString(R.string.app_id) to AppConstants.APP_ID.ifEmpty { AppUtils.getAppId(this).orEmpty() }.ifEmpty { notSet },
            getString(R.string.region) to AppConstants.REGION.ifEmpty { AppUtils.getRegion(this).orEmpty() }.uppercase().ifEmpty { notSet },
            getString(R.string.push_provider_id) to AppUtils.getPushProviderId(this).ifEmpty { notSet },
            getString(R.string.app_version) to BuildConfig.VERSION_NAME,
            getString(R.string.calls_sdk) to CometChatUtils.getSDKVersion(),
            getString(R.string.chat_sdk) to com.cometchat.chat.BuildConfig.VERSION_NAME,
            getString(R.string.push_sdk) to BuildConfig.PUSH_SDK_VERSION
        )
        binding.layoutAppInfo.dividerDrawable = ContextCompat.getDrawable(this, R.drawable.divider_inset_row)
        binding.layoutAppInfo.showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        rows.forEach { (label, value) ->
            val row = ItemInfoRowBinding.inflate(layoutInflater, binding.layoutAppInfo, true)
            row.tvLabel.text = label
            row.tvValue.text = value
        }
    }

    private fun confirmSignOut() {
        val name = CometChat.getLoggedInUser()?.let { it.name ?: it.uid }.orEmpty()
        AlertDialog.Builder(this)
            .setTitle(R.string.sign_out_confirm_title)
            .setMessage(getString(R.string.sign_out_confirm_message, name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.sign_out) { _, _ -> signOut() }
            .show()
    }

    /**
     * Removes this phone's push token first, and logs out only once that is done: the request is
     * authenticated by the session being logged out, and without it this phone keeps ringing for
     * the user who left.
     */
    private fun signOut() {
        binding.btnLogout.isEnabled = false
        PushNotifications.unregisterToken {
            Repository.logoutCallsUser(object : CometChatCalls.CallbackListener<String>() {
                override fun onSuccess(message: String) = logoutChat()

                override fun onError(e: CallsException) {
                    // The Chat session still has to go, or the next launch signs this user back in.
                    Log.e(TAG, "Calls SDK logout failed: ${e.message}")
                    logoutChat()
                }
            })
        }
    }

    private fun logoutChat() {
        Repository.logoutChatUser(object : CometChat.CallbackListener<String>() {
            override fun onSuccess(message: String) {
                Log.d(TAG, "Signed out")
                AppUtils.clearLoggedInUid(this@ProfileActivity)
                runOnUiThread {
                    startActivity(Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
            }

            override fun onError(e: ChatException) {
                Log.e(TAG, "Chat SDK logout failed: ${e.message}")
                runOnUiThread {
                    binding.btnLogout.isEnabled = true
                    Toast.makeText(this@ProfileActivity, getString(R.string.sign_out_failed, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
