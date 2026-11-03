package cz.julienika.reportcam

import android.app.Application
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig

class ReportCamApp :
    Application(),
    CameraXConfig.Provider {
    // Enumerating only the back camera shortens CameraX initialisation; the app never uses another. See D003.
    override fun getCameraXConfig(): CameraXConfig =
        CameraXConfig.Builder
            .fromConfig(Camera2Config.defaultConfig())
            .setAvailableCamerasLimiter(CameraSelector.DEFAULT_BACK_CAMERA)
            .setMinimumLoggingLevel(Log.ERROR)
            .build()
}
