package com.sudokucat.app.ai

import com.sudokucat.app.data.local.AiCacheDao
import com.sudokucat.app.data.local.AiCacheEntity
import com.sudokucat.app.data.prefs.ProfileRepository
import com.sudokucat.app.data.prefs.SettingsRepository
import com.sudokucat.app.domain.engine.LogicalEngine
import com.sudokucat.app.domain.engine.SudokuEngine
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

data class CoachReply(
    val text: String,
    val source: AiSource = AiSources.LOCAL,
    val technique: String? = null,
    val warning: String? = null
) {
    val isAi: Boolean get() = source == AiSources.AI || source == AiSources.CACHE
}

data class GameContext(
    val difficulty: String = "Medio",
    val mistakes: Int = 0,
    val lives: Int = 3,
    val mode: String = "Classica"
)

/** Esito dell'OCR multimodale: griglia oppure null con il motivo del fallback. */
data class GridOcrResult(
    val grid: IntArray?,
    val source: AiSource = AiSources.LOCAL,
    val warning: String? = null
)

/**
 * Mossa suggerita tramite Structured Output.
 * La mossa proposta da Gemini viene sempre verificata con il risolutore locale
 * prima di essere mostrata all'utente.
 */
data class StructuredMove(
    val cell: Int,
    val value: Int,
    val explanation: String,
    val source: AiSource = AiSources.LOCAL,
    val warning: String? = null
)

