# D025 · A photo version is named by a hash of its look, and the note is part of the look
Date: 2026-11-04 · Goal: G-004 M1 · Status: active (superseded by: —)
Context: The review Note row redraws the photo, and the Owner asked that switching a setting away and back must not make a new version.
Decision: A version is `report_<capture time>_<8 hex of SHA-256 over placement, scheme, precision, stamp text>.jpg`; a redraw whose name already exists in the shared cache reuses that file, and saving is tracked per file name.
Force: requirement — Owner instruction in G-004: "changing setting here and back won't make it new version".
Rejected: the scheme suffix of D019 (the note has no short name); a counter suffix (going back makes a new file).
Consequence: Everything drawn into the photo must go into the hash. The message follows the note until it is edited by hand. Restoring an 0.3 entry keeps its saved flag for its own file.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/photo/VersionNameTest.kt; app/src/androidTest/kotlin/cz/julienika/reportcam/ReviewNoteTest.kt
