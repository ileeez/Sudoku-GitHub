package com.sudokucat.app.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudokucat.app.AppContainer
import com.sudokucat.app.data.prefs.UserProfile
import com.sudokucat.app.data.repo.StatsData
import com.sudokucat.app.domain.model.Difficulty
import com.sudokucat.app.domain.model.GameMode
import com.sudokucat.app.domain.model.GameStatus
import com.sudokucat.app.ui.components.AppBackground
import com.sudokucat.app.ui.components.CatAvatar
import com.sudokucat.app.ui.components.CatTopBar
import com.sudokucat.app.ui.components.MiniTag
import com.sudokucat.app.ui.components.ProfileStrip
import com.sudokucat.app.ui.components.SoftCard
import com.sudokucat.app.ui.components.StatTile
import com.sudokucat.app.ui.components.formatAverage
import com.sudokucat.app.ui.components.formatDuration
import com.sudokucat.app.ui.theme.CatPalette
import com.sudokucat.app.ui.theme.CatTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsScreen(
    container: AppContainer,
    onMenu: () -> Unit,
    onOpenCat: () -> Unit
) {
    val stats by container.statsRepository.stats.collectAsStateWithLifecycle(initialValue = StatsData())
    val profile by container.profileRepository.profile.collectAsStateWithLifecycle(initialValue = UserProfile())

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            CatTopBar(
                title = "Statistiche",
                subtitle = "I tuoi progressi, senza giudizio",
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Vittorie", stats.wins.toString(), Modifier.weight(1f), Icons.Rounded.EmojiEvents, CatTheme.colors.success)
                    StatTile("Sconfitte", stats.losses.toString(), Modifier.weight(1f), Icons.Rounded.SentimentDissatisfied, CatTheme.colors.danger)
                    StatTile("Win rate", "${(stats.winRate * 100).toInt()}%", Modifier.weight(1f), Icons.Rounded.BarChart, MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Miglior tempo", stats.bestTime?.let { formatDuration(it) } ?: "—", Modifier.weight(1f), Icons.Rounded.Timer, CatTheme.colors.fishDark)
                    StatTile("Media", formatAverage(stats.averageTime), Modifier.weight(1f), Icons.Rounded.Schedule, MaterialTheme.colorScheme.primary)
                    StatTile("Tempo totale", formatDuration(stats.totalTime), Modifier.weight(1f), Icons.Rounded.Schedule, CatTheme.colors.muted)
                }
                Spacer(Modifier.height(14.dp))

                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vittorie per difficoltà", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Media errori a partita: ${"%.1f".format(Locale.ITALIAN, stats.averageMistakes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CatTheme.colors.muted
                            )
                        }
                        CatAvatar(modifier = Modifier.size(54.dp), expression = com.sudokucat.app.util.CatExpression.HAPPY)
                    }
                    Spacer(Modifier.height(12.dp))
                    val maxWins = (stats.winsByDifficulty.values.maxOrNull() ?: 0).coerceAtLeast(1)
                    Difficulty.entries.forEach { difficulty ->
                        val wins = stats.winsByDifficulty[difficulty] ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                difficulty.shortLabel,
                                modifier = Modifier.width(66.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = CatTheme.colors.muted
                            )
                            LinearProgressIndicator(
                                progress = { wins.toFloat() / maxWins },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(9.dp)
                                    .clip(CircleShape),
                                color = when (difficulty) {
                                    Difficulty.EASY -> CatTheme.colors.success
                                    Difficulty.MEDIUM -> MaterialTheme.colorScheme.primary
                                    Difficulty.HARD -> CatTheme.colors.fishDark
                                    Difficulty.EXPERT -> CatTheme.colors.danger
                                },
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                wins.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.width(22.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MiniTag("Guidate: ${stats.guidedWins}")
                        MiniTag("Da foto: ${stats.photoWins}")
                        MiniTag("Completate: ${stats.completed}")
                    }
                }
                Spacer(Modifier.height(14.dp))

                if (stats.history.isEmpty()) {
                    SoftCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CatAvatar(modifier = Modifier.size(90.dp), expression = com.sudokucat.app.util.CatExpression.CURIOUS)
                            Text("Nessuna partita completata", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Gioca la tua prima griglia: qui troverai tempi, errori e vittorie.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CatTheme.colors.muted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Text(
                        "Ultime partite",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    stats.history.forEach { game ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (game.statusEnum == GameStatus.WON) CatTheme.colors.success.copy(alpha = 0.18f)
                                            else CatTheme.colors.danger.copy(alpha = 0.16f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (game.statusEnum == GameStatus.WON) "✓" else "×",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (game.statusEnum == GameStatus.WON) CatTheme.colors.success else CatTheme.colors.danger
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${game.difficultyEnum.label} · ${modeLabel(game.modeEnum)}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        "${formatHistoryDate(game.updatedAt)} · ${formatDuration(game.elapsedSeconds)} · ${game.mistakes} errori",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CatTheme.colors.muted
                                    )
                                }
                                if (game.modeEnum == GameMode.GUIDED) {
                                    MiniTag("AI", CatPalette.Lavender)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun modeLabel(mode: GameMode): String = when (mode) {
    GameMode.CLASSIC -> "Classica"
    GameMode.GUIDED -> "Guidata"
    GameMode.PHOTO -> "Da foto"
}

private fun formatHistoryDate(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)
    return runCatching {
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
    }.getOrDefault("—")
}