class CoachService(
    private val gemini: GeminiClient,
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val cacheDao: AiCacheDao
) {
    private val systemPrompt = """
        Sei Nero, un piccolo gattino nero intelligente, curioso, simpatico e paziente.
        Parli italiano naturale, caldo e breve. Usi ogni tanto un "miao" discreto, mai in modo invadente.
        Sei un insegnante di Sudoku: spieghi il perché delle mosse, non solo il numero da inserire.
        Non inventare tecniche o numeri. Usa soltanto i dati forniti. Non rivelare l'intera soluzione.
        Massimo 90 parole. Se l'utente è in difficoltà, rassicuralo e proponi un passo alla volta.
    """.trimIndent()

    private val ocrSystemPrompt = """
        Sei un motore OCR specializzato in Sudoku. Leggi la foto di una griglia 9x9 e
        trascrivi solo le cifre visibili. Non risolvere il Sudoku, non completare celle vuote
        e non inventare numeri: ogni cella vuota o illeggibile deve valere 0.
        La risposta deve rispettare esattamente lo schema JSON richiesto.
    """.trimIndent()

    private val moveSystemPrompt = """
        Sei Nero, un tutor di Sudoku preciso e paziente. Analizzi una griglia incompleta e
        scegli la prossima mossa più semplice e giustificabile con la logica.
        Non indovinare: proponi solo un numero certo per quella cella.
        Rispondi in italiano e rispetta esattamente lo schema JSON richiesto.
    """.trimIndent()

    suspend fun explainStep(
        board: IntArray,
        step: LogicalEngine.LogicalStep,
        context: GameContext,
        question: String? = null
    ): CoachReply {
        val local = localStepExplanation(step, context)
        val cacheKey = key(
            "step",
            SudokuEngine.serialize(board),
            step.technique.name,
            step.primaryCell?.toString().orEmpty(),
            step.primaryValue?.toString().orEmpty(),
            question.orEmpty()
        )
        return askWithFallback(
            cacheKey = cacheKey,
            local = local,
            technique = step.technique.title,
            buildPrompt = {
                """
                Stato della partita: ${context.mode}, difficoltà ${context.difficulty}, ${context.lives} vite, ${context.mistakes} errori.
                Griglia attuale (0 = vuota): ${SudokuEngine.serialize(board)}
                Tecnica rilevata dal motore locale: ${step.technique.title}
                Cella consigliata: ${step.primaryCell?.let(SudokuEngine::coordinate) ?: "nessuna"}
                Numero consigliato: ${step.primaryValue ?: "nessuno"}
                Eliminazioni: ${step.eliminations.joinToString { "${SudokuEngine.coordinate(it.cell)} - ${it.digits.joinToString(",")}" }}
                Ragionamento locale: ${step.explanation}
                ${question?.let { "Domanda dell'utente: $it" }.orEmpty()}
                Spiega con parole semplici perché questa mossa è corretta e quale tecnica la giustifica.
                """.trimIndent()
            }
        )
    }

    suspend fun explainCell(
        board: IntArray,
        cell: Int,
        context: GameContext,
        question: String? = null
    ): CoachReply {
        val local = localCellExplanation(board, cell, context)
        val cacheKey = key("cell", SudokuEngine.serialize(board), cell.toString(), question.orEmpty())
        return askWithFallback(
            cacheKey = cacheKey,
            local = local,
            technique = null,
            buildPrompt = {
                """
                Griglia attuale: ${SudokuEngine.serialize(board)}
                Cella scelta: ${SudokuEngine.coordinate(cell)}
                Candidati: ${SudokuEngine.candidates(board, cell).joinToString(", ").ifBlank { "nessuno" }}
                Spiegazione locale: $local
                ${question?.let { "Domanda: $it" }.orEmpty()}
                Aiuta l'utente a capire cosa osservare, senza completare la griglia.
                """.trimIndent()
            }
        )
    }

    suspend fun chat(
        userMessage: String,
        board: IntArray? = null,
        context: GameContext = GameContext()
    ): CoachReply {
        val local = localChat(userMessage, board, context)
        val cacheKey = key(
            "chat",
            userMessage,
            board?.let(SudokuEngine::serialize).orEmpty(),
            context.difficulty
        )
        return askWithFallback(
            cacheKey = cacheKey,
            local = local,
            technique = null,
            buildPrompt = {
                val boardText = board?.let {
                    "Griglia attuale (0 = vuota): ${SudokuEngine.serialize(it)}"
                }.orEmpty()
                """
                Contesto: modalità ${context.mode}, difficoltà ${context.difficulty}, vite ${context.lives}, errori ${context.mistakes}.
                $boardText
                Messaggio dell'utente: $userMessage
                Rispondi come Nero con un consiglio utile e affettuoso. Se non serve la griglia, non citarla.
                """.trimIndent()
            }
        )
    }

    suspend fun explainPhotoStep(
        board: IntArray,
        step: LogicalEngine.LogicalStep,
        question: String? = null
    ): CoachReply {
        val local = localStepExplanation(step, GameContext(mode = "Analisi foto"))
        val cacheKey = key("photo", SudokuEngine.serialize(board), step.technique.name, question.orEmpty())
        return askWithFallback(
            cacheKey = cacheKey,
            local = local,
            technique = step.technique.title,
            buildPrompt = {
                """
                Griglia riconosciuta dalla foto: ${SudokuEngine.serialize(board)}
                Tecnica: ${step.technique.title}
                Cella: ${step.primaryCell?.let(SudokuEngine::coordinate) ?: "nessuna"}
                Numero: ${step.primaryValue ?: "nessuno"}
                Spiegazione locale: ${step.explanation}
                Spiega all'utente il prossimo passo, con tono amichevole e senza fornire tutta la soluzione.
                """.trimIndent()
            }
        )
    }

    suspend fun explainSolution(
        board: IntArray,
        solution: IntArray,
        filledCells: Int
    ): CoachReply {
        val local = "La soluzione è completa: ho riempito $filledCells celle. " +
            "Le tecniche usate sono quelle che hai visto nel percorso: candidati, singoli, coppie e schemi avanzati. " +
            "Riprova a risolvere la prossima griglia senza svelare subito la soluzione: è così che si impara!"
        val cacheKey = key("solution", SudokuEngine.serialize(board), SudokuEngine.serialize(solution))
        return askWithFallback(
            cacheKey = cacheKey,
            local = local,
            technique = "Soluzione completa",
            buildPrompt = {
                """
                Griglia di partenza: ${SudokuEngine.serialize(board)}
                Soluzione completa: ${SudokuEngine.serialize(solution)}
                Celle riempite: $filledCells
                Spiega in modo breve quali tipi di ragionamento servono per arrivare alla soluzione.
                Non ripetere l'intera griglia.
                """.trimIndent()
            }
        )
    }

    suspend fun testConnection(apiKey: String, model: String): Result<String> =
        gemini.test(apiKey, model)

    /** OCR multimodale: la foto viene inviata a Gemini e la risposta è vincolata allo schema `grid`. */
    suspend fun recognizeGrid(
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): GridOcrResult {
        val settings = settingsRepository.snapshot()
        if (!settings.aiEnabled || !settings.hasApiKey || !settings.geminiOcrEnabled) {
            return GridOcrResult(null)
        }
        if (!profileRepository.canCallAi()) {
            return GridOcrResult(
                null,
                AiSources.LOCAL,
                "Limite giornaliero di richieste AI raggiunto: uso il riconoscimento locale."
            )
        }
        profileRepository.registerAiCall()
        val result = gemini.generateJson(
            apiKey = settings.geminiApiKey,
            model = settings.geminiModel,
            systemPrompt = ocrSystemPrompt,
            userPrompt = """
                Analizza l'immagine del Sudoku fornita. Estrai i numeri presenti e la loro posizione.
                Restituisci un oggetto JSON con il campo 'grid': un array 9x9 dove 0 rappresenta una cella
                vuota e i numeri da 1 a 9 rappresentano le cifre presenti nella foto.
                Regole: non inventare numeri, usa 0 per le celle vuote o illeggibili; mantieni l'orientamento
                della foto; la prima riga del JSON è la prima riga in alto dell'immagine.
                Se la foto non contiene una griglia di Sudoku 9x9, restituisci una griglia di soli zeri.
            """.trimIndent(),
            schema = StructuredOutputs.gridSchema,
            imageBytes = imageBytes,
            imageMimeType = mimeType,
            temperature = 0.05,
            maxOutputTokens = 1000,
            timeoutMs = 8_000L,
            retryOnServerErrors = false
        )
        return result.fold(
            onSuccess = { raw ->
                val grid = StructuredOutputs.parseGrid(raw)
                if (grid == null) {
                    GridOcrResult(
                        null,
                        AiSources.OCR,
                        "Gemini ha risposto in un formato inatteso: uso il riconoscimento locale."
                    )
                } else {
                    GridOcrResult(grid, AiSources.OCR)
                }
            },
            onFailure = { error ->
                GridOcrResult(
                    null,
                    AiSources.OCR,
                    "OCR Gemini non disponibile: uso il riconoscimento locale. ${error.message.orEmpty()}".trim()
                )
            }
        )
    }

    /**
     * Suggerimento con Structured Outputs `{row, col, valore, spiegazione}`.
     * La mossa viene verificata contro il risolutore locale: se Gemini sbaglia,
     * l'app mostra il passo del motore logico e segnala il fallback.
     */
    suspend fun suggestMoveStructured(
        board: IntArray,
        local: LogicalEngine.LogicalStep?
    ): StructuredMove? {
        val fallback = local?.primaryCell?.let { cell ->
            local.primaryValue?.let { value ->
                StructuredMove(cell, value, local.explanation, AiSources.LOCAL)
            }
        }
        if (SudokuEngine.isSolved(board)) return null

        val settings = settingsRepository.snapshot()
        if (!settings.aiEnabled || !settings.hasApiKey) {
            return fallback?.copy(
                warning = if (!settings.hasApiKey) {
                    "Chiave Gemini assente: uso il motore logico locale."
                } else {
                    null
                }
            )
        }
        if (!profileRepository.canCallAi()) {
            return fallback?.copy(
                warning = "Limite giornaliero di richieste AI raggiunto: uso il motore logico locale."
            )
        }
        profileRepository.registerAiCall()
        val solution = SudokuEngine.solve(board)
        val result = gemini.generateJson(
            apiKey = settings.geminiApiKey,
            model = settings.geminiModel,
            systemPrompt = moveSystemPrompt,
            userPrompt = """
                Data la seguente matrice Sudoku 9x9 incompleta: ${SudokuEngine.serialize(board)}
                (0 = cella vuota, righe e colonne indicizzate da 0 a 8).
                Trova LA PROSSIMA mossa più facile ed evidente da compiere.
                Restituisci un JSON con la seguente struttura:
                { "row": int (0-8), "col": int (0-8), "valore": int (1-9),
                  "spiegazione": "Stringa con la motivazione logica in italiano" }
                Non proporre una cella già piena. La spiegazione deve citare la tecnica usata
                (Naked Single, Hidden Single, coppie, X-Wing, ecc.) e il motivo per cui il numero è certo.
            """.trimIndent(),
            schema = StructuredOutputs.moveSchema,
            temperature = 0.15,
            maxOutputTokens = 350,
            timeoutMs = 6_000L,
            retryOnServerErrors = false
        )
        return result.fold(
            onSuccess = { raw ->
                val move = StructuredOutputs.parseMove(raw)
                when {
                    move == null -> fallback?.copy(
                        warning = "Gemini non ha restituito una mossa valida: uso il motore locale."
                    )

                    board[move.cell] != 0 -> fallback?.copy(
                        warning = "Gemini ha indicato una cella già piena: uso il motore locale."
                    )

                    solution != null && solution[move.cell] != move.value -> fallback?.copy(
                        warning = "La mossa proposta da Gemini non è corretta: uso il motore locale verificato."
                    )

                    solution == null && move.value !in SudokuEngine.candidates(board, move.cell) -> fallback?.copy(
                        warning = "La mossa proposta da Gemini non è coerente: uso il motore locale."
                    )

                    else -> StructuredMove(
                        cell = move.cell,
                        value = move.value,
                        explanation = move.explanation,
                        source = AiSources.AI
                    )
                }
            },
            onFailure = { error ->
                fallback?.copy(
                    warning = error.message ?: "Gemini non raggiungibile: uso il suggerimento locale."
                )
            }
        )
    }

    suspend fun clearCache() = cacheDao.clear()

    private suspend fun askWithFallback(
        cacheKey: String,
        local: String,
        technique: String?,
        buildPrompt: () -> String
    ): CoachReply {
        val settings = settingsRepository.snapshot()
        if (!settings.aiEnabled) {
            return CoachReply(local, AiSources.LOCAL, technique)
        }
        if (!settings.hasApiKey) {
            return CoachReply(
                local,
                AiSources.LOCAL,
                technique,
                warning = "Aggiungi la chiave Gemini nelle Impostazioni per le spiegazioni AI."
            )
        }
        val cached = runCatching { cacheDao.get(cacheKey) }.getOrNull()
        if (cached != null && System.currentTimeMillis() - cached.createdAt < 7L * 24 * 60 * 60 * 1000) {
            return CoachReply(cached.response, AiSources.CACHE, technique)
        }
        if (!profileRepository.canCallAi()) {
            return CoachReply(
                local,
                AiSources.LOCAL,
                technique,
                warning = "Limite giornaliero di richieste AI raggiunto. Continuo con il motore locale."
            )
        }
        profileRepository.registerAiCall()
        val result = gemini.generate(
            apiKey = settings.geminiApiKey,
            model = settings.geminiModel,
            systemPrompt = systemPrompt,
            userPrompt = buildPrompt(),
            maxOutputTokens = 500,
            timeoutMs = 9_000L,
            retryOnServerErrors = false
        )
        return result.fold(
            onSuccess = { text ->
                runCatching {
                    cacheDao.put(AiCacheEntity(cacheKey, text, System.currentTimeMillis()))
                }
                CoachReply(text, AiSources.AI, technique)
            },
            onFailure = { error ->
                CoachReply(
                    local,
                    AiSources.LOCAL,
                    technique,
                    warning = error.message ?: "Gemini non raggiungibile: uso il suggerimento locale."
                )
            }
        )
    }

    private fun localStepExplanation(
        step: LogicalEngine.LogicalStep,
        context: GameContext
    ): String {
        val intro = when {
            context.mistakes >= 2 -> "Miao, niente panico: guardiamo un passo per volta. "
            context.lives >= 2 -> "Miao! Ecco cosa vedo. "
            else -> "Ottimo lavoro, continuiamo così. "
        }
        val placement = step.primaryCell?.let { cell ->
            "La prossima mossa è ${SudokuEngine.coordinate(cell)} = ${step.primaryValue}. "
        }.orEmpty()
        return intro + placement + step.explanation
    }

    private fun localCellExplanation(board: IntArray, cell: Int, context: GameContext): String {
        val base = LogicalEngine.explainCell(board, cell)
        return if (context.mistakes > 0) {
            "Miao, succede a tutti di sbagliare. $base"
        } else {
            "Osserviamo insieme. $base"
        }
    }

    private fun localChat(userMessage: String, board: IntArray?, context: GameContext): String {
        val lower = userMessage.lowercase()
        val fallback = when {
            "regole" in lower || "come si gioca" in lower ->
                "Le regole sono semplici: ogni riga, colonna e quadrante 3×3 deve contenere i numeri da 1 a 9 una sola volta. " +
                    "Non serve indovinare: cerca una cella con un solo candidato e parti da lì."
            "suggerimento" in lower || "aiuto" in lower || "bloccato" in lower -> {
                val step = board?.let { LogicalEngine.findNextPlacement(it) }
                val cell = step?.primaryCell
                if (step != null && cell != null) {
                    "Prova a guardare ${SudokuEngine.coordinate(cell)}. " +
                        "Con la tecnica ${step.technique.title}, il numero che resta è ${step.primaryValue}. " +
                        step.explanation
                } else {
                    "Riesci a trovare una cella con un solo candidato? Scrivi a matita i numeri possibili e confronta riga, colonna e quadrante. Se vuoi, tocca una cella e chiedimi di spiegartela."
                }
            }
            "tecnica" in lower || "naked" in lower || "hidden" in lower ->
                "Parti dal Naked Single: se una cella ha un solo candidato, quello è il numero. " +
                    "Poi prova l'Hidden Single: un numero può stare in una sola cella della riga, colonna o quadrante. " +
                    "Le tecniche avanzate arrivano dopo, con calma."
            "stanco" in lower || "difficile" in lower || "triste" in lower ->
                "Miao… nessuna fretta. Anche una sola cella al giorno va benissimo. Respira, guarda la griglia con occhi nuovi e concediti una pausa."
            "ciao" in lower || "hello" in lower ->
                "Miao! Sono Nero. Posso darti un indizio, spiegarti una cella o tenerti compagnia mentre risolvi."
            "grazie" in lower ->
                "Miao! È un piacere. Ora torna a caccia di numeri: sono curioso di vederti finire la griglia."
            else ->
                "Miao, ti ascolto! Se mi dici cosa ti blocca posso aiutarti con un indizio, una tecnica o una spiegazione della griglia."
        }
        return fallback
    }

    private fun key(vararg parts: String): String {
        val raw = parts.joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
