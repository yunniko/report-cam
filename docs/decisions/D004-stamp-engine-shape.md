# D004 · Stamp = fields over typed data, placed by a strategy, drawn by a dumb renderer
Date: 2026-11-03 · Goal: G-001 M2 · Status: active (superseded by: —)
Context: The Owner expects more stamp data later (a street address first) and both overlay and frame looks.
Decision: `StampData` holds typed facts (`StampKey`); each `StampField` turns them into lines; a `StampPlacement` (overlay in a corner, frame band) lays out wrapped lines; `StampRenderer` only draws the resulting geometry.
Force: requirement for extensibility (Owner, 2026-11-03); judgment for this particular split.
Rejected: one configurable renderer with a layout switch (each new look would edit the core); templated strings (no wrapping or per-field logic, such as the approximate and age marks).
Consequence: Layout and fields are plain Kotlin, unit-tested with a fixed-width measurer; only `StampRenderer.kt` touches Canvas. An absent `LOCATION` key means no permission, and no line is printed.
Evidence: `app/src/test/kotlin/cz/julienika/reportcam/stamp/StampLayoutTest.kt` (aNewFieldAndANewPlacementNeedNoEngineChange); `app/src/androidTest/kotlin/cz/julienika/reportcam/stamp/StampRendererTest.kt`.
