package com.nebula.executor

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.nebula.executor.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            updateStatus("Status: Permission granted ✓")
        } else {
            updateStatus("Status: Permission denied ✗")
            Toast.makeText(this, getString(R.string.overlay_permission_needed), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAnimations()
        setupClickListeners()
        checkOverlayPermission()
    }

    private fun setupAnimations() {
        val root = binding.root
        root.alpha = 0f
        ObjectAnimator.ofFloat(root, View.ALPHA, 0f, 1f).apply {
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        binding.tvLogo.translationY = -60f
        ObjectAnimator.ofFloat(binding.tvLogo, View.TRANSLATION_Y, -60f, 0f).apply {
            duration = 600
            startDelay = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        binding.tvSubtitle.translationY = -40f
        binding.tvSubtitle.alpha = 0f
        ObjectAnimator.ofFloat(binding.tvSubtitle, View.TRANSLATION_Y, -40f, 0f).apply {
            duration = 600
            startDelay = 400
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(binding.tvSubtitle, View.ALPHA, 0f, 1f).apply {
            duration = 600
            startDelay = 400
            start()
        }

        binding.btnLaunch.scaleX = 0.8f
        binding.btnLaunch.scaleY = 0.8f
        binding.btnLaunch.alpha = 0f
        ObjectAnimator.ofFloat(binding.btnLaunch, View.SCALE_X, 0.8f, 1f).apply {
            duration = 500
            startDelay = 600
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(binding.btnLaunch, View.SCALE_Y, 0.8f, 1f).apply {
            duration = 500
            startDelay = 600
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(binding.btnLaunch, View.ALPHA, 0f, 1f).apply {
            duration = 500
            startDelay = 600
            start()
        }
    }

    private fun setupClickListeners() {
        binding.btnLaunch.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
                return@setOnClickListener
            }
            launchExecutor()
        }
    }

    private fun launchExecutor() {
        updateStatus("Status: Launching overlay…")
        val serviceIntent = Intent(this, FloatingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Fade out and finish
        ObjectAnimator.ofFloat(binding.root, View.ALPHA, 1f, 0f).apply {
            duration = 400
            start()
        }
        binding.root.postDelayed({ finish() }, 450)
    }

    private fun checkOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            updateStatus("Status: Ready ✓")
        } else {
            updateStatus("Status: Overlay permission required")
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun updateStatus(text: String) {
        binding.tvStatus.text = text
    }
}
