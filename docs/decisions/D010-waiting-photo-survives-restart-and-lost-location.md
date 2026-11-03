# D010 · A waiting photo is kept on disk, and losing location does not end its wait
Date: 2026-11-03 · Goal: G-002 M1 · Status: active (superseded by: —)
Context: A photo waiting for a precise fix (D008) lived only in memory, and switching location off or denying it stamped the photo at once without a location.
Decision: The camera's JPEG bytes and the capture metadata go to app-private files at the shutter, are restored in place of the live camera on the next launch, and are deleted once the photo is stamped, retaken or backed out of. Location switched off or denied keeps the photo waiting, with Retake and "Use current" offered.
Force: requirement — Owner instruction, 2026-11-03: the waiting photo "should not be lost on switching between locations or restarting the app".
Rejected: cache storage (the system may clear it); re-encoding the decoded bitmap (a second lossy pass); stamping without location when access is lost (the Owner's complaint).
Consequence: Only one photo waits at a time; a new waiting shot replaces the copy. Restoring stamps with the settings current at restore time, but the shape and size come from the shutter.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/review/PendingStoreTest.kt; app/src/androidTest/kotlin/cz/julienika/reportcam/DeferredLocationTest.kt
