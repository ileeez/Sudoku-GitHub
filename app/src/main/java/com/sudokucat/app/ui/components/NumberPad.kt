package com.sudokucat.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudokucat.app.ui.theme.CatTheme

@Composable
fun NumberPad(
    remaining: IntArray,
    notesMode: Boolean,
    onToggleNotes: () -> Unit,
    onNumber: (Int) -> Unit,
    onErase: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hintsUsed: Int = 0
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (number in 1..9) {
                val left = remaining.getOrElse(number) { 0 }
                NumberKey(
                    number = number,
                    left = left,
                    active = enabled && left > 0,
                    modifier = Modifier.weight(1f),
                    onClick = { onNumber(number) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PadAction(
                icon = Icons.Rounded.Edit,
                label = "Note",
                active = notesMode,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = onToggleNotes
            )
            PadAction(
                icon = Icons.Rounded.Backspace,
                label = "Cancella",
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = onErase
            )
            PadAction(
                icon = Icons.AutoMirrored.Rounded.Undo,
                label = "Indietro",
                enabled = enabled && canUndo,
                modifier = Modifier.weight(1f),
                onClick = onUndo
            )
            PadAction(
                icon = Icons.AutoMirrored.Rounded.Redo,
                label = "Avanti",
                enabled = enabled && canRedo,
                modifier = Modifier.weight(1f),
                onClick = onRedo
            )
            PadAction(
                icon = Icons.Rounded.Lightbulb,
                label = if (hintsUsed > 0) "Aiuto ($hintsUsed)" else "Aiuto",
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = onHint,
                accent = CatTheme.colors.fishDark
            )
        }
    }
}

/**
 * Tasto numerico con micro-animazione di pressione: il tasto si "schiaccia"
 * e torna su con una piccola molla, per rendere il tocco più vivo.
 */
@Composable
private fun NumberKey(
    number: Int,
    left: Int,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "number-key-press"
    )
    Surface(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(13.dp))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = active,
                onClick = onClick
            ),
        shape = RoundedCornerShape(13.dp),
        color = if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number.toString(),
                fontSize = 19.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (active) MaterialTheme.colorScheme.primary else CatTheme.colors.muted.copy(alpha = 0.45f)
            )
            Text(
                text = if (left > 0) left.toString() else "✓",
                fontSize = 8.sp,
                lineHeight = 9.sp,
                color = CatTheme.colors.muted.copy(alpha = if (active) 0.8f else 0.4f)
            )
        }
    }
}

@Composable
private fun PadAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    accent: Color? = null
) {
    val tint = when {
        !enabled -> CatTheme.colors.muted.copy(alpha = 0.35f)
        active -> MaterialTheme.colorScheme.onPrimaryContainer
        accent != null -> accent
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pad-action-press"
    )
    Surface(
        modifier = modifier
            .height(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(13.dp))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(13.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        border = BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp), tint = tint)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.5.sp,
                maxLines = 1,
                color = tint
            )
        }
    }
}
