package cz.julienika.reportcam

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import cz.julienika.reportcam.camera.CameraSession
import cz.julienika.reportcam.databinding.ActivityCameraBinding
import cz.julienika.reportcam.photo.PhotoDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CameraActivity : ComponentActivity() {
    private lateinit var ui: ActivityCameraBinding
    private lateinit var session: CameraSession
    private var reviewing = false
    private var loggedStartup = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onPermissionsResult() }

    private val orientationListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                session.targetRotation =
                    when (orientation) {
                        in 45 until 135 -> Surface.ROTATION_270
                        in 135 until 225 -> Surface.ROTATION_180
                        in 225 until 315 -> Surface.ROTATION_90
                        else -> Surface.ROTATION_0
                    }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(ui.root)
        session = CameraSession(this, ui.preview)
        ui.preview.implementationMode = PreviewView.ImplementationMode.PERFORMANCE

        ViewCompat.setOnApplyWindowInsetsListener(ui.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            ui.topBar.updatePadding(top = bars.top)
            ui.cameraControls.updatePadding(bottom = bars.bottom)
            ui.reviewControls.updatePadding(bottom = bars.bottom)
            insets
        }

        ui.preview.previewStreamState.observe(this) { state ->
            val streaming = state == PreviewView.StreamState.STREAMING
            ui.shutter.isEnabled = streaming
            if (streaming && !loggedStartup) {
                loggedStartup = true
                val ms = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()
                Log.i(TAG, "preview streaming ${ms}ms after process start")
            }
        }

        ui.shutter.setOnClickListener { takePhoto() }
        ui.retake.setOnClickListener { backToCamera() }
        ui.grantPermission.setOnClickListener { requestPermissions() }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (reviewing) backToCamera() else finish()
                }
            },
        )
    }

    override fun onStart() {
        super.onStart()
        if (hasCameraPermission()) {
            ui.permissionPanel.isVisible = false
            if (!reviewing) openCamera()
        } else {
            requestPermissions()
        }
        orientationListener.enable()
    }

    override fun onStop() {
        // Release the camera the moment the app is not visible: home, share sheet, screen off. See D003.
        session.close()
        orientationListener.disable()
        super.onStop()
    }

    private fun openCamera() {
        ui.shutter.isEnabled = false
        session.open(captureLongEdge = DEFAULT_LONG_EDGE)
    }

    private fun takePhoto() {
        if (!session.isOpen) return
        ui.shutter.isEnabled = false
        ui.flash.alpha = 1f
        ui.flash.animate().alpha(0f).setDuration(FLASH_MS).start()
        lifecycleScope.launch {
            val started = SystemClock.elapsedRealtime()
            val jpeg =
                try {
                    session.capture()
                } catch (e: Exception) {
                    Log.e(TAG, "capture failed", e)
                    Toast.makeText(this@CameraActivity, R.string.capture_failed, Toast.LENGTH_LONG).show()
                    ui.shutter.isEnabled = true
                    return@launch
                }
            // Nothing needs the sensor while the photo is reviewed: release it now, not at onStop.
            session.close()
            showBusy(true)
            val photo =
                withContext(Dispatchers.Default) {
                    PhotoDecoder.decodeUpright(jpeg.bytes, jpeg.rotationDegrees, DEFAULT_LONG_EDGE)
                }
            Log.i(TAG, "photo ${photo.width}x${photo.height} ready in ${SystemClock.elapsedRealtime() - started}ms")
            showReview(photo)
        }
    }

    private fun showReview(photo: Bitmap) {
        reviewing = true
        showBusy(false)
        ui.reviewImage.setImageBitmap(photo)
        ui.reviewImage.isVisible = true
        ui.reviewControls.isVisible = true
        ui.cameraControls.isVisible = false
        ui.preview.isVisible = false
    }

    private fun backToCamera() {
        reviewing = false
        ui.reviewImage.setImageDrawable(null)
        ui.reviewImage.isVisible = false
        ui.reviewControls.isVisible = false
        ui.cameraControls.isVisible = true
        ui.preview.isVisible = true
        if (hasCameraPermission()) openCamera()
    }

    private fun showBusy(busy: Boolean) {
        ui.busy.isVisible = busy
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    private fun onPermissionsResult() {
        val granted = hasCameraPermission()
        ui.permissionPanel.isVisible = !granted
        if (granted && !reviewing) openCamera()
    }

    private companion object {
        const val TAG = "ReportCam"
        const val DEFAULT_LONG_EDGE = 1600
        const val FLASH_MS = 150L
    }
}
