# Google Play Console — answer sheet for Report Cam 1.0.0

Every form the Console asks for a first release, with the answer and the reason, so the Owner can fill them in
without re-deriving anything. The Console is the Owner's: nothing here is done by JulAI (VALUES.md → Restraint).
Written 2026-11-06 against Google's Play Console Help pages read that day (linked per section); the Console's
own wording wins where it differs, and a question not listed here is answered from the facts in §0.

## 0. The facts every answer comes from

| Fact | Value | Where it is checked |
|---|---|---|
| Package | `cz.julienika.reportcam` | `app/build.gradle.kts` |
| Version | 1.0.0, code 11 | same |
| SDK levels | minSdk 26, targetSdk 36 | same; Play's 2026 target requirement is met (D002) |
| Permissions | `CAMERA`, `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `WRITE_EXTERNAL_STORAGE` (Android 9 and older only) | `app/src/main/AndroidManifest.xml` |
| Not in the app | no `INTERNET`, no background location, no services, no ads, no analytics, no crash reporting, no accounts, no in-app purchases, no advertising ID, no SDK that talks to a server | manifest; `scripts/play-release-check.sh` |
| What leaves the phone | nothing by the app itself. With "Place name" on (off by default) the coordinates of the photo being stamped go to the phone's geocoder (Google on most phones) through Android's `Geocoder` API. A photo and its message leave only when the user taps a send button | `location/PlaceLookup`, `share/Sharing`; `docs/privacy-policy.md` |
| Signing | release bundle signed with the D007 key (RSA 3072, alias `report-cam`, `C:\Users\Hengenvaara\.android-keys\report-cam-release.jks`); 0.1.0 to 0.6.0 on the Owner's phone carry the same key | D007; `scripts/play-release-check.sh` |
| Bundle | `scripts/play-release-check.sh` → `app/build/outputs/bundle/release/app-release.aab`, 2.6 MB, four ABIs, 16 KB aligned | run before every upload |
| Privacy policy | `docs/privacy-policy.md`; page `docs/site/privacy-policy.html`; the app opens `https://yunniko.github.io/report-cam/site/privacy-policy.html` (`settings_strings.xml`) | §1 says how to put it there |
| Publisher, contact | "Julienika (julienika.cz)", `info@julienika.cz` (assumed; the Owner confirms) | the policy's first paragraph and last line |
| Assets | `docs/play/icon-512.png`, `feature-graphic.png`, `screenshots/01…07`, texts in `listing.md` | `PlayListingTest` |

## 1. Before opening the Console

