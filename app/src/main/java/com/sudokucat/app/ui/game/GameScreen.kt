package com.sudokucat.app.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudokucat.app.AppContainer
import com.sudokucat.app.data.prefs.UserSettings
import com.sudokucat.app.domain.model.GameMode
import com.sudokucat.app.domain.model.GameStatus
import com.sudokucat.app.ui.components.AppBackground
import com.sudokucat.app.ui.components.CatAvatar
import com.sudokucat.app.ui.components.CatCompanionCard
import com.sudokucat.app.ui.components.FishFeast
import com.sudokucat.app.ui.components.FishLives
import com.sudokucat.app.ui.components.MiniTag
import com.sudokucat.app.ui.components.NumberPad
import com.sudokucat.app.ui.components.Pill
import com.sudokucat.app.ui.components.PrimaryActionButton
import com.sudokucat.app.ui.components.SecondaryActionButton
import com.sudokucat.app.ui.components.SoftCard
import com.sudokucat.app.ui.components.StatTile
import com.sudokucat.app.ui.components.SudokuBoard
import com.sudokucat.app.ui.components.formatDuration
import com.sudokucat.app.ui.theme.CatTheme
import com.sudokucat.app.util.CatExpression

@Composable
fun GameScreen(
    container: AppContainer,
    gameId: Long,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onNewGame: () -> Unit,
    onOpenChat: (IntArray) -> Unit
) {
    val viewModel: GameViewModel = viewModel(key = "game_$gameId") {
        GameViewModel(container, gameId)
    }
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val catState by container.catController.state.collectAsStateWithLifecycle()
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = UserSettings())
    val haptic = LocalHapticFeedback.current
    val previousMistakes = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(state.mistakes) }

    LaunchedEffect(state.mistakes) {
        if (state.mistakes > previousMistakes.intValue && settings.hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previousMistakes.intValue = state.mistakes
    }

    LaunchedEffect(state.status) {
        if (state.status == GameStatus.WON && settings.hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            GameTopBar(
                state = state,
                showTimer = settings.showTimer,
                onBack = {
                    viewModel.saveNow()
                    onBack()
                },
                onPause = viewModel::togglePause,
                onExplain = viewModel::explainSelectedCell
            )
            if (state.loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }
            if (state.missing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Partita non trovata", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Potrebbe essere stata eliminata.", color = CatTheme.colors.muted)
                        Spacer(Modifier.height(20.dp))
                        SecondaryActionButton("Torna indietro", onBack)
                    }
                }
                return@Column
            }

            GameProgressBar(
                fraction = (state.filled / 81f).coerceIn(0f, 1f),
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(Modifier.height(8.dp))

            // Il pannello di Nero resta ancorato in cima: l'avatar è sempre
            // visibile per tutta la partita, anche mentre si scorre la griglia.
            val bubbleVisible = catState.bubbleVisible && catState.message != null &&
                state.status == GameStatus.PLAYING && !state.paused
            if (settings.catEnabled) {
                CatCompanionCard(
                    expression = catState.expression,
                    message = if (bubbleVisible) catState.message else null,
                    reactionToken = catState.reactionToken,
                    sourceLabel = if (bubbleVisible) state.coachSource else null,
                    warning = if (bubbleVisible) state.coachWarning else null,
                    busy = state.coachBusy && !bubbleVisible,
                    onSpeak = { container.catController.speakCurrent() },
                    onDismiss = {
                        container.catController.dismissBubble()
                        viewModel.dismissCoachHighlights()
                    },
                    onOpenChat = { onOpenChat(state.values) }
                )
                Spacer(Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Box {
                    SudokuBoard(
                        givens = state.givens,
                        values = state.values,
                        notes = state.notes,
                        selected = state.selected,
                        onCellClick = viewModel::selectCell,
                        wrongCells = state.wrongCells,
                        coachCells = state.coachCells,
                        eliminationCells = state.coachEliminations,
                        enabled = state.status == GameStatus.PLAYING && !state.paused
                    )
                    if (state.paused) {
                        PauseOverlay(onResume = viewModel::togglePause)
                    }
                }
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FishLives(lives = state.lives)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.lives > 0) "${state.lives} vite" else "Vite finite",
                        style = MaterialTheme.typography.labelMedium,
                        color = CatTheme.colors.muted
                    )
                    Spacer(Modifier.weight(1f))
                    if (state.coachTechnique != null) {
                        Pill(text = state.coachTechnique!!)
                    } else {
                        Text(
                            "${state.filled}/81",
                            style = MaterialTheme.typography.labelMedium,
                            color = CatTheme.colors.muted
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (state.mode == GameMode.GUIDED) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GuidedAction(
                            icon = Icons.Rounded.SmartToy,
                            label = "Spiega la cella",
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::explainSelectedCell
                        )
                        GuidedAction(
                            icon = Icons.Rounded.Lightbulb,
                            label = "Prossima mossa",
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::requestHint
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                NumberPad(
                    remaining = state.remaining(),
                    notesMode = state.notesMode,
                    onToggleNotes = viewModel::toggleNotesMode,
                    onNumber = viewModel::enterNumber,
                    onErase = viewModel::erase,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    onHint = viewModel::requestHint,
                    enabled = state.status == GameStatus.PLAYING && !state.paused,
                    hintsUsed = state.hintsUsed
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (state.showWin) {
        WinDialog(
            state = state,
            onNewGame = {
                viewModel.dismissWin()
                onNewGame()
            },
            onHome = {
                viewModel.dismissWin()
                onHome()
            }
        )
    }
    if (state.showLose) {
        LoseDialog(
            state = state,
            onRetry = {
                viewModel.dismissLose()
                onNewGame()
            },
            onHome = {
                viewModel.dismissLose()
                onHome()
            }
        )
    }
}

@Composable
private fun GameProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val animated by androidx.compose.animation.core.animateFloatAsState(
        targetValue = fraction,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 450,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "game-progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        )
    }
}

@Composable
private fun GameTopBar(
    state: GameUiState,
    showTimer: Boolean,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onExplain: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Indietro")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.difficulty.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (state.mode == GameMode.GUIDED) "Gioca con AI · Nero" else "Classica",
                style = MaterialTheme.typography.labelSmall,
                color = CatTheme.colors.muted
            )
        }
        if (showTimer) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = formatDuration(state.elapsedSeconds),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        if (state.mode == GameMode.GUIDED && state.status == GameStatus.PLAYING) {
            IconButton(onClick = onExplain) {
                Icon(
                    Icons.Rounded.SmartToy,
                    contentDescription = "Spiega cella",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        IconButton(onClick = onPause) {
            Icon(
                if (state.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = if (state.paused) "Riprendi" else "Pausa"
            )
        }
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CatAvatar(modifier = Modifier.size(120.dp), expression = CatExpression.SLEEPY)
            Text("In pausa", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Il timer è fermo. Nero fa un riposino.",
                style = MaterialTheme.typography.bodyMedium,
                color = CatTheme.colors.muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            SecondaryActionButton("Riprendi", onResume, modifier = Modifier.width(200.dp))
        }
    }
}

@Composable
private fun GuidedAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun WinDialog(state: GameUiState, onNewGame: () -> Unit, onHome: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Griglia completata!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(
                    "Miao! Nero ha mangiato le aringhe rimaste.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CatTheme.colors.muted,
                    textAlign = TextAlign.Center
                )
                FishFeast(remainingLives = state.lives)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Tempo", formatDuration(state.elapsedSeconds), modifier = Modifier.weight(1f))
                    StatTile("Errori", state.mistakes.toString(), modifier = Modifier.weight(1f))
                    StatTile("Aiuti", state.hintsUsed.toString(), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                if (state.xpEarned > 0) {
                    Pill(text = "+${state.xpEarned} XP", icon = Icons.Rounded.Lightbulb)
                }
                Spacer(Modifier.height(16.dp))
                PrimaryActionButton("Nuova griglia", onClick = onNewGame, icon = Icons.Rounded.Refresh)
                Spacer(Modifier.height(8.dp))
                SecondaryActionButton("Torna alla home", onClick = onHome)
            }
        }
    }
}

@Composable
private fun LoseDialog(state: GameUiState, onRetry: () -> Unit, onHome: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CatAvatar(modifier = Modifier.size(120.dp), expression = CatExpression.SAD)
                Text("Le aringhe sono finite", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(
                    "Hai commesso 3 errori. Nessun dramma: anche i migliori detective sbagliano. " +
                        "Riprova con più calma: cerca un Naked Single o un Hidden Single.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CatTheme.colors.muted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Tempo", formatDuration(state.elapsedSeconds), modifier = Modifier.weight(1f))
                    StatTile("Celle", "${state.filled}/81", modifier = Modifier.weight(1f))
                    StatTile("Aiuti", state.hintsUsed.toString(), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                PrimaryActionButton("Riprova", onClick = onRetry, icon = Icons.Rounded.Refresh)
                Spacer(Modifier.height(8.dp))
                SecondaryActionButton("Torna alla home", onClick = onHome)
            }
        }
    }
}
