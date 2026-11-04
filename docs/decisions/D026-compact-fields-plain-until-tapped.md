# D026 · Compact text fields are plain text until tapped, and closing the keyboard ends the edit
Date: 2026-11-04 · Goal: G-004 M2 · Status: active (superseded by: —)
Context: The Owner asked for a setting that shows the review's Note and Message as non-editable text until pressed, to reduce visual load.
Decision: With "Compact text fields" on, each field drops its underline, cursor, focusability and spelling marks; a tap makes it editable with the keyboard, and Done, Back, closing the keyboard or tapping beside the fields returns it to plain text. Off by default.
Force: judgment — the mode is the Owner's request; ending the edit on keyboard close (rather than only on Done or focus loss) is ours, so the field never stays half-editable.
Rejected: a separate TextView swapped with the EditText (two views to keep in step, layout jumps); keeping spell-check marks while plain (red underlines are the visual noise the mode removes).
Consequence: Any new review text field joins `ReviewTexts.fields` so it follows the mode.
Evidence: app/src/androidTest/kotlin/cz/julienika/reportcam/ReviewCompactTest.kt; app/src/test/kotlin/cz/julienika/reportcam/settings/AppSettingsTest.kt
