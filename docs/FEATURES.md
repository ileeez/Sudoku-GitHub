# Funzioni implementate

Questo documento elenca le funzioni richieste e dove sono realizzate. Tutto ciò che è descritto è collegato a logica reale: nessuna schermata è solo decorativa.

## 1. Piattaforma e architettura

| Requisito | Implementazione |
|---|---|
| App Android nativa | Modulo unico `app`, package `com.sudokucat.app` |
| Kotlin + Jetpack Compose + Material 3 | UI interamente Compose, tema Material 3 personalizzato |
| MVVM | `GameViewModel`, `PhotoViewModel`; stato esposto con `StateFlow` |
| Navigation Compose | `ui/navigation/SudokuCatNavigation.kt` con drawer e 9 route |
| Room | `data/local/AppDatabase.kt`, entità `GameEntity`, `LessonProgressEntity`, `AiCacheEntity` |
| DataStore | `data/prefs/SettingsRepository.kt`, `ProfileRepository` |
| Coroutines/Flow | Timer, persistenza, analisi griglia, chiamate AI |
| CameraX | `ui/photo/CameraCaptureScreen.kt` |
| ML Kit OCR | `ui/photo/PhotoViewModel.kt` (`TextRecognition`, latino) — fallback offline |
| Gemini multimodale | `ai/GeminiClient.kt` (`generateJson` con `inline_data` base64) |
| Structured Outputs | `ai/StructuredOutputs.kt` (`gridSchema`, `moveSchema`, parser) |
| OkHttp | `ai/GeminiClient.kt` |
| Build APK | `assembleDebug` e `assembleRelease` verificati |
| Offline | Giochi, lezioni, statistiche, OCR ML Kit e motore logico funzionano senza rete |

## 2. Navigazione e layout

- Drawer laterale con: **Home, Gioca, Gioca con AI, Risolvi da foto, Impara Sudoku, Statistiche, Impostazioni**.
- Route aggiuntive: `game/{gameId}`, `lesson/{lessonId}`.
- Evidenziazione della voce corrente, inclusi gioco e lezione.
- Fumetto globale di Nero fuori dalle partite; fumetto dedicato dentro la partita.
- Dialogo chat globale richiamabile dall'avatar o dal fumetto.
- Tema chiaro/scuro/sistema con palette charcoal, antracite, bianco caldo, lilla, lavanda e accenti pastello.
- Safe drawing insets e layout scrollabile su schermi piccoli.

File principali: `ui/navigation/SudokuCatNavigation.kt`, `MainActivity.kt`, `ui/theme/*`.

## 3. Gioco classico

| Funzione | Stato | Dettagli |
|---|---|---|
| Generazione griglie | ✅ | `SudokuEngine.generate`, soluzione unica verificata con conteggio soluzioni |
| 4 difficoltà | ✅ | Facile 40 indizi, Medio 34, Difficile 29, Esperto 25 |
| Timer | ✅ | Parte al caricamento, si ferma in pausa, salvataggio periodico |
| Autosalvataggio/ripresa | ✅ | Ogni mossa e ogni 20 secondi; partita attiva ripresa dalla Home |
| Evidenziazioni | ✅ | Riga, colonna, quadrante, stesso numero, cella selezionata |
| Note | ✅ | Maschera di bit per cella, attivazione modalità note |
| Cancella | ✅ | Solo celle non-indizio |
| Annulla/ripristina | ✅ | Stack da 80 snapshot |
| Controllo automatico | ✅ | Impostazione: rifiuta ed evidenzia, oppure accetta perdendo una vita |
| Tre aringhe-vita | ✅ | Ogni errore consuma un'aringa; 3 errori = sconfitta |
| Pausa | ✅ | Overlay dedicato, timer fermo, griglia non modificabile |
| Indizi | ✅ | Motore logico locale + spiegazione Gemini opzionale |
| XP | ✅ | Difficoltà, bonus, penalità per aiuti/errori; assegnati una volta |
| Vittoria | ✅ | Confronto con la soluzione; animazione delle aringhe verso Nero |
| Sconfitta | ✅ | Dialogo con statistiche e riprova |
| Animazioni | ✅ | Numero inserito con molla, sfondo cella animato, errore che trema, suggerimenti pulsanti, barra di completamento, tasti del pad con feedback di pressione, aringhe che si inclinano |

File: `domain/engine/SudokuEngine.kt`, `ui/game/GameViewModel.kt`, `ui/game/GameScreen.kt`, `ui/components/SudokuBoard.kt`, `ui/components/NumberPad.kt`, `ui/components/FishFeast.kt`.

## 4. Gioca con AI

