package com.sudokucat.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudokucat.app.data.prefs.UserProfile
import com.sudokucat.app.domain.model.Difficulty
import com.sudokucat.app.ui.theme.CatTheme
import com.sudokucat.app.ui.theme.CatPalette
import kotlin.math.roundToInt

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val colors = CatTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        listOf(
                            scheme.background,
                            scheme.background,
                            scheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    )
                )
                drawCircle(
                    color = scheme.primary.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 1.02f, size.height * 0.02f)
                )
                drawCircle(
                    color = colors.pink.copy(alpha = 0.07f),
                    radius = size.minDimension * 0.35f,
                    center = Offset(size.width * -0.05f, size.height * 0.78f)
                )
                drawCircle(
                    color = colors.mint.copy(alpha = 0.06f),
                    radius = size.minDimension * 0.25f,
                    center = Offset(size.width * 0.85f, size.height * 0.95f)
                )
            },
        content = content
    )
}

@Composable
fun CatTopBar(
    title: String,
    subtitle: String? = null,
    onMenu: (() -> Unit)? = null,
    onOpenCat: (() -> Unit)? = null,
    actions: RowScopeContent = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onMenu != null) {
            IconButton(onClick = onMenu) {
                Icon(Icons.Rounded.Menu, contentDescription = "Apri menu", tint = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = CatTheme.colors.muted,
                    maxLines = 1
                )
            }
        }
        actions()
        if (onOpenCat != null) {
            CatAvatar(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenCat),
                expression = com.sudokucat.app.util.CatExpression.HAPPY
            )
        }
    }
}

typealias RowScopeContent = @Composable androidx.compose.foundation.layout.RowScope.() -> Unit

@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String? = null,
    onMenu: () -> Unit,
    onOpenCat: () -> Unit,
    actions: RowScopeContent = {},
    content: @Composable ColumnScope.() -> Unit
) {
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            CatTopBar(
                title = title,
                subtitle = subtitle,
                onMenu = onMenu,
                onOpenCat = onOpenCat,
                actions = actions
            )
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                content = content
            )
        }
    }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .then(clickModifier),
        shape = MaterialTheme.shapes.large,
        color = color,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = CatTheme.colors.muted,
        letterSpacing = 1.4.sp
    )
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = contentColor)
                Spacer(Modifier.width(5.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium, color = contentColor)
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = accent)
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = CatTheme.colors.muted,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ProfileStrip(profile: UserProfile, modifier: Modifier = Modifier) {
    SoftCard(modifier = modifier, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = profile.level.toString(),
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Livello ${profile.level}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = CatPalette.Coral
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${profile.streak} giorni",
                        style = MaterialTheme.typography.labelMedium,
                        color = CatTheme.colors.muted
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { profile.levelProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${profile.xpInLevel} / ${profile.xpForNextLevel} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = CatTheme.colors.muted
                )
            }
        }
    }
}

@Composable
fun DifficultySelector(
    selected: Difficulty,
    onSelect: (Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Difficulty.entries.forEach { difficulty ->
            val active = difficulty == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(difficulty) },
                shape = RoundedCornerShape(14.dp),
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        difficulty.shortLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${difficulty.targetClues} indizi",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else CatTheme.colors.muted
                    )
                }
            }
        }
    }
}

@Composable
fun StarRow(stars: Int, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Row(modifier = modifier) {
        repeat(3) { index ->
            Icon(
                Icons.Rounded.Star,
                contentDescription = null,
                modifier = Modifier.size(size),
                tint = if (index < stars) CatTheme.colors.fish else MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
fun ArrowBadge(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

fun formatDuration(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun formatAverage(seconds: Long?): String {
    if (seconds == null) return "—"
    return formatDuration(seconds)
}

@Composable
fun SoftDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}

@Composable
fun MiniTag(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = color.copy(alpha = 0.13f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = color
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
