# Limiti noti

Questa è la lista onesta di ciò che l'app non fa, dei compromessi scelti e dei comportamenti da conoscere.

## Fotocamera e riconoscimento

- **L'OCR non è perfetto.** Dipende da luce, prospettiva, nitidezza, font e ritaglio. Numeri scritti a mano, griglie storte o foto mosse possono essere letti male.
- Due motori: **Gemini multimodale** e **ML Kit** partono in parallelo; ML Kit è sempre disponibile offline e la sorgente effettivamente usata è mostrata nella schermata.
- Se non viene letto alcun numero l'app segnala che in quella foto non ha trovato una griglia 9×9; con meno di 17 numeri mostra un avviso di ritaglio poco nitido. Il rilevamento è euristico: una griglia reale ma molto vuota può comunque richiedere l'inserimento manuale.
- La correzione manuale è prevista dal flusso: dopo il ritaglio la griglia è sempre modificabile.
- ML Kit è configurato per il riconoscimento **latino** e cerca le cifre da 1 a 9. Non riconosce simboli, note a matita o numeri scritti a mano con grafia particolare.
- Il ritaglio non ha zoom o pinch-to-zoom: si trascinano i quattro angoli sull'immagine intera.
- Se più cifre cadono nella stessa cella, viene tenuta la prima riconosciuta.
- La fotocamera richiede il permesso runtime. Se viene negato definitivamente, l'app offre il collegamento alle impostazioni di sistema e il fallback alla galleria.
- Su emulatori senza fotocamera si può usare solo la galleria.
- L'immagine non viene salvata nella galleria. Con l'OCR Gemini attivo la foto ritagliata viene inviata a Google per l'estrazione della griglia; disattivando l'opzione resta sul dispositivo.

## Intelligenza artificiale

- Le spiegazioni e l'OCR Gemini richiedono **rete**, una **chiave valida** e un **modello disponibile** per quella chiave. La chiave integrata nella build è attiva dal primo avvio; senza rete l'app usa subito motore logico e ML Kit.
- I nomi dei modelli cambiano nel tempo. Il predefinito è `gemini-3.8-flash`; se non è disponibile per il tuo account, cambialo nelle impostazioni.
- I limiti gratuiti di Google possono variare e possono produrre errori 429. In quel caso l'app continua con il motore locale.
- Le risposte AI possono contenere imprecisioni: il numero suggerito viene comunque validato dal motore locale prima di essere applicato.
- Gli Structured Outputs riducono gli errori di formato, ma non eliminano quelli di contenuto: per questo ogni mossa passa dal risolutore.
- Un modello che non supporta immagini o JSON Schema produce un errore gestito con fallback locale.
- Le immagini vengono compresse prima dell'invio (lato lungo max 1280 px); immagini enormi o con dettagli minuscoli possono perdere leggibilità.
- Il limite di 40 richieste/giorno e il throttling sono locali e si basano sull'orologio del dispositivo: non sono una protezione server-side.
- La cache AI scade dopo 7 giorni e può essere svuotata manualmente.
- La chat non è un assistente generale: è focalizzata su Sudoku e sul personaggio di Nero.

## Sicurezza della chiave

- La chiave è cifrata con Android Keystore, ma su un dispositivo **rooted**, con debugger o con un APK manomesso può essere estratta.
- Se la chiave è preimpostata a build time, il valore è compilato in `BuildConfig` e quindi contenuto nell'APK: chiunque riceva quel file può potenzialmente estrarlo. Non condividere l'APK personale e usa una chiave dedicata.
- Non esiste un backend proprietario: per un uso condiviso o commerciale la chiave andrebbe custodita lato server.
- La chiave è esclusa dai backup cloud e dal trasferimento tra dispositivi. Di conseguenza, cambiando telefono, va reinserita (oppure serve una build con la chiave preimpostata).
- Se il keystore Android viene invalidato (cambio blocco schermo, ripristino, alcuni aggiornamenti), la chiave salvata può non essere più decifrabile: l'app la tratta come assente e usa il fallback locale.

