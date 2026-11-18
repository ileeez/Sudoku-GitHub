package com.sudokucat.app.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudokucat.app.AppContainer
import com.sudokucat.app.BuildConfig
import com.sudokucat.app.data.prefs.ThemeMode
import com.sudokucat.app.data.prefs.UserProfile
import com.sudokucat.app.data.prefs.UserSettings
import com.sudokucat.app.ui.components.AppBackground
import com.sudokucat.app.ui.components.CatAvatar
import com.sudokucat.app.ui.components.CatTopBar
import com.sudokucat.app.ui.components.DifficultySelector
import com.sudokucat.app.ui.components.MiniTag
import com.sudokucat.app.ui.components.Pill
import com.sudokucat.app.ui.components.SoftCard
import com.sudokucat.app.ui.components.SoftDivider
import com.sudokucat.app.ui.components.formatDuration
import com.sudokucat.app.ui.theme.CatPalette
import com.sudokucat.app.ui.theme.CatTheme
import com.sudokucat.app.util.CatExpression
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    container: AppContainer,
    onMenu: () -> Unit,
    onOpenCat: () -> Unit
) {
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = UserSettings())
    val profile by container.profileRepository.profile.collectAsStateWithLifecycle(initialValue = UserProfile())
    val scope = rememberCoroutineScope()

    var volume by remember { mutableFloatStateOf(settings.soundVolume) }
    var speechRate by remember { mutableFloatStateOf(settings.speechRate) }
    var keyInput by rememberSaveable { mutableStateOf("") }
    var showKey by rememberSaveable { mutableStateOf(false) }
    var modelInput by remember(settings.geminiModel) { mutableStateOf(settings.geminiModel) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testOk by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settings.soundVolume) { volume = settings.soundVolume }
    LaunchedEffect(settings.speechRate) { speechRate = settings.speechRate }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            CatTopBar(
                title = "Impostazioni",
                subtitle = "Fatte su misura per te e per Nero",
                onMenu = onMenu,
                onOpenCat = onOpenCat
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CatAvatar(modifier = Modifier.size(82.dp), expression = CatExpression.HAPPY)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nero", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Livello ${profile.level} · ${profile.xp} XP · serie ${profile.streak} giorni",
                                style = MaterialTheme.typography.bodySmall,
                                color = CatTheme.colors.muted
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MiniTag("Tema: ${settings.theme.label}")
                                MiniTag(if (settings.hasApiKey) "AI pronta" else "Locale")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                SettingsSection(title = "Aspetto", subtitle = "Charcoal, lavanda e luce calda") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            val active = settings.theme == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { scope.launch { container.settingsRepository.setTheme(mode) } },
                                shape = RoundedCornerShape(14.dp),
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Text(
                                    mode.label,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Il tema «Sistema» segue l'impostazione del telefono.",
                        style = MaterialTheme.typography.labelSmall,
                        color = CatTheme.colors.muted
                    )
                }
                Spacer(Modifier.height(14.dp))

                SettingsSection(title = "Gioco", subtitle = "Come vuoi affrontare la griglia") {
                    Text("Difficoltà predefinita", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    DifficultySelector(
                        selected = settings.defaultDifficulty,
                        onSelect = { scope.launch { container.settingsRepository.setDefaultDifficulty(it) } }
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingSwitch(
                        title = "Controllo automatico degli errori",
                        subtitle = "Se attivo, un numero sbagliato viene rifiutato e evidenziato. Se disattivo, resta sulla griglia.",
                        checked = settings.highlightErrors,
                        onCheckedChange = { scope.launch { container.settingsRepository.setHighlightErrors(it) } }
                    )
                    SettingSwitch(
                        title = "Rimuovi note automaticamente",
                        subtitle = "Quando inserisci un numero, cancella quel candidato dalle celle collegate.",
                        checked = settings.autoRemoveNotes,
                        onCheckedChange = { scope.launch { container.settingsRepository.setAutoRemoveNotes(it) } }
                    )
                    SettingSwitch(
                        title = "Mostra il timer",
                        subtitle = "Mostra il cronometro in alto durante la partita. Il tempo viene comunque salvato nei progressi.",
                        checked = settings.showTimer,
                        onCheckedChange = { scope.launch { container.settingsRepository.setShowTimer(it) } }
                    )
                    SettingSwitch(
                        title = "Vibrazione",
                        subtitle = "Un piccolo feedback aptico su errori e vittoria.",
                        checked = settings.hapticsEnabled,
                        onCheckedChange = { scope.launch { container.settingsRepository.setHapticsEnabled(it) } }
                    )
                }
                Spacer(Modifier.height(14.dp))

                SettingsSection(title = "Voce e suoni", subtitle = "Nero parla solo se vuoi") {
                    SliderRow(
                        label = "Volume effetti",
                        value = volume,
                        valueLabel = "${(volume * 100).toInt()}%",
                        onChange = {
                            volume = it
                            scope.launch { container.settingsRepository.setSoundVolume(it) }
                        }
                    )
                    SettingSwitch(
                        title = "Voce di Nero",
                        subtitle = "Legge ad alta voce spiegazioni e messaggi. Usa la voce italiana del sistema.",
                        checked = settings.voiceEnabled,
                        onCheckedChange = {
                            scope.launch { container.settingsRepository.setVoiceEnabled(it) }
                            if (it) container.catController.speakCurrent()
                        }
                    )
                    SliderRow(
                        label = "Velocità voce",
                        value = speechRate,
                        range = 0.6f..1.4f,
                        valueLabel = "${(speechRate * 100).toInt()}%",
                        onChange = {
                            speechRate = it
                            scope.launch { container.settingsRepository.setSpeechRate(it) }
                        }
                    )
                    SettingSwitch(
                        title = "Gatto e reazioni",
                        subtitle = "Mostra Nero, le sue espressioni e le bolle di dialogo.",
                        checked = settings.catEnabled,
                        onCheckedChange = { scope.launch { container.settingsRepository.setCatEnabled(it) } }
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallButton("Prova voce") { container.catController.speakCurrent() }
                        SmallButton("Prova suono") { container.soundManager.meow() }
                    }
                }
                Spacer(Modifier.height(14.dp))

                SettingsSection(
                    title = "Intelligenza artificiale",
                    subtitle = "Gemini è opzionale: tutto il resto funziona offline"
                ) {
                    SettingSwitch(
                        title = "Abilita spiegazioni AI",
                        subtitle = "Se disattiva, Nero usa solo il motore logico locale, senza rete.",
                        checked = settings.aiEnabled,
                        onCheckedChange = { scope.launch { container.settingsRepository.setAiEnabled(it) } }
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingSwitch(
                        title = "OCR foto con Gemini",
                        subtitle = "Invia la foto a Gemini per leggere la griglia 9x9 con Structured Output. " +
                            "Se fallisce, l'app usa ML Kit sul dispositivo.",
                        checked = settings.geminiOcrEnabled,
                        onCheckedChange = { scope.launch { container.settingsRepository.setGeminiOcrEnabled(it) } }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Key,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (settings.hasApiKey) CatTheme.colors.success else CatTheme.colors.warning
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                settings.hasApiKey && BuildConfig.GEMINI_API_KEY.isNotBlank() ->
                                    "Chiave Gemini integrata nella build: attiva subito, cifrata sul dispositivo"
                                settings.hasApiKey -> "Chiave salvata e cifrata sul dispositivo"
                                else -> "Nessuna chiave configurata"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = CatTheme.colors.muted
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nuova chiave API Gemini") },
                        placeholder = { Text("Incolla qui la chiave da Google AI Studio") },
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showKey) "Nascondi chiave" else "Mostra chiave"
                                )
                            }
                        },
                        supportingText = {
                            Text(
                                "La chiave personale di questa build viene cifrata al primo avvio con Android Keystore. " +
                                    "Le chiavi inserite qui non vengono mai scritte nel codice sorgente.",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Modello Gemini") },
                        singleLine = true,
                        supportingText = {
                            Text(
                                "Predefinito: gemini-3.8-flash. Puoi usare un altro modello disponibile per la tua chiave.",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("gemini-3.8-flash", "gemini-3.8-pro").forEach { model ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { modelInput = model }
                            ) {
                                Text(
                                    model,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (keyInput.isNotBlank()) {
                                    scope.launch {
                                        container.settingsRepository.setGeminiApiKey(keyInput)
                                        keyInput = ""
                                        infoMessage = "Chiave salvata. Nero può usare Gemini."
                                    }
                                }
                            },
                            enabled = keyInput.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) { Text("Salva chiave") }
                        Button(
                            onClick = {
                                testing = true
                                testResult = null
                                scope.launch {
                                    val snapshot = container.settingsRepository.snapshot()
                                    val key = keyInput.trim().ifBlank { snapshot.geminiApiKey }
                                    val model = modelInput.trim().ifBlank { snapshot.geminiModel }
                                    if (key.isBlank()) {
                                        testing = false
                                        testOk = false
                                        testResult = "Inserisci prima una chiave API."
                                        return@launch
                                    }
                                    container.settingsRepository.setGeminiModel(model)
                                    val result = container.coachService.testConnection(key, model)
                                    testing = false
                                    result.onSuccess {
                                        testOk = true
                                        testResult = "Connessione riuscita: Gemini risponde."
                                    }.onFailure {
                                        testOk = false
                                        testResult = it.message ?: "Connessione non riuscita."
                                    }
                                }
                            },
                            enabled = !testing && (settings.hasApiKey || keyInput.isNotBlank()),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (testing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Testa connessione")
                            }
                        }
                    }
                    if (testResult != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            testResult!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (testOk) CatTheme.colors.success else CatTheme.colors.danger
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallButton("Rimuovi chiave") {
                            scope.launch {
                                container.settingsRepository.setGeminiApiKey("")
                                infoMessage = "Chiave rimossa."
                            }
                        }
                        SmallButton("Svuota cache AI") {
                            scope.launch {
                                container.coachService.clearCache()
                                infoMessage = "Cache AI svuotata."
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Richieste AI oggi: ${profile.aiCallsToday}/40. Le risposte vengono messe in cache per 7 giorni. " +
                            "In caso di rete assente o limiti raggiunti, Nero continua con il motore locale.",
                        style = MaterialTheme.typography.labelSmall,
                        color = CatTheme.colors.muted
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sicurezza: su un dispositivo con root o con APK manomesso, una chiave salvata può essere estratta. " +
                            "Per un uso condiviso o professionale, è consigliabile un piccolo backend che custodisca la chiave. " +
                            "Non condividere mai la tua chiave e revocala da Google AI Studio se la sospetti compromessa.",
                        style = MaterialTheme.typography.labelSmall,
                        color = CatTheme.colors.warning
                    )
                }
                Spacer(Modifier.height(14.dp))

                SettingsSection(title = "Privacy e dati", subtitle = "Tutto resta sul tuo telefono") {
                    Text(
                        "Progressi, partite, statistiche e lezioni sono salvati localmente con Room e DataStore. " +
                            "Nessun account, nessun server, nessun tracciamento. L'unica connessione esterna è la richiesta " +
                            "a Gemini, e solo quando l'AI è attiva e una chiave è configurata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CatTheme.colors.muted
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallButton("Ripristina progressi", danger = true) { showReset = true }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ripristinare cancella partite, statistiche, progressi delle lezioni, XP e serie. " +
                            "Tema e chiave API restano salvati.",
                        style = MaterialTheme.typography.labelSmall,
                        color = CatTheme.colors.muted
                    )
                }
                Spacer(Modifier.height(14.dp))

                if (infoMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CatTheme.colors.success.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = CatTheme.colors.success)
                            Spacer(Modifier.width(8.dp))
                            Text(infoMessage!!, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CatAvatar(modifier = Modifier.size(54.dp), expression = CatExpression.SLEEPY)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sudoku Cat", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Versione ${BuildConfig.VERSION_NAME} · Kotlin, Compose, Room, CameraX, ML Kit",
                                style = MaterialTheme.typography.labelSmall,
                                color = CatTheme.colors.muted
                            )
                            Text(
                                "Fatto per chi ama i numeri e i gatti neri.",
                                style = MaterialTheme.typography.labelSmall,
                                color = CatTheme.colors.muted
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Azzerare tutti i progressi?") },
            text = {
                Text(
                    "Partite, statistiche, XP, serie e lezioni verranno cancellati. " +
                        "L'operazione non si può annullare."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReset = false
                        scope.launch {
                            container.resetProgress()
                            infoMessage = "Progressi azzerati. Si riparte insieme, miao."
                            container.catController.say(
                                "Miao! Un nuovo inizio. Non serve ricominciare da zero: serve ricominciare con calma.",
                                CatExpression.HAPPY,
                                speak = true
                            )
                        }
                    }
                ) { Text("Azzera tutto", color = CatTheme.colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("Annulla") }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    SoftCard {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = CatTheme.colors.muted)
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = CatTheme.colors.muted)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueLabel: String,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = CatTheme.colors.muted)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.height(34.dp))
    }
}

@Composable
private fun SmallButton(text: String, danger: Boolean = false, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (danger) CatTheme.colors.danger.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (danger) {
                Icon(
                    Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = CatTheme.colors.danger
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = if (danger) CatTheme.colors.danger else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
