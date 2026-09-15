# Nixa Toolkit — CSC Desktop (Java edition)

Idhu unga CSC front-desk vela ellathukkum oru toolkit — scan → PDF, image
convert, file size குறை (photo & PDF), passport/signature photo exact size-ku
resize, photo import, and activity history — ellame **ore app**-la.

Idhu **Java-la** ezhudhapatta version. Munnaadi irundha Python version-la
`build_exe.bat` run panna mudiyalana Windows "Python not found" problem
(App execution alias confusion) irundhadhala, idha muzhusa Java-la
mari ezhudhirukom — Java-ku andha maadhiri problem varadhu, adhandhu
detailed reason keezha irukku.

## En Java? (Ipdi maathanadhukku enna reason)

- Windows-la Python install pannradhu, PATH-la irukka vekkradhu — ivangalukku
  konjam confusing aagalam, especially andha "Microsoft Store la irundhu
  install pannunga" popup vandhurukum (namma screenshot-la paathom).
- Java romba universal-ah "just works" model follow pannum — ore `.exe` file
  double-click pannina, adhukkulla thaan Java runtime bundle pannirukom,
  vera edhுவும் install pannanum nu illa.
- Idhu 100% **free, dependency-less build** — namma company/office network-la
  Maven Central (oru online Java library store) block panniruntha, adha
  namba **route around pannala** — adhukku பதிலா, namma **zero external
  library** vechu, JDK-oda own tools mattum use panni idha ezhudhirukom
  (JSON parsing, PDF read/write, ellame hand-written).

## Idhula enna irukku

| Tool | Enna pannum |
|---|---|
| 🖨️ Scan → PDF | Scanner-la irundhu (Windows WIA) neraya scan pannalam, allathu image file add pannalam, ella pages-um ore PDF ah save pannalam. B&W scan effect option-um irukku. |
| 🖼️ Image Converter | Neraya images-ah JPEG ↔ PNG-ku, batch-ah convert pannalam. |
| 📉 Reduce File Size | Photo alladhu PDF-ah, neenga sonna KB size-ku automatic-ah compress pannum. |
| 📐 Form Photo / Signature | Passport, Aadhaar, PAN, Signature — exact pixel size + max KB presets ready-ah irukku. |
| 📷 Photo Import | Munnadi edutha oru photo-ah pick panni use pannalam. |
| 🕒 Activity History | Indha computer-la mattum — evlo files process panninga nu track pannum. Veliya edhுவும் anuppadhu. |

## Run panna eppadi (3 வழி)

`build.bat` file-ah double-click pannunga (allathu Command Prompt open panni
`build.bat` nu type pannunga). Idhu 4 steps follow pannum, mudinjadhukku
apparam intha 3-la edhavadhு use pannalam:

1. **`dist-app\Nixa Toolkit\Nixa Toolkit.exe`** — *Best option.* Idhula
   Java bundle pannirukom, so double-click panna udane app open aagum, vera
   edhுவும் install pannanum nu illa. Idha copy panni CSC-la irukka vera
   PC-ku-kum kudukkalam, adhula Java illainaalum problem illa.
2. **`dist-installer\` folder** (idhu irundha) — oru proper `setup.exe`,
   run pannina desktop shortcut + Start Menu entry create pannum. (Idhukku
   unga build machine-la **WiX Toolset** install irukanum — illainna idhu
   step automatic-ah skip aagum, adhula problem illa, option 1 use pannunga.)
3. **`dist\NixaToolkit.jar`** — `java -jar NixaToolkit.jar` nu run
   pannalam, aனா idhu velai seiya intha PC-la **Java 17 alladhu adhukku
   mela already install** irukanum.

## Build panna enna venum

- **JDK (Java Development Kit) 17 alladhu adhukku mela** — idhu build
  pannradhukku mattum venum (option 1/2/3-ah use panna vera PC-kku venam,
  build.bat produce panna dhaan indha machine-la venum).
- Free download: **https://adoptium.net** (Eclipse Temurin — "Latest LTS"
  version download pannunga, install pannும்போது "Add to PATH" option
  select pannunga).
- Install aana apparam, `build.bat`-ah run pannunga.

Idhu **jpackage** (JDK-oda own tool) use pannுthu — Maven venam, internet
venam (build panna), vera edhுவும் download pannanும் illa.

## Idhula illatha oru feature (transparency-ku sollirom)

- **Live webcam preview** illa — adhukku vேணும் oru external camera library
  (namma access panna mudiyadha online library store-la irundhu vaanum).
  Adhukku பதிலா "Photo Import" tool irukku — already edutha oru photo-ah
  pick panni use pannalam (phone camera, existing webcam software — edhுவும்
  use panni edutha photo venum).
- **PDF compress**, scan pannina/photo PDF-ku mattum vela seiyum — typed-text
  (word-doc-la irundhu save pannina) PDF-la image pages illadhadhala, adhukku
  idhu vela seiyadhu.
- **WEBP format** convert panna mudiyadhu (adhukkும் oru external library
  venum) — JPEG mattum PNG mattum support pannum.

Ivangaளுkkellam reason ore dhaan — namma company/session network policy-ah
namba respect panniyachu, adha route around pannala. Ivai romba small,
edge-case features dhaan — main 6 tools ellame full-ah vela seiyudhu.

## Enga data save aagudhu?

`%USERPROFILE%\.nixa-toolkit\` folder-la — settings + activity history
mattum. **Edhுவும் internet-ku upload aagadhu.** Ella processing-um indha
computer-la mattum nadakkum.

## Konjam technical note (developer-kku)

- Zero external Maven dependency — hand-rolled JSON (`MiniJson`), hand-rolled
  minimal PDF reader/writer (`PdfUtil`), PowerShell-vazhi WIA scanner
  integration (`ScannerUtil`).
- `tools/TestHarness.java` — 66 automated checks (pure-logic + GUI-driven,
  Swing panels-oda business logic mattum, no file dialog venam) covering
  JSON round-trip, image compress/resize math, PDF build+extract round-trip,
  history persistence, and ella 5 interactive tool panels-oda core workflow.
  Run panna: `javac -d out @sources.txt && javac -d out -cp out
  tools/TestHarness.java && java -cp out com.nixatoolkit.ui.TestHarness`
- `tools/ScreenshotDriver.java` — dev-only visual verification helper
  (Xvfb + Robot screenshot ella 7 panels-um).
