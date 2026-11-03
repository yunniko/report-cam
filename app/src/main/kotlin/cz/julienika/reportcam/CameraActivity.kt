package cz.julienika.reportcam

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.ScaleGestureDetector
import android.view.Surface
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import cz.julienika.reportcam.camera.CameraSession
import cz.julienika.reportcam.camera.ExposureDefault
import cz.julienika.reportcam.databinding.ActivityCameraBinding
import cz.julienika.reportcam.location.FixPolicy
import cz.julienika.reportcam.location.LocationAccess
import cz.julienika.reportcam.location.LocationMarker
import cz.julienika.reportcam.location.LocationPermissions
import cz.julienika.reportcam.location.LocationStatus
import cz.julienika.reportcam.location.LocationTracker
import cz.julienika.reportcam.photo.PhotoPipeline
import cz.julienika.reportcam.review.ReviewScreen
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.settings.PrefsSource
import cz.julienika.reportcam.settings.SettingsActivity
import cz.julienika.reportcam.share.SharedFiles
import cz.julienika.reportcam.stamp.LocationLabels
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.ZonedDateTime

/**
 * The one screen on the hot path: live camera, then review and send ([ReviewScreen]). The camera
 * is open only while the live preview shows (D003); location is tracked only while visible (D005).
 */
class CameraActivity : ComponentActivity() {
    private lateinit var ui: ActivityCameraBinding
    private lateinit var session: CameraSession
    private lateinit var tracker: LocationTracker
    private lateinit var locationPermissions: LocationPermissions
    private lateinit var settings: AppSettings

    private lateinit var review: ReviewScreen
    private var capturing = false

    /** EV index the user dialled in; kept across retakes, dropped when the app leaves the screen. */
    private var chosenEvIndex: Int? = null
    private var loggedStartup = false
    private var pulse: ObjectAnimator? = null

    private val sharedDir by lazy { File(cacheDir, "shared") }

