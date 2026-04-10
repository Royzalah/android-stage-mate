package com.roei.stagemate.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.roei.stagemate.databinding.ActivityPrivacyPolicyBinding

// Static privacy policy display screen, opened from ProfileFragment settings.
class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() {
        binding.privacyIMGBack.setOnClickListener { finish() }
    }
}