- Modalità `GameMode.GUIDED`, stessa griglia del classico.
- Pulsanti **Spiega la cella** e **Prossima mossa**.
- Motore logico con otto tecniche:
  1. Naked Single
  2. Hidden Single
  3. Naked Pair
  4. Hidden Pair
  5. Pointing Pair/Triple
  6. Box-Line Reduction
  7. X-Wing
  8. Swordfish
- Evidenzia celle coinvolte ed eliminazioni di candidati.
- `CoachService.explainStep` / `explainCell`: Gemini se configurato, altrimenti spiegazione locale dettagliata.
- Fallback automatico su errori di rete, chiave assente, AI disattivata, limiti raggiunti.
- Cache Room delle risposte (7 giorni) e limite di 40 richieste/giorno.

File: `domain/engine/LogicalEngine.kt`, `ai/CoachService.kt`, `ai/GeminiClient.kt`, `ui/game/*`.

## 5. Risolvi da foto

| Fase | Funzione |
|---|---|
| Acquisizione | Fotocamera CameraX con permesso runtime e fallback galleria |
| Ritaglio | `CropEditor` con quattro angoli trascinabili, limiti minimi |
| OCR primario | Gemini multimodale: foto ritagliata in `inline_data` + JSON Schema `grid` 9×9 |
| OCR fallback | ML Kit on-device, mapping dei digit nelle 81 celle tramite bounding box, avviato in parallelo con Gemini |
| Sorgente OCR | Chip nella UI («Gemini OCR» / «ML Kit locale») e avviso in caso di fallback |
| Nessuna griglia | Errore esplicito se non vengono letti numeri; avviso se ne vengono letti meno di 17 |
| Correzione | Griglia editabile con number pad, note non usate, undo |
| Analisi | Conflitti, griglia vuota, soluzione assente, multipla, unica, completata |
| Suggerisci mossa | Structured Output `{row, col, valore, spiegazione}`, validato dal risolutore locale; passo locale immediato e raffinamento AI in background |
| Indizi 3 livelli | 1) quadrante evidenziato, 2) cella indicata, 3) numero rivelato con spiegazione |
| Applica mossa | Inserisce la cella suggerita con undo |
| Svela | Una cella oppure «Risolvi Sudoku» con conferma |
| Celle AI | Le celle inserite da AI/indizio/soluzione sono evidenziate in verde acqua |
| Overlay AR | I numeri mancanti sono disegnati con Canvas sopra la foto ritagliata |
| Registrazione | Partita PHOTO vinta, +60 XP una volta, statistiche aggiornate |
| Privacy | Foto inviata solo per l'OCR Gemini; le altre richieste contengono solo lo stato numerico |

File: `ui/photo/PhotoScreen.kt`, `PhotoViewModel.kt`, `CropEditor.kt`, `CameraCaptureScreen.kt`.

### Structured Outputs

Le risposte AI non sono testo libero interpretato a posteriori: ogni chiamata che deve produrre dati usa `responseMimeType = application/json` e un `responseSchema` esplicito.

| Schema | Campi | Uso |
|---|---|---|
| `gridSchema` | `grid`: array 9×9 di interi (0 = vuota) | OCR multimodale della foto |
| `moveSchema` | `row`, `col`, `valore`, `spiegazione` | Suggerimento della prossima mossa |

Il parser (`StructuredOutputs.parseGrid` / `parseMove`) accetta anche JSON racchiuso in code fence, rifiuta dimensioni errate, valori fuori range e spiegazioni vuote. Una mossa proposta da Gemini viene applicata solo se la cella è vuota e il valore coincide con la soluzione calcolata localmente (oppure è un candidato valido); altrimenti si usa il passo del motore logico e si mostra un avviso con la fonte. Quando la mossa AI è valida ma diversa dal passo locale, la tecnica mostrata è **Mossa AI verificata** (`Technique.AI_VERIFIED`).

## 6. Impara Sudoku

- 5 livelli, 15 lezioni, percorso a serpentina con nodi sbloccati in sequenza.
- Ogni lezione: sezioni di lettura con esempi visivi, quiz a scelta multipla, esercizio «trova la cella» sulla mini-griglia.
- Valutazione a 3 stelle in base a errori/aiuti; XP solo alla prima completion.
- Reazioni di Nero, prossima lezione, ritorno al percorso.
- Progressi persistiti in Room (`LessonProgressEntity`).

Contenuti: regole, scansione, candidati, Naked Single, Hidden Single, errori, Naked Pair, Hidden Pair, ordine delle tecniche, Pointing, Box-Line, X-Wing, Swordfish, sfide.

