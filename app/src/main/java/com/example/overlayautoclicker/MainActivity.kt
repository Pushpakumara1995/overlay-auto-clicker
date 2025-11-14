package com.example.overlayautoclicker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val overlayPermissionRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnRequestOverlay = findViewById<Button>(R.id.btnRequestOverlay)
        val btnStartOverlay = findViewById<Button>(R.id.btnStartOverlay)

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
    }
}
