package com.sudokucat.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sudokucat.app.ui.theme.CatPalette
import com.sudokucat.app.ui.theme.CatTheme
import com.sudokucat.app.util.CatExpression

@Composable
fun CatAvatar(
    modifier: Modifier = Modifier,
    expression: CatExpression = CatExpression.CURIOUS,
    animated: Boolean = true,
    reactionToken: Long = 0L
) {
    val colors = CatTheme.colors
    val transition = rememberInfiniteTransition(label = "cat")
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 5200
                1f at 0
                1f at 4300
                0.06f at 4460
                1f at 4620
            }
        ),
        label = "blink"
    )
    val bob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    val reactionScale = remember { Animatable(1f) }
    val reactionTilt = remember { Animatable(0f) }
    LaunchedEffect(reactionToken) {
        if (reactionToken == 0L || !animated) return@LaunchedEffect
        reactionScale.snapTo(1f)
        reactionTilt.snapTo(0f)
        reactionScale.animateTo(1.14f, tween(120, easing = FastOutSlowInEasing))
        reactionTilt.animateTo(-6f, tween(100))
        reactionScale.animateTo(0.97f, tween(110))
        reactionTilt.animateTo(5f, tween(120))
        reactionScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
        reactionTilt.animateTo(0f, tween(160))
    }
    Canvas(
        modifier = modifier.graphicsLayer {
            val scale = if (animated) reactionScale.value else 1f
            scaleX = scale
            scaleY = scale
            rotationZ = if (animated) reactionTilt.value else 0f
        }
    ) {
        drawCatFace(
            expression = expression,
            blink = if (animated) blink else 1f,
            bob = if (animated) bob else 0f,
            rim = colors.lavender,
            blush = colors.pink
        )
    }
}

