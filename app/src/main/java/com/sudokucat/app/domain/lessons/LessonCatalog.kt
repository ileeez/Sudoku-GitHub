package com.sudokucat.app.domain.lessons

import com.sudokucat.app.domain.engine.LogicalEngine

data class LessonSection(
    val title: String,
    val text: String,
    val board: String? = null,
    val highlight: List<Int> = emptyList(),
    val tip: String? = null
)

sealed interface LessonPractice {
    data class FindCell(
        val prompt: String,
        val board: String,
        val technique: LogicalEngine.Technique,
        val explanation: String
    ) : LessonPractice

    data class Quiz(
        val question: String,
        val options: List<String>,
        val correctIndex: Int,
        val explanation: String,
        val board: String? = null,
        val highlight: List<Int> = emptyList()
    ) : LessonPractice
}

data class Lesson(
    val id: String,
    val level: Int,
    val order: Int,
    val title: String,
    val subtitle: String,
    val technique: LogicalEngine.Technique?,
    val xp: Int,
    val sections: List<LessonSection>,
    val practice: LessonPractice
)

data class LessonLevel(
    val index: Int,
    val title: String,
    val subtitle: String
)

object LessonCatalog {

    const val BOARD_A = ".7..8...9.24...5....84..2........3971.....84.4...2...678.9..6..24.1......6..7.9.."
    const val BOARD_B = ".47.2....1..6......3.4895..58.732..14.1.9..3.3.684..59.6..5....79.214386..4......"

    val levels = listOf(
        LessonLevel(1, "Prime impronte", "Regole, scansione e candidati"),
        LessonLevel(2, "Occhi di gatto", "I due singoli che sbloccano tutto"),
        LessonLevel(3, "Coppie inseparabili", "Naked Pair, Hidden Pair e ordine mentale"),
        LessonLevel(4, "Detective dei numeri", "Pointing, Box-Line e prima sfida"),
        LessonLevel(5, "Maestro dei numeri", "X-Wing, Swordfish e sfida finale")
    )

