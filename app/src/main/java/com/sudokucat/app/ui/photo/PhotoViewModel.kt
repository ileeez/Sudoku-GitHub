package com.sudokucat.app.ui.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudokucat.app.AppContainer
import com.sudokucat.app.domain.engine.LogicalEngine
import com.sudokucat.app.domain.engine.SudokuEngine
import com.sudokucat.app.util.CatExpression
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class PhotoStage { CAPTURE, CROP, REVIEW }

enum class SolutionStatus {
    EMPTY,
    INCOMPLETE,
    CONFLICT,
    NO_SOLUTION,
    MULTIPLE,
    UNIQUE,
    SOLVED
}

data class CropRect(
    val left: Float = 0.035f,
    val top: Float = 0.035f,
    val right: Float = 0.965f,
    val bottom: Float = 0.965f
) {
    fun normalized(): CropRect {
        val l = left.coerceIn(0f, 1f)
        val t = top.coerceIn(0f, 1f)
        val r = right.coerceIn(0f, 1f)
        val b = bottom.coerceIn(0f, 1f)
        val nl = min(l, r - 0.12f).coerceIn(0f, 0.88f)
        val nt = min(t, b - 0.12f).coerceIn(0f, 0.88f)
        val nr = max(r, nl + 0.12f).coerceIn(0.12f, 1f)
        val nb = max(b, nt + 0.12f).coerceIn(0.12f, 1f)
        return CropRect(nl, nt, nr, nb)
    }
}

data class PhotoUiState(
    val stage: PhotoStage = PhotoStage.CAPTURE,
    val sourceBitmap: Bitmap? = null,
    val croppedBitmap: Bitmap? = null,
    val crop: CropRect = CropRect(),
    val grid: IntArray = IntArray(81),
    val originalGivens: IntArray = IntArray(81),
    val selected: Int = -1,
    val conflicts: Set<Int> = emptySet(),
    val status: SolutionStatus = SolutionStatus.EMPTY,
    val solution: IntArray? = null,
    val processing: Boolean = false,
    val statusMessage: String? = null,
    val ocrSource: String? = null,
    val ocrWarning: String? = null,
    val error: String? = null,
    val ocrCount: Int = 0,
    /** Messaggio specifico quando la foto non contiene una griglia di Sudoku leggibile. */
    val ocrError: String? = null,
    val step: LogicalEngine.LogicalStep? = null,
    val coachTitle: String? = null,
    val coachText: String? = null,
    val coachSource: String? = null,
    val coachWarning: String? = null,
    val coachBusy: Boolean = false,
    val hintLevel: Int = 0,
    val aiCells: Set<Int> = emptySet(),
    val elapsedSeconds: Long = 0,
    val solved: Boolean = false,
    val saved: Boolean = false,
    val xpEarned: Int = 0,
    val canUndo: Boolean = false
)

