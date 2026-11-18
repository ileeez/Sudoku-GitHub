package com.sudokucat.app.ui.learn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sudokucat.app.AppContainer
import com.sudokucat.app.domain.engine.LogicalEngine
import com.sudokucat.app.domain.engine.SudokuEngine
import com.sudokucat.app.domain.lessons.Lesson
import com.sudokucat.app.domain.lessons.LessonCatalog
import com.sudokucat.app.domain.lessons.LessonPractice
import com.sudokucat.app.ui.components.AppBackground
import com.sudokucat.app.ui.components.CatAvatar
import com.sudokucat.app.ui.components.MiniBoard
import com.sudokucat.app.ui.components.MiniTag
import com.sudokucat.app.ui.components.PrimaryActionButton
import com.sudokucat.app.ui.components.SecondaryActionButton
import com.sudokucat.app.ui.components.SoftCard
import com.sudokucat.app.ui.components.SoftDivider
import com.sudokucat.app.ui.components.StarRow
import com.sudokucat.app.ui.components.SudokuBoard
import com.sudokucat.app.ui.theme.CatTheme
import com.sudokucat.app.util.CatExpression

private enum class LessonPhase { READING, PRACTICE, RESULT }

@Composable
fun LessonScreen(
    container: AppContainer,
    lessonId: String,
    onBack: () -> Unit,
    onNextLesson: (String) -> Unit,
    onOpenCat: () -> Unit
) {
    val lesson = LessonCatalog.lesson(lessonId)
    if (lesson == null) {
        AppBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Lezione non trovata", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(20.dp))
                SecondaryActionButton("Torna al percorso", onBack)
            }
        }
        return
    }

    var phase by remember(lesson.id) { mutableStateOf(LessonPhase.READING) }
    var sectionIndex by remember(lesson.id) { mutableIntStateOf(0) }
    var attempts by remember(lesson.id) { mutableIntStateOf(0) }
    var revealed by remember(lesson.id) { mutableStateOf(false) }
    var solved by remember(lesson.id) { mutableStateOf(false) }
    var selectedOption by rememberSaveable(lesson.id) { mutableStateOf<Int?>(null) }
    var wrongCells by remember(lesson.id) { mutableStateOf<Set<Int>>(emptySet()) }
    var saved by remember(lesson.id) { mutableStateOf(false) }
    var firstCompletion by remember(lesson.id) { mutableStateOf(false) }
    var previousStars by remember(lesson.id) { mutableIntStateOf(0) }

    val stars = when {
        !solved -> 0
        revealed || attempts >= 2 -> 1
        attempts == 1 -> 2
        else -> 3
    }
    val expectedCell = remember(lesson.id) {
        if (lesson.practice is LessonPractice.FindCell) findCell(lesson.practice) else null
    }
    val nextLesson = LessonCatalog.nextLesson(lesson.id)

    LaunchedEffect(solved) {
        if (!solved || saved) return@LaunchedEffect
        saved = true
        val previous = runCatching { container.lessonRepository.getProgress(lesson.id) }.getOrNull()
        previousStars = previous?.stars ?: 0
        firstCompletion = previous?.completed != true
        runCatching {
            container.lessonRepository.saveResult(
                lessonId = lesson.id,
                completed = true,
                stars = stars,
                score = 100 - attempts * 15
            )
            if (firstCompletion) container.profileRepository.addXp(lesson.xp)
        }
        container.catController.react(
            if (stars == 3) CatExpression.EXCITED else CatExpression.HAPPY,
            sound = true
        )
        container.catController.say(
            if (stars == 3) "Perfetto! Tre stelle meritate. Miao!"
            else "Lezione completata. Ripassala quando vuoi: ogni tentativo insegna qualcosa.",
            if (stars == 3) CatExpression.EXCITED else CatExpression.HAPPY,
            speak = true,
            autoHide = true
        )
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            LessonTopBar(
                lesson = lesson,
                onBack = onBack,
                onOpenCat = onOpenCat
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                val totalSteps = lesson.sections.size + 2
                val currentStep = when (phase) {
                    LessonPhase.READING -> sectionIndex + 1
                    LessonPhase.PRACTICE -> lesson.sections.size + 1
                    LessonPhase.RESULT -> totalSteps
                }
                LinearProgressIndicator(
                    progress = { currentStep.toFloat() / totalSteps },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
                Spacer(Modifier.height(16.dp))

                when (phase) {
                    LessonPhase.READING -> ReadingSection(
                        lesson = lesson,
                        sectionIndex = sectionIndex,
                        onNext = {
                            if (sectionIndex < lesson.sections.lastIndex) {
                                sectionIndex++
                            } else {
                                phase = LessonPhase.PRACTICE
                                container.catController.say(
                                    "Ora tocca a te. Leggi bene la richiesta: niente fretta.",
                                    CatExpression.FOCUSED,
                                    speak = false
                                )
                            }
                        },
                        onBack = onBack
                    )

                    LessonPhase.PRACTICE -> PracticeSection(
                        lesson = lesson,
                        expectedCell = expectedCell,
                        selectedOption = selectedOption,
                        wrongCells = wrongCells,
                        revealed = revealed,
                        solved = solved,
                        attempts = attempts,
                        onSelectOption = { index ->
                            if (!solved) {
                                selectedOption = index
                                val quiz = lesson.practice as? LessonPractice.Quiz ?: return@PracticeSection
                                if (index == quiz.correctIndex) {
                                    solved = true
                                } else {
                                    attempts++
                                    container.catController.react(CatExpression.CONFUSED)
                                }
                            }
                        },
                        onSelectCell = { cell ->
                            if (!solved) {
                                if (cell == expectedCell) {
                                    solved = true
                                } else {
                                    attempts++
                                    wrongCells = wrongCells + cell
                                    container.catController.react(CatExpression.CONFUSED)
                                }
                            }
                        },
                        onReveal = {
                            revealed = true
                            solved = true
                        },
                        onContinue = { phase = LessonPhase.RESULT }
                    )

                    LessonPhase.RESULT -> ResultSection(
                        lesson = lesson,
                        stars = stars,
                        attempts = attempts,
                        firstCompletion = firstCompletion,
                        previousStars = previousStars,
                        nextLesson = nextLesson,
                        onNextLesson = { nextLesson?.let { onNextLesson(it.id) } },
                        onBack = onBack
                    )
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun LessonTopBar(
    lesson: Lesson,
    onBack: () -> Unit,
    onOpenCat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Torna al percorso",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                lesson.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1
            )
            Text(
                "Livello ${lesson.level} · Lezione ${lesson.order}",
                style = MaterialTheme.typography.labelSmall,
                color = CatTheme.colors.muted
            )
        }
        CatAvatar(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onOpenCat),
            expression = CatExpression.HAPPY
        )
    }
}

