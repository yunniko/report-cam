# D028 · Action and position buttons carry iconify icons; corner chips show only theirs
Date: 2026-11-04 · Goal: G-004 M4 · Status: active (superseded by: —)
Context: The Owner asked for icons from given sets: OpenSearch UI (save, retake, share, corner positions), css.gg polaroid for the frames (turned 180 degrees for the frame above) and Material Symbols dark/light for the stamp schemes.
Decision: The review buttons keep their labels with the icon before them; the frame chips keep theirs too; the four corner chips show the icon alone, centred, with the corner name as content description.
Force: requirement for the icons and their sets (Owner, G-004); judgment for icon-only corners, since the 82 dp chips barely fit "Bottom right" and the icon is the clearer sign.
Rejected: labels on corner chips beside the icon (does not fit); icon-only everywhere (Retake and Save are not obvious enough without words).
Consequence: Icons are tinted to the button text colour, so they follow the theme and the scheme; each drawable names its source and license, listed in README.
Evidence: app/src/main/kotlin/cz/julienika/reportcam/settings/ui/CornerPreference.kt; app/src/main/res/layout/activity_camera.xml
