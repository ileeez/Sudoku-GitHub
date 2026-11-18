# Configurare Gemini in Sudoku Cat

L'AI è **opzionale**. Senza chiave, senza rete o con l'AI disattivata, Sudoku Cat usa il motore logico locale e resta completamente giocabile.

## 0. Chiave già preimpostata (build personali)

Questa build può contenere la tua chiave personale, inserita prima della compilazione in `secrets.properties` oppure in `.env` (copia `.env.example`):

```properties
GEMINI_API_KEY=la_tua_chiave
```

In alternativa usa la variabile d'ambiente `GEMINI_API_KEY` o `-PGEMINI_API_KEY=...`. L'ordine di precedenza è `secrets.properties` → ambiente → `.env` → `-P`. Gradle inserisce il valore in `BuildConfig`; l'app lo usa **subito** come fallback in memoria e, solo se serve, lo copia nel DataStore cifrato (`sudoku_cat_secret`). La semina non sovrascrive una chiave inserita manualmente. In Impostazioni compare «Chiave Gemini integrata nella build: attiva subito...».

Se vuoi cambiare chiave, incollane una nuova in **Nuova chiave API Gemini** e premi **Salva chiave**: la nuova chiave ha la precedenza. **Rimuovi chiave** la cancella dal dispositivo; alla successiva apertura dell'app viene ripristinata quella preimpostata nella build.

> Per il repository GitHub lascia `GEMINI_API_KEY` vuoto: `.env`, `secrets.properties` e gli APK in `dist/` sono esclusi dal versionamento perché potrebbero contenere il segreto.

> La chiave compilata in un APK distribuito è estraibile da chiunque abbia il file. Per un uso condiviso o commerciale usa un backend che custodisca la chiave.

## 1. Ottenere una chiave API

