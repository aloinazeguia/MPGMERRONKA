# Kamara — Errezeten aplikazioa

Android aplikazioa (Kotlin + Jetpack Compose) errezetak sortzeko: **kamerarekin errezetaren testuaren argazkia atera, OCR bidez testua irakurri eta Firestore-en gorde**.

> Argazkia telefonoan bertan gordetzen da (galerian), baina **ez da Firebase Storage-ra igotzen** (Storage ez delako dohainik). Firestore-en errezetaren datuak soilik gordetzen dira (testua edo izena/osagaiak/pausoak), ez argazkia.

---

## Ezaugarriak

- 📷 **Kamera**: obturadorea, miniatura, **zoom botoiak (+ / −)** eta atzera botoia. Ez dago mezurik ezta kamera alderantzikatzeko botoirik.
- ✂️ **Moztu**: argazkia atera ondoren eta OCRra pasatu aurretik, irudia tamaina osoan ikusten da (ez txikituta) eta mozketa-laukia arrastatu daiteke. **Crop ikonoak** argazkia pantalla osoan irekitzen du; han laukia arrastatu edo ertzak erabiliz testua soilik geratzen den eremua hautatzen da. Pantalla osoko moduan **X** ikonoak mozketa ezabatzen du (argazki osoa hautatzen du) eta **OK** ikonoak mozketa onartzen du. Pantaila arruntean **OK** ikonoak argazkia aukeratu eta OCRra pasatzen du.
- 🔍 **OCR**: ML Kit Text Recognition-ek argazkitik (edo moztutako eremutik) testua irakurtzen du (lokalean, dohainik).
- 📝 **Bi modu**:
  - **Kameratik**: eskaneatutako **testu osoa** eremu bakarrean erakusten da (osagaiak, pausoak, dena), editatu daiteke eta horrela gordetzen da — ez da ezer automatikoki bereizten.
  - **Errezetak → `+`**: formulario **klasikoa** mantentzen da: `Izena`, `Osagaiak` eta `Sukaldatzeko pausoak` eremu bereiziekin.
- 📋 **Zerrenda trinkoa**: errezetak txartel txikitan ikusten dira (izenburua + bi lerroko laburpena). **Txartelaren gainean klik egitean** eduki osoa zabaltzen da; bigarren klik batekin berriro txikitzen da.
- ✏️ **Editatu**: `Errezetak` zerrendako arkatz-botoiak edozein errezeta irekitzen du bere datuekin, eta gordetzean Firestore-eko dokumentu bera eguneratzen da.
- ☁️ **Firebase Firestore**: errezetak gorde, **eguneratu**, irakurri eta ezabatu (datu-base ez-erlazionala).
- 🔙 **Nabigazioa**: atzera botoiak kamera, moztu, errezetak eta formulario pantailan.
- 💾 **Ez dago Storage-erabilerarik**: argazkiak ez dira hodeira igotzen.

---

## Teknologiak

| Teknologia | Erabilera |
|---|---|
| Kotlin | Hizkuntza nagusia |
| Jetpack Compose | UI deklarativoa |
| CameraX | Kamera eta argazkia |
| ML Kit Text Recognition | OCR (testu-ezagutza) |
| Firebase Firestore | Datu-base ez-erlazionala |
| Coil | Irudiak erakusteko |
| Accompanist Permissions | Baimenen kudeaketa |
| Gradle (version catalog) | Dependentziak |

---

## Instalazioa

### 1. Firebase konfiguratu

