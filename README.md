# Sudoku Cat 🐈‍⬛

Un'app Android nativa per il Sudoku, interamente in italiano, con un gattino nero di nome **Nero** che spiega le mosse, tre aringhe-biscotto come vite e un percorso di apprendimento ispirato a Duolingo.

Funziona **offline sul dispositivo**: partite, progressi, statistiche, lezioni e riconoscimento foto ML Kit restano nel telefono. Le uniche funzioni che possono usare la rete sono la spiegazione Gemini e l'OCR foto con Gemini, sempre opzionali: senza chiave, con l'OCR AI disattivato o senza connessione l'app continua a funzionare con il motore logico e ML Kit locali.

---

## APK pronti

| File | Uso | Firma |
|---|---|---|
| `dist/SudokuCat-1.1.1-release.apk` | Installazione normale | Firmato con chiave release locale (self-signed) |
| `dist/SudokuCat-1.1.1-debug.apk` | Sviluppo e debug | Firma debug Android |

- Versione **1.1.1** (versionCode 3): OCR Gemini multimodale, Structured Outputs, indizi a 3 livelli, «Risolvi Sudoku» e overlay AR sulla foto, con risposta locale immediata e raffinamento AI in background. La chiave Gemini integrata è attiva fin dal primo avvio; se la foto non contiene una griglia 9×9 l'app mostra un errore dedicato.
- `minSdk 26` (Android 8.0+), `targetSdk 36` (Android 16)
- Debug e release hanno application ID diversi (`com.sudokucat.app.debug` / `com.sudokucat.app`), quindi possono convivere con dati separati.
- La release è minificata con R8: se modifichi il codice, ricompilala prima di distribuirla.

Verifica della build 1.1.1:

- `apksigner`: firma valida, certificato `CN=Sudoku Cat`, SHA-256 certificato `bf14e380cc1bf214828a9d61400d1db8e5700740a1785321414e61cb52ffc4d4`.
- SHA-256 APK release: `7A9B7820C171531984D8A13DF0CECF575A3710B03252EC637D21095BEAF3A9CE`.
- SHA-256 APK debug: `4B1D45B7ABF716B1EFC56573DD0E9218871E3D2376BE5F55075F6FDD7E6C960B`.

Installazione da PC:

```powershell
adb install -r dist\SudokuCat-1.1.1-release.apk
```

## Consegna su GitHub senza la chiave

Non versionare questi file (già gestiti da `.gitignore`):

- `secrets.properties` e `.env` — contengono/possono contenere la chiave Gemini.
- `keystore.properties`, `keystore/` e tutti i `*.jks`/`*.keystore`/`*.p12` — firma release.
- `dist/*.apk`, `dist/archive/*.apk` e `*.aab` — gli APK attuali sono stati compilati **con** la chiave, quindi la contengono nei DEX.
- `local.properties`, `.gradle/`, `.kotlin/`, `app/build/`, `.idea/`, `*.log` — file locali di macchina/build.

Da includere: sorgenti, `gradle/`, `gradlew*`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `.gitignore`, `.env.example`, `secrets.properties.example`, `README.md`, `docs/`, `artifacts/` e `app/schemas/`.

Per una build pubblica sicura: copia `.env.example` in `.env` e lascia `GEMINI_API_KEY=` vuoto, poi ricompila. Se vuoi pubblicare un APK, usa solo quello ricompilato senza chiave.

## Cosa sa fare

### Gioco classico
- Generazione di griglie con **soluzione unica garantita** e quattro difficoltà (Facile 40 indizi, Medio 34, Difficile 29, Esperto 25).
- Timer con pausa reale (il tempo si ferma), salvataggio automatico e ripresa della partita attiva.
- Evidenziazione di riga, colonna, quadrante e dello stesso numero.
- Note a matita, cancellazione, annulla/ripristina.
- Controllo automatico degli errori opzionale: se attivo il numero sbagliato viene rifiutato ed evidenziato; se disattivo resta sulla griglia ma consuma comunque un'aringa.
- Tre aringhe-vita: ogni errore ne toglie una; alla terza la partita finisce.
- Schermata di vittoria con le aringhe rimaste che volano verso Nero e vengono mangiate.
- XP in base a difficoltà, errori e aiuti; la vittoria richiede che i valori coincidano con la soluzione.

