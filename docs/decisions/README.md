# Decisions — index

- D001 — Native Kotlin + CameraX + Views, written from scratch — active
- D002 — minSdk 26, compile/target SDK 36; runs on Android 17 — active
- D003 — The camera is open only while a live preview is visible — active
- D004 — Stamp = fields over typed data, placed by a strategy, drawn by a dumb renderer — active
- D005 — Location is tracked in a foreground duty cycle, never on capture — active
- D006 — Small output by default; stamp data in EXIF + ASCII-safe XMP and as the share caption — active
- D007 — Release key lives in the user profile, its password in git-ignored properties — active
- D008 — With "wait for precise location", the photo is taken at once and stamped when the fix settles — active
- D009 — Photo shape is short:long and follows the hold; the preview shows the whole frame — active
- D010 — A waiting photo is kept on disk, and losing location does not end its wait — active
