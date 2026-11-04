# D023 · Quick buttons wear their messenger's colour; a single one is its icon alone
Date: 2026-11-04 · Goal: G-003 M5 · Status: active (superseded by: —)
Context: After the phone test the Owner asked for the quick buttons in their messengers' colours, and for a single button beside Share to show only its icon.
Decision: Each `Messenger` carries its main brand colour as the button fill; icon and name are white where white reaches 4.5:1 on it, otherwise black. The single button is an 80 dp icon button named for TalkBack.
Force: requirement — Owner, 2026-11-04 (colours, icon only); judgment for the foreground rule.
Rejected: white on every brand colour (WhatsApp, Telegram, Snapchat fall below 3:1); darkened brand shades to fit white (they stop looking like the brand).
Consequence: A new messenger needs a brand colour, and `MessengerTest` checks that its foreground is readable. The colours are each brand's commonly published primary, not checked against current brand guidelines.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/share/MessengerTest.kt; app/src/androidTest/kotlin/cz/julienika/reportcam/ReviewActionsTest.kt