### Gioca con AI
- Stessa griglia del gioco classico, ma con Nero accanto.
- Motore logico locale per otto tecniche: **Naked Single, Hidden Single, Naked Pair, Hidden Pair, Pointing Pair/Triple, Box-Line Reduction, X-Wing, Swordfish**.
- Spiegazione della prossima mossa e della cella selezionata.
- Se Gemini è configurato e attivo, la spiegazione viene arricchita dall'AI; altrimenti si usa la spiegazione locale, senza bloccare il gioco.
- Nella modalità foto le mosse AI passano da **Structured Outputs** e vengono validate dal risolutore locale prima di comparire sulla griglia.
- Cache locale delle risposte AI (7 giorni), limite giornaliero e throttling anti-abuso.

### Risolvi da foto
- Foto dalla **fotocamera** (CameraX) o dalla **galleria**.
- Editor di ritaglio con angoli trascinabili.
- **OCR multimodale con Gemini**: la foto ritagliata viene inviata con `inline_data` e la risposta è vincolata da un **JSON Schema** (`grid`: matrice 9×9 di interi). È il percorso principale quando AI e OCR Gemini sono attivi.
- **Fallback offline ML Kit**: Gemini e ML Kit partono in parallelo, così il riconoscimento locale è pronto anche se la rete è lenta. Se Gemini non è configurato, non c'è rete o la risposta non rispetta lo schema, si usa il risultato locale. La sorgente usata è mostrata con un chip nella schermata.
- **Errore «nessuna griglia»**: se non viene letto alcun numero, la schermata mostra un messaggio esplicito che invita a inquadrare la griglia 9×9 o a inserire i numeri a mano; con meno di 17 numeri compare un avviso di ritaglio poco nitido.
- Griglia riconosciuta modificabile a mano, con controllo di coerenza, conflitti, soluzione unica/multipla/assente.
- **Suggerisci mossa** con Structured Output (`row`, `col`, `valore`, `spiegazione`): il passo del motore logico appare subito e Gemini lo rifinisce in background; la mossa proposta viene sempre verificata con il risolutore locale e non blocca mai la UI in attesa della rete.
- **Indizi progressivi a 3 livelli**: 1) evidenzia il quadrante, 2) indica la cella, 3) rivela il numero con la spiegazione.
- **Risolvi Sudoku**: completa la griglia con il risolutore locale verificato, evidenzia in verde acqua le celle inserite dall'AI e consente di annullare.
- **Overlay AR**: disegna i numeri mancanti direttamente sopra la foto della griglia cartacea, senza modificare l'immagine.
- Al completamento la sessione viene registrata nelle statistiche e assegna 60 XP una sola volta.

### Impara Sudoku
- Percorso a serpentina in stile Duolingo con **5 livelli e 15 lezioni**.
- Contenuti: regole, scansione, candidati, Naked/Hidden Single, gestione degli errori, Naked/Hidden Pair, ordine delle tecniche, Pointing, Box-Line, X-Wing, Swordfish e sfide finali.
- Ogni lezione ha spiegazioni, esempi visivi, quiz e esercizi «trova la cella».
- Sblocco sequenziale, valutazione a 3 stelle, XP alla prima completion, reazioni di Nero.

### Nero, il gatto
- Avatar nero 3D cartoon disegnato con Compose Canvas, con espressioni e animazioni.
- Pannello di Nero sempre ancorato in partita: l'avatar resta visibile per tutta la durata del gioco, anche mentre si scorre la griglia o mentre parla.
- La bolla si chiude solo dopo la fine della voce e dopo il tempo necessario a leggere il testo; chiuderla manualmente ferma anche l'audio.
- Reazioni contestuali a errori, vittorie, pause e suggerimenti, con rimbalzo dell'avatar e pesci che si inclinano quando vengono persi.
- Fumetto toccabile, dialoghi con l'utente e sintesi vocale italiana (TTS di sistema) con voce stabile.
- Numero appena inserito con animazione a molla, celle sbagliate che tremano, sfondi e suggerimenti che pulsano, barra di completamento e tasti del pad animati.
- Effetti sonori generati e inclusi nell'app.

