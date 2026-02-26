package com.cometchat.samplecalls.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException
import com.cometchat.samplecalls.R
import com.cometchat.samplecalls.data.repository.Repository
import com.cometchat.samplecalls.utils.AppUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class HomeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HomeActivity"
        private const val PERMISSION_REQUEST_CODE = 1001

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

    private lateinit var sessionIdEditText: TextInputEditText
    private lateinit var joinMeetingButton: MaterialButton
    private lateinit var startInstantMeetingButton: MaterialButton
    private lateinit var logoutButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        applyWindowInsets()
        initViews()

        if (!hasRequiredPermissions()) {
            requestPermissions()
        }

        setupButtonListeners()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        sessionIdEditText = findViewById(R.id.edit_session_id)
        joinMeetingButton = findViewById(R.id.btn_join_meeting)
        startInstantMeetingButton = findViewById(R.id.btn_start_instant)
        logoutButton = findViewById(R.id.btn_logout)
    }

    private fun setupButtonListeners() {
        startInstantMeetingButton.setOnClickListener {
            if (!hasRequiredPermissions()) {
                requestPermissions()
                return@setOnClickListener
            }
            val sessionId = UUID.randomUUID().toString()
            startCall(sessionId)
        }

        joinMeetingButton.setOnClickListener {
            if (!hasRequiredPermissions()) {
                requestPermissions()
                return@setOnClickListener
            }

            val sessionId = sessionIdEditText.text?.toString()?.trim() ?: ""
            if (sessionId.isEmpty()) {
                sessionIdEditText.error = getString(R.string.enter_session_id)
                sessionIdEditText.requestFocus()
                return@setOnClickListener
            }
            startCall(sessionId)
        }

        logoutButton.setOnClickListener { logout() }
    }

    private fun startCall(sessionId: String) {
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra(CallActivity.EXTRA_SESSION_ID, sessionId)
        }
        startActivity(intent)
    }

    private fun logout() {
        Repository.logout(object : CometChatCalls.CallbackListener<String>() {
            override fun onSuccess(message: String) {
                Log.d(TAG, "Logout successful")
                AppUtils.clearLoggedInUid(this@HomeActivity)

                runOnUiThread {
                    startActivity(Intent(this@HomeActivity, LoginActivity::class.java))
                    finish()
                }
            }

            override fun onError(e: CometChatException) {
                Log.e(TAG, "Logout failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
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
                Toast.makeText(
                    this,
                    R.string.permissions_required,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
