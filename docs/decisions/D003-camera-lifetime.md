# D003 · The camera is open only while a live preview is visible
Date: 2026-11-03 · Goal: G-001 M1 · Status: active (superseded by: —)
Context: The Owner requires that the camera, the most power-hungry part, is held only while a photo is being taken.
Decision: `CameraSession.close()` unbinds every use case in `onStop` and straight after each capture. The lifecycle-bound CameraX binding is a second guard.
Force: requirement — Owner instruction, 2026-11-03 ("be very sure to free the camera").
Rejected: relying on `bindToLifecycle` alone (it keeps the camera bound during review); keeping the camera open during review for a faster retake (it costs power for every photo to save half a second on a rare retake).
Consequence: Any new screen state must reopen the camera explicitly. CameraX enumerates only the back camera, and the preview runs at 1280×960 on a SurfaceView; both are judgment calls for start-up and power.
Evidence: `app/src/main/kotlin/cz/julienika/reportcam/camera/CameraSession.kt`; dumpsys checks in the G-001 progress log.
