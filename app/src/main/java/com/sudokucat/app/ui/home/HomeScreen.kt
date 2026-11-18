package com.sudokucat.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudokucat.app.AppContainer
import com.sudokucat.app.data.local.GameEntity
import com.sudokucat.app.data.prefs.UserProfile
import com.sudokucat.app.data.prefs.UserSettings
import com.sudokucat.app.data.repo.StatsData
import com.sudokucat.app.domain.lessons.LessonCatalog
import com.sudokucat.app.ui.components.AppBackground
import com.sudokucat.app.ui.components.CatAvatar
import com.sudokucat.app.ui.components.CatTopBar
import com.sudokucat.app.ui.components.FishLives
import com.sudokucat.app.ui.components.MiniTag
import com.sudokucat.app.ui.components.Pill
import com.sudokucat.app.ui.components.PrimaryActionButton
import com.sudokucat.app.ui.components.ProfileStrip
import com.sudokucat.app.ui.components.SecondaryActionButton
import com.sudokucat.app.ui.components.SoftCard
import com.sudokucat.app.ui.components.StatTile
import com.sudokucat.app.ui.components.formatDuration
import com.sudokucat.app.ui.theme.CatTheme
import com.sudokucat.app.util.CatExpression
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    container: AppContainer,
    onMenu: () -> Unit,
    onOpenCat: () -> Unit,
    onPlay: () -> Unit,
    onGuided: () -> Unit,
    onPhoto: () -> Unit,
    onLearn: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onResume: (Long) -> Unit
) {
    val profile by container.profileRepository.profile.collectAsStateWithLifecycle(initialValue = UserProfile())
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = UserSettings())
    val activeGames by container.gameRepository.observeAllActive()
        .collectAsStateWithLifecycle(initialValue = emptyList<GameEntity>())
    val stats by container.statsRepository.stats.collectAsStateWithLifecycle(initialValue = StatsData())
    val lessonProgress by container.lessonRepository.observeProgress()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val completedLessons = lessonProgress.count { it.completed }
    val active = activeGames.maxByOrNull { it.updatedAt }
    val nextLesson = LessonCatalog.lessons.firstOrNull { lesson ->
        lessonProgress.none { it.lessonId == lesson.id && it.completed }
    } ?: LessonCatalog.lessons.last()
    val dayLesson = LessonCatalog.lessons[LocalDate.now().dayOfYear % LessonCatalog.lessons.size]

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            CatTopBar(
                title = greeting(),
                subtitle = "Sudoku Cat · con Nero",
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
                    ResumeCard(game = active, onResume = { onResume(active.id) })
                    Spacer(Modifier.height(12.dp))
                }

                SoftCard(padding = 18.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Una griglia alla volta",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Nero ha preparato una nuova avventura. Scegli come vuoi giocare.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CatTheme.colors.muted
                            )
                        }
                        CatAvatar(
                            modifier = Modifier.size(86.dp),
                            expression = if (active != null) CatExpression.CURIOUS else CatExpression.HAPPY,
                            animated = settings.catEnabled
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    PrimaryActionButton(
                        text = if (active != null) "Nuova partita classica" else "Gioca ora",
                        onClick = onPlay,
                        icon = Icons.Rounded.PlayArrow
                    )
                    Spacer(Modifier.height(8.dp))
                    SecondaryActionButton(
                        text = "Gioca con AI · Nero ti guida",
                        onClick = onGuided,
                        icon = Icons.Rounded.SmartToy
                    )
                }
                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Vittorie", stats.wins.toString(), Modifier.weight(1f), Icons.Rounded.EmojiEvents, CatTheme.colors.success)
                    StatTile("Miglior tempo", stats.bestTime?.let { formatDuration(it) } ?: "—", Modifier.weight(1f), Icons.Rounded.Timer, MaterialTheme.colorScheme.primary)
                    StatTile("Serie", "${profile.streak} g", Modifier.weight(1f), Icons.Rounded.AutoStories, CatTheme.colors.fishDark)
                }
                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(
                        title = "Risolvi da foto",
                        subtitle = "Scatta o scegli",
                        icon = Icons.Rounded.PhotoCamera,
                        color = CatTheme.colors.mint,
                        modifier = Modifier.weight(1f),
                        onClick = onPhoto
                    )
                    QuickAction(
                        title = "Impara Sudoku",
                        subtitle = "Percorso guidato",
                        icon = Icons.Rounded.School,
                        color = CatTheme.colors.lavender,
                        modifier = Modifier.weight(1f),
                        onClick = onLearn
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(
                        title = "Statistiche",
                        subtitle = "I tuoi progressi",
                        icon = Icons.Rounded.BarChart,
                        color = CatTheme.colors.pink,
                        modifier = Modifier.weight(1f),
                        onClick = onStats
                    )
                    QuickAction(
                        title = "Impostazioni",
                        subtitle = "Tema, voce, AI",
                        icon = Icons.Rounded.Settings,
                        color = CatTheme.colors.fish,
                        modifier = Modifier.weight(1f),
                        onClick = onSettings
                    )
                }
                Spacer(Modifier.height(14.dp))

                SoftCard(onClick = onLearn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Il tuo percorso", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(8.dp))
                                MiniTag("$completedLessons/${LessonCatalog.lessons.size}")
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (completedLessons == 0) "Inizia dalla lezione «${nextLesson.title}»"
                                else "Prossima tappa: ${nextLesson.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CatTheme.colors.muted
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { completedLessons.toFloat() / LessonCatalog.lessons.size },
                                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CatTheme.colors.fish.copy(alpha = 0.25f)
                        ) {
                            Icon(
                                Icons.Rounded.AutoStories,
                                contentDescription = null,
                                tint = CatTheme.colors.fishDark,
                                modifier = Modifier.padding(10.dp).size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tecnica del giorno", style = MaterialTheme.typography.labelMedium, color = CatTheme.colors.muted)
                            Text(dayLesson.title, style = MaterialTheme.typography.titleMedium)
                            if (dayLesson.technique != null) {
                                Text(
                                    dayLesson.technique.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CatTheme.colors.muted
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ripassala con Nero nel percorso Impara Sudoku.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onLearn)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "Sudoku Cat funziona offline. Le spiegazioni AI usano Gemini solo se configuri una chiave.",
                    style = MaterialTheme.typography.labelSmall,
                    color = CatTheme.colors.muted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ResumeCard(game: GameEntity, onResume: () -> Unit) {
    SoftCard(
        onClick = onResume,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(text = "Partita in corso", icon = Icons.Rounded.PlayArrow)
                    Spacer(Modifier.width(6.dp))
                    MiniTag(if (game.modeEnum.name == "GUIDED") "AI" else "Classica")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Difficoltà ${game.difficultyEnum.label}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${formatDuration(game.elapsedSeconds)} · ${game.valueBoard.count { it != 0 }}/81 celle · ${game.mistakes} errori",
                    style = MaterialTheme.typography.bodySmall,
                    color = CatTheme.colors.muted
                )
                Spacer(Modifier.height(8.dp))
                FishLives(lives = (3 - game.mistakes).coerceAtLeast(0))
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Riprendi",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.22f)) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(9.dp).size(20.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = CatTheme.colors.muted)
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..12 -> "Buongiorno"
    in 13..17 -> "Buon pomeriggio"
    else -> "Buonasera"
}
