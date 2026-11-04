# D027 · The frame band has two columns, where on the right, and stacks when they do not fit
Date: 2026-11-04 · Goal: G-004 M3 · Status: active (superseded by: —)
Context: The Owner asked for the place name and coordinates on the right side of the frame stamp.
Decision: Fields that say where (`describesPlace`) go in a right column, right-aligned, place above coordinates; the rest in a left column; both top-aligned, the band as tall as the taller. The right column keeps its width up to half the room (less a 1.5 em gap), the left gets the rest; if a column that must wrap would be under 6 em or split a word, all lines stack left as before. Overlays keep one column.
Force: requirement — Owner, G-004: "make coordinates and location name shown on right side"; the widths and the stacking rule are judgment.
Rejected: fixed half-and-half columns (a short coordinate line wastes half the band); shrinking the text to fit (the size setting would stop meaning anything).
Consequence: A new where-field sets `describesPlace`; a placement gets the lines through `StampText`.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/stamp/StampLayoutTest.kt; app/src/androidTest/kotlin/cz/julienika/reportcam/stamp/StampRendererTest.kt
