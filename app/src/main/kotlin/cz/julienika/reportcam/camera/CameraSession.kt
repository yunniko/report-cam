package cz.julienika.reportcam.camera

import android.util.Size
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** JPEG bytes straight from the camera, plus the rotation needed to make them upright. */
class CapturedJpeg(
    val bytes: ByteArray,
    val rotationDegrees: Int,
)

/**
 * Owns the camera for one Activity. The camera is open only between [open] and [close]; the Activity
 * calls [close] in onStop and right after a capture, so the device is never held while the app is not
 * showing a live preview. Binding is also tied to the Activity lifecycle as a second guard. See D003.
 */
class CameraSession(
    private val activity: ComponentActivity,
    private val previewView: PreviewView,
) {
    // Requested in the constructor so CameraX initialises while the layout inflates.
    private val providerFuture = ProcessCameraProvider.getInstance(activity)
    private var provider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var wanted = false

    var camera: Camera? = null
        private set

    /** Called on the main thread each time the camera is bound. */
    var onBound: ((Camera) -> Unit)? = null

    var targetRotation: Int = Surface.ROTATION_0
        set(value) {
            field = value
            imageCapture?.targetRotation = value
        }

    val isOpen: Boolean get() = camera != null

    /** [captureLongEdge] is the long edge in pixels to aim the sensor output at, or 0 for the largest. */
    fun open(captureLongEdge: Int) {
        wanted = true
        providerFuture.addListener({
            val p = providerFuture.get()
            provider = p
            // The Activity may have stopped, or a capture closed us, while CameraX was initialising.
            if (wanted && activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                bind(p, captureLongEdge)
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    fun close() {
        wanted = false
        provider?.unbindAll()
        camera = null
        imageCapture = null
    }

    private fun bind(
        p: ProcessCameraProvider,
        captureLongEdge: Int,
    ) {
        p.unbindAll()
        val preview =
            Preview
                .Builder()
                .setResolutionSelector(previewSelector())
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }
        val capture =
            ImageCapture
                .Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setResolutionSelector(captureSelector(captureLongEdge))
                // The photo is re-encoded after stamping; a high first pass keeps the double encode mild.
                .setJpegQuality(JPEG_QUALITY_FIRST_PASS)
                .setTargetRotation(targetRotation)
                .build()
        imageCapture = capture
        val bound = p.bindToLifecycle(activity, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
        camera = bound
        onBound?.invoke(bound)
    }

    suspend fun capture(): CapturedJpeg {
        val ic = imageCapture ?: throw IllegalStateException("capture() called while the camera is closed")
        return suspendCancellableCoroutine { cont ->
            ic.takePicture(
                ContextCompat.getMainExecutor(activity),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val result =
                            image.use {
                                val buffer = it.planes[0].buffer
                                val bytes = ByteArray(buffer.remaining()).also { b -> buffer.get(b) }
                                CapturedJpeg(bytes, it.imageInfo.rotationDegrees)
                            }
                        cont.resume(result)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cont.resumeWithException(exception)
                    }
                },
            )
        }
    }

    private fun previewSelector(): ResolutionSelector =
        ResolutionSelector
            .Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            // A phone screen needs no more; a smaller stream costs less ISP and GPU power.
            .setResolutionStrategy(
                ResolutionStrategy(PREVIEW_SIZE, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
            ).build()

    private fun captureSelector(longEdge: Int): ResolutionSelector {
        val strategy =
            if (longEdge <= 0) {
                ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
            } else {
                // Sensor coordinates are landscape. Closest-higher means we only ever scale down.
                ResolutionStrategy(
                    Size(longEdge, longEdge * 3 / 4),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                )
            }
        return ResolutionSelector
            .Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(strategy)
            .build()
    }

    private companion object {
        val PREVIEW_SIZE = Size(1280, 960)
        const val JPEG_QUALITY_FIRST_PASS = 95
    }
}
