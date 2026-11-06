# Google Play listing texts

Character counts are Play's limits: title 30, short description 80, full description 4000. The counts
below are measured by `app/src/test/.../about/PlayListingTest.kt`, which fails when a text grows past its limit.
English is the default language; the Czech block is a draft for the Owner to approve or drop.

## English (en-US)

### Title (≤ 30)

Report Cam – stamped photos

### Short description (≤ 80)

Photos stamped with date, place and note, sent to a messenger in one tap.

### Full description (≤ 4000)

Report Cam is a camera for photo reports. Open it, take the photo, and it is already stamped with the date
and time, your location and the note you set. Send it to WhatsApp, Telegram or another messenger with one
tap, or through the usual Share sheet.

WHAT THE STAMP SHOWS
• Date and time, optionally with the day of the week
• Your coordinates with their accuracy, and the street address if you turn that on
• A note of your choice: the site, the job, the customer
• In the corner as an overlay, or in a band under or above the photo

SENDING
• Quick buttons for up to three messengers you pick (WhatsApp, Telegram, Messenger, Discord, Slack,
  Snapchat, Instagram); only installed ones are shown
• The stamp text goes along as the message, so it stays readable even where messengers strip photo metadata
• Edit the note and the message for one photo before sending
• Save a copy to the gallery, automatically when sent or by hand

LOCATION YOU CAN TRUST
• The stamp says how precise the fix is and marks an approximate or old one
• Optionally wait for a precise fix: the photo is taken at once and stamped when the fix arrives
• Street address through the phone's own geocoder, off by default

YOUR STYLE
• Dark or light stamp, with colours and transparency of your choice
• Photo shape (full frame, 1:1, 4:5, 2:3, 9:16), size and frame width
• Light, dark or system theme

PRIVATE BY DESIGN
• No internet permission: the app cannot send anything by itself
• No account, no analytics, no ads
• The camera runs only while the live preview is on screen, and nothing runs in the background
• The stamp data is also written into the photo's EXIF and XMP metadata

Open source: github.com/yunniko/report-cam

## Czech (cs-CZ), draft

### Title (≤ 30)

Report Cam – fotky s razítkem

### Short description (≤ 80)

Fotka s datem, místem a poznámkou, odeslaná do messengeru jedním klepnutím.

### Full description (≤ 4000)

Report Cam je fotoaparát pro fotoreporty. Otevřete ho, vyfoťte, a snímek už nese datum a čas, vaši polohu
a poznámku, kterou jste si nastavili. Pošlete ho jedním klepnutím do WhatsAppu, Telegramu nebo jiného
messengeru, anebo přes běžné sdílení.

CO JE NA RAZÍTKU
• Datum a čas, volitelně i den v týdnu
• Souřadnice s přesností a na přání i adresa
• Vaše poznámka: stavba, zakázka, zákazník
• V rohu jako překryv, nebo v pruhu pod či nad fotkou

ODESÍLÁNÍ
• Rychlá tlačítka až pro tři messengery podle vašeho výběru (WhatsApp, Telegram, Messenger, Discord, Slack,
  Snapchat, Instagram); zobrazí se jen nainstalované
• Text razítka jde i jako zpráva, takže zůstane čitelný i tam, kde messenger metadata z fotky odstraní
• Poznámku i zprávu lze pro jednu fotku před odesláním upravit
• Kopie do galerie, automaticky při odeslání nebo ručně

POLOHA, NA KTEROU JE SPOLEHNUTÍ
• Razítko říká, jak přesná poloha je, a označí přibližnou nebo starou
• Volitelně počká na přesnou polohu: fotka se pořídí hned a orazítkuje, jakmile přesná poloha dorazí
• Adresa přes geokodér telefonu, ve výchozím stavu vypnuto

VÁŠ STYL
• Tmavé nebo světlé razítko, barvy a průhlednost podle vás
• Tvar fotky (celý snímek, 1:1, 4:5, 2:3, 9:16), velikost a šířka rámu
• Světlý, tmavý nebo systémový vzhled

SOUKROMÍ OD ZÁKLADU
• Bez oprávnění k internetu: aplikace sama nic nikam neposílá
• Žádný účet, žádná analytika, žádné reklamy
• Fotoaparát běží jen při zobrazeném náhledu a na pozadí neběží nic
• Údaje z razítka se zapisují i do metadat fotky (EXIF a XMP)

Otevřený zdrojový kód: github.com/yunniko/report-cam