    private val startupPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onStartupPermissions() }

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
        // Edge to edge on every version, as Android 15+ enforces; the insets listener below places the bars.
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        ui = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(ui.root)
        session = CameraSession(this, ui.preview)
        tracker = LocationTracker(this)
        locationPermissions = LocationPermissions(this) { restartTracking() }
        review = ReviewScreen(this, ui, tracker, sharedDir) { if (hasCameraPermission()) openCamera() }
        ui.preview.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        // The whole frame, not a screen-filling crop: what the preview shows is what the photo holds (D009).
        ui.preview.scaleType = PreviewView.ScaleType.FIT_CENTER

        ViewCompat.setOnApplyWindowInsetsListener(ui.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            ui.topBar.updatePadding(top = bars.top)
            ui.cameraControls.updatePadding(bottom = bars.bottom)
            ui.reviewControls.updatePadding(bottom = bars.bottom + REVIEW_PAD)
            insets
        }

        ui.preview.previewStreamState.observe(this) { state ->
            ui.shutter.isEnabled = state == PreviewView.StreamState.STREAMING
            if (state == PreviewView.StreamState.STREAMING && !loggedStartup) {
                loggedStartup = true
                val ms = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()
                Log.i(TAG, "preview streaming ${ms}ms after process start")
            }
        }
        session.onBound = ::onCameraBound
        tracker.onStatus = ::onLocationStatus

        ui.shutter.setOnClickListener { onShutter() }
        ui.settings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        ui.locationMarker.setOnClickListener { locationPermissions.resolve(tracker.access()) }
        ui.grantPermission.setOnClickListener { requestStartupPermissions() }
        setUpGestures()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (review.isActive) review.leave() else finish()
                }
            },
        )

        lifecycleScope.launch(Dispatchers.IO) { SharedFiles.prune(sharedDir, System.currentTimeMillis()) }
    }

    override fun onStart() {
        super.onStart()
        // Settings may have changed while the settings screen was open.
        settings = AppSettings.from(PrefsSource.of(this), stampLabels())
        ui.cropGuide.ratio = settings.aspect
        // After a send, coming back means "next photo", not the old review.
        if (review.isActive && review.sentSinceCapture) review.leave()
        if (hasCameraPermission()) {
            ui.permissionPanel.isVisible = false
            if (!review.isActive) openCamera()
        } else {
            requestStartupPermissions()
        }
        // Location refreshes on every start and wake, never on capture (Owner, 2026-11-03).
        if (settings.showLocation) tracker.start() else onLocationStatus(tracker.status)
        orientationListener.enable()
    }

    override fun onStop() {
        // Release the camera the moment the app is not visible: home, share sheet, screen off. See D003.
        session.close()
        tracker.stop()
        // A dialled-in exposure belongs to the scene in front of the user; the next visit opens at the default.
        if (!review.isActive) chosenEvIndex = null
        orientationListener.disable()
        super.onStop()
    }

    // ---- camera -------------------------------------------------------------------------------

    private fun openCamera() {
        ui.shutter.isEnabled = false
        session.open(captureLongEdge = settings.outputLongEdge)
    }

    private fun onCameraBound(camera: Camera) {
        val exposure = camera.cameraInfo.exposureState
        val range = exposure.exposureCompensationRange
        ui.exposureRow.isVisible = true
        if (exposure.isExposureCompensationSupported) {
            ui.exposure.isVisible = true
            val step = exposure.exposureCompensationStep.toFloat()
            val defaultIndex = ExposureDefault.index(settings.defaultEv, step, range.lower, range.upper)
            val start = chosenEvIndex ?: defaultIndex
            camera.cameraControl.setExposureCompensationIndex(start)
            ui.exposure.max = range.upper - range.lower
            ui.exposure.progress = start - range.lower
            ui.exposure.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        bar: SeekBar,
                        progress: Int,
                        fromUser: Boolean,
                    ) {
                        val index = progress + range.lower
                        if (fromUser) {
                            camera.cameraControl.setExposureCompensationIndex(index)
                            chosenEvIndex = index
                        }
                        showExposure(index * step)
                    }

                    override fun onStartTrackingTouch(bar: SeekBar) = Unit

                    override fun onStopTrackingTouch(bar: SeekBar) = Unit
                },
            )
            showExposure(start * step)
            // Tapping the label returns to the default from settings.
            ui.exposureLabel.setOnClickListener {
                camera.cameraControl.setExposureCompensationIndex(defaultIndex)
                chosenEvIndex = null
                ui.exposure.progress = defaultIndex - range.lower
            }
        } else {
            ui.exposure.isVisible = false
            ui.exposureLabel.text = ""
        }
        camera.cameraInfo.zoomState.observe(this) { z ->
            ui.zoomLabel.text = getString(R.string.zoom_label, z.zoomRatio)
        }
        ui.zoomLabel.setOnClickListener { camera.cameraControl.setZoomRatio(1f) }
    }

    private fun showExposure(ev: Float) {
        ui.exposureLabel.text = getString(R.string.exposure_label, ev)
    }

    @SuppressLint("ClickableViewAccessibility") // pinch and tap-to-focus; the shutter stays a real button
    private fun setUpGestures() {
        val scale =
            ScaleGestureDetector(
                this,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(d: ScaleGestureDetector): Boolean {
                        val cam = session.camera ?: return false
                        val ratio = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                        cam.cameraControl.setZoomRatio(ratio * d.scaleFactor)
                        return true
                    }
                },
            )
        val tap =
            GestureDetector(
                this,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        val cam = session.camera ?: return false
                        val point = ui.preview.meteringPointFactory.createPoint(e.x, e.y)
                        cam.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
                        showFocusRing(e.x, e.y)
                        return true
                    }
                },
            )
        ui.preview.setOnTouchListener { _, e ->
            scale.onTouchEvent(e)
            if (!scale.isInProgress) tap.onTouchEvent(e)
            true
        }
    }

    private fun showFocusRing(
        x: Float,
        y: Float,
    ) {
        val ring = ui.focusRing
        ring.x = x - ring.width / 2f
        ring.y = y - ring.height / 2f
        ring.alpha = 1f
        ring.isVisible = true
        ring.animate().alpha(0f).setStartDelay(FOCUS_RING_MS).setDuration(FOCUS_FADE_MS).start()
    }

    // ---- capture ------------------------------------------------------------------------------

    private fun onShutter() {
        if (!session.isOpen || capturing) return
        capturing = true
        ui.shutter.isEnabled = false
        ui.flash.alpha = 1f
        ui.flash.animate().alpha(0f).setDuration(FLASH_MS).start()
        // The newest fix at the moment of the shutter; capture itself never asks for one (D005). With
        // "wait for precise location", a fix that is not good enough sends the photo to wait in review (D008).
        val s = settings
        val waitForFix = s.waitForPrecise && FixPolicy.shouldWait(tracker.status, System.currentTimeMillis())
        val data =
            StampData.EMPTY
                .with(StampKeys.CAPTURE_TIME, ZonedDateTime.now())
                .with(StampKeys.USER_TEXT, s.userText)
                .with(StampKeys.LOCATION, if (s.showLocation) tracker.reading() else null)
        lifecycleScope.launch {
            val jpeg =
                try {
                    session.capture()
                } catch (e: Exception) {
                    Log.e(TAG, "capture failed", e)
                    toast(R.string.capture_failed)
                    capturing = false
                    ui.shutter.isEnabled = session.isOpen
                    return@launch
                }
            // Nothing needs the sensor while the photo is reviewed: release it now, not at onStop.
            session.close()
            ui.busy.isVisible = true
            val base =
                try {
                    withContext(Dispatchers.Default) { PhotoPipeline(sharedDir).decode(jpeg, s.outputLongEdge, s.aspect) }
                } catch (e: Exception) {
                    Log.e(TAG, "decode failed", e)
                    null
                }
            capturing = false
            ui.busy.isVisible = false
            when {
                base == null -> {
                    toast(R.string.process_failed)
                    if (hasCameraPermission()) openCamera()
                }
                waitForFix -> review.waitForLocation(base, data, s)
                else -> review.stampNow(base, data, s)
            }
        }
    }

    // ---- location marker ----------------------------------------------------------------------

    private fun onLocationStatus(status: LocationStatus) {
        // Location switched off in settings: no marker, nothing to wait for.
        ui.locationMarker.isVisible = settings.showLocation
        if (!settings.showLocation) {
            setPulsing(false)
            return
        }
        val m = LocationMarker.of(status, System.currentTimeMillis(), markerLabels())
        ui.locationIcon.setImageResource(if (m.crossedOut) R.drawable.ic_location_off else R.drawable.ic_location_on)
        val color =
            when (m.tone) {
                LocationMarker.Tone.GRAY -> GRAY
                LocationMarker.Tone.AMBER -> AMBER
                LocationMarker.Tone.GREEN -> GREEN
            }
        ui.locationIcon.imageTintList = ColorStateList.valueOf(color)
        ui.locationStatus.text = m.text
        ui.locationStatus.setTextColor(if (m.tone == LocationMarker.Tone.GRAY) GRAY else WHITE)
        setPulsing(m.pulsing)
        review.onLocation(status)
    }

    private fun setPulsing(on: Boolean) {
        if (on && pulse == null) {
            pulse =
                ObjectAnimator.ofFloat(ui.locationIcon, "alpha", 1f, PULSE_MIN_ALPHA).apply {
                    duration = PULSE_MS
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    start()
                }
        } else if (!on) {
            pulse?.cancel()
            pulse = null
            ui.locationIcon.alpha = 1f
        }
    }

    private fun restartTracking() {
        tracker.stop()
        if (settings.showLocation && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
            tracker.start()
        }
    }

    // ---- permissions --------------------------------------------------------------------------

    private fun hasCameraPermission() = granted(Manifest.permission.CAMERA)

    private fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun requestStartupPermissions() {
        val wanted = mutableListOf(Manifest.permission.CAMERA)
        if (settings.showLocation) wanted += LocationPermissions.PERMISSIONS
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && settings.saveToGallery) {
            wanted += Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        startupPermissions.launch(wanted.toTypedArray())
    }

    private fun onStartupPermissions() {
        val granted = hasCameraPermission()
        ui.permissionPanel.isVisible = !granted
        if (granted && !review.isActive) openCamera()
        restartTracking()
    }

    // ---- strings ------------------------------------------------------------------------------

    private fun stampLabels() =
        LocationLabels(
            unavailable = getString(R.string.stamp_location_unavailable),
            approximate = getString(R.string.stamp_approximate),
            minutesOld = { getString(R.string.stamp_minutes_old, it) },
            hoursOld = { getString(R.string.stamp_hours_old, it) },
        )

    private fun markerLabels() =
        LocationMarker.Labels(
            denied = getString(R.string.marker_denied),
            servicesOff = getString(R.string.marker_services_off),
            searching = getString(R.string.marker_searching),
            updating = getString(R.string.marker_updating),
            approximate = getString(R.string.marker_approximate),
            approximateOnly = getString(R.string.marker_approx_only),
            minutesOld = { getString(R.string.stamp_minutes_old, it) },
        )

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_LONG).show()

    private companion object {
        const val TAG = "ReportCam"
        const val FLASH_MS = 150L
        const val FOCUS_RING_MS = 600L
        const val FOCUS_FADE_MS = 300L
        const val PULSE_MS = 600L
        const val PULSE_MIN_ALPHA = 0.25f
        const val REVIEW_PAD = 12
        const val GRAY = 0xFF9E9E9E.toInt()
        const val AMBER = 0xFFF2A33A.toInt()
        const val GREEN = 0xFF4CAF50.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
    }
}