### Statistiche, progressi e impostazioni
- Partite giocate/vinte/perse, tempi, difficoltà, serie di giorni, XP e livello.
- Tema chiaro/scuro/sistema.
- Difficoltà predefinita, timer, controllo errori, rimozione automatica delle note, vibrazione.
- Volume effetti, voce, velocità della voce, gatto on/off.
- Chiave Gemini cifrata, modello configurabile, test di connessione, rimozione chiave, svuotamento cache.
- Interruttore separato **OCR foto con Gemini**: se disattivato, la foto resta sempre sul dispositivo e si usa solo ML Kit.
- Ripristino completo dei progressi con conferma.
- Privacy: nessun account, nessun server proprietario, nessun tracciamento.

## Stack tecnico

- **Kotlin 2.2.21**, **Jetpack Compose** + **Material 3**, MVVM, Navigation Compose
- **Room 2.7.1** per partite, progressi lezioni e cache AI
- **DataStore Preferences 1.1.4** per impostazioni e profilo
- **Android Keystore** (AES-256-GCM) per la chiave Gemini
- **CameraX 1.4.2** + **ML Kit Text Recognition 16.0.1** (fallback OCR offline)
- **Gemini API** con input multimodale (`inline_data`) e **Structured Outputs** (`responseSchema` + `responseMimeType`)
- **OkHttp 4.12.0** per le chiamate Gemini
- **Coroutines/Flow**, **kotlinx.serialization**
- AGP 8.13.2, Gradle 8.13, JDK 21 (il progetto compila con target Java 17)

## Struttura del progetto

```
Sudoku/
├─ app/src/main/java/com/sudokucat/app/
│  ├─ ai/                 # GeminiClient, StructuredOutputs, CoachService, fallback locali
│  ├─ data/
│  │  ├─ local/           # Room: entità, DAO, database
│  │  ├─ prefs/           # DataStore, profilo, chiave cifrata
│  │  └─ repo/            # GameRepository, StatsRepository, LessonRepository
│  ├─ domain/
│  │  ├─ engine/          # SudokuEngine, LogicalEngine
│  │  ├─ lessons/         # LessonCatalog (15 lezioni)
│  │  └─ model/           # Difficulty, GameMode, GameStatus
│  ├─ ui/
│  │  ├─ ai/ photo/ game/ home/ play/ learn/ stats/ settings/
│  │  ├─ components/      # board, number pad, gatto, aringhe, card
│  │  ├─ navigation/      # drawer e grafo di navigazione
│  │  └─ theme/           # colori, tipografia, tema
│  ├─ util/               # CatController, TTS, suoni
│  ├─ AppContainer.kt     # dependency graph manuale
│  ├─ MainActivity.kt
│  └─ SudokuCatApp.kt
├─ app/src/test/          # test unitari del motore e degli Structured Outputs
├─ docs/                  # guida Gemini, funzioni, limiti
├─ keystore/              # chiave di firma locale (non distribuire)
├─ keystore.properties    # credenziali di firma locali (non distribuire)
└─ dist/                  # APK pronti
```

## Compilare da sorgente

Prerequisiti:

- JDK 21 (o 17+) impostato in `JAVA_HOME`
- Android SDK con platform `android-36` e build-tools
- Connessione a Internet al primo build per scaricare le dipendenze Gradle

### Chiave Gemini preimpostata (opzionale)

Per una build con la tua chiave già configurata, crea nella radice del progetto il file `secrets.properties` (escluso dal versionamento):

```properties
GEMINI_API_KEY=la_tua_chiave
```