private fun DrawScope.drawCatFace(
    expression: CatExpression,
    blink: Float,
    bob: Float,
    rim: Color,
    blush: Color
) {
    val s = size.minDimension
    val fur = Color(0xFF211E2B)
    val furDeep = Color(0xFF141119)
    val eyeColor = Color(0xFFF2C879)
    val pink = Color(0xFFE8A8B8)
    val ink = Color(0xFF0C0A10)

    withTransform({
        translate(left = 0f, top = bob * s * 0.015f)
    }) {
        // Alone luminoso per staccare il gatto nero dallo sfondo scuro.
        drawCircle(
            color = rim.copy(alpha = 0.16f),
            radius = s * 0.42f,
            center = Offset(s * 0.5f, s * 0.54f)
        )
        drawOval(
            color = Color.Black.copy(alpha = 0.10f),
            topLeft = Offset(s * 0.18f, s * 0.88f),
            size = Size(s * 0.64f, s * 0.09f)
        )

        // Orecchie
        val leftEar = Path().apply {
            moveTo(s * 0.18f, s * 0.44f)
            lineTo(s * 0.19f, s * 0.10f)
            quadraticBezierTo(s * 0.30f, s * 0.22f, s * 0.46f, s * 0.28f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(s * 0.82f, s * 0.44f)
            lineTo(s * 0.81f, s * 0.10f)
            quadraticBezierTo(s * 0.70f, s * 0.22f, s * 0.54f, s * 0.28f)
            close()
        }
        drawPath(leftEar, fur)
        drawPath(rightEar, fur)
        val innerLeft = Path().apply {
            moveTo(s * 0.24f, s * 0.38f)
            lineTo(s * 0.25f, s * 0.19f)
            quadraticBezierTo(s * 0.33f, s * 0.26f, s * 0.42f, s * 0.31f)
            close()
        }
        val innerRight = Path().apply {
            moveTo(s * 0.76f, s * 0.38f)
            lineTo(s * 0.75f, s * 0.19f)
            quadraticBezierTo(s * 0.67f, s * 0.26f, s * 0.58f, s * 0.31f)
            close()
        }
        drawPath(innerLeft, pink.copy(alpha = 0.55f))
        drawPath(innerRight, pink.copy(alpha = 0.55f))

        // Testa
        drawOval(
            color = fur,
            topLeft = Offset(s * 0.155f, s * 0.235f),
            size = Size(s * 0.69f, s * 0.63f)
        )
        drawOval(
            color = furDeep.copy(alpha = 0.35f),
            topLeft = Offset(s * 0.185f, s * 0.60f),
            size = Size(s * 0.63f, s * 0.26f)
        )
        drawOval(
            color = rim.copy(alpha = 0.28f),
            topLeft = Offset(s * 0.155f, s * 0.235f),
            size = Size(s * 0.69f, s * 0.63f),
            style = Stroke(width = s * 0.012f)
        )

        // Guance
        drawCircle(blush.copy(alpha = 0.20f), radius = s * 0.055f, center = Offset(s * 0.285f, s * 0.665f))
        drawCircle(blush.copy(alpha = 0.20f), radius = s * 0.055f, center = Offset(s * 0.715f, s * 0.665f))

        val baseOpen = when (expression) {
            CatExpression.SLEEPY, CatExpression.SAD -> 0.18f
            CatExpression.FOCUSED -> 0.55f
            CatExpression.EXCITED -> 1.2f
            else -> 1f
        }
        val openness = (baseOpen * if (expression == CatExpression.EXCITED) 1f else blink)
            .coerceIn(0.05f, 1.25f)
        val eyeRadiusY = s * 0.105f * openness
        val leftEyeX = if (expression == CatExpression.CONFUSED) s * 0.375f else s * 0.385f
        val rightEyeX = s * 0.615f
        val eyeY = s * 0.545f

        if (expression == CatExpression.HAPPY || expression == CatExpression.EXCITED) {
            // Occhi sorridenti a mezzaluna.
            listOf(leftEyeX, rightEyeX).forEach { x ->
                val arc = Path().apply {
                    moveTo(x - s * 0.075f, eyeY + s * 0.02f)
                    quadraticBezierTo(x, eyeY - s * 0.085f, x + s * 0.075f, eyeY + s * 0.02f)
                }
                drawPath(arc, eyeColor, style = Stroke(width = s * 0.018f, cap = StrokeCap.Round))
            }
        } else if (openness < 0.3f) {
            listOf(leftEyeX, rightEyeX).forEach { x ->
                val arc = Path().apply {
                    moveTo(x - s * 0.065f, eyeY)
                    quadraticBezierTo(x, eyeY + s * 0.045f, x + s * 0.065f, eyeY)
                }
                drawPath(arc, Color(0xFFD9D2E8), style = Stroke(width = s * 0.014f, cap = StrokeCap.Round))
            }
        } else {
            drawEye(leftEyeX, eyeY, s, eyeRadiusY, eyeColor, pink, expression == CatExpression.CONFUSED)
            drawEye(rightEyeX, eyeY, s, eyeRadiusY * if (expression == CatExpression.CONFUSED) 0.65f else 1f, eyeColor, pink, false)
            if (expression == CatExpression.EXCITED) {
                drawCircle(Color.White.copy(alpha = 0.9f), s * 0.018f, Offset(leftEyeX + s * 0.028f, eyeY - s * 0.04f))
                drawCircle(Color.White.copy(alpha = 0.9f), s * 0.018f, Offset(rightEyeX + s * 0.028f, eyeY - s * 0.04f))
            }
        }

        // Naso
        val nose = Path().apply {
            moveTo(s * 0.47f, s * 0.625f)
            lineTo(s * 0.53f, s * 0.625f)
            lineTo(s * 0.50f, s * 0.655f)
            close()
        }
        drawPath(nose, pink)

        // Bocca
        val mouthWidth = s * 0.026f
        when (expression) {
            CatExpression.SAD -> {
                val mouth = Path().apply {
                    moveTo(s * 0.45f, s * 0.705f)
                    quadraticBezierTo(s * 0.50f, s * 0.665f, s * 0.55f, s * 0.705f)
                }
                drawPath(mouth, Color(0xFFCFC8DD), style = Stroke(width = mouthWidth, cap = StrokeCap.Round))
                drawCircle(Color(0xFF9FD3C3).copy(alpha = 0.85f), s * 0.018f, Offset(s * 0.335f, s * 0.635f))
            }
            CatExpression.EXCITED -> {
                drawOval(
                    color = ink,
                    topLeft = Offset(s * 0.445f, s * 0.672f),
                    size = Size(s * 0.11f, s * 0.085f)
                )
                drawOval(
                    color = pink.copy(alpha = 0.85f),
                    topLeft = Offset(s * 0.47f, s * 0.716f),
                    size = Size(s * 0.06f, s * 0.035f)
                )
            }
            CatExpression.FOCUSED -> {
                drawLine(
                    color = Color(0xFFCFC8DD),
                    start = Offset(s * 0.47f, s * 0.70f),
                    end = Offset(s * 0.53f, s * 0.70f),
                    strokeWidth = mouthWidth,
                    cap = StrokeCap.Round
                )
            }
            CatExpression.CURIOUS -> {
                drawCircle(
                    color = ink.copy(alpha = 0.75f),
                    radius = s * 0.018f,
                    center = Offset(s * 0.50f, s * 0.70f)
                )
            }
            else -> {
                val mouth = Path().apply {
                    moveTo(s * 0.455f, s * 0.665f)
                    quadraticBezierTo(s * 0.50f, s * 0.715f, s * 0.545f, s * 0.665f)
                }
                drawPath(mouth, Color(0xFFCFC8DD), style = Stroke(width = mouthWidth, cap = StrokeCap.Round))
            }
        }

        // Baffi
        val whisker = Color(0xFFBFB8D0).copy(alpha = 0.65f)
        val whiskerWidth = s * 0.007f
        listOf(-0.02f, 0.03f, 0.08f).forEachIndexed { index, offset ->
            drawLine(
                whisker,
                Offset(s * 0.34f, s * (0.645f + offset)),
                Offset(s * 0.12f, s * (0.615f + offset * 1.6f)),
                whiskerWidth,
                StrokeCap.Round
            )
            drawLine(
                whisker,
                Offset(s * 0.66f, s * (0.645f + offset)),
                Offset(s * 0.88f, s * (0.615f + offset * 1.6f)),
                whiskerWidth,
                StrokeCap.Round
            )
        }

        if (expression == CatExpression.EXCITED || expression == CatExpression.HAPPY) {
            drawSparkle(Offset(s * 0.86f, s * 0.22f), s * 0.075f, rim)
            drawSparkle(Offset(s * 0.12f, s * 0.30f), s * 0.045f, blush)
        }
        if (expression == CatExpression.SLEEPY) {
            drawLine(
                Color(0xFFBFB8D0).copy(alpha = 0.6f),
                Offset(s * 0.78f, s * 0.18f),
                Offset(s * 0.88f, s * 0.18f),
                s * 0.012f,
                StrokeCap.Round
            )
            drawLine(
                Color(0xFFBFB8D0).copy(alpha = 0.6f),
                Offset(s * 0.80f, s * 0.13f),
                Offset(s * 0.87f, s * 0.13f),
                s * 0.012f,
                StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawEye(
    x: Float,
    y: Float,
    s: Float,
    radiusY: Float,
    eyeColor: Color,
    blush: Color,
    big: Boolean
) {
    val radiusX = s * if (big) 0.09f else 0.078f
    drawOval(
        color = Color(0xFFF8F4FF),
        topLeft = Offset(x - radiusX, y - radiusY),
        size = Size(radiusX * 2f, radiusY * 2f)
    )
    drawOval(
        color = eyeColor,
        topLeft = Offset(x - radiusX * 0.72f, y - radiusY * 0.98f),
        size = Size(radiusX * 1.44f, radiusY * 1.96f)
    )
    drawOval(
        color = Color(0xFF0C0A10),
        topLeft = Offset(x - radiusX * 0.22f, y - radiusY * 0.88f),
        size = Size(radiusX * 0.44f, radiusY * 1.76f)
    )
    drawCircle(
        color = Color.White,
        radius = s * 0.013f,
        center = Offset(x + radiusX * 0.28f, y - radiusY * 0.36f)
    )
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    rotate(45f, pivot = center) {
        drawLine(color, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), radius * 0.22f, StrokeCap.Round)
        drawLine(color, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), radius * 0.22f, StrokeCap.Round)
    }
}

@Composable
fun FishBiscuit(
    modifier: Modifier = Modifier,
    color: Color = CatTheme.colors.fish,
    dark: Color = CatTheme.colors.fishDark,
    eaten: Boolean = false
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val alpha = if (eaten) 0.35f else 1f
        // Corpo
        drawOval(
            color = dark.copy(alpha = alpha),
            topLeft = Offset(s * 0.16f, s * 0.28f),
            size = Size(s * 0.58f, s * 0.44f)
        )
        drawOval(
            color = color.copy(alpha = alpha),
            topLeft = Offset(s * 0.18f, s * 0.30f),
            size = Size(s * 0.54f, s * 0.40f)
        )
        // Coda
        val tail = Path().apply {
            moveTo(s * 0.70f, s * 0.50f)
            lineTo(s * 0.92f, s * 0.25f)
            lineTo(s * 0.92f, s * 0.75f)
            close()
        }
        drawPath(tail, dark.copy(alpha = alpha))
        val tailInner = Path().apply {
            moveTo(s * 0.72f, s * 0.50f)
            lineTo(s * 0.88f, s * 0.32f)
            lineTo(s * 0.88f, s * 0.68f)
            close()
        }
        drawPath(tailInner, color.copy(alpha = alpha))
        // Pinna
        val fin = Path().apply {
            moveTo(s * 0.40f, s * 0.30f)
            quadraticBezierTo(s * 0.48f, s * 0.13f, s * 0.58f, s * 0.30f)
            close()
        }
        drawPath(fin, dark.copy(alpha = alpha))
        // Occhio e sorriso
        drawCircle(Color(0xFF3A2A18).copy(alpha = alpha), s * 0.035f, Offset(s * 0.30f, s * 0.45f))
        drawCircle(Color.White.copy(alpha = alpha), s * 0.013f, Offset(s * 0.31f, s * 0.44f))
        val smile = Path().apply {
            moveTo(s * 0.27f, s * 0.56f)
            quadraticBezierTo(s * 0.33f, s * 0.63f, s * 0.40f, s * 0.56f)
        }
        drawPath(smile, Color(0xFF3A2A18).copy(alpha = 0.7f), style = Stroke(width = s * 0.018f, cap = StrokeCap.Round))
        // Scaglie
        drawCircle(color.copy(alpha = alpha * 0.7f), s * 0.035f, Offset(s * 0.52f, s * 0.44f))
        drawCircle(dark.copy(alpha = alpha * 0.32f), s * 0.016f, Offset(s * 0.62f, s * 0.56f))
        drawCircle(dark.copy(alpha = alpha * 0.32f), s * 0.014f, Offset(s * 0.45f, s * 0.60f))
    }
}

@Composable
fun FishLives(
    lives: Int,
    modifier: Modifier = Modifier,
    total: Int = 3,
    animated: Boolean = true
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { index ->
            val alive = index < lives
            val scale by animateFloatAsState(
                targetValue = if (alive) 1f else 0.74f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "fish-scale-$index"
            )
            val rotation by animateFloatAsState(
                targetValue = if (alive) 0f else -22f,
                animationSpec = tween(350, easing = FastOutSlowInEasing),
                label = "fish-rotation-$index"
            )
            FishBiscuit(
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        scaleX = if (animated) scale else 1f
                        scaleY = if (animated) scale else 1f
                        rotationZ = if (animated) rotation else 0f
                    },
                color = if (alive) CatTheme.colors.fish else Color.Gray,
                dark = if (alive) CatTheme.colors.fishDark else Color.DarkGray,
                eaten = !alive
            )
        }
    }
}
