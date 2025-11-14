package com.example.overlayautoclicker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.media.projection.MediaProjectionManager

class MainActivity : AppCompatActivity() {

    private val overlayPermissionRequestCode = 1001
    private val screenCaptureRequestCode = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnRequestOverlay = findViewById<Button>(R.id.btnRequestOverlay)
        val btnStartOverlay = findViewById<Button>(R.id.btnStartOverlay)
        val btnStartCapture = findViewById<Button>(R.id.btnStartCapture)

        // Button 1: ask Android to show the "draw over other apps" screen
        btnRequestOverlay.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, overlayPermissionRequestCode)
            }
        }

        // Button 2: start the overlay service (shows the red box)
        btnStartOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please allow overlay first", Toast.LENGTH_SHORT).show()
            } else {
                val serviceIntent = Intent(this, OverlayService::class.java)
                startService(serviceIntent)
                Toast.makeText(this, "Overlay started", Toast.LENGTH_SHORT).show()
            }
        }

        // Button 3: start screen capture (shows system dialog "Start now")
        btnStartCapture.setOnClickListener {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val captureIntent = projectionManager.createScreenCaptureIntent()
            startActivityForResult(captureIntent, screenCaptureRequestCode)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == screenCaptureRequestCode) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                    putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(ScreenCaptureService.EXTRA_DATA, data)
                }
                startService(serviceIntent)
                Toast.makeText(this, "Screen capture started", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
