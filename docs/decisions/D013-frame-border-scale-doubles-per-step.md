# D013 · Frame border widths: thin 2.5 %, medium 5 %, thick 10 % of the short edge
Date: 2026-11-03 · Goal: G-002 follow-up · Status: active (superseded by: —)
Context: After the phone test the Owner asked for the old thick border (5 %) to become medium, with a thicker one as thick.
Decision: Thin 0.025, medium 0.05 (the default), thick 0.10; each step doubles, so the old medium became thin and the old 1 % step is gone.
Force: requirement — Owner instruction, 2026-11-03, for medium and thick; thin moving up to 2.5 % is a judgment keeping the steps even.
Rejected: keeping thin at 1 % (a jump to 5 % leaves a gap and 1 % was the thinnest of a scale judged too thin).
Consequence: Stored ids are unchanged, so existing settings show the wider border without migration.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/settings/AppSettingsTest.kt