## Gioco

- Le difficoltà sono basate sul numero di indizi (40/34/29/25) e sulla generazione con soluzione unica; non c'è una garanzia formale che una griglia «Esperto» richieda X-Wing o Swordfish.
- La generazione avviene sul dispositivo e può richiedere qualche istante, soprattutto alla difficoltà Esperto.
- La partita attiva è una per modalità: creando una nuova partita classica, quella precedente non completata viene eliminata e **non** conteggiata come sconfitta.
- Le sconfitte nelle statistiche derivano solo dal termine delle tre aringhe. Uscire dalla partita non la registra come persa: resta riprendibile.
- L'undo/redo è in memoria: si azzera chiudendo la partita o riavviando l'app.
- Con il controllo errori disattivato, un numero sbagliato resta sulla griglia e consuma una vita; la vittoria richiede comunque che tutti i valori coincidano con la soluzione.
- Il timer conta mentre il processo dell'app è attivo, anche se l'app va in background. Non c'è un servizio foreground dedicato.
- Non ci sono classifiche, sfide online o sincronizzazione multi-dispositivo.

## Modalità foto

- Una sessione foto viene registrata nelle statistiche solo quando la griglia è completata o svelata; viene salvata come difficoltà **Medio** e assegna **60 XP** una sola volta per sessione.
- Le note a matita non sono disponibili nella griglia foto.
- L'undo è limitato alla sessione corrente e viene svuotato quando si torna al ritaglio o si ricomincia.
- Se i numeri riconosciuti non ammettono soluzione, l'app lo segnala ma non corregge automaticamente la griglia.
- La soluzione multipla è segnalata; l'app ne mostra una possibile, non tutte.
- Gli indizi a 3 livelli e «Risolvi Sudoku» inseriscono celle marcate come AI; la conferma registra la sessione come risolta con aiuto.
- L'overlay AR è un disegno 2D sovrapposto alla foto ritagliata: presuppone che la griglia riempia il ritaglio e non insegue la carta in tempo reale con la fotocamera. Non è quindi un AR con tracking, ma una visualizzazione aumentata della foto.
- Ruotando o sostituendo la foto l'overlay viene ricalcolato; se il ritaglio non è preciso, i numeri possono risultare leggermente fuori cella.

## Interfaccia e dispositivi

- L'app è ottimizzata per smartphone. Su tablet e in orizzontale i contenuti si adattano e restano scrollabili, ma il layout non è stato progettato specificamente per schermi grandi.
- L'interfaccia è solo in **italiano**. La lingua di sistema non viene localizzata.
- Il tema «Sistema» segue le impostazioni di Android.
- Alcune animazioni e il disegno del gatto usano Compose Canvas: su dispositivi molto lenti possono risultare meno fluide.
- La vibrazione passa dal feedback aptico di sistema: su alcuni dispositivi o con risparmio energetico attivo può non essere percepita.
- La voce di Nero richiede un motore TTS italiano installato. Se manca, la sintesi può restare muta o usare una voce non italiana.
- Il volume degli effetti non controlla la voce TTS, che segue il volume multimedia del telefono.

## Build e distribuzione

- La release inclusa è firmata con una **chiave locale self-signed**, non con una chiave da Google Play. Non è pubblicabile su Play Store così com'è.
- Se `keystore.properties` non è presente, il build release produce un APK non firmato.
- Debug e release hanno application ID diversi e dati separati: i progressi non si trasferiscono automaticamente dall'uno all'altro.
- La release usa R8/minificazione: dopo modifiche al codice va ricompilata e, se si cambiano regole di reflection, ritestata.
- Non sono presenti test strumentali su dispositivo; la verifica runtime è stata fatta manualmente su emulatore per le funzioni di base. I test unitari coprono il motore Sudoku e gli Structured Outputs; le aggiunte 1.1.1 sono state verificate con compilazione, test e build firmata.
- Il progetto non include un backend, un'integrazione Play Billing o funzioni cloud.
