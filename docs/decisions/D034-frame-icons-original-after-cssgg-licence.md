# D034 · The frame icons are original drawings; css.gg is out
Date: 2026-11-06 · Goal: G-006 M2 · Status: active (superseded by: —)
Context: the two frame-position icons were css.gg "polaroid", one turned, credited as MIT from Iconify. The set's own licence (`https://css.gg/licence.txt`, read 2026-11-06) is MIT only before 2.1.2; from 2.1.2 it forbids derivative works and commercial use without permission, and Iconify does not say which version it carries.
Decision: replace both icons with an original drawing (a rounded frame with a solid band below or above the window) and drop css.gg from the attributions.
Force: requirement — VALUES.md (respect licences) and a Play listing: a turned copy is a derivative, and the version Iconify ships cannot be shown to predate the licence change.
Rejected: keeping the icons under the MIT claim (unprovable); asking css.gg for permission (a shape this simple needs no third party).
Consequence: every remaining third-party asset is Apache 2.0 or MIT with a known copyright line, listed in the app's licence notice (`app/src/main/kotlin/cz/julienika/reportcam/about/Licences.kt`). Any new icon set is checked against its own licence file, not an aggregator's label.
Evidence: `app/src/main/res/drawable/ic_frame_bottom.xml`; G-006 progress log 2026-11-06.