1. [Firebase Console](https://console.firebase.google.com) → proiektua sortu.
2. **Add app → Android** → paketea: `com.example.kamara`.
3. `google-services.json` deskargatu eta kopiatu: `app/google-services.json`.
   - ⚠️ JSON-eko `package_name` **minuskulaz** egon behar da: `com.example.kamara`.
4. **Build → Firestore Database → Create database** → *Start in test mode*.
   - Storage-a **ez** dago behar (ez dugu erabiltzen).

### 2. Proiektua ireki

1. Android Studio → **File → Open** → `Kamara` karpeta.
2. **File → Sync Project with Gradle Files** (elefantea ⬇️).
3. Exekutatu ▶ (telefono fisikoa edo emuladorea).

### 3. Komando-lerrotik (aukerakoa)

```bash
sh gradlew assembleDebug
```

APK-a: `app/build/outputs/apk/debug/app-debug.apk`

---

## Erabilera

### A. Errezeta eskuz gehitu (modu klasikoa)

1. **Errezetak** sakatu → `+` botoia.
2. Betegu **Izena**, **Osagaiak** (lerro bakoitzeko bat) eta **Sukaldatzeko pausoak** (lerro bakoitzeko bat).
3. **Gorde Firebasen** → eremu bereiziekin gordetzen da.

### B. Errezeta kameratik gehitu (OCR)

1. **Kamera** sakatu → **`+` / `−` botoiekin zooma egokitu** (pantailaren eskuinaldean, uneko maila `1.0x` moduan ikusten da).
2. Obturadorearekin argazkia egin errezetaren testuari.
3. **Moztu** pantailan:
   - **✂️ crop ikonoa** → argazkia pantalla osoan ireki; laukia arrastatu mugitzeko, ertzak tamaina aldatzeko. Han **✕** ikonoak mozketa ezabatzen du (argazki osoa erabiltzeko) eta **✓** ikonoak mozketa onartzen du.
   - **✓ OK ikonoa** → argazkia aukeratu eta eremu horri OCR aplikatu.
   - Goiko **atzera ikonoa** → kamerara itzuli, argazkia berriz egiteko.
4. OCR-ek testua irakurri → formularioa testu osoarekin irekitzen da.
5. Berrikusi/zuendu → **Gorde Firebasen**.
6. **Errezetak** atalean txartel trinkoak ikusten dira: sakatu txartela testu osoa (edo osagaiak eta pausoak) zabaltzeko, eta berriro sakatu txikitzeko. Arkatz- eta ezabatzeko botoiak txartelaren barruan daude.

### C. Errezeta editatu

1. **Errezetak** atalean, sakatu errezetaren **arkatz-botoia** (🖊).
2. Formularioa datuekin irekitzen da:
   - Errezeta klasikoa bada: `Izena`, `Osagaiak`, `Pausoak`.
   - Kameratik sortutakoa bada: `Testua`.
3. Aldaketak egin → **Gorde aldaketak**. Firestore-eko dokumentu bera eguneratzen da (ez da berria sortzen).

### OCR emaitza hobetzeko

Testua ondo ikusten dela ziurtatu: **erabili zoom botoiak** testua handitzeko, fokatu eta argiztapen ona erabili. Ez da beharrezkoa etiketarik (`Izena:`, `Osagaiak:`...) jartzea — irakurritako lerroak bere horretan gordetzen dira.

Adibidez, hau argazkiari ateratzen badiozu:

```
Lentejak
Dilistak
txorizoa
Patatak
30´ sukaldatu su xuabean
```

horrela ikusiko duzu formularioan, lerroz lerro, eta horrela gordeko da Firestore-en.

---

## Proiektuaren egitura

```
Kamara/
├── app/
│   ├── google-services.json          # Firebase konfigurazioa
│   └── src/main/
│       ├── AndroidManifest.xml       # Baimenak (CAMERA + INTERNET)
│       ├── java/com/example/kamara/
│       │   ├── MainActivity.kt           # Nabigazioa + kamera + zoom + OCR deia
│       │   ├── MoztuPantaila.kt          # Irudia moztu OCRra pasatu aurretik
│       │   ├── TestuIrakurlea.kt         # OCR (Uri edo Bitmap -> testua)
│       │   ├── ErrezetaFormularioa.kt    # Formulario klasikoa + OCR + edizioa
│       │   ├── ErrezetakPantaila.kt      # Zerrenda: gehitu, editatu, ezabatu
│       │   ├── Errezeta.kt               # Datu-eredua (bi moduetako eremuak)
│       │   ├── ErrezetaRepository.kt     # Firestore: gorde/eguneratu/irakurri/ezabatu
│       │   ├── ErrezetaViewModel.kt      # UI ↔ Repository zubia
│       │   └── ui/theme/                 # Gaia (koloreak, tipografia)
│       └── res/                          # Layout, drawable, balioak
├── gradle/libs.versions.toml         # Dependentzien katalogoa
├── build.gradle.kts                  # Root Gradle
├── ORC.md                            # OCR sistemaren gida teknikoa
└── README.md                         # Fitxategi hau
```

---

## Datuak gordetzeko modua

Firestore-en `errezetak` bilduman dokumentu bat sortzen da errezetako bakoitzerako. Bi motako dokumentuak elkarrekin bizi dira:

```
errezetak/
  {autoId-1}/                    # Errezetak → + (modu klasikoa)
    izena: "Patata tortilla"
    osagaiak: ["5 patata", "3 arrautza", "tipula"]
    pausoak: ["Patatak moztu", "Olioarekin frijitu"]
    sortuta: 1731000000000

  {autoId-2}/                    # Kamera (OCR)
    testua: "Lentejak\nDilistak\ntxorizoa\nPatatak\n30´ sukaldatu su xuabean"
    sortuta: 1731000001000
```

Editatzean dokumentuaren `id` bera mantentzen da (ez da dokumentu berririk sortzen), eta `sortuta` ere bere horretan uzten da zerrendako ordena ez aldatzeko.

---

## Arazo ohikoak

| Arazoa | Konponbidea |
|---|---|
| Run botoia gris | Egin **Sync Project with Gradle Files** |
| `google-services.json` errorea | Egiaztatu `package_name`: `com.example.kamara` |
| Argazkian ez da testurik aurkitu | Erabili zoom `+` botoia, hurbildu eta argiztapen ona erabili; probatu mozteko laukia testura doitzen |
| Moztu pantaila ez da irekitzen | Argazkia berriz egin; goiko atzera ikonoak kamerara itzultzen du |
| Editatzean aldaketak ez dira gordetzen | Firestore arauak egiaztatu (`set` baimena behar da dokumentu existentean) |
| Zoom botoiak ez dira agertzen / ez dute erantzuten | Kamera berriro ireki (kamera aldatzean zoom berriro lotzen da) |
| Gordetzean errorea | Firestore arauak egiaztatu Firebase consolan |
| Kamera ez da irekitzen | Baimenak ezarri → Aplikazioak → Kamera |

Xehetasun gehiago: **[ORC.md](ORC.md)**

---

## Garapena

Proiektua ikasleentzako ariketa gisa garatu da: CameraX → QR → OCR → Firestore bilakaera jarraitu da. Aldaketen historia teknikoa `ORC.md`-n dago.
