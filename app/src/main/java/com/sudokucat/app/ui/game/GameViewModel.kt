package com.sudokucat.app.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudokucat.app.AppContainer
import com.sudokucat.app.ai.CoachService
import com.sudokucat.app.ai.GameContext
import com.sudokucat.app.data.local.GameEntity
import com.sudokucat.app.data.prefs.UserSettings
import com.sudokucat.app.data.repo.GameRepository
import com.sudokucat.app.domain.engine.LogicalEngine
import com.sudokucat.app.domain.engine.SudokuEngine
import com.sudokucat.app.domain.model.Difficulty
import com.sudokucat.app.domain.model.GameMode
import com.sudokucat.app.domain.model.GameStatus
import com.sudokucat.app.util.CatController
import com.sudokucat.app.util.CatExpression
import com.sudokucat.app.util.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

data class GameUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val gameId: Long = -1L,
    val mode: GameMode = GameMode.CLASSIC,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val givens: IntArray = IntArray(81),
    val values: IntArray = IntArray(81),
    val notes: IntArray = IntArray(81),
    val solution: IntArray = IntArray(81),
    val selected: Int = -1,
    val notesMode: Boolean = false,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val elapsedSeconds: Long = 0,
    val status: GameStatus = GameStatus.PLAYING,
    val paused: Boolean = false,
    val wrongCells: Set<Int> = emptySet(),
    val coachCells: Set<Int> = emptySet(),
    val coachEliminations: Set<Int> = emptySet(),
    val coachTechnique: String? = null,
    val coachSource: String? = null,
    val coachWarning: String? = null,
    val coachBusy: Boolean = false,
    val showWin: Boolean = false,
    val showLose: Boolean = false,
    val xpEarned: Int = 0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
) {
    val lives: Int get() = (3 - mistakes).coerceAtLeast(0)
    val filled: Int get() = values.count { it != 0 }
    val selectedValue: Int get() = if (selected in values.indices) values[selected] else 0

    fun remaining(): IntArray {
        val counts = IntArray(10)
        values.forEach { if (it in 1..9) counts[it]++ }
        return IntArray(10) { index -> if (index == 0) 0 else (9 - counts[index]).coerceAtLeast(0) }
    }
}

