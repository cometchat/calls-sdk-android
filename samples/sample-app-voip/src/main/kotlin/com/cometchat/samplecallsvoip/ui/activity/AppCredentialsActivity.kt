package com.cometchat.samplecallsvoip.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.samplecallsvoip.R
import com.cometchat.samplecallsvoip.databinding.ActivityAppCredentialsBinding
import com.cometchat.samplecallsvoip.utils.AppUtils
import com.cometchat.samplecallsvoip.viewmodels.AppCredentialsViewModel
import com.google.android.material.card.MaterialCardView

class AppCredentialsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppCredentialsBinding
    private lateinit var viewModel: AppCredentialsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppCredentialsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        initViewModel()
        initClickListeners()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, systemBars.bottom)
            insets
        }
    }

    private fun initViewModel() {
        viewModel = AppCredentialsViewModel()

        viewModel.error.observe(this) { e ->
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun initClickListeners() {
        binding.btnContinue.setOnClickListener {
            when {
                viewModel.selectedRegion.value == null -> {
                    Toast.makeText(this, R.string.please_select_region, Toast.LENGTH_SHORT).show()
                }
                binding.etAppId.text.toString().isEmpty() -> {
                    Toast.makeText(this, R.string.invalid_app_id, Toast.LENGTH_SHORT).show()
                }
                binding.etAuthKey.text.toString().isEmpty() -> {
                    Toast.makeText(this, R.string.invalid_auth_key, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    viewModel.initSDKs(
                        this,
                        binding.etAppId.text.toString(),
                        binding.etAuthKey.text.toString()
                    )
                }
            }
        }

        binding.cardUs.setOnClickListener {
            viewModel.setSelectedRegion(AppUtils.REGION_US)
            updateRegionCards(binding.cardUs, binding.cardEu, binding.cardIn)
        }

        binding.cardEu.setOnClickListener {
            viewModel.setSelectedRegion(AppUtils.REGION_EU)
            updateRegionCards(binding.cardEu, binding.cardUs, binding.cardIn)
        }

        binding.cardIn.setOnClickListener {
            viewModel.setSelectedRegion(AppUtils.REGION_IN)
            updateRegionCards(binding.cardIn, binding.cardUs, binding.cardEu)
        }
    }

    private fun updateRegionCards(
        selected: MaterialCardView,
        unselected1: MaterialCardView,
        unselected2: MaterialCardView
    ) {
        selected.strokeColor = ContextCompat.getColor(this, R.color.primary)
        selected.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary_light))

        unselected1.strokeColor = ContextCompat.getColor(this, R.color.stroke_default)
        unselected1.setCardBackgroundColor(ContextCompat.getColor(this, R.color.background))

        unselected2.strokeColor = ContextCompat.getColor(this, R.color.stroke_default)
        unselected2.setCardBackgroundColor(ContextCompat.getColor(this, R.color.background))
    }
}