In alternativa copia `.env.example` in `.env` (anch'esso escluso), imposta `GEMINI_API_KEY=...`, usa la variabile d'ambiente `GEMINI_API_KEY` o passa `-PGEMINI_API_KEY=...` a Gradle. L'ordine di lettura è `secrets.properties` → variabile d'ambiente → `.env` → `-P`. Il valore viene inserito in `BuildConfig` e, al primo avvio, copiato nel DataStore cifrato. Se la chiave è vuota, la build resta valida e l'app chiede il valore dalle Impostazioni: **per la consegna su GitHub lascia tutto vuoto**.

```powershell
# Debug
.\gradlew.bat :app:assembleDebug

# Release firmata (usa keystore.properties)
.\gradlew.bat :app:assembleRelease

# Test unitari
.\gradlew.bat :app:testDebugUnitTest
```

Gli APK vengono creati in:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

### Firma release

Il progetto include una chiave locale di esempio in `keystore/sudoku-cat-release.jks` e le credenziali in `keystore.properties`. Per una distribuzione reale:

1. Genera una tua chiave con `keytool`.
2. Aggiorna `keystore.properties`.
3. Conserva keystore e password in un luogo sicuro: senza di essi non potrai aggiornare l'app installata.
4. Non pubblicare né il keystore né `keystore.properties`.

Se `keystore.properties` non esiste, la release viene prodotta non firmata (`app-release-unsigned.apk`) e il build continua.

## Gemini (opzionale)

Guida completa: [`docs/GEMINI_SETUP.md`](docs/GEMINI_SETUP.md).

In sintesi:

1. Crea una chiave API da [Google AI Studio](https://aistudio.google.com/apikey).
2. Inseriscila in `secrets.properties` o in `.env` **prima della build** per averla già preimpostata, oppure direttamente nell'app: **Impostazioni → Intelligenza artificiale**. Per il repository pubblico non inserirla da nessuna parte.
3. Nell'app puoi sempre premere **Testa connessione** per verificare chiave e modello.
4. Il modello predefinito è `gemini-3.8-flash`; puoi cambiarlo dal campo dedicato.
5. L'OCR foto con Gemini è attivo per impostazione predefinita e si può disattivare separatamente. In quel caso la foto non lascia il telefono.

La chiave preimpostata viene letta a build time da `secrets.properties`/`.env`/ambiente e salvata in `BuildConfig`; viene usata **subito** come fallback in memoria, senza attendere la copia cifrata, e al primo avvio viene cifrata con Android Keystore nell'archivio separato `sudoku_cat_secret`, escluso dai backup. La semina automatica non sovrascrive una chiave già presente. Viene usata solo nell'header `x-goog-api-key` verso `generativelanguage.googleapis.com`. Un valore compilato in un APK distribuito è estraibile da chi possiede l'APK: per un uso condiviso o commerciale serve un backend che custodisca la chiave.

## Privacy e sicurezza

- Nessun account, nessun backend proprietario, nessuna pubblicità, nessun analytics.
- Con **OCR foto con Gemini** attivo, la sola foto ritagliata viene inviata a Google per estrarre la griglia; nessun'altra immagine viene caricata e la foto non viene salvata sul server dell'app.
- Se l'OCR Gemini è disattivato, senza chiave o senza rete, foto e riconoscimento restano sul dispositivo con ML Kit.
- Le altre richieste AI contengono solo stato della griglia e contesto di gioco, mai la foto.
- La chiave API è cifrata a riposo e non è inclusa nei backup cloud/trasferimento dispositivo; quella preimpostata è comunque compilata nell'APK e va trattata come un segreto del file. Per questo `secrets.properties`, `.env` e gli APK in `dist/` sono esclusi dal versionamento.
- Cache AI locale con scadenza a 7 giorni e limite di 40 richieste al giorno.
- «Ripristina progressi» cancella partite, statistiche, XP, serie e lezioni; tema, chiave e impostazioni AI restano.

## Test e verifica

- Test unitari del motore: generazione, unicità della soluzione, risoluzione, conflitti, candidati.
- Test unitari degli Structured Outputs: schema, parsing di griglie e mosse, casi malformati e integrazione con il risolutore.
- Build `assembleDebug` e `assembleRelease` verificate.
- Verifica runtime su emulatore Android per le funzioni di base; le aggiunte 1.1.1 sono verificate con compilazione, test unitari e build firmata.
- APK release verificato con `apksigner` (certificato `CN=Sudoku Cat`).

## Limiti noti

Elenco completo in [`docs/LIMITATIONS.md`](docs/LIMITATIONS.md). In breve:

- La qualità dell'OCR dipende da luce, prospettiva e ritaglio; la correzione manuale è parte del flusso.
- L'OCR Gemini richiede rete e chiave; se manca, si usa ML Kit sul dispositivo.
- L'AI richiede rete, chiave valida e un modello disponibile per la propria chiave; in caso contrario si usa il fallback locale.
- La vibrazione dipende dal dispositivo e dalle impostazioni di sistema.
- La voce italiana richiede un motore TTS italiano installato.
- L'interfaccia è ottimizzata per smartphone e si adatta a tablet e rotazione, ma non è stata progettata specificamente per schermi grandi.
- Non c'è sincronizzazione cloud né multi-dispositivo.
- La release inclusa è firmata con una chiave locale, non con una chiave da Play Store.

## Crediti

Design, codice e illustrazioni Compose realizzati per questo progetto. Nero, le aringhe e la palette charcoal/lavanda sono originali dell'app.
