# Google Play store assets

Everything the listing needs, at Play's sizes, with the script that makes it. `PlayListingTest` (unit tests)
checks the text limits and the image sizes.

| File | Play requirement | How it is made |
|---|---|---|
| `icon-512.png` | App icon, 512 × 512 PNG | `scripts/play-graphics.ps1` from the launcher artwork (original, D034) |
| `feature-graphic.png` | Feature graphic, 1024 × 500 | the same script: artwork, name and tagline |
| `screenshots/01-review-light.png` … `07-settings-dark.png` | Phone screenshots, 2 to 8, 16:9 to 9:16; these are 1080 × 2220 | `scripts/play-screenshots.sh` on the Pixel 3a API 34 emulator |
| `listing.md` | Title ≤ 30, short description ≤ 80, full description ≤ 4000 characters | written by hand; English, Czech draft |
| `console-answers.md` | every Console form of a first release: account, key upload, listing, declarations, Data safety (D035), content rating, target audience, closed test, production | written against Google's help pages, read 2026-11-06 |

The photo in the screenshots is the Owner's own (Český Krumlov, the same file as the app's colour-dialog
preview), fed through the real stamping pipeline with the emulator's location set to Český Krumlov, so the
stamp and the message are what the app really produces. The live camera screen is not among them: the
emulator's camera is a test pattern, so that screenshot, if wanted, is taken on a phone.

Not made here, by Play's rules or by choice: a 7-inch and 10-inch tablet screenshot (optional, the app is
phone-first), a promo video (optional), translated screenshots (the English ones serve every language).