1. **Developer account** (Owner's part 1): personal or organization, 25 USD once, identity check, and a verified
   Android device for a new personal account (Google's "device verification requirements for new developer
   accounts"). A personal account created after 13 November 2023 must run the closed test in §8 before production;
   an organization account (D-U-N-S number, legal name and address shown publicly) skips it.
2. **Privacy policy URL.** Play wants an https page, publicly reachable, no login, not a PDF. The app's link points
   at GitHub Pages of the public repo, so: repo → Settings → Pages → Source "Deploy from a branch", branch `main`,
   folder `/docs`, Save; a few minutes later
   `https://yunniko.github.io/report-cam/site/privacy-policy.html` answers. Hosting it on `julienika.cz` instead
   changes the one string `privacy_policy_url` in `settings_strings.xml` and needs a new build before upload.
   This is a publish step and the Owner's (Owner's part 3).
3. **Key backup.** Copy `C:\Users\Hengenvaara\.android-keys\` somewhere safe before uploading the key (D007).

## 2. Create the app

Console → All apps → Create app:
- App name: **Report Cam** (the listing title in §4 is longer; the app name is the short one). Default language
  **English (United States)**. **App** (not game). **Free** — Play never lets a free app become paid; this app
  has no plan to charge.
- Declarations: tick that the app follows the Developer Program Policies and US export laws. There is nothing
  encrypted in the app beyond the platform's own TLS, so the export answer is the standard one.

## 3. Play App Signing — keep the D007 key (Owner's part 4)

Why: a phone that installed a sideloaded 0.x build only updates from Play if Play's APKs carry the same signing
key. A Google-generated key would force every tester to uninstall first. A new app is enrolled by default in
Google-generated, quantum-ready hybrid signing, so the key is changed **before the first release is rolled out**
(Google: "Use Play App Signing", answer 9842756):

1. Console → the app → Test and release → Setup → App signing (the page is also reachable as "Play app signing"
   under Protected with Play). If a key is already shown, choose **Change key** → "Provide a copy of your app
   signing key by following the instructions" → **Export and upload a key from Java keystore**.
2. Download the PEPK tool (`pepk.jar`) and the encryption public key the page offers.
3. On this PC, with Android Studio's Java (`C:\Program Files\Android\Android Studio\jbr\bin\java.exe`, JDK 17):

   ```
   java -jar pepk.jar --keystore=C:\Users\Hengenvaara\.android-keys\report-cam-release.jks --alias=report-cam --output=C:\Users\Hengenvaara\.android-keys\report-cam-play-upload.zip --include-cert --rsa-aes-encryption --encryption-key-path=<the downloaded .pem>
   ```

   Copy the command from the Console page rather than from here: it fills in the flag that carries the
   encryption key, and older Console versions used `--encryptionkey=<hex>` instead of the `.pem` file. The
   store and key passwords are the ones in `keystore.properties` (beside the key, git-ignored); pepk asks for
   them. If pepk reports a missing crypto provider, run it with a JDK 11 or 17, not a newer one.
4. Upload the zip the tool wrote. Play then shows the app signing certificate; its SHA-256 must equal the one
   `jarsigner -verify -verbose -certs app-release.aab` prints for the bundle.
5. Upload key: the first bundle's signature becomes the upload key certificate, so the D007 key serves as both.
   Google recommends a separate upload key; it can be registered later and reset from the Console if lost
   ("Request upload key reset"), while the app signing key cannot be reset once Google holds it as our own.

Delete the zip after the upload; it holds the encrypted private key.

## 4. Store presence

**Main store listing** (Grow → Store presence → Main store listing), from `docs/play/listing.md`:
- App name (≤ 30): `Report Cam – stamped photos`; short description (≤ 80) and full description (≤ 4000): the
  English block. Czech: add the `cs-CZ` translation from the same file only if the Owner approves the draft.
- Graphics: `icon-512.png` (512 × 512), `feature-graphic.png` (1024 × 500), phone screenshots `screenshots/01`
  to `07` in that order (the first two show the stamped photo, which is what a visitor should see first). Tablet
  screenshots and a video are optional and not made.

**Store settings:** App category **Photography** (where timestamp and stamp cameras live; Business or
Productivity would also fit). Tags: pick up to five from the Console's list that say camera, photo or
business; the list changes, so none is prescribed here. Store listing contact details: e-mail
`info@julienika.cz` (shown publicly), website `https://julienika.cz`, phone optional.

**Countries / regions** (production and testing tracks): all countries. **Pricing:** free, no in-app products.

## 5. App content declarations (Policy → App content)

| Declaration | Answer | Reason |
|---|---|---|
| Privacy policy | the URL from §1 | required for every app |
| Ads | **No** | no ad SDK, no house ads |
| App access (sign-in details) | **All functionality is available without special access** / no instructions | no login, no gate; only runtime permissions, which the reviewer can grant |
| Content ratings | questionnaire in §7 | |
| Target audience and content | §8 | |
| News apps | **No** | |
| COVID-19 contact tracing and status apps | **No** | |
| Data safety | §6 | |
| Government apps | **No** (not developed for or on behalf of a government) | |
| Financial features | **No financial features** | |
| Health apps | **No** (none of the listed health features) | |
| Advertising ID | **No, the app does not use advertising ID** | targetSdk 33+ asks it; the manifest has no `AD_ID` permission and no library adds one |
| Photo and video permissions | not shown, or "the app does not request them" | it applies to `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` and `READ_EXTERNAL_STORAGE`; the app has none, only `WRITE_EXTERNAL_STORAGE` up to Android 9 |
| Foreground service / full-screen intent permissions | not shown | none requested |
| Account deletion | not applicable | no accounts |
| AI-generated content | **No** | |
| Permissions declaration form | not shown | appears only for high-risk permissions (SMS, call log, accessibility, all-packages); camera and location are not among them |

A declaration the Console shows that is not in this table gets the answer the facts in §0 give; if none fits, stop
and ask before guessing.

## 6. Data safety (answer 10787469, read 2026-11-06)

**Recommended answers:** "Does your app collect or share any of the required user data types?" → **No**. The
form ends there; the listing says "No data collected" and "No data shared". The privacy policy already says the
same and names the one exception.

Why this is honest, type by type:
- *Precise and approximate location:* read on the phone and written into the photo. Google's definitions:
  collection is "transmitting data from your app off a user's device"; "user data accessed by your app that is
  only processed locally on the user's device and not sent off device does not need to be disclosed." The app
  transmits nothing. The one transfer is the optional place lookup: when the user switches "Place name" on, the
  coordinates of the photo being stamped go to the phone's geocoder. That row's summary reads "Looked up online
  through Google for the photo being stamped; offline the stamp keeps coordinates only", so it is a transfer
  "based on a specific user-initiated action, where the user reasonably expects the data to be shared, or based
  on a prominent in-app disclosure and consent", which Google lists as not sharing. See D035.
- *Photos:* taken, stamped and kept on the phone. Handing one to WhatsApp or Telegram is the user tapping a
  send button: the same user-initiated exemption. A gallery copy is on-device.
- *Other user-generated content (the note and message):* on the phone; they leave only inside that same send.
- *Device IDs, app activity, crash logs:* nothing is read or sent; there is no analytics or crash SDK.

**If the Owner prefers the cautious declaration** (it changes the label, not the app): answer Yes; data type
Location → Precise location, **Shared** (not collected), processed ephemerally **Yes**, "Users can choose
whether this data is collected" (Place name is off by default), purpose **App functionality**. Then the
collection section asks about encryption in transit (**Yes**: Google Play services sends it over TLS) and a
deletion request mechanism (**No**: the publisher holds no data to delete, which the label states as such).
Pick one before the first submission; changing it later re-opens the review.

Security practices: no independent security review badge (optional, paid, not sought).

## 7. Content rating questionnaire (IARC, answer 9859655)

E-mail for the rating certificate: `info@julienika.cz`. Category: **Utility, Productivity, Communication, or
Other**. Every content question (violence, sexuality, language, controlled substances, gambling, hateful or
discriminatory content, crude humour, horror) is **No**: the app shows only the user's own photo. The
"miscellaneous" questions, as the questionnaire words them:
- Does the app allow users to interact or exchange content with other users? **No** — there is no user network
  or chat; the user sends a photo through another app, which is that app's feature.
- Does the app share the user's current location with other users? **No** — the location goes onto the user's
  own photo; no other user of Report Cam sees it.
- Digital purchases, ads, gambling, mature content from the web, unrestricted web access: **No**.
- Is the app a web browser or search engine: **No**.

Expected result: Everyone / PEGI 3 / USK 0 ("utility programs… tool apps" per USK) and the equivalents. The
questionnaire is redone only when a feature changes an answer (a chat feature would).

## 8. Target audience and content (answer 9867159)

- Target age group: **Ages 18 and over** only. The app is a work tool; the policy says it "is not directed at
  children under 13". Adding 16–17 would be defensible but adds nothing, and 13–15 "may be considered to include
  children in some locales" and pulls in the Families questions.
- "Your app is not designed for children." Do **not** enable Restrict Minor Access (it is offered with 18+ and
  not required for this content).
- Appeal to children (the store-listing question): **No**; the listing shows a building site and a town.

## 9. Testing track, the tester note, and the production application (answer 14151465)

Personal account: the closed test is required; organization account: skip to §10 (an internal-testing or
closed-testing round is still a good idea for the pre-launch report).

1. Test and release → Testing → Closed testing → create a track ("Alpha" by default), **Testers** tab: a list of
   tester Gmail addresses (CSV or typed) or a Google Group; a feedback channel (`info@julienika.cz`); countries.
2. Create a release: upload `app-release.aab` from a fresh `scripts/play-release-check.sh` run, release name
   `1.0.0 (11)`, release notes (§10's text), review, **Start rollout to Closed testing**. Google's review of the
   first upload takes hours to a few days; the opt-in link appears on the Testers tab once it is live.
3. Twelve testers must be opted in **continuously for the 14 days before the application** (Owner's part 5).
   One who opts out and back in restarts their own 14 days. Send each the opt-in link and this note:

   > Report Cam is a camera that stamps the date, place and a note on the photo and sends it to WhatsApp or
   > Telegram in one tap. Please open the link, press "Become a tester", install the app from Play, and keep it
   > installed and opted in for the next 14 days (Google counts the days). Take a few photos, try the Settings,
   > send one to a messenger, and write to info@julienika.cz with anything that looks wrong. You can leave the
   > test after the two weeks.

4. After the 14 days: Dashboard → **Apply for production**. The form, with the answers that are true today:
   - *About your closed test:* recruitment: the testers were colleagues and acquaintances the publisher asked;
     engagement: testers shot and sent photos and used the stamp settings (say what they really did); feedback:
     collected by e-mail and in person, summarize it honestly, including "none" if none came.
   - *About your app:* target audience: tradespeople, inspectors, property managers and field workers who report
     by photo over a messenger; value: the date, place and note are on the photo and in the message, in one tap,
     with no account and no internet access; first-year installs: pick the lowest band that is not a lie.
   - *Production readiness:* changes made from the test (list them, or "none were needed"); readiness: every
     path has unit and instrumented tests on an emulator, the release is checked by `scripts/play-release-check.sh`,
     and the Owner used each version on a phone.
   Google answers within about seven days. It may ask for more testing when fewer than 12 testers were opted in or
   engagement looked thin.

## 10. Production release

Test and release → Production → Create release: the same bundle (or a newer one if M5 changed anything; then
the version code rises to 12). Release notes (≤ 500 characters per language):

```
<en-US>
First release. Photos stamped with the date, time, place and your note, sent to WhatsApp, Telegram or any app in one tap. No account, no ads, no internet access.
</en-US>
<cs-CZ>
První vydání. Fotky s datem, časem, místem a vaší poznámkou, odeslané jedním klepnutím do WhatsAppu, Telegramu nebo jiné aplikace. Bez účtu, bez reklam, bez přístupu k internetu.
</cs-CZ>
```

Roll out to 100 %. Google's review of a first production release takes from hours to about a week; the Owner
gets an e-mail. Until it is live the listing is not searchable.

## 11. What Play may raise, and whose it is

- **Pre-launch report** (Test and release → Pre-launch report, generated for every uploaded bundle): crashes,
  accessibility warnings (touch targets, contrast), security findings. Each is a G-006 M5 item for JulAI; the Owner
  forwards the report or its screenshots.
- **Policy findings** during review (a wrong declaration, a listing asset problem): JulAI drafts the fix, the
  Owner resubmits.
- **Anything asking for a payment, an account or a credential** is the Owner's, always.

Unverified as of the writing date, to be read from the Console when the Owner gets there: the exact PEPK flags of
the current tool, the exact wording of the content-rating questions, and whether the hybrid-signing default shows
the key change in the place §3 names.
