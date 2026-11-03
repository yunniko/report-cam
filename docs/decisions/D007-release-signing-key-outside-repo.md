# D007 · Release key lives in the user profile, its password in git-ignored properties
Date: 2026-11-03 · Goal: G-001 M5 · Status: active (superseded by: —)
Context: A sideloaded APK must be signed with one key forever, or the next version will not install over the old one; the key must never enter git.
Decision: A PKCS12 key (RSA 3072, alias `report-cam`, valid 10,000 days) at `C:\Users\Hengenvaara\.android-keys\report-cam-release.jks`; `keystore.properties` (git-ignored) points to it, with a copy of that file beside the key.
Force: requirement — charter: no secrets in commits; Android: updates need the same signing key. Location and key size are judgment.
Rejected: debug signing for the Owner's phone (debug keys differ per machine, so updates break); key inside the project folder (one `git add -A` from a leak); Play App Signing (needs a Play account, escalation-tier).
Consequence: Losing that folder means a new key and an uninstall on every phone: the Owner should back it up. Without `keystore.properties` the release build is unsigned rather than failing.
Evidence: `app/build.gradle.kts`; `apksigner verify` output in the G-001 progress log.
