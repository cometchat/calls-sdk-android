package com.cometchat.samplecallsringing.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.cometchat.chat.core.CometChat
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.RingingApplication
import com.cometchat.samplecallsringing.databinding.ActivityLoginBinding
import com.cometchat.samplecallsringing.ui.adapters.SampleUsersAdapter
import com.cometchat.samplecallsringing.utils.AppConstants
import com.cometchat.samplecallsringing.utils.AppUtils
import com.cometchat.samplecallsringing.viewmodels.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private var sampleUsersAdapter: SampleUsersAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        initRecyclerView()
        initViewModel()
        initClickListeners()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        sampleUsersAdapter = SampleUsersAdapter(mutableListOf()) { user ->
            binding.etUid.setText("")
            viewModel.selectUser(user)
        }
        binding.recyclerView.adapter = sampleUsersAdapter
    }

    private fun initViewModel() {
        viewModel = LoginViewModel()

        // Check if already logged in
        if (CometChat.getLoggedInUser() != null) {
            navigateToHome()
            return
        }

        // Fetch sample users
        viewModel.fetchSampleUsers()

        viewModel.loginStatus.observe(this) { isLoggedIn ->
            if (isLoggedIn == true) {
                viewModel.selectedUser.value?.let { user ->
                    AppUtils.saveLoggedInUid(this, user.uid)
                } ?: run {
                    val enteredUid = binding.etUid.text.toString().trim()
                    if (enteredUid.isNotEmpty()) {
                        AppUtils.saveLoggedInUid(this, enteredUid)
                    }
                }
                // Register global incoming call listener
                (application as RingingApplication).registerCallListener()
                navigateToHome()
            } else if (isLoggedIn == false) {
                Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.users.observe(this) { users ->
            sampleUsersAdapter?.updateList(users)
            if (users.isEmpty()) {
                binding.tvSubtitle.visibility = View.GONE
                binding.recyclerView.visibility = View.GONE
                binding.viewDivider.visibility = View.GONE
            } else {
                binding.tvSubtitle.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.VISIBLE
                binding.viewDivider.visibility = View.VISIBLE
            }
        }

        viewModel.onError().observe(this) { e ->
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun initClickListeners() {
        binding.btnContinue.setOnClickListener {
            val selectedUser = viewModel.selectedUser.value
            val enteredUid = binding.etUid.text.toString().trim()

            when {
                selectedUser == null && enteredUid.isEmpty() -> {
                    Toast.makeText(this, R.string.select_user_or_enter_uid, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val uid = selectedUser?.uid ?: enteredUid
                    val apiKey = getAuthKey()
                    viewModel.login(uid, apiKey)
                }
            }
        }

        binding.etUid.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.selectUser(null)
                sampleUsersAdapter?.clearSelection()
            }
        }

        binding.viewChangeAppCredentials.setOnClickListener {
            startActivity(Intent(this, AppCredentialsActivity::class.java))
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun getAuthKey(): String {
        return if (AppConstants.AUTH_KEY.isNotEmpty() && AppConstants.AUTH_KEY != "YOUR_AUTH_KEY") {
            AppConstants.AUTH_KEY
        } else {
            AppUtils.getAuthKey(this) ?: ""
        }
    }
}
