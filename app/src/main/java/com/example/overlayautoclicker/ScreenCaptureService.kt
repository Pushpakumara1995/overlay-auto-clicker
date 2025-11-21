package com.example.overlayautoclicker

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
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

    private var lastProcessTime: Long = 0L
    private var lastToastTime: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)

        if (resultCode != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        Toast.makeText(this, "Capture service starting", Toast.LENGTH_SHORT).show()

        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        startCapture()

        return START_STICKY
    }

    private fun startCapture() {
        try {
            // Simpler: use resources.displayMetrics instead of WindowManager
            val metrics: DisplayMetrics = resources.displayMetrics
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
            val density = metrics.densityDpi

            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

            if (mediaProjection == null) {
                Toast.makeText(this, "MediaProjection is null", Toast.LENGTH_LONG).show()
                stopSelf()
                return
            }

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

            Toast.makeText(this, "Virtual display created", Toast.LENGTH_SHORT).show()

            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

                val now = System.currentTimeMillis()
                if (now - lastProcessTime < 1000) { // at most once per second
                    image.close()
                    return@setOnImageAvailableListener
                }
                lastProcessTime = now

                val bitmap = imageToBitmap(image, screenWidth, screenHeight)
                image.close()

                if (bitmap != null) {
                    showFrameToast()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Failed to convert image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "startCapture error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            stopSelf()
        }
    }

    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap? {
        return try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            Bitmap.createBitmap(bitmap, 0, 0, width, height)
        } catch (e: Exception) {
            null
        }
    }

    private fun showFrameToast() {
        val now = System.currentTimeMillis()
        if (now - lastToastTime > 1000) {
            lastToastTime = now
            Toast.makeText(
                applicationContext,
                "Got frame from screen",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }
}
