package com.sudokucat.app.ui.photo

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudokucat.app.AppContainer
import com.sudokucat.app.domain.engine.SudokuEngine
import com.sudokucat.app.ui.components.AppBackground
import com.sudokucat.app.ui.components.CatAvatar
import com.sudokucat.app.ui.components.CatTopBar
import com.sudokucat.app.ui.components.MiniTag
import com.sudokucat.app.ui.components.Pill
import com.sudokucat.app.ui.components.PrimaryActionButton
import com.sudokucat.app.ui.components.SecondaryActionButton
import com.sudokucat.app.ui.components.SoftCard
import com.sudokucat.app.ui.components.SoftDivider
import com.sudokucat.app.ui.components.SudokuBoard
import com.sudokucat.app.ui.components.formatDuration
import com.sudokucat.app.ui.theme.CatTheme
import com.sudokucat.app.util.CatExpression
import kotlin.math.min

@Composable
fun PhotoScreen(
    container: AppContainer,
    onMenu: () -> Unit,
    onOpenCat: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: PhotoViewModel = viewModel(key = "photo_session") {
        PhotoViewModel(container, context)
    }
    val state by viewModel.ui.collectAsStateWithLifecycle()
    var showCamera by rememberSaveable { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.loadImage(uri)
    }

    if (showCamera) {
        CameraCaptureScreen(
            onClose = { showCamera = false },
            onCaptured = { uri ->
                showCamera = false
                viewModel.loadImage(uri)
            }
        )
        return
    }

    when (state.stage) {
        PhotoStage.CAPTURE -> CaptureStage(
            state = state,
            onMenu = onMenu,
            onOpenCat = onOpenCat,
            onCamera = { showCamera = true },
            onGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )

        PhotoStage.CROP -> CropStage(
            state = state,
            onCropChange = viewModel::updateCrop,
            onApply = viewModel::applyCrop,
            onCancel = viewModel::reset,
            onOpenCat = onOpenCat
        )

        PhotoStage.REVIEW -> ReviewStage(
            state = state,
            onBackToCrop = viewModel::backToCrop,
            onSelectCell = viewModel::selectCell,
            onNumber = viewModel::setCellValue,
            onErase = viewModel::eraseSelected,
            onUndo = viewModel::undo,
            onNextStep = viewModel::nextStep,
            onHint = viewModel::requestHint,
            onApplyStep = viewModel::applyStep,
            onRevealOne = viewModel::revealOne,
            onRevealAll = viewModel::revealAll,
            onDismissCoach = viewModel::dismissCoach,
            onNewPhoto = viewModel::reset,
            onOpenCat = onOpenCat
        )
    }
}

