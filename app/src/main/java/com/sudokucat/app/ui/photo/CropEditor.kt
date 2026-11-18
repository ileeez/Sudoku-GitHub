package com.sudokucat.app.ui.photo

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Editor di ritaglio: l'immagine viene mostrata con adattamento "fit" e
 * l'utente può trascinare gli angoli o l'intera area per inquadrare la griglia.
 */
@Composable
fun CropEditor(
    bitmap: Bitmap,
    crop: CropRect,
    onCropChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCrop by rememberUpdatedState(crop)
    val currentCallback by rememberUpdatedState(onCropChange)
    val borderColor = MaterialTheme.colorScheme.primary
    val guideColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .pointerInput(bitmap) {
                var mode: Int? = null
                var working = currentCrop
                detectDragGestures(
                    onDragStart = { start ->
                        val rect = imageRect(bitmap, size.width.toFloat(), size.height.toFloat())
                        val scaleX = rect.second.width
                        val scaleY = rect.second.height
                        val corners = listOf(
                            Offset(rect.first.x + working.left * scaleX, rect.first.y + working.top * scaleY),
                            Offset(rect.first.x + working.right * scaleX, rect.first.y + working.top * scaleY),
                            Offset(rect.first.x + working.right * scaleX, rect.first.y + working.bottom * scaleY),
                            Offset(rect.first.x + working.left * scaleX, rect.first.y + working.bottom * scaleY)
                        )
                        val threshold = 64.dp.toPx()
                        val nearest = corners.mapIndexed { index, point -> index to (point - start).getDistance() }
                            .minByOrNull { it.second }
                        mode = when {
                            nearest != null && nearest.second < threshold -> nearest.first
                            start.x in (rect.first.x + working.left * scaleX)..(rect.first.x + working.right * scaleX) &&
                                start.y in (rect.first.y + working.top * scaleY)..(rect.first.y + working.bottom * scaleY) -> 4
                            else -> null
                        }
                        working = currentCrop
                    },
                    onDragEnd = { mode = null },
                    onDragCancel = { mode = null },
                    onDrag = { change, amount ->
                        val dragMode = mode ?: return@detectDragGestures
                        change.consume()
                        val rect = imageRect(bitmap, size.width.toFloat(), size.height.toFloat())
                        val dx = amount.x / rect.second.width
                        val dy = amount.y / rect.second.height
                        working = when (dragMode) {
                            0 -> working.copy(left = working.left + dx, top = working.top + dy)
                            1 -> working.copy(right = working.right + dx, top = working.top + dy)
                            2 -> working.copy(right = working.right + dx, bottom = working.bottom + dy)
                            3 -> working.copy(left = working.left + dx, bottom = working.bottom + dy)
                            else -> CropRect(
                                left = working.left + dx,
                                top = working.top + dy,
                                right = working.right + dx,
                                bottom = working.bottom + dy
                            )
                        }.normalized()
                        currentCallback(working)
                    }
                )
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val imageRect = imageRect(bitmap, widthPx, heightPx)
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Foto da ritagliare",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val imageLeft = imageRect.first.x
            val imageTop = imageRect.first.y
            val imageWidth = imageRect.second.width
            val imageHeight = imageRect.second.height
            val left = imageLeft + crop.left * imageWidth
            val top = imageTop + crop.top * imageHeight
            val right = imageLeft + crop.right * imageWidth
            val bottom = imageTop + crop.bottom * imageHeight
            val scrim = Color.Black.copy(alpha = 0.55f)
            drawRect(scrim, topLeft = Offset(0f, 0f), size = Size(size.width, top))
            drawRect(scrim, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
            drawRect(scrim, topLeft = Offset(0f, top), size = Size(left, bottom - top))
            drawRect(scrim, topLeft = Offset(right, top), size = Size(size.width - right, bottom - top))

            for (i in 1..2) {
                val x = left + (right - left) * i / 3f
                val y = top + (bottom - top) * i / 3f
                drawLine(guideColor, Offset(x, top), Offset(x, bottom), 1.5f)
                drawLine(guideColor, Offset(left, y), Offset(right, y), 1.5f)
            }
            drawRect(
                color = borderColor,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 3.dp.toPx())
            )
            val handleRadius = 9.dp.toPx()
            listOf(
                Offset(left, top),
                Offset(right, top),
                Offset(right, bottom),
                Offset(left, bottom)
            ).forEach { point ->
                drawCircle(Color.White, handleRadius, point)
                drawCircle(borderColor, handleRadius, point, style = Stroke(width = 3.dp.toPx()))
            }
        }
    }
}

private fun imageRect(bitmap: Bitmap, boxWidth: Float, boxHeight: Float): Pair<Offset, Size> {
    if (boxWidth <= 0f || boxHeight <= 0f) return Offset.Zero to Size.Zero
    val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    val boxAspect = boxWidth / boxHeight
    val width: Float
    val height: Float
    if (imageAspect > boxAspect) {
        width = boxWidth
        height = boxWidth / imageAspect
    } else {
        height = boxHeight
        width = boxHeight * imageAspect
    }
    val left = (boxWidth - width) / 2f
    val top = (boxHeight - height) / 2f
    return Offset(left, top) to Size(width, height)
}

internal fun CropRect.bounds(width: Int, height: Int): android.graphics.Rect {
    val l = (min(left, right) * width).toInt().coerceIn(0, width - 1)
    val t = (min(top, bottom) * height).toInt().coerceIn(0, height - 1)
    val r = (max(left, right) * width).toInt().coerceIn(l + 1, width)
    val b = (max(top, bottom) * height).toInt().coerceIn(t + 1, height)
    return android.graphics.Rect(l, t, r, b)
}