1. Apri [Google AI Studio](https://aistudio.google.com/apikey) e accedi con un account Google.
2. Crea una nuova API key per un progetto.
3. Copia la chiave (di solito inizia con `AIza...` o `AQ....`). Trattala come una password.

> La chiave personale serve solo a questo dispositivo. Non inserirla nel codice sorgente e non condividerla.

## 2. Inserirla nell'app (se non è preimpostata)

1. Apri il menu laterale → **Impostazioni**.
2. Scorri fino alla sezione **Intelligenza artificiale**.
3. Assicurati che **Abilita spiegazioni AI** sia attivo.
4. Incolla la chiave nel campo **Nuova chiave API Gemini**.
5. Premi **Salva chiave**.
6. Premi **Testa connessione**: deve comparire «Connessione riuscita: Gemini risponde».

Dopo il salvataggio il campo si svuota e compare «Chiave salvata e cifrata sul dispositivo». La chiave non viene mai mostrata di nuovo: per sostituirla incollane una nuova.

Nella stessa sezione trovi **OCR foto con Gemini**: lascialo attivo per il riconoscimento multimodale della foto; disattivalo se preferisci che nessuna immagine venga inviata e usare solo ML Kit.

## 3. Scegliere il modello

Il modello predefinito è:

```
gemini-3.8-flash
```

Puoi cambiarlo nel campo **Modello Gemini**. Sono presenti scorciatoie per `gemini-3.8-flash` e `gemini-3.8-pro`; puoi digitare qualsiasi modello disponibile per la tua chiave.

Alcune alternative, se il tuo account le abilita:

- `gemini-3.8-flash` — veloce ed economico, adatto alle spiegazioni brevi (predefinito)
- `gemini-3.8-pro` — risposte più ricche, più lento e con limiti più bassi
- `gemini-3.5-flash-lite` — alternativa leggera per volumi elevati

I nomi e la disponibilità dei modelli cambiano nel tempo e dipendono dal progetto/account. Se ricevi un errore «Modello Gemini non disponibile», prova un altro nome o controlla [AI Studio](https://aistudio.google.com/).

Lo stesso modello scelto viene usato sia per le spiegazioni sia per l'OCR multimodale e per gli Structured Outputs. Se un modello non supporta le immagini o gli schemi JSON, l'app lo segnala e passa al riconoscimento locale.

## 4. Come viene usata la chiave

- La chiave integrata nella build viene usata **subito** come fallback in memoria, senza attendere la semina cifrata: l'AI è disponibile dal primo avvio. La copia cifrata nei DataStore è solo la persistenza.
- La chiave viene letta solo quando serve una funzione AI (spiegazione, suggerimento o OCR foto).
- Viene inviata esclusivamente nell'header `x-goog-api-key` verso:

  ```
  https://generativelanguage.googleapis.com/v1beta/models/<modello>:generateContent
  ```

- Il traffico è HTTPS; l'app non accetta traffico HTTP in chiaro.
- La chiave viene cifrata con **Android Keystore** (AES-256-GCM) e salvata in un DataStore dedicato (`sudoku_cat_secret`), escluso dai backup cloud e dal trasferimento tra dispositivi.
- Non viene scritta nei log e non compare nel codice sorgente. Se è preimpostata, il valore resta compilato in `BuildConfig` dentro l'APK: trattalo come un segreto del file e non distribuire quell'APK a terzi.
- Con **OCR foto con Gemini** attivo (predefinito), la sola foto ritagliata viene inviata come `inline_data` per estrarre la griglia. Le altre richieste contengono solo i numeri della griglia. Se disattivi l'opzione, nessuna immagine lascia il telefono.

## 5. Structured Outputs

Le funzioni che producono dati usano il formato JSON vincolato di Gemini:

```json
// OCR
{ "grid": [[5,3,0,0,7,0,0,0,0], "... 8 righe ..." ] }

// Suggerimento
{ "row": 0, "col": 2, "valore": 4, "spiegazione": "Naked Single: ..." }
```

La richiesta imposta `generationConfig.responseMimeType = "application/json"` e `generationConfig.responseSchema`. Gli schemi sono definiti in `ai/StructuredOutputs.kt`; se Gemini restituisce un formato inatteso, l'app scarta la risposta e usa il fallback locale. Le mosse AI vengono inoltre validate contro il risolutore: una proposta errata non viene mai applicata.

## 6. Limiti e fallback

| Protezione | Comportamento |
|---|---|
| Limite giornaliero | 40 richieste AI al giorno per dispositivo |
| Intervallo minimo | 2,5 secondi tra due richieste |
| Limite di sessione | 30 richieste per ora di esecuzione |
| Cache | Risposte riutilizzate per 7 giorni |
| Timeout | 12 s connessione; 8 s per l'OCR, 6 s per la mossa strutturata, 9 s per le spiegazioni, 15 s per i test |
| Retry | Disattivato sulle chiamate che bloccano la UI; al massimo un secondo tentativo per le sole richieste di spiegazione |
| Errori/limiti | Si continua con la spiegazione locale, mostrando un avviso |
| OCR fallito | ML Kit viene eseguito in parallelo e il suo risultato è usato senza attendere Gemini |
| Suggerimenti | Il passo locale viene mostrato subito; Gemini rifinisce in background e non sostituisce mai una mossa non validata |
| Nessuna griglia | Messaggio dedicato se non vengono letti numeri; avviso se ne vengono letti meno di 17 |
| AI disattivata | Nessuna chiamata di rete |

Se la chiave è stata revocata, scaduta o non ha accesso al modello scelto, l'app mostra un avviso e usa il fallback locale. Non perdi progressi né funzioni di gioco.

## 7. Rimuovere o ruotare la chiave

- **Rimuovi chiave** cancella il valore cifrato dal dispositivo. Se la build contiene una chiave preimpostata, viene ripristinata al successivo avvio: per eliminarla davvero serve una build senza `GEMINI_API_KEY`.
- **Svuota cache AI** elimina le risposte memorizzate.
- Se sospetti che la chiave sia compromessa, revocala da Google AI Studio e creane una nuova.

## 8. Sicurezza: cosa non protegge

La cifratura con Keystore protegge la chiave a riposo da accessi accidentali e da backup, ma **non** da un dispositivo rooted, da un debugger collegato o da un APK modificato in grado di eseguire codice nell'app. Per un uso condiviso, commerciale o ad alto rischio, la chiave va custodita su un piccolo backend e l'app deve chiamare il backend, non Gemini direttamente.