@Composable
private fun CaptureStage(
    state: PhotoUiState,
    onMenu: () -> Unit,
    onOpenCat: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            CatTopBar(
                title = "Risolvi da foto",
                subtitle = "Scatta, controlla, risolvi",
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
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "La griglia diventa digitale",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Fotografa un Sudoku su carta. Nero può leggerlo con Gemini " +
                                    "(OCR multimodale e JSON Schema) oppure offline con ML Kit. " +
                                    "Poi ti suggerisce la prossima mossa e ti spiega perché.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CatTheme.colors.muted
                            )
                        }
                        CatAvatar(
                            modifier = Modifier.size(88.dp),
                            expression = CatExpression.CURIOUS
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    PrimaryActionButton(
                        "Scatta una foto",
                        onClick = onCamera,
                        icon = Icons.Rounded.CameraAlt
                    )
                    Spacer(Modifier.height(8.dp))
                    SecondaryActionButton(
                        "Scegli dalla galleria",
                        onClick = onGallery,
                        icon = Icons.Rounded.PhotoLibrary
                    )
                }
                Spacer(Modifier.height(14.dp))

                if (state.error != null) {
                    SoftCard(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(state.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                if (state.processing) {
                    SoftCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Sto preparando la foto…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                SoftCard {
                    Text("Come funziona", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    PhotoStep(1, "Scatta o scegli", "Inquadra la griglia dall'alto, con luce uniforme e senza ombre.")
                    PhotoStep(2, "Ritaglia", "Trascina gli angoli per lasciare solo il quadrato 9×9. Aiuta molto il riconoscimento.")
                    PhotoStep(3, "Controlla", "Confronta i numeri riconosciuti con la foto e correggi con la tastiera.")
                    PhotoStep(4, "Risolvi", "Chiedi il prossimo passo, svela una cella o guarda la soluzione completa.")
                }
                Spacer(Modifier.height(14.dp))

                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = CatTheme.colors.mint.copy(alpha = 0.25f)) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = CatTheme.colors.success,
                                modifier = Modifier.padding(8.dp).size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Tutto sul dispositivo", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Il risolutore e il riconoscimento ML Kit funzionano offline. " +
                                    "Con l'OCR Gemini attivo, la foto ritagliata viene inviata a Google solo per " +
                                    "estrarre la griglia; se la rete manca, l'app continua in locale. " +
                                    "Per i suggerimenti viene inviato solo lo stato dei numeri.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CatTheme.colors.muted
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PhotoStep(number: Int, title: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 5.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                number.toString(),
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
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

@Composable
private fun CropStage(
    state: PhotoUiState,
    onCropChange: (CropRect) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    onOpenCat: () -> Unit
) {
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            SimpleTopBar(
                title = "Ritaglia la griglia",
                subtitle = "Trascina gli angoli bianchi",
                onBack = onCancel,
                onOpenCat = onOpenCat
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                val bitmap = state.sourceBitmap
                if (bitmap != null) {
                    CropEditor(
                        bitmap = bitmap,
                        crop = state.crop,
                        onCropChange = onCropChange
                    )
                }
                Spacer(Modifier.height(10.dp))
                SoftCard(padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Crop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Lascia dentro solo il quadrato della griglia: meno margini significano numeri riconosciuti meglio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CatTheme.colors.muted
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                PrimaryActionButton(
                    if (state.processing) "Riconoscimento in corso…" else "Ritaglia e riconosci",
                    onClick = onApply,
                    enabled = !state.processing
                )
                Spacer(Modifier.height(8.dp))
                SecondaryActionButton("Annulla", onClick = onCancel, icon = Icons.Rounded.Close)
                if (state.processing) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Sto leggendo con Gemini e, se serve, con ML Kit sul dispositivo…",
                        style = MaterialTheme.typography.labelSmall,
                        color = CatTheme.colors.muted
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ReviewStage(
    state: PhotoUiState,
    onBackToCrop: () -> Unit,
    onSelectCell: (Int) -> Unit,
    onNumber: (Int) -> Unit,
    onErase: () -> Unit,
    onUndo: () -> Unit,
    onNextStep: () -> Unit,
    onHint: () -> Unit,
    onApplyStep: () -> Unit,
    onRevealOne: () -> Unit,
    onRevealAll: () -> Unit,
    onDismissCoach: () -> Unit,
    onNewPhoto: () -> Unit,
    onOpenCat: () -> Unit
) {
    var confirmSolution by remember { mutableStateOf(false) }
    var showOverlay by rememberSaveable { mutableStateOf(false) }
    val coachCells = remember(state.step, state.hintLevel) {
        val step = state.step ?: return@remember emptySet()
        val cell = step.primaryCell
        when (state.hintLevel) {
            1 -> if (cell != null) {
                SudokuEngine.boxes[SudokuEngine.boxOf(cell) - 1].toSet()
            } else {
                step.highlights.toSet()
            }
            2, 3 -> setOfNotNull(cell)
            else -> (step.placements.keys + step.highlights).toSet()
        }
    }
    val eliminationCells = remember(state.step) {
        state.step?.eliminations?.map { it.cell }?.toSet() ?: emptySet()
    }
    val remaining = remember(state.grid) {
        val counts = IntArray(10)
        state.grid.forEach { if (it in 1..9) counts[it]++ }
        IntArray(10) { index -> if (index == 0) 0 else (9 - counts[index]).coerceAtLeast(0) }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            SimpleTopBar(
                title = if (state.solved) "Foto risolta" else "Griglia riconosciuta",
                subtitle = if (state.solved) "Miao, che bel lavoro" else "Controlla i numeri e correggi",
                onBack = onBackToCrop,
                onOpenCat = onOpenCat
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                StatusCard(state = state)
                Spacer(Modifier.height(10.dp))

                val cropped = state.croppedBitmap
                val solution = state.solution
                if (cropped != null && solution != null && !state.solved) {
                    SoftCard(padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Overlay AR sulla foto", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Disegna i numeri mancanti direttamente sopra la tua griglia cartacea.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CatTheme.colors.muted
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        SecondaryActionButton(
                            if (showOverlay) "Nascondi overlay" else "Mostra overlay AR",
                            onClick = { showOverlay = !showOverlay },
                            icon = Icons.Rounded.Visibility
                        )
                        if (showOverlay) {
                            Spacer(Modifier.height(10.dp))
                            ArOverlay(
                                bitmap = cropped,
                                grid = state.grid,
                                solution = solution
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "I cerchi colorati sono i numeri che mancano. La foto non viene modificata.",
                                style = MaterialTheme.typography.labelSmall,
                                color = CatTheme.colors.muted
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                SudokuBoard(
                    givens = state.originalGivens,
                    values = state.grid,
                    notes = IntArray(81),
                    selected = state.selected,
                    onCellClick = onSelectCell,
                    wrongCells = state.conflicts,
                    coachCells = coachCells,
                    eliminationCells = eliminationCells,
                    aiCells = state.aiCells,
                    enabled = !state.solved
                )
                if (state.aiCells.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CatTheme.colors.mint.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        ) {}
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Celle inserite dall'AI: ${state.aiCells.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CatTheme.colors.muted
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                PhotoNumberPad(
                    remaining = remaining,
                    canUndo = state.canUndo,
                    enabled = !state.solved,
                    onNumber = onNumber,
                    onErase = onErase,
                    onUndo = onUndo
                )
                Spacer(Modifier.height(12.dp))

                if (state.coachText != null) {
                    CoachPhotoCard(
                        state = state,
                        onDismiss = onDismissCoach,
                        onApplyStep = onApplyStep
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (state.solved) {
                    SoftCard(
                        color = CatTheme.colors.success.copy(alpha = 0.14f),
                        borderColor = CatTheme.colors.success.copy(alpha = 0.5f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CatAvatar(modifier = Modifier.size(54.dp), expression = CatExpression.EXCITED)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Griglia completata!", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (state.xpEarned > 0) "Hai guadagnato ${state.xpEarned} XP. Tempo: ${formatDuration(state.elapsedSeconds)}."
                                    else "Soluzione verificata. Tempo: ${formatDuration(state.elapsedSeconds)}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CatTheme.colors.muted
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    PrimaryActionButton("Nuova foto", onClick = onNewPhoto, icon = Icons.Rounded.Refresh)
                } else {
                    PrimaryActionButton(
                        if (state.coachBusy) "Nero sta pensando…" else "Suggerisci mossa",
                        onClick = onNextStep,
                        icon = Icons.Rounded.Lightbulb,
                        enabled = !state.coachBusy
                    )
                    Spacer(Modifier.height(8.dp))
                    SecondaryActionButton(
                        hintButtonLabel(state.hintLevel),
                        onClick = onHint,
                        icon = Icons.Rounded.Visibility
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            SecondaryActionButton(
                                "Svela una cella",
                                onClick = onRevealOne,
                                icon = Icons.Rounded.Visibility
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SecondaryActionButton(
                                "Risolvi Sudoku",
                                onClick = { confirmSolution = true },
                                icon = Icons.Rounded.CheckCircle
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SecondaryActionButton("Nuova foto", onClick = onNewPhoto, icon = Icons.Rounded.Refresh)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmSolution) {
        AlertDialog(
            onDismissRequest = { confirmSolution = false },
            title = { Text("Risolvi il Sudoku?") },
            text = {
                Text(
                    "Nero completerà la griglia con il risolutore locale verificato e colorerà " +
                        "le celle inserite dall'AI. La partita verrà registrata nelle statistiche " +
                        "come risolta con aiuto. Potrai annullare con il pulsante Indietro."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmSolution = false
                    onRevealAll()
                }) { Text("Sì, risolvilo") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSolution = false }) { Text("No, continuo da solo") }
            }
        )
    }
}

private fun hintButtonLabel(level: Int): String = when (level) {
    1 -> "Indizio 2/3 · indica la cella"
    2 -> "Indizio 3/3 · rivela il numero"
    3 -> "Nuovo indizio a 3 livelli"
    else -> "Indizio progressivo 1/3"
}

@Composable
private fun StatusCard(state: PhotoUiState) {
    val (label, accent) = statusInfo(state.status)
    SoftCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(
                        text = label,
                        color = accent.copy(alpha = 0.18f),
                        contentColor = accent,
                        icon = if (state.status == SolutionStatus.CONFLICT ||
                            state.status == SolutionStatus.NO_SOLUTION ||
                            state.ocrError != null
                        ) {
                            Icons.Rounded.Warning
                        } else {
                            Icons.Rounded.CheckCircle
                        }
                    )
                    Spacer(Modifier.width(6.dp))
                    MiniTag("${state.grid.count { it != 0 }}/81 celle")
                    if (state.ocrSource != null) {
                        Spacer(Modifier.width(6.dp))
                        MiniTag(state.ocrSource)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    when (state.status) {
                        SolutionStatus.CONFLICT -> "Le celle rosse hanno numeri ripetuti. Tocca una cella e correggi il valore."
                        SolutionStatus.NO_SOLUTION -> "Con questi numeri non esiste una soluzione. Ricontrolla il riconoscimento o torna al ritaglio."
                        SolutionStatus.MULTIPLE -> "Ci sono più soluzioni possibili: probabilmente manca qualche indizio. Controlla la foto e inseriscilo."
                        SolutionStatus.UNIQUE -> "La griglia è coerente e ha una sola soluzione. Puoi chiedere il prossimo passo a Nero."
                        SolutionStatus.SOLVED -> "Tutti i numeri sono al loro posto. Miao!"
                        SolutionStatus.EMPTY -> "Nessun numero riconosciuto. Inseriscili manualmente con la tastiera."
                        SolutionStatus.INCOMPLETE -> "Griglia parziale: aggiungi i numeri mancanti."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = CatTheme.colors.muted
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatDuration(state.elapsedSeconds),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "tempo",
                    style = MaterialTheme.typography.labelSmall,
                    color = CatTheme.colors.muted
                )
            }
        }
        if (state.statusMessage != null && state.status != SolutionStatus.SOLVED) {
            Spacer(Modifier.height(6.dp))
            Text(
                state.statusMessage,
                style = MaterialTheme.typography.labelSmall,
                color = if (state.ocrError != null) CatTheme.colors.danger else CatTheme.colors.muted
            )
        }
        if (state.ocrError != null) {
            Spacer(Modifier.height(8.dp))
            SoftCard(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(state.ocrError, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (state.ocrWarning != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                state.ocrWarning,
                style = MaterialTheme.typography.labelSmall,
                color = CatTheme.colors.warning
            )
        }
    }
}

@Composable
private fun statusInfo(status: SolutionStatus): Pair<String, Color> = when (status) {
    SolutionStatus.UNIQUE -> "Valida e unica" to CatTheme.colors.success
    SolutionStatus.SOLVED -> "Completa" to CatTheme.colors.success
    SolutionStatus.CONFLICT -> "Conflitti" to CatTheme.colors.danger
    SolutionStatus.NO_SOLUTION -> "Senza soluzione" to CatTheme.colors.danger
    SolutionStatus.MULTIPLE -> "Soluzioni multiple" to CatTheme.colors.warning
    SolutionStatus.EMPTY -> "Vuota" to CatTheme.colors.muted
    SolutionStatus.INCOMPLETE -> "Incompleta" to CatTheme.colors.warning
}

@Composable
private fun PhotoNumberPad(
    remaining: IntArray,
    canUndo: Boolean,
    enabled: Boolean,
    onNumber: (Int) -> Unit,
    onErase: () -> Unit,
    onUndo: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (number in 1..9) {
                val left = remaining.getOrElse(number) { 0 }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = enabled) { onNumber(number) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            number.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (enabled) MaterialTheme.colorScheme.primary else CatTheme.colors.muted
                        )
                        Text(
                            if (left > 0) left.toString() else "✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = CatTheme.colors.muted
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                SecondaryActionButton(
                    "Cancella",
                    onClick = onErase,
                    icon = Icons.Rounded.Backspace
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SecondaryActionButton(
                    "Indietro",
                    onClick = onUndo,
                    icon = Icons.Rounded.Restore
                )
            }
        }
    }
}

@Composable
private fun CoachPhotoCard(
    state: PhotoUiState,
    onDismiss: () -> Unit,
    onApplyStep: () -> Unit
) {
    SoftCard(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            CatAvatar(modifier = Modifier.size(48.dp), expression = CatExpression.FOCUSED)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Nero dice", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Chiudi spiegazione",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (state.step != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MiniTag(state.coachTitle ?: state.step.technique.title)
                        if (state.hintLevel in 1..3) {
                            Spacer(Modifier.width(6.dp))
                            MiniTag("Indizio ${state.hintLevel}/3")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    state.coachText.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (state.hintLevel in 1..3) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (state.hintLevel) {
                            1 -> "Livello 1: osserva il blocco evidenziato. C'è un numero che può stare in una sola cella?"
                            2 -> "Livello 2: la cella è indicata. Prova a dedurre il numero con riga, colonna e quadrante."
                            else -> "Livello 3: numero rivelato. Ripeti il ragionamento per la prossima volta."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (state.coachWarning != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.coachWarning,
                        style = MaterialTheme.typography.labelSmall,
                        color = CatTheme.colors.warning
                    )
                }
                if (state.coachSource != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Fonte: ${state.coachSource}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CatTheme.colors.muted
                    )
                }
                val step = state.step
                val cell = step?.primaryCell
                val value = step?.primaryValue
                if (cell != null && value != null) {
                    Spacer(Modifier.height(10.dp))
                    SoftDivider()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Mossa suggerita: ${SudokuEngine.coordinate(cell)} = $value",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (state.grid[cell] == 0) {
                        Spacer(Modifier.height(8.dp))
                        PrimaryActionButton(
                            "Applica la mossa",
                            onClick = onApplyStep,
                            icon = Icons.Rounded.CheckCircle
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Numero rivelato dall'indizio.",
                            style = MaterialTheme.typography.labelSmall,
                            color = CatTheme.colors.success
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArOverlay(
    bitmap: Bitmap,
    grid: IntArray,
    solution: IntArray
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val marker = CatTheme.colors.fish
    val textArgb = 0xFF17151F.toInt()
    val ratio = remember(bitmap) { bitmap.width.toFloat() / bitmap.height.toFloat() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio.coerceIn(0.5f, 2f))
            .clip(RoundedCornerShape(16.dp))
    ) {
        Image(
            bitmap = image,
            contentDescription = "Foto del Sudoku con overlay dei numeri mancanti",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = size.width / 9f
            val cellHeight = size.height / 9f
            val radius = min(cellWidth, cellHeight) * 0.34f
            val paint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
                color = textArgb
            }
            for (index in 0 until 81) {
                if (grid[index] != 0 || solution[index] == 0) continue
                val row = index / 9
                val col = index % 9
                val centerX = cellWidth * (col + 0.5f)
                val centerY = cellHeight * (row + 0.5f)
                drawCircle(
                    color = marker.copy(alpha = 0.9f),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
                paint.textSize = radius * 1.15f
                drawContext.canvas.nativeCanvas.drawText(
                    solution[index].toString(),
                    centerX,
                    centerY + paint.textSize * 0.36f,
                    paint
                )
            }
        }
    }
}

@Composable
private fun SimpleTopBar(
    title: String,
    subtitle: String,
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
                contentDescription = "Indietro",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = CatTheme.colors.muted)
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
