# D035 · Data safety declares no collection and no sharing; the place lookup is the user's opt-in
Date: 2026-11-06 · Goal: G-006 M4 · Status: active (superseded by: —)
Context: Play's Data safety form counts an on-device transfer to a third party as sharing. The app's only transfer is the optional place lookup: with "Place name" on (off by default) the photo's coordinates go to the phone's geocoder, Google on most phones. All else stays on the phone or leaves by the user's send tap.
Decision: answer "No" to "Does your app collect or share any of the required user data types?"; the geocoder stays named in the privacy policy and the Place name row.
Force: judgment — Google exempts a transfer "based on a specific user-initiated action … or based on a prominent in-app disclosure and consent" (answer 10787469, read 2026-11-06); the opt-in row says "Looked up online through Google". Declaring it as shared is also honest; the answer sheet gives the Owner that alternative.
Rejected: Precise location as collected (nothing reaches the publisher); dropping the geocoder (asked for in G-002).
Consequence: anything sent without a tap or an opt-in reopens this answer.
Evidence: `docs/play/console-answers.md` §6; `app/src/main/res/values/settings_strings.xml`; `docs/privacy-policy.md`.