class GameViewModel(
    private val container: AppContainer,
    private val gameId: Long
) : ViewModel() {

    private val repository: GameRepository = container.gameRepository
    private val coach: CoachService = container.coachService
    private val cat: CatController = container.catController
    private val sound: SoundManager = container.soundManager

    private val _ui = MutableStateFlow(GameUiState(loading = true, gameId = gameId))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private var entity: GameEntity? = null
    private var settings: UserSettings = UserSettings()
    private var timerJob: Job? = null
    private var xpAwarded = false
    private var completedAt: Long? = null
    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()

    private data class Snapshot(val values: IntArray, val notes: IntArray)

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collectLatest { settings = it }
        }
        viewModelScope.launch {
            val loaded = repository.get(gameId)
            if (loaded == null) {
                _ui.update { it.copy(loading = false, missing = true) }
                return@launch
            }
            entity = loaded
            xpAwarded = loaded.xpAwarded
            completedAt = loaded.completedAt
            _ui.value = GameUiState(
                loading = false,
                gameId = loaded.id,
                mode = loaded.modeEnum,
                difficulty = loaded.difficultyEnum,
                givens = loaded.givenBoard,
                values = loaded.valueBoard,
                notes = loaded.noteMasks,
                solution = loaded.solutionBoard,
                mistakes = loaded.mistakes,
                hintsUsed = loaded.hints,
                elapsedSeconds = loaded.elapsedSeconds,
                status = loaded.statusEnum,
                showWin = loaded.statusEnum == GameStatus.WON,
                showLose = loaded.statusEnum == GameStatus.LOST,
                coachSource = null
            )
            if (loaded.statusEnum == GameStatus.PLAYING) {
                startTimer()
                cat.react(CatExpression.HAPPY)
                if (loaded.hints == 0 && loaded.mistakes == 0) {
                    cat.say(
                        greeting(loaded.modeEnum, loaded.difficultyEnum),
                        CatExpression.CURIOUS,
                        speak = false,
                        autoHide = true
                    )
                }
            }
        }
    }

    fun selectCell(cell: Int) {
        if (cell !in 0..80) return
        val state = _ui.value
        if (state.status != GameStatus.PLAYING) return
        if (!settings.hapticsEnabled || state.selected == cell) {
            // evita vibrazioni ridondanti
        }
        sound.tap()
        _ui.update {
            it.copy(
                selected = cell,
                wrongCells = it.wrongCells - cell,
                coachCells = emptySet(),
                coachEliminations = emptySet()
            )
        }
    }

    fun toggleNotesMode() {
        sound.tap()
        _ui.update { it.copy(notesMode = !it.notesMode) }
    }

    fun enterNumber(number: Int) {
        val state = _ui.value
        if (state.loading || state.status != GameStatus.PLAYING || state.paused) return
        val cell = state.selected
        if (cell !in 0..80) {
            cat.say("Tocca prima una cella della griglia, poi scegli il numero. Miao!", CatExpression.CURIOUS, speak = false)
            return
        }
        if (state.givens[cell] != 0) {
            cat.say("Questo numero è un indizio: non si può cambiare.", CatExpression.CONFUSED, speak = false)
            sound.error()
            return
        }

        if (state.notesMode) {
            if (state.values[cell] != 0) return
            pushUndo(state)
            val notes = state.notes.copyOf()
            notes[cell] = notes[cell] xor (1 shl number)
            _ui.update { it.copy(notes = notes, canUndo = true, canRedo = false, wrongCells = emptySet()) }
            sound.tap()
            persist()
            return
        }

        val correct = state.solution[cell] == number
        if (correct) {
            pushUndo(state)
            val values = state.values.copyOf().also { it[cell] = number }
            val notes = state.notes.copyOf().also { it[cell] = 0 }
            if (settings.autoRemoveNotes) {
                for (peer in SudokuEngine.peers[cell]) {
                    notes[peer] = notes[peer] and (1 shl number).inv()
                }
            }
            _ui.update {
                it.copy(
                    values = values,
                    notes = notes,
                    wrongCells = it.wrongCells - cell,
                    coachCells = emptySet(),
                    coachEliminations = emptySet(),
                    canUndo = true,
                    canRedo = false
                )
            }
            sound.tap()
            cat.react(CatExpression.HAPPY)
            if (settings.hapticsEnabled) {
                // vibrazione gestita a livello di schermata tramite feedback aptico
            }
            checkWin()
            persist()
        } else {
            val mistakes = state.mistakes + 1
            val rejected = settings.highlightErrors
            val values = state.values.copyOf()
            val notes = state.notes.copyOf()
            if (!rejected) {
                values[cell] = number
                notes[cell] = 0
            }
            pushUndo(state)
            _ui.update {
                it.copy(
                    mistakes = mistakes,
                    values = values,
                    notes = notes,
                    wrongCells = if (rejected) it.wrongCells + cell else emptySet(),
                    canUndo = true,
                    canRedo = false
                )
            }
            sound.error()
            cat.react(CatExpression.SAD)
            cat.say(
                wrongNumberMessage(mistakes),
                CatExpression.SAD,
                speak = true,
                autoHide = true
            )
            if (mistakes >= 3) {
                lose()
            }
            persist()
        }
    }

    fun erase() {
        val state = _ui.value
        if (state.status != GameStatus.PLAYING || state.paused) return
        val cell = state.selected
        if (cell !in 0..80 || state.givens[cell] != 0) return
        if (state.values[cell] == 0 && state.notes[cell] == 0) return
        pushUndo(state)
        val values = state.values.copyOf().also { it[cell] = 0 }
        val notes = state.notes.copyOf().also { it[cell] = 0 }
        _ui.update {
            it.copy(
                values = values,
                notes = notes,
                wrongCells = it.wrongCells - cell,
                canUndo = true,
                canRedo = false
            )
        }
        sound.tap()
        persist()
    }

    fun undo() {
        val state = _ui.value
        val snapshot = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(Snapshot(state.values.copyOf(), state.notes.copyOf()))
        _ui.update {
            it.copy(
                values = snapshot.values,
                notes = snapshot.notes,
                wrongCells = emptySet(),
                coachCells = emptySet(),
                coachEliminations = emptySet(),
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        sound.tap()
        persist()
    }

    fun redo() {
        val state = _ui.value
        val snapshot = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(Snapshot(state.values.copyOf(), state.notes.copyOf()))
        _ui.update {
            it.copy(
                values = snapshot.values,
                notes = snapshot.notes,
                wrongCells = emptySet(),
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        sound.tap()
        persist()
    }

    private fun pushUndo(state: GameUiState) {
        undoStack.addLast(Snapshot(state.values.copyOf(), state.notes.copyOf()))
        while (undoStack.size > 80) undoStack.removeFirst()
        redoStack.clear()
    }

    fun togglePause() {
        val state = _ui.value
        if (state.status != GameStatus.PLAYING) return
        _ui.update { it.copy(paused = !it.paused) }
        sound.tap()
        if (!state.paused) {
            cat.say("Pausa rigenerante: io faccio un riposino. Miao…", CatExpression.SLEEPY, speak = false, autoHide = true)
            persist()
        }
    }

    fun requestHint() {
        val state = _ui.value
        if (state.loading || state.status != GameStatus.PLAYING || state.paused || state.coachBusy) return
        val step = LogicalEngine.findNextPlacement(state.values)
        if (step == null || step.primaryCell == null) {
            cat.say(
                "Non trovo un passo logico da qui. Controlla di non aver inserito numeri sbagliati: puoi cancellarli e riprovare.",
                CatExpression.CONFUSED,
                speak = true
            )
            return
        }
        _ui.update {
            it.copy(
                coachBusy = true,
                coachCells = step.highlights.toSet(),
                coachEliminations = step.eliminations.map { e -> e.cell }.toSet(),
                coachTechnique = step.technique.title,
                coachSource = null,
                coachWarning = null
            )
        }
        viewModelScope.launch {
            val reply = coach.explainStep(
                board = state.values,
                step = step,
                context = GameContext(
                    difficulty = state.difficulty.label,
                    mistakes = state.mistakes,
                    lives = state.lives,
                    mode = if (state.mode == GameMode.GUIDED) "Gioca con AI" else "Classica"
                )
            )
            val cell = step.primaryCell
            val value = step.primaryValue
            val current = _ui.value
            if (cell != null && value != null && current.values[cell] == 0) {
                pushUndo(current)
                val values = current.values.copyOf().also { it[cell] = value }
                val notes = current.notes.copyOf().also { it[cell] = 0 }
                if (settings.autoRemoveNotes) {
                    for (peer in SudokuEngine.peers[cell]) {
                        notes[peer] = notes[peer] and (1 shl value).inv()
                    }
                }
                _ui.update {
                    it.copy(
                        values = values,
                        notes = notes,
                        hintsUsed = it.hintsUsed + 1,
                        selected = cell,
                        coachBusy = false,
                        coachCells = step.highlights.toSet(),
                        coachTechnique = reply.technique ?: step.technique.title,
                        coachSource = reply.source.label,
                        coachWarning = reply.warning,
                        canUndo = true,
                        canRedo = false
                    )
                }
                cat.say(reply.text, CatExpression.FOCUSED, speak = true)
                checkWin()
                persist()
            } else {
                _ui.update {
                    it.copy(
                        coachBusy = false,
                        coachTechnique = reply.technique ?: step.technique.title,
                        coachSource = reply.source.label,
                        coachWarning = reply.warning
                    )
                }
                cat.say(reply.text, CatExpression.FOCUSED, speak = true)
            }
        }
    }

    fun explainSelectedCell() {
        val state = _ui.value
        if (state.loading || state.status != GameStatus.PLAYING) return
        val cell = state.selected
        if (cell !in 0..80) {
            cat.say("Tocca una cella e poi chiedimi di spiegartela.", CatExpression.CURIOUS, speak = false)
            return
        }
        _ui.update {
            it.copy(
                coachBusy = true,
                coachCells = setOf(cell),
                coachEliminations = emptySet(),
                coachTechnique = "Analisi cella",
                coachSource = null,
                coachWarning = null
            )
        }
        viewModelScope.launch {
            val reply = coach.explainCell(
                board = state.values,
                cell = cell,
                context = GameContext(
                    difficulty = state.difficulty.label,
                    mistakes = state.mistakes,
                    lives = state.lives,
                    mode = if (state.mode == GameMode.GUIDED) "Gioca con AI" else "Classica"
                )
            )
            _ui.update {
                it.copy(
                    coachBusy = false,
                    coachCells = setOf(cell),
                    coachTechnique = "Analisi cella",
                    coachSource = reply.source.label,
                    coachWarning = reply.warning
                )
            }
            cat.say(reply.text, CatExpression.CURIOUS, speak = true)
        }
    }

    fun dismissCoachHighlights() {
        _ui.update {
            it.copy(
                coachCells = emptySet(),
                coachEliminations = emptySet(),
                coachTechnique = null,
                coachSource = null,
                coachWarning = null
            )
        }
    }

    fun clearWrongFlash() {
        if (_ui.value.wrongCells.isNotEmpty()) {
            _ui.update { it.copy(wrongCells = emptySet()) }
        }
    }

    private fun checkWin() {
        val state = _ui.value
        if (state.status != GameStatus.PLAYING) return
        if (state.values.contentEquals(state.solution)) {
            completeWin()
        }
    }

    private fun completeWin() {
        if (_ui.value.status != GameStatus.PLAYING) return
        val state = _ui.value
        val bonus = when (state.difficulty) {
            Difficulty.EASY -> 0
            Difficulty.MEDIUM -> 10
            Difficulty.HARD -> 20
            Difficulty.EXPERT -> 30
        }
        val earned = if (xpAwarded) 0 else max(
            30,
            state.difficulty.xpReward + bonus - state.hintsUsed * 12 - state.mistakes * 8
        )
        completedAt = System.currentTimeMillis()
        _ui.update {
            it.copy(
                status = GameStatus.WON,
                showWin = true,
                xpEarned = earned,
                paused = false,
                coachCells = emptySet(),
                coachEliminations = emptySet()
            )
        }
        sound.win()
        cat.react(CatExpression.EXCITED)
        cat.say(
            "Miao! Griglia completata! Hai usato ${state.hintsUsed} aiuti e ${state.mistakes} errori. " +
                "Le mie tre aringhe preferite sono salve… per ora!",
            CatExpression.EXCITED,
            speak = true,
            autoHide = false
        )
        if (!xpAwarded) {
            xpAwarded = true
            viewModelScope.launch { container.profileRepository.addXp(earned) }
        }
        persist()
    }

    private fun lose() {
        if (_ui.value.status != GameStatus.PLAYING) return
        completedAt = System.currentTimeMillis()
        _ui.update {
            it.copy(
                status = GameStatus.LOST,
                showLose = true,
                paused = false,
                coachCells = emptySet(),
                coachEliminations = emptySet()
            )
        }
        cat.react(CatExpression.SAD)
        cat.say(
            "Miao… le tre aringhe sono finite. Non è un addio: è solo una pausa. " +
                "Riprova con calma, un numero alla volta.",
            CatExpression.SAD,
            speak = true,
            autoHide = false
        )
        persist()
    }

    fun dismissWin() {
        _ui.update { it.copy(showWin = false) }
    }

    fun dismissLose() {
        _ui.update { it.copy(showLose = false) }
    }

    fun abandon() {
        if (_ui.value.status == GameStatus.PLAYING) {
            _ui.update { it.copy(status = GameStatus.LOST, paused = true, showLose = false) }
            persist()
        }
    }

    fun saveNow() {
        persist()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val state = _ui.value
                if (state.loading || state.paused || state.status != GameStatus.PLAYING) continue
                _ui.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                if (_ui.value.elapsedSeconds % 20L == 0L) persist()
            }
        }
    }

    private fun persist() {
        val base = entity ?: return
        val state = _ui.value
        if (state.missing) return
        val updated = base.copy(
            values = SudokuEngine.serialize(state.values),
            notes = state.notes.joinToString(","),
            mistakes = state.mistakes,
            hints = state.hintsUsed,
            elapsedSeconds = state.elapsedSeconds,
            status = state.status.name,
            xpAwarded = xpAwarded,
            updatedAt = System.currentTimeMillis(),
            completedAt = completedAt
        )
        entity = updated
        viewModelScope.launch { runCatching { repository.save(updated) } }
    }

    override fun onCleared() {
        timerJob?.cancel()
        persist()
        super.onCleared()
    }

    private fun greeting(mode: GameMode, difficulty: Difficulty): String {
        val base = when (mode) {
            GameMode.GUIDED -> "Miao! In questa modalità ti spiego ogni passo. Difficoltà ${difficulty.label.lowercase()}."
            else -> "Miao! Nuova griglia ${difficulty.label.lowercase()}. Guarda righe, colonne e quadranti: il primo numero è già lì che ti aspetta."
        }
        return if (Random.nextInt(3) == 0) "$base Tocca una cella e scegli un numero." else base
    }

    private fun wrongNumberMessage(mistakes: Int): String = when (mistakes) {
        1 -> "Ops, quel numero non va lì. Ho perso un'aringa, ma niente panico: guarda di nuovo riga, colonna e quadrante."
        2 -> "Miao… seconda aringa andata. Resta calmo: cancella il dubbio e cerca una cella con un solo candidato."
        else -> "Oh no, ho finito le aringhe. Succede anche ai migliori detective. Riproveremo insieme, un passo alla volta."
    }
}
