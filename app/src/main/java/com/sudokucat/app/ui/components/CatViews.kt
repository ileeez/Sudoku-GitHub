package com.sudokucat.app.ui.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudokucat.app.ai.CoachService
import com.sudokucat.app.ai.GameContext
import com.sudokucat.app.domain.engine.SudokuEngine
import com.sudokucat.app.ui.theme.CatTheme
import com.sudokucat.app.util.CatController
import com.sudokucat.app.util.CatExpression
import kotlinx.coroutines.launch

@Composable
fun CatSpeechBubble(
    message: String,
    expression: CatExpression,
    onDismiss: () -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
    sourceLabel: String? = null,
    warning: String? = null,
    reactionToken: Long = 0L,
    onOpenChat: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            CatAvatar(
                modifier = Modifier.size(44.dp),
                expression = expression,
                animated = true,
                reactionToken = reactionToken
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Nero",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (sourceLabel != null) {
                        Spacer(Modifier.width(6.dp))
                        MiniTag(sourceLabel)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onSpeak, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Rounded.VolumeUp,
                            contentDescription = "Ascolta",
                            modifier = Modifier.size(16.dp),
                            tint = CatTheme.colors.muted
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Chiudi",
                            modifier = Modifier.size(16.dp),
                            tint = CatTheme.colors.muted
                        )
                    }
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable(enabled = onOpenChat != null) { onOpenChat?.invoke() }
                )
                if (warning != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        warning,
                        style = MaterialTheme.typography.labelSmall,
                        color = CatTheme.colors.warning
                    )
                }
                if (onOpenChat != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tocca per parlare con Nero ›",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Pannello sempre visibile durante la partita: l'avatar di Nero resta anche
 * quando la bolla si chiude, così non "sparisce" a metà conversazione.
 */
@Composable
fun CatCompanionCard(
    expression: CatExpression,
    message: String?,
    modifier: Modifier = Modifier,
    reactionToken: Long = 0L,
    sourceLabel: String? = null,
    warning: String? = null,
    busy: Boolean = false,
    onSpeak: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onOpenChat: (() -> Unit)? = null
) {
    val idle = message.isNullOrBlank()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
        shadowElevation = if (idle) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (idle && onOpenChat != null) Modifier.clickable { onOpenChat() }
                    else Modifier
                )
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CatAvatar(
                modifier = Modifier.size(44.dp),
                expression = expression,
                animated = true,
                reactionToken = reactionToken
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Nero",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!idle && sourceLabel != null) {
                        Spacer(Modifier.width(6.dp))
                        MiniTag(sourceLabel)
                    }
                    Spacer(Modifier.weight(1f))
                    if (!idle) {
                        IconButton(onClick = onSpeak, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Rounded.VolumeUp,
                                contentDescription = "Ascolta",
                                modifier = Modifier.size(16.dp),
                                tint = CatTheme.colors.muted
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Chiudi",
                                modifier = Modifier.size(16.dp),
                                tint = CatTheme.colors.muted
                            )
                        }
                    } else if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (!idle) {
                    Text(
                        text = message.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable(enabled = onOpenChat != null) { onOpenChat?.invoke() }
                    )
                    if (warning != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            warning,
                            style = MaterialTheme.typography.labelSmall,
                            color = CatTheme.colors.warning
                        )
                    }
                    if (onOpenChat != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tocca per parlare con Nero ›",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        if (busy) "Sto osservando la griglia…" else "Sono qui, accanto a te. Tocca per parlare.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CatTheme.colors.muted
                    )
                }
            }
        }
    }
}

@Composable
fun CatChatDialog(
    coachService: CoachService,
    catController: CatController,
    onDismiss: () -> Unit,
    board: IntArray? = null,
    context: GameContext = GameContext()
) {
    val catState by catController.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var sourceLabel by remember { mutableStateOf<String?>(null) }
    var warning by remember { mutableStateOf<String?>(null) }
    var lastUserMessage by remember { mutableStateOf<String?>(null) }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || busy) return
        lastUserMessage = trimmed
        input = ""
        warning = null
        sourceLabel = null
        scope.launch {
            busy = true
            val reply = coachService.chat(trimmed, board, context)
            catController.say(reply.text, CatExpression.CURIOUS, speak = true)
            sourceLabel = reply.source.label
            warning = reply.warning
            busy = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CatAvatar(
                        modifier = Modifier.size(96.dp),
                        expression = catState.expression
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nero", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Il tuo compagno di Sudoku",
                            style = MaterialTheme.typography.labelMedium,
                            color = CatTheme.colors.muted
                        )
                        if (sourceLabel != null) {
                            Spacer(Modifier.height(4.dp))
                            MiniTag(sourceLabel!!)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Chiudi")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp, max = 190.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (lastUserMessage != null) {
                            Text(
                                "Tu: $lastUserMessage",
                                style = MaterialTheme.typography.labelMedium,
                                color = CatTheme.colors.muted
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(
                            text = catState.message ?: "Miao! Chiedimi un indizio, una spiegazione o semplicemente di farti compagnia.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (warning != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                warning!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = CatTheme.colors.warning
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Dammi un indizio", "Spiegami una tecnica", "Sono bloccato").forEach { quick ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable { send(quick) }
                        ) {
                            Text(
                                quick,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.take(300) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Scrivi a Nero…", style = MaterialTheme.typography.bodyMedium) },
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(enabled = !busy) { send(input) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Invia",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (board != null) "Nero vede la tua griglia attuale" else "Offline: risposte locali sempre disponibili",
                        style = MaterialTheme.typography.labelSmall,
                        color = CatTheme.colors.muted
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Ascolta",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { catController.speakCurrent() }
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}
