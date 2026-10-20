package com.cometchat.samplecallsringing.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.cometchat.chat.core.CometChat
import com.cometchat.pushnotification.CometChatPushNotifications
import com.cometchat.pushnotification.PNCallback
import com.cometchat.pushnotification.PNResult
import com.cometchat.pushnotification.RingReadiness
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.databinding.ActivityHomeBinding
import com.cometchat.samplecallsringing.ui.adapters.HomePagerAdapter

class HomeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HomeActivity"
        private const val PERMISSION_REQUEST_CODE = 1001

        /** Each readiness problem is pointed out once per process, not on every return to this screen. */
        private val READINESS_SHOWN = mutableSetOf<RingReadiness>()

        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupViewPager()
        setupToolbar()

        if (!hasRequiredPermissions()) {
            requestPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        checkRingReadiness()
    }

    /**
     * Each of these silences an incoming call without an error anywhere: notifications off, the
     * incoming-call channel muted, full-screen notifications revoked (Android 14+, off by default on
     * some devices), Do Not Disturb. The push SDK names the one that applies; this shows it once and
     * offers the exact settings page, as calls-core does.
     */
    private fun checkRingReadiness() {
        CometChatPushNotifications.ensureRingReady(this, object : PNCallback<RingReadiness> {
            override fun onResult(result: PNResult<RingReadiness>) {
                val readiness = result.valueOrNull() ?: return
                // READY needs nothing; for NOTIFICATIONS_OFF the SDK asks for the permission itself.
                if (readiness == RingReadiness.READY || readiness == RingReadiness.NOTIFICATIONS_OFF) return
                val message = when (readiness) {
                    RingReadiness.FSI_REVOKED -> R.string.ring_readiness_fsi_revoked
                    RingReadiness.CHANNEL_MUTED -> R.string.ring_readiness_channel_muted
                    RingReadiness.DND_BLOCKING -> R.string.ring_readiness_dnd
                    else -> return
                }
                if (!READINESS_SHOWN.add(readiness)) return
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    val settings = CometChatPushNotifications.settingsIntentFor(this@HomeActivity, readiness)
                    val dialog = AlertDialog.Builder(this@HomeActivity)
                        .setTitle(R.string.ring_readiness_title)
                        .setMessage(message)
                        .setNegativeButton(R.string.ring_readiness_later, null)
                    if (settings != null) {
                        dialog.setPositiveButton(R.string.ring_readiness_open_settings) { _, _ -> startActivity(settings) }
                    }
                    dialog.show()
                }
            }
        })
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupViewPager() {
        val adapter = HomePagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_users -> { binding.viewPager.currentItem = 0; true }
                R.id.nav_call_logs -> { binding.viewPager.currentItem = 1; true }
                else -> false
            }
        }
    }

    private fun setupToolbar() {
        val user = CometChat.getLoggedInUser()
        if (user != null && !user.avatar.isNullOrEmpty()) {
            Glide.with(this)
                .load(user.avatar)
                .circleCrop()
                .placeholder(R.drawable.ic_default_avatar)
                .into(binding.ivUserAvatar)
        }

        binding.ivUserAvatar.setOnClickListener {
            startActivity(Intent(this, UserDetailActivity::class.java))
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                Toast.makeText(this, R.string.permissions_required, Toast.LENGTH_LONG).show()
            }
        }
    }
}
