package com.sudokucat.app.ui.photo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.sudokucat.app.ui.components.SecondaryActionButton
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun CameraCaptureScreen(
    onClose: () -> Unit,
    onCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var asked by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        asked = true
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var captured by remember { mutableStateOf<Uri?>(null) }
    var capturing by remember { mutableStateOf(false) }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        error = null
        runCatching {
            val cameraProvider = context.cameraProvider()
            provider = cameraProvider
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture
            )
            imageCapture = capture
        }.onFailure {
            error = "Fotocamera non disponibile su questo dispositivo. Puoi comunque scegliere una foto dalla galleria."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { provider?.unbindAll() }
        }
    }

    LaunchedEffect(captured) {
        captured?.let { onCaptured(it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Chiudi", tint = Color.White)
            }
            Text(
                "Inquadra la griglia e ritaglia bene i bordi",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (!hasPermission) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (asked) "Permesso fotocamera negato."
                        else "Consenti l'accesso alla fotocamera per scattare la foto.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    SecondaryActionButton(
                        if (asked) "Apri le impostazioni" else "Consenti fotocamera",
                        onClick = {
                            if (asked) {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        )
                                    )
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryActionButton("Torna alla schermata foto", onClick = onClose)
                }
            }
            if (error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        error.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    SecondaryActionButton("Torna indietro", onClick = onClose)
                }
            }
            if (hasPermission && error == null) {
                if (imageCapture == null) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 52.dp)
                            .size(28.dp),
                        strokeWidth = 3.dp,
                        color = Color.White
                    )
                } else {
                    Surface(
                        onClick = {
                            if (capturing) return@Surface
                            val capture = imageCapture ?: return@Surface
                            capturing = true
                            val file = File(context.cacheDir, "sudoku_${System.currentTimeMillis()}.jpg")
                            val options = ImageCapture.OutputFileOptions.Builder(file).build()
                            capture.takePicture(
                                options,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        capturing = false
                                        captured = Uri.fromFile(file)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        capturing = false
                                        error = "Scatto non riuscito: ${exception.message ?: "errore sconosciuto"}"
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 28.dp)
                            .size(76.dp),
                        shape = CircleShape,
                        color = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (capturing) {
                                CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
                            } else {
                                Icon(
                                    Icons.Rounded.PhotoCamera,
                                    contentDescription = "Scatta",
                                    tint = Color(0xFF2A2633),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun Context.cameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { continuation.resume(it) }
                    .onFailure { continuation.resumeWith(Result.failure(it)) }
            },
            ContextCompat.getMainExecutor(this)
        )
    }
