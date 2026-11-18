package com.sudokucat.app.ui.play

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudokucat.app.AppContainer
import com.sudokucat.app.data.local.GameEntity
import com.sudokucat.app.data.prefs.UserProfile
import com.sudokucat.app.data.prefs.UserSettings
import com.sudokucat.app.domain.model.Difficulty
import com.sudokucat.app.domain.model.GameMode
import com.sudokucat.app.ui.components.AppBackground
import com.sudokucat.app.ui.components.CatAvatar
import com.sudokucat.app.ui.components.CatTopBar
import com.sudokucat.app.ui.components.DifficultySelector
import com.sudokucat.app.ui.components.FishLives
import com.sudokucat.app.ui.components.MiniTag
import com.sudokucat.app.ui.components.Pill
import com.sudokucat.app.ui.components.PrimaryActionButton
import com.sudokucat.app.ui.components.ProfileStrip
import com.sudokucat.app.ui.components.SoftCard
import com.sudokucat.app.ui.components.formatDuration
import com.sudokucat.app.ui.theme.CatPalette
import com.sudokucat.app.ui.theme.CatTheme
import com.sudokucat.app.util.CatExpression
import kotlinx.coroutines.launch

@Composable
fun PlayScreen(
    container: AppContainer,
    onMenu: () -> Unit,
    onOpenCat: () -> Unit,
    onStartGame: (Long) -> Unit
) {
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = UserSettings())
    val profile by container.profileRepository.profile.collectAsStateWithLifecycle(initialValue = UserProfile())
    val active by container.gameRepository.observeActive(GameMode.CLASSIC)
        .collectAsStateWithLifecycle(initialValue = null as GameEntity?)
    var difficulty by remember(settings.defaultDifficulty) { mutableStateOf(settings.defaultDifficulty) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun startNew() {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            runCatching {
                val id = container.gameRepository.createGame(difficulty, GameMode.CLASSIC)
                container.settingsRepository.setDefaultDifficulty(difficulty)
                id
            }.onSuccess { id ->
                busy = false
                onStartGame(id)
            }.onFailure {
                busy = false
                error = "Generazione non riuscita. Riprova: il motore locale è sempre disponibile."
            }
        }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            CatTopBar(
                title = "Gioca",
                subtitle = "Sudoku classico, senza aiuti obbligatori",
                onMenu = onMenu,
                onOpenCat = onOpenCat
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                ProfileStrip(profile)
                Spacer(Modifier.height(14.dp))

                if (active != null) {
                    SoftCard(
                        onClick = { onStartGame(active!!.id) },
                        borderColor = CatPalette.Mint.copy(alpha = 0.7f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Pill(text = "Riprendi", icon = Icons.Rounded.PlayArrow)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Partita ${active!!.difficultyEnum.label.lowercase()}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "${formatDuration(active!!.elapsedSeconds)} · ${active!!.valueBoard.count { it != 0 }}/81 celle",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CatTheme.colors.muted
                                )
                                Spacer(Modifier.height(6.dp))
                                FishLives(lives = (3 - active!!.mistakes).coerceAtLeast(0))
                            }
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = CatPalette.Mint)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nuova griglia", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Ogni puzzle è generato sul dispositivo con soluzione unica garantita.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CatTheme.colors.muted
                            )
                        }
                        CatAvatar(
                            modifier = Modifier.size(78.dp),
                            expression = if (busy) CatExpression.FOCUSED else CatExpression.HAPPY
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    DifficultySelector(selected = difficulty, onSelect = { difficulty = it })
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MiniTag(difficulty.shortLabel)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${difficulty.targetClues} indizi · +${difficulty.xpReward} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CatTheme.colors.muted
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(difficulty.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (active != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Iniziando una nuova partita, quella in corso verrà sostituita.",
                            style = MaterialTheme.typography.labelSmall,
                            color = CatTheme.colors.warning
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    if (busy) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Nero sta preparando la griglia…", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        PrimaryActionButton(
                            text = if (active != null) "Sostituisci e gioca" else "Inizia la partita",
                            onClick = { startNew() },
                            icon = Icons.Rounded.Refresh
                        )
                    }
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, style = MaterialTheme.typography.labelMedium, color = CatTheme.colors.danger)
                    }
                }
                Spacer(Modifier.height(14.dp))

                SoftCard {
                    Text("Come funziona", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    RuleRow("1", "Riempi la griglia", "Ogni riga, colonna e quadrante deve contenere 1–9 una sola volta.")
                    RuleRow("2", "Tre aringhe", "Ogni errore consuma un'aringa-biscotto. A zero, la partita termina.")
                    RuleRow("3", "Note e aiuti", "Usa la matita per i candidati. Concediti un indizio quando sei bloccato.")
                    RuleRow("4", "Salvataggio automatico", "Puoi uscire quando vuoi: la partita riprende esattamente da dove l'hai lasciata.")
                }
                Spacer(Modifier.height(14.dp))

                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("I tuoi tempi", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Miglior tempo: ${if (profile.xp > 0) "in crescita" else "ancora da scrivere"}. " +
                            "La costanza batte la fretta: una griglia al giorno tiene la mente affilata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CatTheme.colors.muted
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RuleRow(number: String, title: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                number,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodySmall, color = CatTheme.colors.muted)
        }
    }
}