class PhotoViewModel(
    private val container: AppContainer,
    private val context: Context
) : ViewModel() {

    private val recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val _ui = MutableStateFlow(PhotoUiState())
    val ui: StateFlow<PhotoUiState> = _ui.asStateFlow()

    private val history = ArrayDeque<IntArray>()
    private var timerJob: Job? = null
    private var analysisJob: Job? = null
    private var refinementJob: Job? = null

    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            _ui.update { it.copy(processing = true, error = null) }
            val bitmap = withContext(Dispatchers.IO) {
                runCatching { decode(uri) }.getOrNull()
            }
            if (bitmap == null) {
                _ui.update {
                    it.copy(
                        processing = false,
                        error = "Non riesco a leggere l'immagine. Prova con un'altra foto."
                    )
                }
            } else {
                _ui.value = PhotoUiState(
                    stage = PhotoStage.CROP,
                    sourceBitmap = bitmap,
                    processing = false
                )
            }
        }
    }

    fun updateCrop(rect: CropRect) {
        _ui.update { it.copy(crop = rect.normalized()) }
    }

    /**
     * Ritaglia e riconosce la griglia.
     *
     * Percorso principale: OCR multimodale Gemini (immagine + JSON Schema).
     * Fallback: ML Kit sul dispositivo, sempre disponibile e offline.
     */
    fun applyCrop() {
        val source = _ui.value.sourceBitmap ?: return
        if (_ui.value.processing) return
        val rect = _ui.value.crop
        _ui.update { it.copy(processing = true, error = null) }
        viewModelScope.launch {
            val cropped = withContext(Dispatchers.IO) { crop(source, rect) }
            val imageBytes = withContext(Dispatchers.IO) {
                runCatching { toJpeg(cropped) }.getOrNull()
            }
            // Gemini e ML Kit partono insieme: il riconoscimento locale resta
            // sempre disponibile e non deve aspettare la rete per essere avviato.
            val aiTask = if (imageBytes != null) {
                async(Dispatchers.IO) {
                    runCatching { container.coachService.recognizeGrid(imageBytes) }.getOrNull()
                }
            } else {
                null
            }
            val localTask = async(Dispatchers.IO) {
                runCatching { recognize(cropped) }.getOrNull()
            }
            val aiResult = aiTask?.await()
            val text = localTask.await()
            val aiGrid = aiResult?.grid?.takeIf { grid -> grid.any { it != 0 } }
            val localGrid = text?.let { parseGrid(it, cropped.width, cropped.height) }
            val grid = aiGrid ?: localGrid ?: IntArray(81)
            val sourceLabel = when {
                aiGrid != null -> "Gemini OCR"
                localGrid != null -> "ML Kit locale"
                else -> "Nessun OCR"
            }
            val analysis = withContext(Dispatchers.Default) { analyze(grid) }
            val readCount = grid.count { value -> value != 0 }
            val ocrError = if (readCount == 0) {
                "Non ho trovato una griglia di Sudoku in questa foto. Inquadra tutte le 9 righe e " +
                    "le 9 colonne, senza tagli e con buona luce, oppure inserisci i numeri a mano."
            } else {
                null
            }
            val fewNumbersWarning = if (readCount in 1..16) {
                "Ho letto solo $readCount numeri: la griglia potrebbe essere tagliata o poco nitida. " +
                    "Controlla il ritaglio."
            } else {
                null
            }
            val combinedWarning = listOfNotNull(aiResult?.warning, fewNumbersWarning)
                .distinct()
                .joinToString(" ")
                .takeIf { it.isNotBlank() }
            history.clear()
            _ui.update {
                it.copy(
                    stage = PhotoStage.REVIEW,
                    processing = false,
                    croppedBitmap = cropped,
                    grid = grid,
                    originalGivens = grid.copyOf(),
                    selected = -1,
                    conflicts = analysis.conflicts,
                    status = analysis.status,
                    solution = analysis.solution,
                    ocrCount = readCount,
                    ocrError = ocrError,
                    ocrSource = sourceLabel,
                    ocrWarning = combinedWarning,
                    elapsedSeconds = 0,
                    statusMessage = if (readCount == 0) {
                        "Nessun numero letto: controlla il messaggio qui sotto."
                    } else {
                        "Letti $readCount numeri con $sourceLabel. Controllali e correggi se serve."
                    },
                    canUndo = false
                )
            }
            startTimer()
            container.catController.react(CatExpression.CURIOUS)
        }
    }

    fun selectCell(cell: Int) {
        _ui.update { it.copy(selected = cell) }
    }

    fun setCellValue(value: Int) {
        val state = _ui.value
        val cell = state.selected
        if (cell !in 0..80 || state.solved) return
        if (state.grid[cell] == value) return
        pushHistory(state.grid)
        val next = state.grid.copyOf()
        next[cell] = value
        // Le correzioni aggiornano gli indizi riconosciuti dalla foto; i numeri
        // inseriti in celle vuote restano valori dell'utente e non diventano indizi.
        val clues = state.originalGivens.copyOf()
        if (clues[cell] != 0 || value == 0) clues[cell] = value
        _ui.update {
            it.copy(
                originalGivens = clues,
                aiCells = it.aiCells - cell,
                hintLevel = 0,
                // L'utente sta correggendo a mano: il messaggio "nessuna griglia"
                // non è più pertinente.
                ocrError = null,
                statusMessage = null
            )
        }
        applyGrid(next)
    }

    fun eraseSelected() {
        val state = _ui.value
        if (state.selected !in 0..80) return
        setCellValue(0)
    }

    fun undo() {
        val previous = history.removeLastOrNull() ?: return
        _ui.update {
            it.copy(
                canUndo = history.isNotEmpty(),
                aiCells = emptySet(),
                hintLevel = 0
            )
        }
        applyGrid(previous, push = false)
    }

    fun nextStep() {
        val state = _ui.value
        if (state.processing || state.solved) return
        viewModelScope.launch {
            computeStep(state, hint = false)
        }
    }

    /**
     * Indizio progressivo a 3 livelli:
     * 1) evidenzia il blocco, 2) indica la cella, 3) rivela il numero con spiegazione.
     */
    fun requestHint() {
        val state = _ui.value
        if (state.processing || state.solved) return
        viewModelScope.launch {
            when {
                state.step == null -> {
                    computeStep(state, hint = true)
                    val step = _ui.value.step ?: return@launch
                    sayHintLevelOne(step)
                }

                state.hintLevel == 0 -> {
                    // Riusa il suggerimento già mostrato: non serve una nuova chiamata AI.
                    _ui.update { it.copy(hintLevel = 1) }
                    sayHintLevelOne(state.step)
                }

                state.hintLevel == 1 -> {
                    _ui.update { it.copy(hintLevel = 2) }
                    val cell = state.step?.primaryCell
                    container.catController.say(
                        if (cell != null) {
                            "Indizio 2 di 3: la cella giusta è ${SudokuEngine.coordinate(cell)}. " +
                                "Ora prova a capire quale numero ci va."
                        } else {
                            "Indizio 2 di 3: guarda la cella evidenziata."
                        },
                        CatExpression.FOCUSED,
                        speak = true
                    )
                }

                state.hintLevel == 2 -> revealHintCell(state)

                else -> {
                    _ui.update { it.copy(step = null, hintLevel = 0, coachText = null) }
                    computeStep(_ui.value, hint = true)
                }
            }
        }
    }

    private fun sayHintLevelOne(step: LogicalEngine.LogicalStep) {
        val cell = step.primaryCell ?: return
        container.catController.say(
            "Indizio 1 di 3: guarda il quadrante attorno a ${SudokuEngine.coordinate(cell)}. " +
                "Cerca un numero che può stare in una sola cella.",
            CatExpression.CURIOUS,
            speak = true
        )
    }

    private fun revealHintCell(state: PhotoUiState) {
        val step = state.step ?: return
        val cell = step.primaryCell ?: return
        val value = step.primaryValue ?: return
        if (state.grid[cell] != 0) return
        pushHistory(state.grid)
        val next = state.grid.copyOf()
        next[cell] = value
        _ui.update {
            it.copy(
                hintLevel = 3,
                aiCells = it.aiCells + cell,
                coachText = step.explanation
            )
        }
        applyGrid(next)
        container.catController.say(
            "Indizio 3 di 3: ${SudokuEngine.coordinate(cell)} = $value. ${step.explanation}",
            CatExpression.FOCUSED,
            speak = true
        )
    }

    fun applyStep() {
        val state = _ui.value
        val step = state.step ?: return
        val cell = step.primaryCell
        val value = step.primaryValue
        if (cell == null || value == null) {
            _ui.update {
                it.copy(
                    coachText = "Questa tecnica elimina candidati ma non riempie ancora una cella. " +
                        "Usa le note mentali e continua a cercare un singolo.",
                    step = null,
                    hintLevel = 0
                )
            }
            return
        }
        pushHistory(state.grid)
        val next = state.grid.copyOf()
        next[cell] = value
        _ui.update { it.copy(step = null, hintLevel = 0) }
        applyGrid(next)
    }

    fun revealOne() {
        val state = _ui.value
        if (state.solved || state.processing) return
        val solution = state.solution ?: withContextSolution(state.grid) ?: run {
            _ui.update {
                it.copy(
                    coachText = "Non posso svelare una cella: i numeri attuali non portano a una soluzione coerente. " +
                        "Correggi le celle rosse o torna al ritaglio.",
                    coachSource = "Motore locale",
                    coachWarning = null,
                    hintLevel = 0
                )
            }
            return
        }
        val cell = state.grid.indexOfFirst { it == 0 }
        if (cell == -1) return
        pushHistory(state.grid)
        val next = state.grid.copyOf()
        next[cell] = solution[cell]
        _ui.update { it.copy(aiCells = it.aiCells + cell, hintLevel = 0) }
        applyGrid(next)
        container.catController.say(
            "Ti svelo ${SudokuEngine.coordinate(cell)} = ${solution[cell]}. Ora prova a continuare da solo.",
            CatExpression.CURIOUS,
            speak = false
        )
    }

    /**
     * «Risolvi Sudoku»: completa la griglia e conserva l'elenco delle celle
     * inserite dall'AI, così la UI può evidenziarle.
     */
    fun revealAll() {
        val state = _ui.value
        if (state.solved) return
        refinementJob?.cancel()
        val solution = state.solution ?: withContextSolution(state.grid) ?: run {
            _ui.update {
                it.copy(
                    coachText = "Non posso completare questa griglia: i numeri inseriti non portano a una soluzione coerente.",
                    hintLevel = 0,
                    coachWarning = null
                )
            }
            return
        }
        val ai = (0 until 81).filter { state.grid[it] == 0 }.toSet()
        pushHistory(state.grid)
        _ui.update {
            it.copy(
                grid = solution.copyOf(),
                solution = solution.copyOf(),
                selected = -1,
                conflicts = emptySet(),
                status = SolutionStatus.SOLVED,
                solved = true,
                step = null,
                hintLevel = 0,
                aiCells = ai,
                canUndo = history.isNotEmpty(),
                coachTitle = "Risolvi Sudoku",
                coachText = "Ho inserito ${ai.size} celle con il risolutore locale. " +
                    "Le celle verde acqua sono quelle aggiunte dall'AI: studia i passaggi con calma.",
                coachSource = "Soluzione locale verificata"
            )
        }
        finishSolved()
    }

    fun reset() {
        timerJob?.cancel()
        analysisJob?.cancel()
        refinementJob?.cancel()
        history.clear()
        _ui.value = PhotoUiState()
    }

    fun backToCrop() {
        timerJob?.cancel()
        refinementJob?.cancel()
        _ui.update {
            it.copy(
                stage = PhotoStage.CROP,
                selected = -1,
                coachText = null,
                coachTitle = null,
                step = null,
                hintLevel = 0,
                aiCells = emptySet()
            )
        }
    }

    fun dismissCoach() {
        _ui.update {
            it.copy(
                coachText = null,
                coachWarning = null,
                coachSource = null,
                coachTitle = null,
                hintLevel = 0
            )
        }
    }

    fun setError(message: String?) {
        _ui.update { it.copy(error = message) }
    }

    private suspend fun computeStep(state: PhotoUiState, hint: Boolean) {
        refinementJob?.cancel()
        _ui.update {
            it.copy(coachBusy = true, coachWarning = null, hintLevel = if (hint) it.hintLevel else 0)
        }
        val board = state.grid
        if (SudokuEngine.isSolved(board)) {
            _ui.update {
                it.copy(
                    coachBusy = false,
                    step = null,
                    coachTitle = null,
                    coachText = "La griglia è già completa. Miao, che spettacolo!",
                    coachSource = "Motore locale"
                )
            }
            return
        }
        val local = withContext(Dispatchers.Default) {
            LogicalEngine.findNextPlacement(board) ?: LogicalEngine.findNextStep(board)
        }
        if (local == null) {
            // Nessun passo logico disponibile: in questo caso solo l'AI può
            // proporre una mossa, con il timeout breve impostato in CoachService.
            val structured = runCatching {
                container.coachService.suggestMoveStructured(board, null)
            }.getOrNull()
            publishStep(structured?.let { LogicalStepBuilder.build(it, null, false) }, structured, hint)
            return
        }
        // Il passo locale viene mostrato subito; Gemini lo rifinisce in background
        // senza tenere la UI in attesa della rete.
        publishStep(local, null, hint)
        refinementJob = viewModelScope.launch {
            val structured = runCatching {
                container.coachService.suggestMoveStructured(board, local)
            }.getOrNull()
            if (!isActive || structured == null) return@launch
            if (!_ui.value.grid.contentEquals(board)) return@launch
            val sameAsLocal = local.primaryCell == structured.cell
            val refined = LogicalStepBuilder.build(structured, local, sameAsLocal)
            _ui.update {
                it.copy(
                    step = refined,
                    coachTitle = refined.technique.title,
                    coachText = structured.explanation,
                    coachSource = structured.source.label,
                    coachWarning = structured.warning
                )
            }
        }
    }

    private fun publishStep(
        step: LogicalEngine.LogicalStep?,
        structured: com.sudokucat.app.ai.StructuredMove?,
        hint: Boolean
    ) {
        if (step == null) {
            _ui.update {
                it.copy(
                    coachBusy = false,
                    step = null,
                    coachTitle = null,
                    coachText = "Il motore logico non trova passi semplici su questa griglia. " +
                        "Controlla i numeri riconosciuti oppure usa «Svela una cella».",
                    coachSource = "Motore locale",
                    coachWarning = null,
                    hintLevel = 0
                )
            }
            return
        }
        _ui.update {
            it.copy(
                coachBusy = false,
                step = step,
                coachTitle = step.technique.title,
                coachText = structured?.explanation ?: step.explanation,
                coachSource = structured?.source?.label ?: "Motore locale",
                coachWarning = structured?.warning,
                hintLevel = if (hint) 1 else 0
            )
        }
        container.catController.react(CatExpression.FOCUSED)
    }

    private fun withContextSolution(grid: IntArray): IntArray? =
        SudokuEngine.solve(grid)

    private fun pushHistory(grid: IntArray) {
        history.addLast(grid.copyOf())
        if (history.size > 40) history.removeFirst()
        _ui.update { it.copy(canUndo = true) }
    }

    private fun applyGrid(grid: IntArray, push: Boolean = false) {
        if (push) pushHistory(grid)
        refinementJob?.cancel()
        _ui.update { it.copy(grid = grid.copyOf(), coachBusy = false) }
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            val analysis = withContext(Dispatchers.Default) { analyze(grid) }
            _ui.update {
                it.copy(
                    conflicts = analysis.conflicts,
                    status = analysis.status,
                    solution = analysis.solution,
                    solved = analysis.status == SolutionStatus.SOLVED,
                    canUndo = history.isNotEmpty()
                )
            }
            if (analysis.status == SolutionStatus.SOLVED) finishSolved()
        }
    }

    private fun finishSolved() {
        timerJob?.cancel()
        val state = _ui.value
        if (state.saved) return
        val solution = state.solution ?: return
        val puzzle = state.originalGivens.copyOf()
        val earned = 60
        _ui.update { it.copy(saved = true, xpEarned = earned) }
        container.catController.react(CatExpression.EXCITED, sound = true)
        container.catController.say(
            "Miao! Foto risolta. Hai guadagnato $earned XP: la prossima griglia sarà ancora più facile.",
            CatExpression.EXCITED,
            speak = true
        )
        viewModelScope.launch {
            runCatching {
                container.gameRepository.recordPhotoSession(
                    puzzle = puzzle,
                    solution = solution,
                    elapsedSeconds = state.elapsedSeconds
                )
                container.profileRepository.addXp(earned)
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _ui.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private data class Analysis(
        val status: SolutionStatus,
        val conflicts: Set<Int>,
        val solution: IntArray?
    )

    private fun analyze(grid: IntArray): Analysis {
        val conflicts = SudokuEngine.conflictCells(grid)
        if (conflicts.isNotEmpty()) {
            return Analysis(SolutionStatus.CONFLICT, conflicts, null)
        }
        if (grid.all { it == 0 }) {
            return Analysis(SolutionStatus.EMPTY, emptySet(), null)
        }
        if (SudokuEngine.isSolved(grid)) {
            return Analysis(SolutionStatus.SOLVED, emptySet(), grid.copyOf())
        }
        val count = SudokuEngine.countSolutions(grid, 2)
        return when (count) {
            0 -> Analysis(SolutionStatus.NO_SOLUTION, emptySet(), null)
            1 -> Analysis(SolutionStatus.UNIQUE, emptySet(), SudokuEngine.solve(grid))
            else -> Analysis(SolutionStatus.MULTIPLE, emptySet(), SudokuEngine.solve(grid))
        }
    }

    private fun decode(uri: Uri): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        var sample = 1
        while (max(options.outWidth, options.outHeight) / (sample * 2) > 2400) {
            sample *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        return rotate(raw, orientation)
    }

    private fun rotate(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(-90f)
            else -> return bitmap
        }
        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrDefault(bitmap)
    }

    private fun crop(bitmap: Bitmap, rect: CropRect): Bitmap {
        val left = (rect.left * bitmap.width).roundToInt().coerceIn(0, bitmap.width - 1)
        val top = (rect.top * bitmap.height).roundToInt().coerceIn(0, bitmap.height - 1)
        val right = (rect.right * bitmap.width).roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = (rect.bottom * bitmap.height).roundToInt().coerceIn(top + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    /** Riduce e comprime la foto prima di inviarla a Gemini come `inline_data`. */
    private fun toJpeg(bitmap: Bitmap): ByteArray {
        val longest = max(bitmap.width, bitmap.height)
        val scaled = if (longest > 1280) {
            val ratio = 1280f / longest
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).roundToInt().coerceAtLeast(1),
                (bitmap.height * ratio).roundToInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        return ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 84, output)
            output.toByteArray()
        }
    }

    private suspend fun recognize(bitmap: Bitmap) = suspendCancellableCoroutine { continuation ->
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }

    private fun parseGrid(text: com.google.mlkit.vision.text.Text, width: Int, height: Int): IntArray {
        val grid = IntArray(81)
        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val digits = element.text.filter { it in '1'..'9' }
                    if (digits.isEmpty()) continue
                    val box = element.boundingBox ?: continue
                    val cellWidth = box.width().toFloat() / digits.length
                    digits.forEachIndexed { index, char ->
                        val centerX = box.left + cellWidth * (index + 0.5f)
                        val centerY = box.top + box.height() / 2f
                        val col = (centerX / width * 9).toInt().coerceIn(0, 8)
                        val row = (centerY / height * 9).toInt().coerceIn(0, 8)
                        val cell = row * 9 + col
                        if (grid[cell] == 0) grid[cell] = char.digitToInt()
                    }
                }
            }
        }
        return grid
    }

    override fun onCleared() {
        timerJob?.cancel()
        analysisJob?.cancel()
        refinementJob?.cancel()
        runCatching { recognizer.close() }
        super.onCleared()
    }

    private object LogicalStepBuilder {
        fun build(
            move: com.sudokucat.app.ai.StructuredMove,
            local: LogicalEngine.LogicalStep?,
            sameAsLocal: Boolean
        ): LogicalEngine.LogicalStep = LogicalEngine.LogicalStep(
            technique = if (sameAsLocal) {
                local?.technique ?: LogicalEngine.Technique.AI_VERIFIED
            } else {
                LogicalEngine.Technique.AI_VERIFIED
            },
            placements = mapOf(move.cell to move.value),
            eliminations = if (sameAsLocal) local?.eliminations ?: emptyList() else emptyList(),
            highlights = if (sameAsLocal) {
                local?.highlights ?: listOf(move.cell)
            } else {
                listOf(move.cell)
            },
            explanation = move.explanation
        )
    }
}
