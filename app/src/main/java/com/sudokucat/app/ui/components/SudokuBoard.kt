package com.sudokucat.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudokucat.app.domain.engine.SudokuEngine
import com.sudokucat.app.ui.theme.CatTheme

/**
 * Griglia Sudoku disegnata a mano con evidenziazioni contestuali.
 *
 * [values] contiene i numeri attuali con 0 nelle celle vuote.
 * [givens] contiene gli indizi non modificabili.
 * [notes] è una maschera di bit per cella.
 */
@Composable
fun SudokuBoard(
    givens: IntArray,
    values: IntArray,
    notes: IntArray,
    selected: Int,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    wrongCells: Set<Int> = emptySet(),
    coachCells: Set<Int> = emptySet(),
    eliminationCells: Set<Int> = emptySet(),
    aiCells: Set<Int> = emptySet(),
    highlightPeers: Boolean = true,
    highlightSameNumber: Boolean = true,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val colors = CatTheme.colors
    val selectedValue = if (selected in 0..80) values[selected] else 0
    val peerSet = if (selected in 0..80 && highlightPeers) SudokuEngine.peers[selected].toSet() else emptySet()
    val gridLine = scheme.outline.copy(alpha = 0.32f)
    val boxLine = scheme.onSurface.copy(alpha = 0.55f)

    // Una sola transizione per l'intera griglia: le celle del suggerimento
    // pulsano insieme senza creare 81 animazioni infinite separate.
    val pulseTransition = rememberInfiniteTransition(label = "board-pulse")
    val coachPulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(920, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coach-pulse"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surface)
    ) {
        val cellDp = maxWidth / 9f
        val valueFont = with(LocalDensity.current) { (cellDp * 0.56f).toSp() }
        val noteFont = with(LocalDensity.current) { (cellDp * 0.22f).toSp() }
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until 9) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    for (col in 0 until 9) {
                        val index = row * 9 + col
                        val given = givens[index] != 0
                        val value = values[index]
                        val isSelected = index == selected
                        val isWrong = index in wrongCells
                        val isCoach = index in coachCells
                        val isElimination = index in eliminationCells
                        val isAi = index in aiCells
                        val isSame = highlightSameNumber && selectedValue != 0 && value == selectedValue && !isSelected
                        val isPeer = index in peerSet && !isSelected
                        BoardCell(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            value = value,
                            given = given,
                            notesMask = notes[index],
                            selectedValue = selectedValue,
                            isSelected = isSelected,
                            isWrong = isWrong,
                            isCoach = isCoach,
                            isElimination = isElimination,
                            isAi = isAi,
                            isSame = isSame,
                            isPeer = isPeer,
                            valueFont = valueFont,
                            noteFont = noteFont,
                            pulse = coachPulse,
                            enabled = enabled,
                            onClick = { onCellClick(index) }
                        )
                    }
                }
            }
        }
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val thin = 1f
            val thick = (w / 9f) * 0.075f
            for (i in 1 until 9) {
                if (i % 3 == 0) continue
                val x = w * i / 9f
                drawLine(gridLine, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, h), thin)
                val y = h * i / 9f
                drawLine(gridLine, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(w, y), thin)
            }
            for (i in 1..2) {
                val x = w * i / 3f
                drawLine(boxLine, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, h), thick)
                val y = h * i / 3f
                drawLine(boxLine, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(w, y), thick)
            }
            drawRoundRect(
                color = boxLine,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                style = Stroke(width = thick)
            )
        }
    }
}

/**
 * Singola cella animata:
 * - il numero appena inserito "salta" in scala con una molla;
 * - lo sfondo cambia colore con una transizione morbida;
 * - una risposta sbagliata fa tremare la cella;
 * - le celle suggerite da Nero pulsano.
 */
