package com.sudokucat.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sudokucat.app.util.CatExpression
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animazione della vittoria: le aringhe-biscotto rimaste volano verso la bocca
 * del gatto e vengono mangiate una dopo l'altra.
 */
@Composable
fun FishFeast(
    remainingLives: Int,
    modifier: Modifier = Modifier,
    startDelayMillis: Long = 450L
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        val width = maxWidth
        CatAvatar(
            modifier = Modifier
                .size(128.dp)
                .align(Alignment.BottomCenter),
            expression = CatExpression.EXCITED
        )
        val visibleFish = remainingLives.coerceIn(0, 3)
        repeat(visibleFish) { index ->
            var eaten by remember(index) { mutableStateOf(false) }
            LaunchedEffect(index) {
                delay(startDelayMillis + index * 360L)
                eaten = true
            }
            val progress by animateFloatAsState(
                targetValue = if (eaten) 1f else 0f,
                animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
                label = "fish-$index"
            )
            val startX = width * (0.16f + index * 0.32f)
            val endX = width * 0.5f
            val x = startX + (endX - startX) * progress
            val baseY = 26f + (index % 2) * 12f
            val y = baseY + (108f - baseY) * progress - (sin(progress * PI).toFloat() * 52f)
            FishBiscuit(
                modifier = Modifier
                    .offset { IntOffset(x.roundToPx(), y.dp.roundToPx()) }
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = 1f - 0.55f * progress
                        scaleY = 1f - 0.55f * progress
                        rotationZ = -18f + 60f * progress
                        alpha = 1f - (progress * 0.95f)
                    }
            )
        }
    }
}