File: `domain/lessons/LessonCatalog.kt`, `ui/learn/LearnScreen.kt`, `ui/learn/LessonScreen.kt`, `data/repo/LessonRepository.kt`.

## 7. Nero, voce e suoni

- Avatar disegnato con Canvas: gatto nero cartoon 3D, espressioni (felice, curioso, concentrato, triste, assonnato, entusiasta, confuso).
- Animazioni di respirazione/oscillazione, rimbalzo a ogni reazione, aringhe che si inclinano quando vengono perse e pannello che si espande con animazione.
- Pannello di Nero sempre visibile in partita: l'avatar resta anche quando la bolla è chiusa, con invito a parlare.
- Fumetto toccabile con chiusura, lettura ad alta voce e apertura chat.
- `CatController` centralizza messaggi, espressioni, auto-hide, TTS e suoni.
- Auto-hide consapevole della sintesi vocale: la bolla non si chiude a metà frase e resta visibile almeno per il tempo di lettura del testo. La chiusura manuale ferma anche la voce.
- TTS italiano di sistema con voce locale stabile, velocità configurabile, disattivabile.
- Sei effetti sonori WAV inclusi (tap, correct, error, win, fish, meow), volume configurabile.

File: `ui/components/CatArt.kt`, `CatViews.kt`, `util/CatController.kt`, `util/TtsManager.kt`, `util/SoundManager.kt`.

## 8. Statistiche e progressi

- Partite vinte/perse, percentuale di vittoria.
- Tempo migliore, tempo medio, tempo totale, media errori.
- Vittorie per difficoltà, vittorie in modalità guidata e foto.
- Storico delle ultime partite con difficoltà, modalità, tempo ed esito.
- XP, livello (250 XP per livello), barra di avanzamento, serie giornaliera.

File: `data/repo/StatsRepository.kt`, `ui/stats/StatsScreen.kt`, `data/prefs/SettingsRepository.kt` (`UserProfile`).

## 9. Impostazioni

| Impostazione | Effetto reale |
|---|---|
| Tema | Cambia subito il tema dell'app |
| Difficoltà predefinita | Preselezionata in Gioca e Gioca con AI |
| Controllo errori | Cambia il comportamento di inserimento |
| Rimozione note | Cancella i candidati dai peer |
| Mostra timer | Nasconde il cronometro in partita |
| Vibrazione | Abilita/disabilita feedback aptico su errore e vittoria |
| Volume effetti | Applicato a SoundPool |
| Voce e velocità | Applicate a TTS |
| Gatto | Mostra/nasconde avatar, fumetti e reazioni |
| AI on/off | Disabilita ogni chiamata di rete |
| OCR foto con Gemini | Attivo per default; se off la foto non viene inviata |
| Chiave Gemini | Cifrata, salvata, testabile, rimovibile |
| Modello Gemini | Usato nelle richieste |
| Cache AI | Svuotabile |
| Ripristina progressi | Cancella partite, lezioni, XP, serie e cache AI |

File: `ui/settings/SettingsScreen.kt`, `data/prefs/SettingsRepository.kt`.

## 10. Dati e sicurezza

- Chiave API cifrata a riposo; mai scritta nel codice sorgente. Se preimpostata, è compilata in `BuildConfig` da `secrets.properties`, `.env`, ambiente o `-P` (tutti untracked) e usata subito come fallback in memoria, prima ancora che la semina cifrata finisca.
- Cifratura AES-256-GCM con chiave Android Keystore.
- Semina automatica della chiave preimpostata solo al primo avvio, senza sovrascrivere chiavi già presenti.
- DataStore segreto separato, escluso da backup cloud e trasferimento dispositivo.
- Nessun log di chiavi o contenuti sensibili.
- `usesCleartextTraffic=false`.
- Backup di partite/statistiche consentito, chiave esclusa.
- Reset selettivo che preserva tema e chiave.

## 11. Test e build

- Test unitari del motore in `app/src/test/java/com/sudokucat/app/SudokuEngineTest.kt`.
- Test degli Structured Outputs in `app/src/test/java/com/sudokucat/app/StructuredOutputsTest.kt`: schema, parsing, input malformati, integrazione con il risolutore.
- `:app:assembleDebug` ✅
- `:app:testDebugUnitTest` ✅ (tutti i test)
- `:app:assembleRelease` ✅ con R8 e firma.
- APK in `dist/` (`SudokuCat-1.1.1-release.apk`, `SudokuCat-1.1.1-debug.apk`).
- Release verificata con `apksigner` (certificato `CN=Sudoku Cat`); SHA-256 APK release `7A9B7820C171531984D8A13DF0CECF575A3710B03252EC637D21095BEAF3A9CE`.
