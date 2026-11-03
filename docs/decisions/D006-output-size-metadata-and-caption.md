# D006 · Small output by default; stamp data in EXIF + ASCII-safe XMP and as the share caption
Date: 2026-11-03 · Goal: G-001 M3 · Status: active (superseded by: —)
Context: The Owner asked for a small default photo size, for all stamp data in the file's metadata, and, since messengers strip metadata, for the stamp text to go as the message instead, switchable.
Decision: Long edge 1280 px by default (1920, 2560, full in settings); EXIF holds time, offset, GPS and accuracy, XMP holds every field; XMP is written as ASCII with character references; the caption setting defaults on.
Force: requirement — Owner instructions, 2026-11-03; ExifInterface turns non-ASCII XMP into "?" (measured on API 34); 1280 is judgment.
Rejected: sending as a document to keep metadata (Owner chose the caption instead); raw UTF-8 XMP through ExifInterface (corrupts diacritics); a second EXIF library (one more dependency for one tag).
Consequence: A new stamp field reaches the XMP and the caption with no change to `PhotoMetadata`; the EXIF description stays a lossy ASCII summary.
Evidence: `app/src/test/kotlin/cz/julienika/reportcam/photo/PhotoMetadataTest.kt`; `app/src/androidTest/kotlin/cz/julienika/reportcam/photo/MetadataRoundTripTest.kt`.
