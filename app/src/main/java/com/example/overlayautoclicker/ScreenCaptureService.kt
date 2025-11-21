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
import android.view.WindowManager
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

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

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private var lastProcessTime: Long = 0L
    private var lastValue: Float? = null
    private var prevValue: Float? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)

        if (resultCode != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        startCapture()

        return START_STICKY
    }

    private fun startCapture() {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getRealMetrics(metrics)

        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "screen-capture",
            screenWidth,
            screenHeight,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            val now = System.currentTimeMillis()
            // Only process about twice a second to avoid overload
            if (now - lastProcessTime < 500) {
                image.close()
                return@setOnImageAvailableListener
            }
            lastProcessTime = now

            val bitmap = imageToBitmap(image, screenWidth, screenHeight)
            image.close()

            if (bitmap != null) {
                runTextRecognition(bitmap)
            }
        }, Handler(Looper.getMainLooper()))
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

            // Crop to the actual screen size (remove padding)
            Bitmap.createBitmap(bitmap, 0, 0, width, height)
        } catch (e: Exception) {
            null
        }
    }

    private fun runTextRecognition(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text ?: ""
                val value = extractValueWithX(fullText)

                if (value != null) {
                    prevValue = lastValue
                    lastValue = value

                    // Our rule: value <= 2.00x twice in a row
                    val p = prevValue
                    val c = lastValue

                    if (p != null && c != null && p <= 2.0f && c <= 2.0f) {
                        Toast.makeText(
                            applicationContext,
                            "Condition met: $p and $c",
                            Toast.LENGTH_SHORT
                        ).show()

                        // LATER: here we will trigger auto‑tap
                    } else {
                        // Optional debug:
                        // Toast.makeText(applicationContext, "Value: $c", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                // Ignore for now
            }
    }

    private fun extractValueWithX(text: String): Float? {
        // Look for patterns like "2.21X" or "1.95x"
        val regex = Regex("""(\d+(?:\.\d+)?)\s*[xX]""")
        val matches = regex.findAll(text)
        val last = matches.lastOrNull() ?: return null
        val numberPart = last.groupValues[1]
        return numberPart.toFloatOrNull()
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }
}
