# D030 · The review's stamp row is built from Settings ticks by one pure rule
Date: 2026-11-04 · Goal: G-005 M2 · Status: active (superseded by: —)
Context: the Dark/Light pair became a row of position and colour buttons, each one ticked separately in Settings.
Decision: `StampChoices` turns the ticks and the Settings defaults into the buttons shown, by the Owner's per-group rule (none or only the default → no group; one other → it plus the default); `StampButtons` draws them as 40 dp icons, or with names when two remain.
Force: requirement — the group rules and the named two-button form are the Owner's (2026-11-04); the sizes and the accent fill for the one in use are judgment.
Rejected: fixed layout ids per button (eight ids, and hiding rules spread over the layout); marking the Settings default instead of the photo's own look (it would contradict D029).
Consequence: a new placement or scheme needs an entry in `StampLooks` and `StampChoices.POSITION_ORDER`; a unit test fails until it has both.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/review/StampChoicesTest.kt; app/src/androidTest/kotlin/cz/julienika/reportcam/ReviewActionsTest.kt
