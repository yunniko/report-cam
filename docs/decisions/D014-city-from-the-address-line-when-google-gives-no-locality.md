# D014 · With no locality, the city is the address-line part after the postcode
Date: 2026-11-03 · Goal: G-002 follow-up · Status: active (superseded by: —)
Context: Google returns no locality for Prague, so the city fell back to the region "Hlavní město Praha"; its address line carries "110 00 Praha 1-Staré Město".
Decision: City = locality, else the comma-separated address-line part containing the postcode with the postcode removed, else the sub-admin region.
Force: requirement — Owner instruction, 2026-11-03 ("get it from address").
Rejected: parsing the address line always (a locality, where Google gives one, is cleaner); `subLocality` (null in the measured answer).
Consequence: Depends on Google's line keeping the postcode and city in one part; when it does not, the region fallback applies and nothing breaks.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/location/PlaceLookupTest.kt; G-002 progress log 2026-11-03