@Composable
private fun ReadingSection(
    lesson: Lesson,
    sectionIndex: Int,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val section = lesson.sections[sectionIndex]
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Rounded.School,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                MiniTag("Parte ${sectionIndex + 1} di ${lesson.sections.size}")
                Spacer(Modifier.height(4.dp))
                Text(section.title, style = MaterialTheme.typography.headlineSmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            section.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (section.board != null) {
            Spacer(Modifier.height(14.dp))
            MiniBoard(
                board = SudokuEngine.parse(section.board),
                highlight = section.highlight.toSet()
            )
            if (section.highlight.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Le celle evidenziate sono quelle da osservare in questo esempio.",
                    style = MaterialTheme.typography.labelSmall,
                    color = CatTheme.colors.muted
                )
            }
        }
        if (section.tip != null) {
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CatTheme.colors.fish.copy(alpha = 0.18f)
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(
                        Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = CatTheme.colors.fishDark
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        section.tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    PrimaryActionButton(
        text = if (sectionIndex < lesson.sections.lastIndex) "Avanti" else "Inizia la pratica",
        onClick = onNext
    )
    if (sectionIndex == 0) {
        Spacer(Modifier.height(8.dp))
        SecondaryActionButton("Torna al percorso", onBack)
    }
}

@Composable
private fun PracticeSection(
    lesson: Lesson,
    expectedCell: Int?,
    selectedOption: Int?,
    wrongCells: Set<Int>,
    revealed: Boolean,
    solved: Boolean,
    attempts: Int,
    onSelectOption: (Int) -> Unit,
    onSelectCell: (Int) -> Unit,
    onReveal: () -> Unit,
    onContinue: () -> Unit
) {
    val practice = lesson.practice
    SoftCard {
        MiniTag("Mettiti alla prova", icon = Icons.Rounded.Lightbulb)
        Spacer(Modifier.height(8.dp))
        Text(
            when (practice) {
                is LessonPractice.Quiz -> practice.question
                is LessonPractice.FindCell -> practice.prompt
            },
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(6.dp))
        Text(
            when (practice) {
                is LessonPractice.Quiz -> "Scegli la risposta che ti convince di più."
                is LessonPractice.FindCell -> "Tocca la cella giusta sulla griglia. Se sbagli, niente panico: riprova."
            },
            style = MaterialTheme.typography.bodySmall,
            color = CatTheme.colors.muted
        )
        Spacer(Modifier.height(14.dp))

        when (practice) {
            is LessonPractice.Quiz -> {
                practice.options.forEachIndexed { index, option ->
                    val isSelected = selectedOption == index
                    val isCorrect = solved && index == practice.correctIndex
                    val isWrong = isSelected && !solved
                    SoftCard(
                        color = when {
                            isCorrect -> CatTheme.colors.success.copy(alpha = 0.18f)
                            isWrong -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        borderColor = when {
                            isCorrect -> CatTheme.colors.success
                            isWrong -> MaterialTheme.colorScheme.error
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                        },
                        padding = 14.dp,
                        onClick = if (!solved) {
                            { onSelectOption(index) }
                        } else null
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    ('A'.code + index).toChar().toString(),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                option,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (isCorrect) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = CatTheme.colors.success,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (isWrong) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (practice.board != null) {
                    Spacer(Modifier.height(4.dp))
                    MiniBoard(
                        board = SudokuEngine.parse(practice.board),
                        highlight = practice.highlight.toSet()
                    )
                }
            }

            is LessonPractice.FindCell -> {
                val board = remember(practice.board) { SudokuEngine.parse(practice.board) }
                SudokuBoard(
                    givens = board,
                    values = board,
                    notes = IntArray(81),
                    selected = -1,
                    onCellClick = onSelectCell,
                    wrongCells = wrongCells,
                    coachCells = if (revealed && expectedCell != null) setOf(expectedCell) else emptySet(),
                    enabled = !solved
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tocca una cella per rispondere.",
                    style = MaterialTheme.typography.labelSmall,
                    color = CatTheme.colors.muted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (solved) {
        Spacer(Modifier.height(12.dp))
        val explanation = when (practice) {
            is LessonPractice.Quiz -> practice.explanation
            is LessonPractice.FindCell -> practice.explanation
        }
        SoftCard(
            color = CatTheme.colors.success.copy(alpha = 0.14f),
            borderColor = CatTheme.colors.success.copy(alpha = 0.55f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CatAvatar(modifier = Modifier.size(46.dp), expression = CatExpression.HAPPY)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (revealed) "Ecco la soluzione" else "Esatto!",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryActionButton("Continua", onClick = onContinue, icon = Icons.Rounded.Check)
    } else {
        if (attempts > 0) {
            Spacer(Modifier.height(12.dp))
            SoftCard(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (attempts == 1) "Non ancora. Guarda bene riga, colonna e quadrante."
                        else "Ci sei quasi. Vuoi un aiuto?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        if (attempts >= 1) {
            Spacer(Modifier.height(10.dp))
            SecondaryActionButton(
                if (revealed) "Soluzione mostrata" else "Mostrami la soluzione",
                onClick = onReveal,
                icon = Icons.Rounded.Lightbulb
            )
        }
    }
}

@Composable
private fun ResultSection(
    lesson: Lesson,
    stars: Int,
    attempts: Int,
    firstCompletion: Boolean,
    previousStars: Int,
    nextLesson: Lesson?,
    onNextLesson: () -> Unit,
    onBack: () -> Unit
) {
    SoftCard(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CatAvatar(modifier = Modifier.size(110.dp), expression = CatExpression.EXCITED)
            Spacer(Modifier.height(8.dp))
            Text(
                if (stars == 3) "Lezione perfetta!" else "Lezione completata!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                lesson.title,
                style = MaterialTheme.typography.bodyMedium,
                color = CatTheme.colors.muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            StarRow(stars = stars, size = 32.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                if (attempts == 0) "Nessun tentativo sprecato." else "$attempts tentativi prima della soluzione.",
                style = MaterialTheme.typography.labelMedium,
                color = CatTheme.colors.muted
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ricompensa", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            MiniTag(if (firstCompletion) "+${lesson.xp} XP" else "XP già ottenuti")
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (firstCompletion) "Hai guadagnato ${lesson.xp} XP per il tuo profilo."
            else "La lezione era già completata: il ripasso non aggiunge XP, ma tiene la mente affilata.",
            style = MaterialTheme.typography.bodySmall,
            color = CatTheme.colors.muted
        )
        if (previousStars > 0) {
            Spacer(Modifier.height(8.dp))
            SoftDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                "Il tuo record precedente era di $previousStars stelle. Conta il risultato migliore.",
                style = MaterialTheme.typography.labelSmall,
                color = CatTheme.colors.muted
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    SoftCard {
        Text("Cosa hai imparato", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            lesson.sections.joinToString(" ") { it.title } + ". " +
                when (lesson.practice) {
                    is LessonPractice.Quiz -> lesson.practice.explanation
                    is LessonPractice.FindCell -> lesson.practice.explanation
                },
            style = MaterialTheme.typography.bodySmall,
            color = CatTheme.colors.muted
        )
    }
    Spacer(Modifier.height(16.dp))
    if (nextLesson != null) {
        PrimaryActionButton(
            "Prossima lezione: ${nextLesson.title}",
            onClick = onNextLesson
        )
        Spacer(Modifier.height(8.dp))
    }
    SecondaryActionButton("Torna al percorso", onClick = onBack)
}

private fun findCell(practice: LessonPractice.FindCell): Int? {
    val board = SudokuEngine.parse(practice.board)
    val step = when (practice.technique) {
        LogicalEngine.Technique.NAKED_SINGLE -> LogicalEngine.findNakedSingle(board)
        LogicalEngine.Technique.HIDDEN_SINGLE -> LogicalEngine.findHiddenSingle(board)
        LogicalEngine.Technique.NAKED_PAIR -> LogicalEngine.findNakedPair(board)
        LogicalEngine.Technique.HIDDEN_PAIR -> LogicalEngine.findHiddenPair(board)
        LogicalEngine.Technique.POINTING -> LogicalEngine.findPointing(board)
        LogicalEngine.Technique.BOX_LINE -> LogicalEngine.findBoxLine(board)
        LogicalEngine.Technique.X_WING -> LogicalEngine.findXWing(board)
        LogicalEngine.Technique.SWORDFISH -> LogicalEngine.findSwordfish(board)
        LogicalEngine.Technique.AI_VERIFIED -> null
    }
    return step?.primaryCell ?: step?.highlights?.firstOrNull()
}
