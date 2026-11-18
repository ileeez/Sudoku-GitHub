package com.sudokucat.app.ui.ai

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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartToy
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudokucat.app.AppContainer
import com.sudokucat.app.data.local.GameEntity
import com.sudokucat.app.data.prefs.UserProfile
import com.sudokucat.app.data.prefs.UserSettings
import com.sudokucat.app.domain.engine.LogicalEngine
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
import com.sudokucat.app.ui.components.SecondaryActionButton
import com.sudokucat.app.ui.components.SoftCard
import com.sudokucat.app.ui.components.formatDuration
import com.sudokucat.app.ui.theme.CatPalette
import com.sudokucat.app.ui.theme.CatTheme
import com.sudokucat.app.util.CatExpression
import kotlinx.coroutines.launch

@Composable
fun AiHubScreen(
    container: AppContainer,
    onMenu: () -> Unit,
    onOpenCat: () -> Unit,
    onStartGame: (Long) -> Unit,
    onSettings: () -> Unit
) {
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = UserSettings())
    val profile by container.profileRepository.profile.collectAsStateWithLifecycle(initialValue = UserProfile())
    val active by container.gameRepository.observeActive(GameMode.GUIDED)
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
                val id = container.gameRepository.createGame(difficulty, GameMode.GUIDED)
                container.settingsRepository.setDefaultDifficulty(difficulty)
                id
            }.onSuccess {
                busy = false
                onStartGame(it)
            }.onFailure {
                busy = false
                error = "Generazione non riuscita. Riprova."
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
                title = "Gioca con AI",
                subtitle = "Nero spiega, il motore locale controlla",
                onMenu = onMenu,
                onOpenCat = onOpenCat
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                SoftCard(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Pill(text = "Modalità guidata", icon = Icons.Rounded.SmartToy)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Nero ti siede accanto",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Ogni mossa ha una spiegazione. Il motore logico locale trova il passo, " +
                                    "Gemini lo racconta con parole calde. Se l'AI non c'è, il gioco continua.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CatTheme.colors.muted
                            )
                        }
                        CatAvatar(modifier = Modifier.size(96.dp), expression = CatExpression.CURIOUS)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MiniTag("Naked Single")
                        MiniTag("Hidden Single")
                        MiniTag("Coppie")
                        MiniTag("X-Wing")
                    }
                }
                Spacer(Modifier.height(14.dp))

                if (active != null) {
                    SoftCard(
                        onClick = { onStartGame(active!!.id) },
                        borderColor = CatPalette.Lavender
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Pill(text = "Riprendi la lezione", icon = Icons.Rounded.PlayArrow)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Partita guidata ${active!!.difficultyEnum.label.lowercase()}",
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
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                SoftCard {
                    Text("Nuova avventura guidata", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    DifficultySelector(selected = difficulty, onSelect = { difficulty = it })
                    Spacer(Modifier.height(8.dp))
                    Text(
                        difficulty.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = CatTheme.colors.muted
                    )
                    if (active != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "La partita guidata in corso verrà sostituita.",
                            style = MaterialTheme.typography.labelSmall,
                            color = CatTheme.colors.warning
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    if (busy) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Preparo la griglia e la lezione…", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        PrimaryActionButton(
                            text = if (active != null) "Sostituisci e inizia" else "Inizia con Nero",
                            onClick = { startNew() },
                            icon = Icons.Rounded.PlayArrow
                        )
                    }
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, style = MaterialTheme.typography.labelMedium, color = CatTheme.colors.danger)
                    }
                }
                Spacer(Modifier.height(14.dp))

                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (settings.hasApiKey && settings.aiEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.Key,
                            contentDescription = null,
                            tint = if (settings.hasApiKey && settings.aiEnabled) CatTheme.colors.success else CatTheme.colors.warning
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                when {
                                    !settings.aiEnabled -> "AI disattivata"
                                    settings.hasApiKey -> "Gemini collegato · ${settings.geminiModel}"
                                    else -> "Motore locale attivo"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                when {
                                    !settings.aiEnabled -> "Nero usa solo il motore locale. Puoi attivare l'AI nelle impostazioni."
                                    settings.hasApiKey -> "Richieste AI oggi: ${profile.aiCallsToday}/40 · cache locale attiva."
                                    else -> "Tutto funziona offline. Aggiungi una chiave Gemini per spiegazioni più naturali."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = CatTheme.colors.muted
                            )
                        }
                    }
                    if (settings.hasApiKey) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (profile.aiCallsToday / 40f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    SecondaryActionButton("Gestisci AI e chiave", onSettings, icon = Icons.Rounded.Key)
                }
                Spacer(Modifier.height(14.dp))

                SoftCard {
                    Text("Le tecniche che Nero conosce", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LogicalEngine.Technique.entries.forEach { technique ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    technique.level.toString(),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(technique.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    technique.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CatTheme.colors.muted
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                SoftCard {
                    Text("Come si gioca", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    GuideRow("1", "Tocca una cella", "Nero ti dice quali candidati restano e cosa osservare.")
                    GuideRow("2", "Chiedi la prossima mossa", "Il motore locale trova un passo dimostrabile e te lo spiega.")
                    GuideRow("3", "Ascolta o leggi", "Puoi far leggere la spiegazione ad alta voce dal gatto.")
                    GuideRow("4", "Continua da solo", "Quando ti senti sicuro, passa alla modalità classica.")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun GuideRow(number: String, title: String, text: String) {
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
