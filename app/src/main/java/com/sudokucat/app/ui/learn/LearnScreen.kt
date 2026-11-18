package com.sudokucat.app.ui.learn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudokucat.app.AppContainer
import com.sudokucat.app.data.prefs.UserProfile
import com.sudokucat.app.domain.lessons.Lesson
import com.sudokucat.app.domain.lessons.LessonCatalog
import com.sudokucat.app.ui.components.AppBackground
import com.sudokucat.app.ui.components.CatAvatar
import com.sudokucat.app.ui.components.CatTopBar
import com.sudokucat.app.ui.components.MiniTag
import com.sudokucat.app.ui.components.ProfileStrip
import com.sudokucat.app.ui.components.SoftCard
import com.sudokucat.app.ui.components.StarRow
import com.sudokucat.app.ui.theme.CatTheme
import com.sudokucat.app.util.CatExpression

@Composable
fun LearnScreen(
    container: AppContainer,
    onMenu: () -> Unit,
    onOpenCat: () -> Unit,
    onOpenLesson: (String) -> Unit
) {
    val profile by container.profileRepository.profile.collectAsStateWithLifecycle(initialValue = UserProfile())
    val progress by container.lessonRepository.observeProgress()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val completedIds = progress.filter { it.completed }.map { it.lessonId }.toSet()
    val starsById = progress.associate { it.lessonId to it.stars }
    val current = LessonCatalog.lessons.firstOrNull { it.id !in completedIds }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            CatTopBar(
                title = "Impara Sudoku",
                subtitle = "Il percorso di Nero, una zampa alla volta",
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

                SoftCard(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            MiniTag("${completedIds.size}/${LessonCatalog.lessons.size} lezioni", icon = Icons.Rounded.School)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (completedIds.isEmpty()) "Si comincia dalle basi"
                                else "Continua la scalata",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                if (current != null) "Prossima tappa: ${current.title}"
                                else "Hai completato tutto il percorso. Miao, che talento!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CatTheme.colors.muted
                            )
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { completedIds.size.toFloat() / LessonCatalog.lessons.size },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        }
                        CatAvatar(
                            modifier = Modifier.size(94.dp),
                            expression = if (completedIds.isEmpty()) CatExpression.CURIOUS else CatExpression.HAPPY
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                LessonCatalog.levels.forEach { level ->
                    val lessons = LessonCatalog.lessonsForLevel(level.index)
                    val levelUnlocked = lessons.any { LessonCatalog.isUnlocked(it, completedIds) }
                    SoftCard(
                        color = if (levelUnlocked) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = levelColor(level.index).copy(alpha = 0.22f)
                            ) {
                                Text(
                                    level.index.toString(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(level.title, style = MaterialTheme.typography.titleMedium)
                                Text(level.subtitle, style = MaterialTheme.typography.labelSmall, color = CatTheme.colors.muted)
                            }
                            val levelCompleted = lessons.count { it.id in completedIds }
                            MiniTag("$levelCompleted/${lessons.size}")
                        }
                        Spacer(Modifier.height(6.dp))
                        LessonPath(
                            lessons = lessons,
                            completedIds = completedIds,
                            starsById = starsById,
                            currentLessonId = current?.id,
                            onOpenLesson = onOpenLesson,
                            levelIndex = level.index
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                SoftCard {
                    Text("Come funziona il percorso", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LearnRule("3 stelle", "Completa un esercizio al primo tentativo per ottenere il massimo.")
                    LearnRule("XP", "Ogni lezione completata dà XP al tuo profilo. Le ripetizioni non gonfiano il punteggio.")
                    LearnRule("Sblocco", "Ogni lezione apre la successiva. Il percorso segue un ordine pensato per non lasciarti indietro.")
                    LearnRule("Nero", "Il gatto ti accompagna con esempi, quiz e piccoli esercizi interattivi.")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LessonPath(
    lessons: List<Lesson>,
    completedIds: Set<String>,
    starsById: Map<String, Int>,
    currentLessonId: String?,
    onOpenLesson: (String) -> Unit,
    levelIndex: Int
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(238.dp)
    ) {
        val w = maxWidth
        val positions = listOf(
            w * 0.16f to 78.dp,
            w * 0.50f to 148.dp,
            w * 0.84f to 78.dp
        )
        val primary = MaterialTheme.colorScheme.primary
        val mutedLine = MaterialTheme.colorScheme.outlineVariant
        Canvas(modifier = Modifier.fillMaxWidth().height(238.dp)) {
            val centers = positions.map { Offset(it.first.toPx(), it.second.toPx() + 32.dp.toPx()) }
            val path = Path().apply {
                moveTo(centers[0].x, centers[0].y)
                cubicTo(
                    centers[0].x + (centers[1].x - centers[0].x) * 0.6f, centers[0].y,
                    centers[1].x, centers[1].y - 80.dp.toPx(),
                    centers[1].x, centers[1].y
                )
                cubicTo(
                    centers[1].x, centers[1].y + 80.dp.toPx(),
                    centers[2].x - (centers[2].x - centers[1].x) * 0.4f, centers[2].y,
                    centers[2].x, centers[2].y
                )
            }
            drawPath(
                path,
                color = mutedLine,
                style = Stroke(
                    width = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 10f))
                )
            )
        }
        lessons.forEachIndexed { index, lesson ->
            val done = lesson.id in completedIds
            val unlocked = LessonCatalog.isUnlocked(lesson, completedIds)
            val isCurrent = lesson.id == currentLessonId
            val pos = positions.getOrElse(index) { positions.last() }
            Column(
                modifier = Modifier
                    .offset(x = pos.first - 44.dp, y = pos.second - 34.dp)
                    .width(88.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable(enabled = unlocked) { onOpenLesson(lesson.id) },
                    shape = CircleShape,
                    color = when {
                        done -> levelColor(levelIndex)
                        !unlocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    border = BorderStroke(
                        if (isCurrent) 3.dp else 1.dp,
                        when {
                            isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            done -> levelColor(levelIndex)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    shadowElevation = if (isCurrent) 3.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            done -> Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Completata",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                            !unlocked -> Icon(
                                Icons.Rounded.Lock,
                                contentDescription = "Bloccata",
                                tint = CatTheme.colors.muted,
                                modifier = Modifier.size(22.dp)
                            )
                            else -> Text(
                                lesson.order.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                if (isCurrent) {
                    CatAvatar(modifier = Modifier.size(34.dp).offset(y = (-2).dp), expression = CatExpression.FOCUSED)
                } else {
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    lesson.title,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color = if (unlocked) MaterialTheme.colorScheme.onSurface else CatTheme.colors.muted.copy(alpha = 0.7f)
                )
                if (done) {
                    StarRow(stars = starsById[lesson.id] ?: 0, size = 10.dp)
                }
            }
        }
    }
}

@Composable
private fun levelColor(index: Int) = when (index) {
    1 -> CatTheme.colors.mint
    2 -> CatTheme.colors.lavender
    3 -> CatTheme.colors.pink
    4 -> CatTheme.colors.fish
    else -> CatTheme.colors.warning
}

@Composable
private fun LearnRule(title: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("•", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodySmall, color = CatTheme.colors.muted)
        }
    }
}