@Composable
private fun BoardCell(
    value: Int,
    given: Boolean,
    notesMask: Int,
    selectedValue: Int,
    isSelected: Boolean,
    isWrong: Boolean,
    isCoach: Boolean,
    isElimination: Boolean,
    isAi: Boolean,
    isSame: Boolean,
    isPeer: Boolean,
    valueFont: TextUnit,
    noteFont: TextUnit,
    pulse: Float,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val colors = CatTheme.colors
    val targetBackground = when {
        isWrong -> colors.danger.copy(alpha = 0.22f)
        isCoach -> colors.fish.copy(alpha = 0.30f + 0.22f * pulse)
        isElimination -> colors.warning.copy(alpha = 0.22f)
        isAi -> colors.mint.copy(alpha = 0.42f)
        isSelected -> scheme.primaryContainer
        isSame -> colors.sameNumber
        isPeer -> colors.peerHighlight
        else -> Color.Transparent
    }
    val animatedBackground by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = tween(
            durationMillis = if (isCoach) 140 else 260,
            easing = FastOutSlowInEasing
        ),
        label = "cell-background"
    )

    val contentScale = remember { Animatable(1f) }
    var previousValue by remember { mutableIntStateOf(value) }
    LaunchedEffect(value) {
        if (value != previousValue) {
            previousValue = value
            if (value != 0) {
                contentScale.snapTo(0.35f)
                contentScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            } else {
                contentScale.animateTo(1f, tween(120))
            }
        }
    }

    val shake = remember { Animatable(0f) }
    val shakeAmplitude = with(LocalDensity.current) { 2.4.dp.toPx() }
    LaunchedEffect(isWrong) {
        if (isWrong) {
            repeat(3) {
                shake.animateTo(1f, tween(45))
                shake.animateTo(-1f, tween(45))
            }
            shake.animateTo(0f, tween(70))
        }
    }

    Box(
        modifier = modifier
            .background(animatedBackground)
            .graphicsLayer { translationX = shake.value * shakeAmplitude }
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) {
            Text(
                text = value.toString(),
                fontSize = valueFont,
                lineHeight = valueFont,
                fontWeight = if (given) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isWrong -> colors.danger
                    given -> scheme.onSurface
                    isAi -> colors.success
                    else -> scheme.primary
                },
                modifier = Modifier.graphicsLayer {
                    scaleX = contentScale.value
                    scaleY = contentScale.value
                }
            )
        } else if (notesMask != 0) {
            NotesGrid(
                mask = notesMask,
                selectedValue = selectedValue,
                fontSize = noteFont,
                highlight = scheme.primary,
                muted = colors.muted
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(2.dp)
                    .border(
                        width = 2.dp,
                        color = scheme.primary.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(7.dp)
                    )
            )
        }
    }
}

@Composable
private fun NotesGrid(
    mask: Int,
    selectedValue: Int,
    fontSize: androidx.compose.ui.unit.TextUnit,
    highlight: Color,
    muted: Color
) {
    val digits = SudokuEngine.maskToDigits(mask).toSet()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
    ) {
        for (row in 0 until 3) {
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                for (col in 0 until 3) {
                    val digit = row * 3 + col + 1
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (digit in digits) {
                            Text(
                                text = digit.toString(),
                                fontSize = fontSize,
                                lineHeight = fontSize,
                                color = if (digit == selectedValue) highlight else muted,
                                fontWeight = if (digit == selectedValue) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniBoard(
    board: IntArray,
    modifier: Modifier = Modifier,
    highlight: Set<Int> = emptySet(),
    highlightValue: Int = 0,
    cellSize: androidx.compose.ui.unit.Dp? = null
) {
    val scheme = MaterialTheme.colorScheme
    val colors = CatTheme.colors
    BoxWithConstraints(
        modifier = modifier
            .then(if (cellSize != null) Modifier.size(cellSize * 9f) else Modifier.fillMaxWidth())
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(scheme.surface)
    ) {
        val cell = maxWidth / 9f
        val font = with(LocalDensity.current) { (cell * 0.56f).toSp() }
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until 9) {
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    for (col in 0 until 9) {
                        val index = row * 9 + col
                        val value = board[index]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    when {
                                        index in highlight -> colors.fish.copy(alpha = 0.5f)
                                        highlightValue != 0 && value == highlightValue -> colors.sameNumber
                                        else -> Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (value != 0) {
                                Text(
                                    value.toString(),
                                    fontSize = font,
                                    lineHeight = font,
                                    color = scheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val thin = 0.8f
            for (i in 0 until 10) {
                val x = w * i / 9f
                val y = h * i / 9f
                val width = if (i % 3 == 0) 2.2f else thin
                drawLine(scheme.outline.copy(alpha = 0.45f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, h), width)
                drawLine(scheme.outline.copy(alpha = 0.45f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(w, y), width)
            }
        }
    }
}
