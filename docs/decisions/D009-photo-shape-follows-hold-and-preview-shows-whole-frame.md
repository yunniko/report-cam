# D009 · Photo shape is short:long and follows the hold; the preview shows the whole frame
Date: 2026-11-03 · Goal: G-002 M2 · Status: active (superseded by: —)
Context: The Owner asked for photo shapes (3:4, 1:1 and so on) applied to the photo only, with the frame added around it. The preview filled the screen, so it showed less than the photo held.
Decision: A shape is stored as short edge : long edge and cut from the centre of the upright photo, portrait or landscape as the phone was held. The preview is shown whole (FIT_CENTER), with the cut-away part dimmed by the same arithmetic the pipeline uses.
Force: judgment — the Owner asked for the shapes; the orientation rule and the whole-frame preview are this project's choice.
Rejected: fixed-orientation shapes (16:9 on a portrait hold leaves a thin strip); keeping the screen-filling preview (the guide could not show the real edges).
Consequence: The crop happens before stamping, so overlay and frame lay out on the cropped photo. Each frame border is a fraction of the short edge of the cropped photo, by default 2.5%.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/photo/AspectCropTest.kt; app/src/androidTest/kotlin/cz/julienika/reportcam/CaptureFlowTest.kt
