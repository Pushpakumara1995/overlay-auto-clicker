package com.example.overlayautoclicker

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.widget.Toast

class ScreenCaptureService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            Toast.makeText(this, "S1: onStartCommand", Toast.LENGTH_SHORT).show()

            val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                ?: Activity.RESULT_CANCELED
            val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)

            Toast.makeText(this, "S2: resultCode=$resultCode, dataNull=${data == null}", Toast.LENGTH_SHORT).show()

            if (resultCode != Activity.RESULT_OK || data == null) {
                Toast.makeText(this, "S3: bad result, stopping", Toast.LENGTH_SHORT).show()
                stopSelf()
                return START_NOT_STICKY
            }

            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            if (mediaProjection == null) {
                Toast.makeText(this, "S4: mediaProjection null", Toast.LENGTH_LONG).show()
                stopSelf()
                return START_NOT_STICKY
            }

            Toast.makeText(this, "S5: mediaProjection ok", Toast.LENGTH_SHORT).show()

            startCaptureDebug()

            return START_STICKY
        } catch (e: Exception) {
            Toast.makeText(this, "S_ERR: ${e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }
    }

    private fun startCaptureDebug() {
        try {
            Toast.makeText(this, "C1: startCapture", Toast.LENGTH_SHORT).show()

            val metrics: DisplayMetrics = resources.displayMetrics
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
            val density = metrics.densityDpi

            Toast.makeText(this, "C2: w=$screenWidth h=$screenHeight", Toast.LENGTH_SHORT).show()

            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

            Toast.makeText(this, "C3: imageReader ok", Toast.LENGTH_SHORT).show()

            virtualDisplay = mediaProjection!!.createVirtualDisplay(
                "screen-capture",
                screenWidth,
                screenHeight,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )

            Toast.makeText(this, "C4: virtualDisplay ok", Toast.LENGTH_SHORT).show()

            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                image?.close()

                Toast.makeText(
                    applicationContext,
                    "C5: got frame",
                    Toast.LENGTH_SHORT
                ).show()
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "C_ERR: ${e.javaClass.simpleName}",
                Toast.LENGTH_LONG
            ).show()
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }
}