    val lessons: List<Lesson> = listOf(
        Lesson(
            id = "l1-regole",
            level = 1,
            order = 1,
            title = "Le regole del Sudoku",
            subtitle = "Una griglia, nove numeri, zero ripetizioni",
            technique = null,
            xp = 30,
            sections = listOf(
                LessonSection(
                    title = "La griglia",
                    text = "Il Sudoku è una griglia 9×9 divisa in nove quadranti 3×3. Ogni riga, ogni colonna e ogni quadrante devono contenere i numeri da 1 a 9 una sola volta. All'inizio alcune celle sono già riempite: sono gli indizi.",
                    board = BOARD_A
                ),
                LessonSection(
                    title = "Non si indovina",
                    text = "Ogni numero ha un posto preciso. Il divertimento è trovare il ragionamento che lo dimostra. Anche quando sembra impossibile, c'è sempre una deduzione logica che aspetta di essere vista.",
                    tip = "Riga, colonna e quadrante: i tre sguardi del detective."
                )
            ),
            practice = LessonPractice.Quiz(
                question = "Quante volte può comparire il numero 7 in una singola riga?",
                options = listOf("Una sola volta", "Due volte", "Dipende dalla riga"),
                correctIndex = 0,
                explanation = "In ogni riga, colonna e quadrante i numeri da 1 a 9 compaiono esattamente una volta."
            )
        ),
        Lesson(
            id = "l1-scansione",
            level = 1,
            order = 2,
            title = "La scansione",
            subtitle = "Incrocia righe e colonne come un gatto",
            technique = null,
            xp = 30,
            sections = listOf(
                LessonSection(
                    title = "Scegli un numero",
                    text = "La prima tecnica è la più semplice: scegli un numero, per esempio il 4, e osserva dove può ancora stare. Se in una riga il 4 è già presente, nessun'altra cella di quella riga può contenerlo.",
                    board = BOARD_A,
                    highlight = listOf(0, 9, 18, 27, 36, 45, 54, 63, 72)
                ),
                LessonSection(
                    title = "Incrocia gli sguardi",
                    text = "Combina l'informazione di riga, colonna e quadrante. Quando due direzioni si incrociano, spesso resta una sola cella possibile. Questa è la base di tutte le tecniche successive.",
                    tip = "Guarda prima i numeri più frequenti: danno più indizi."
                )
            ),
            practice = LessonPractice.Quiz(
                question = "In una riga c'è già il numero 4. Dove puoi inserire un altro 4?",
                options = listOf(
                    "In nessuna cella della stessa riga",
                    "In qualsiasi cella libera della riga",
                    "Solo nel quadrante centrale"
                ),
                correctIndex = 0,
                explanation = "Il 4 può comparire una sola volta per riga: le altre celle della riga sono escluse."
            )
        ),
        Lesson(
            id = "l1-candidati",
            level = 1,
            order = 3,
            title = "I candidati",
            subtitle = "La matita del piccolo detective",
            technique = null,
            xp = 35,
            sections = listOf(
                LessonSection(
                    title = "Cosa annotare",
                    text = "I candidati sono i numeri che possono ancora entrare in una cella. Si ricavano escludendo i numeri già presenti nella riga, nella colonna e nel quadrante. Nel gioco puoi attivare la modalità Note e scriverli in piccolo.",
                    board = BOARD_A,
                    highlight = listOf(3, 4, 5, 12, 13, 14)
                ),
                LessonSection(
                    title = "Non serve scriverli tutti",
                    text = "Annota i candidati solo nelle celle davvero incerte. Troppi appunti confondono. Quando in una cella resta un solo candidato, hai trovato un Naked Single e puoi scrivere il numero.",
                    tip = "La modalità Note si attiva con il pulsante Note nella tastiera."
                )
            ),
            practice = LessonPractice.Quiz(
                question = "Cosa sono i candidati di una cella?",
                options = listOf(
                    "I numeri che possono ancora stare in quella cella",
                    "I numeri già inseriti nella griglia",
                    "Le soluzioni della partita"
                ),
                correctIndex = 0,
                explanation = "I candidati sono i numeri compatibili con la riga, la colonna e il quadrante della cella."
            )
        ),
        Lesson(
            id = "l2-naked-single",
            level = 2,
            order = 4,
            title = "Naked Single",
            subtitle = "Quando resta un solo numero",
            technique = LogicalEngine.Technique.NAKED_SINGLE,
            xp = 45,
            sections = listOf(
                LessonSection(
                    title = "L'ultimo candidato",
                    text = "Se in una cella vuota resta un solo candidato, quel numero è obbligatorio. Si chiama Naked Single perché il numero è nudo davanti a te: non serve confrontarlo con altre celle.",
                    board = BOARD_A,
                    highlight = listOf(28)
                ),
                LessonSection(
                    title = "Come verificarlo",
                    text = "Guarda la cella R4C2: i numeri della riga 4, della colonna 2 e del suo quadrante escludono tutti i valori tranne il 5. Quindi R4C2 = 5. Semplice, ma potentissimo.",
                    tip = "Tocca la cella, attiva Note e leggi i candidati rimasti."
                )
            ),
            practice = LessonPractice.FindCell(
                prompt = "Tocca la cella che contiene un Naked Single.",
                board = BOARD_A,
                technique = LogicalEngine.Technique.NAKED_SINGLE,
                explanation = "In R4C2 resta un solo candidato: 5. È la tua prima Naked Single!"
            )
        ),
        Lesson(
            id = "l2-hidden-single",
            level = 2,
            order = 5,
            title = "Hidden Single",
            subtitle = "L'unico posto possibile",
            technique = LogicalEngine.Technique.HIDDEN_SINGLE,
            xp = 50,
            sections = listOf(
                LessonSection(
                    title = "Non guardare solo la cella",
                    text = "Un Hidden Single si nasconde nell'unità: un numero può stare in una sola cella della riga, della colonna o del quadrante. La cella può avere più candidati, ma quel numero ha un solo posto possibile.",
                    board = BOARD_A,
                    highlight = listOf(4, 13, 22, 31, 40, 49, 58, 67, 76)
                ),
                LessonSection(
                    title = "Il trucco dello sguardo largo",
                    text = "Scegli un numero e percorri tutta la riga, poi la colonna, poi il quadrante. Se trovi una sola cella libera dove può stare, è lui. Gli altri candidati di quella cella si eliminano.",
                    tip = "Riga per riga, numero per numero: si parte sempre dal più frequente."
                )
            ),
            practice = LessonPractice.FindCell(
                prompt = "Tocca la cella che ospita un Hidden Single.",
                board = BOARD_A,
                technique = LogicalEngine.Technique.HIDDEN_SINGLE,
                explanation = "Il numero ha un solo posto possibile nella sua unità: questo è un Hidden Single."
            )
        ),
        Lesson(
            id = "l2-errori",
            level = 2,
            order = 6,
            title = "Errori e sangue freddo",
            subtitle = "Le tre aringhe insegnano",
            technique = null,
            xp = 40,
            sections = listOf(
                LessonSection(
                    title = "Sbagliare è normale",
                    text = "Nel gioco hai tre aringhe-biscotto: ogni errore ne consuma una. Ma un errore non è un fallimento, è un'informazione. Il numero giusto non era compatibile con la riga, la colonna o il quadrante.",
                    board = BOARD_A
                ),
                LessonSection(
                    title = "La routine del recupero",
                    text = "Cancella il numero sbagliato, poi ricontrolla l'unità coinvolta. Cerca un Naked Single o un Hidden Single: sono i passi più sicuri per ripartire. Evita di indovinare: il Sudoku premia la calma.",
                    tip = "Prima di scrivere, chiediti: «Perché questo numero deve stare proprio qui?»"
                )
            ),
            practice = LessonPractice.Quiz(
                question = "Hai inserito un numero sbagliato. Qual è la mossa migliore?",
                options = listOf(
                    "Cancellarlo e ricontrollare riga, colonna e quadrante",
                    "Provare un altro numero a caso",
                    "Ricominciare subito da capo"
                ),
                correctIndex = 0,
                explanation = "Il recupero parte sempre dall'unità: riga, colonna e quadrante ti dicono perché il numero era sbagliato."
            )
        ),
        Lesson(
            id = "l3-naked-pair",
            level = 3,
            order = 7,
            title = "Naked Pair",
            subtitle = "Due celle, due numeri",
            technique = LogicalEngine.Technique.NAKED_PAIR,
            xp = 55,
            sections = listOf(
                LessonSection(
                    title = "La coppia inseparabile",
                    text = "Se due celle della stessa unità hanno esattamente gli stessi due candidati, quei due numeri devono occupare quelle due celle. Di conseguenza nessun'altra cella dell'unità può contenerli.",
                    board = BOARD_B,
                    highlight = listOf(54, 72)
                ),
                LessonSection(
                    title = "Cosa eliminare",
                    text = "Nella colonna 1 le celle R7C1 e R9C1 possono contenere solo 2 e 8. Sono una Naked Pair: elimina 2 e 8 da tutte le altre celle della colonna 1. Non stai riempiendo celle, stai togliendo possibilità.",
                    tip = "Una Naked Pair non dà subito un numero: prepara il terreno."
                )
            ),
            practice = LessonPractice.Quiz(
                question = "Cosa puoi eliminare grazie alla Naked Pair in colonna 1?",
                options = listOf(
                    "Il 2 e l'8 dalle altre celle della colonna 1",
                    "Il 2 e l'8 dalle due celle della coppia",
                    "Tutti i numeri dalle altre celle della colonna"
                ),
                correctIndex = 0,
                explanation = "I due numeri della coppia restano confinati nelle due celle: si eliminano solo dalle altre celle dell'unità."
            )
        ),
        Lesson(
            id = "l3-hidden-pair",
            level = 3,
            order = 8,
            title = "Hidden Pair",
            subtitle = "Due numeri timidi",
            technique = LogicalEngine.Technique.HIDDEN_PAIR,
            xp = 60,
            sections = listOf(
                LessonSection(
                    title = "Nascosti ma inseparabili",
                    text = "Se due numeri compaiono solo nelle stesse due celle di un'unità, quelle celle devono contenerli entrambi. È il rovescio della Naked Pair: non guardi i candidati della cella, ma dove può andare ogni numero.",
                    board = BOARD_A,
                    highlight = listOf(31, 32)
                ),
                LessonSection(
                    title = "Pulisci le celle",
                    text = "Nella riga 4 i numeri 1 e 4 possono stare solo in R4C5 e R4C6. Quindi quelle due celle formeranno la coppia 1-4: tutti gli altri candidati presenti in quelle celle vanno eliminati.",
                    tip = "Cerca due numeri con esattamente le stesse due possibili celle."
                )
            ),
            practice = LessonPractice.Quiz(
                question = "In una Hidden Pair, cosa elimini dalle due celle coinvolte?",
                options = listOf(
                    "Tutti i candidati che non fanno parte della coppia",
                    "I due numeri della coppia",
                    "Niente: si riempiono subito le celle"
                ),
                correctIndex = 0,
                explanation = "Le due celle devono contenere i due numeri nascosti, quindi gli altri candidati si eliminano."
            )
        ),
        Lesson(
            id = "l3-strategia",
            level = 3,
            order = 9,
            title = "L'ordine delle tecniche",
            subtitle = "Dal semplice al complesso",
            technique = null,
            xp = 60,
            sections = listOf(
                LessonSection(
                    title = "Una scala di difficoltà",
                    text = "Non serve partire dalle tecniche avanzate. Cerca prima Naked Single e Hidden Single, poi le coppie, poi Pointing e Box-Line. Solo se la griglia resiste passa a X-Wing e Swordfish.",
                    board = BOARD_B
                ),
                LessonSection(
                    title = "Meno errori, più velocità",
                    text = "Ogni tecnica elimina possibilità e crea nuove occasioni. Dopo una Naked Pair, ricontrolla subito i singoli: spesso la coppia ha appena creato un Naked Single.",
                    tip = "Dopo ogni eliminazione, torna sempre ai singoli."
                )
            ),
            practice = LessonPractice.Quiz(
                question = "Da quale tecnica conviene iniziare una griglia?",
                options = listOf(
                    "Naked Single e Hidden Single",
                    "Swordfish",
                    "X-Wing"
                ),
                correctIndex = 0,
                explanation = "I singoli sono i passi più semplici e sicuri. Le tecniche avanzate servono solo quando i singoli scarseggiano."
            )
        ),
        Lesson(
            id = "l4-pointing",
            level = 4,
            order = 10,
            title = "Pointing Pair/Triple",
            subtitle = "Il quadrante indica la direzione",
            technique = LogicalEngine.Technique.POINTING,
            xp = 70,
            sections = listOf(
                LessonSection(
                    title = "Confinati in una linea",
                    text = "Dentro un quadrante, se tutte le celle possibili per un numero stanno sulla stessa riga o colonna, quel numero è confinato lì. Le altre celle di quella riga o colonna, fuori dal quadrante, non possono contenerlo.",
                    board = BOARD_A,
                    highlight = listOf(16, 25)
                ),
                LessonSection(
                    title = "Perché funziona",
                    text = "Il quadrante deve contenere quel numero una volta sola. Se può stare solo su una linea, la linea «presta» una cella al quadrante. Quindi il numero non può comparire altrove sulla stessa linea.",
                    tip = "Pointing: dal quadrante verso riga o colonna."
                )
            ),
            practice = LessonPractice.Quiz(
                question = "Nel quadrante 3 il 7 può stare solo sulla colonna 8. Cosa fai?",
                options = listOf(
                    "Elimino il 7 dalle altre celle della colonna 8",
                    "Elimino il 7 dalle celle del quadrante 3",
                    "Inserisco subito il 7 al centro del quadrante"
                ),
                correctIndex = 0,
                explanation = "Il 7 è confinato nella colonna 8 dal quadrante: si elimina dal resto della colonna, fuori dal quadrante."
            )
        ),
        Lesson(
            id = "l4-box-line",
            level = 4,
            order = 11,
            title = "Box-Line Reduction",
            subtitle = "La linea entra nel quadrante",
            technique = LogicalEngine.Technique.BOX_LINE,
            xp = 75,
            sections = listOf(
                LessonSection(
                    title = "Il percorso inverso",
                    text = "Se in una riga o colonna tutte le celle possibili per un numero stanno dentro un solo quadrante, quel numero è confinato nel quadrante. Le altre celle del quadrante, fuori dalla linea, sono escluse.",
                    board = BOARD_A,
                    highlight = (27..35).toList()
                ),
                LessonSection(
                    title = "Pointing e Box-Line",
                    text = "Sono due facce della stessa idea: un numero confinato all'intersezione tra quadrante e linea. Pointing parte dal quadrante, Box-Line parte dalla linea. Il risultato è sempre un'eliminazione.",
                    tip = "Chiediti: «Dove può stare questo numero nella riga?» e guarda il quadrante."
                )
            ),
            practice = LessonPractice.Quiz(
                question = "Nella riga 4 il numero 1 può stare solo nel quadrante 5. Cosa deduci?",
                options = listOf(
                    "Il 1 va eliminato dalle altre celle del quadrante 5",
                    "Il 1 va eliminato dalle altre celle della riga 4",
                    "Il 1 è sicuramente in R4C5"
                ),
                correctIndex = 0,
                explanation = "La riga 4 confina il 1 nel quadrante 5: il numero non può stare nelle altre celle del quadrante."
            )
        ),
        Lesson(
            id = "l4-sfida",
            level = 4,
            order = 12,
            title = "Sfida del detective",
            subtitle = "Trova il prossimo passo",
            technique = LogicalEngine.Technique.NAKED_SINGLE,
            xp = 85,
            sections = listOf(
                LessonSection(
                    title = "Mettiamo insieme tutto",
                    text = "Questa griglia è più densa. Non cercare la soluzione completa: cerca solo la prossima certezza. Una Naked Single è sufficiente per sbloccare la situazione.",
                    board = BOARD_B
                )
            ),
            practice = LessonPractice.FindCell(
                prompt = "Tocca la prima cella con una Naked Single.",
                board = BOARD_B,
                technique = LogicalEngine.Technique.NAKED_SINGLE,
                explanation = "In R2C5 i candidati si riducono a uno solo: 7. Ogni passo parte da qui."
            )
        ),
        Lesson(
            id = "l5-xwing",
            level = 5,
            order = 13,
            title = "X-Wing",
            subtitle = "Un rettangolo di possibilità",
            technique = LogicalEngine.Technique.X_WING,
            xp = 95,
            sections = listOf(
                LessonSection(
                    title = "Quattro angoli",
                    text = "Per un certo numero, due righe hanno esattamente due possibili colonne, e sono le stesse due. I quattro angoli formano un rettangolo: il numero deve occupare due angoli opposti, quindi le colonne sono «prenotate».",
                    board = BOARD_B,
                    highlight = listOf(3, 5, 39, 41)
                ),
                LessonSection(
                    title = "L'eliminazione",
                    text = "Nell'esempio il 5 è confinato nelle colonne 4 e 6 delle righe 1 e 5. Elimina il 5 dalle altre celle di quelle due colonne. Non sai ancora dove va, ma sai dove non può andare.",
                    tip = "X-Wing: due righe, due colonne, un numero."
                )
            ),
            practice = LessonPractice.Quiz(
                question = "Quante righe e quante colonne coinvolge un X-Wing?",
                options = listOf("Due righe e due colonne", "Tre righe e tre colonne", "Una riga e una colonna"),
                correctIndex = 0,
                explanation = "L'X-Wing è un rettangolo: due righe e due colonne, con quattro celle candidate agli angoli."
            )
        ),
        Lesson(
            id = "l5-swordfish",
            level = 5,
            order = 14,
            title = "Swordfish",
            subtitle = "Il pesce a tre file",
            technique = LogicalEngine.Technique.SWORDFISH,
            xp = 110,
            sections = listOf(
                LessonSection(
                    title = "Tre righe, tre colonne",
                    text = "Lo Swordfish è la versione a tre dello X-Wing: in tre righe il candidato compare al massimo in tre colonne, e collettivamente proprio in tre colonne. Quelle colonne devono contenere il numero in quelle righe.",
                    board = BOARD_B,
                    highlight = listOf(3, 5, 8, 12, 14, 17, 75, 77, 80)
                ),
                LessonSection(
                    title = "Cosa eliminare",
                    text = "Nell'esempio il 3 è confinato nelle colonne 4, 6 e 9 delle righe 1, 2 e 9. Elimina il 3 dalle altre righe in quelle colonne. È un pesce grande, ma nuota sempre nella stessa logica.",
                    tip = "Se X-Wing non basta, prova Swordfish: tre file, tre colonne."
                )
            ),
            practice = LessonPractice.Quiz(
                question = "Quante righe coinvolge uno Swordfish?",
                options = listOf("Tre", "Due", "Quattro"),
                correctIndex = 0,
                explanation = "Lo Swordfish usa tre righe (o tre colonne) e tre colonne (o righe) per confinare un candidato."
            )
        ),
        Lesson(
            id = "l5-finale",
            level = 5,
            order = 15,
            title = "Sfida finale",
            subtitle = "Il maestro dei numeri",
            technique = LogicalEngine.Technique.HIDDEN_SINGLE,
            xp = 130,
            sections = listOf(
                LessonSection(
                    title = "L'ultima lezione",
                    text = "Hai imparato a guardare la griglia in molti modi: celle, unità, coppie, linee e rettangoli. Ora dimostra di saper scegliere il passo giusto senza fretta.",
                    board = BOARD_B
                ),
                LessonSection(
                    title = "Il segreto del maestro",
                    text = "Non esiste una tecnica migliore in assoluto. Esiste la tecnica giusta per la situazione. Se ti blocchi, torna ai singoli, poi alle coppie, poi alle linee. Il Sudoku è pazienza organizzata.",
                    tip = "Nero è fiero di te. Miao!"
                )
            ),
            practice = LessonPractice.FindCell(
                prompt = "Tocca la cella di un Hidden Single in questa griglia.",
                board = BOARD_B,
                technique = LogicalEngine.Technique.HIDDEN_SINGLE,
                explanation = "Un numero ha un solo posto possibile nella sua unità: hai trovato l'Hidden Single. Livello completato!"
            )
        )
    )

    fun lesson(id: String): Lesson? = lessons.firstOrNull { it.id == id }

    fun nextLesson(id: String): Lesson? {
        val index = lessons.indexOfFirst { it.id == id }
        return if (index == -1) null else lessons.getOrNull(index + 1)
    }

    fun lessonsForLevel(level: Int): List<Lesson> = lessons.filter { it.level == level }

    fun isUnlocked(lesson: Lesson, completedIds: Set<String>): Boolean {
        if (lesson.order == 1) return true
        val previous = lessons.firstOrNull { it.order == lesson.order - 1 } ?: return true
        return previous.id in completedIds
    }
}
