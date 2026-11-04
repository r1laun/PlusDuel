package com.plusduel.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plusduel.mobile.ui.theme.PlusDuelTheme

fun Modifier.dashedBorder(
    strokeWidth: Dp = 1.5.dp,
    color: Color,
    cornerRadius: Dp = 10.dp,
    dashOn: Dp = 6.dp,
    dashOff: Dp = 4.dp
): Modifier = this.drawBehind {
    val strokePx = strokeWidth.toPx()
    val halfStroke = strokePx / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(halfStroke, halfStroke),
        size = Size(
            width = size.width - strokePx,
            height = size.height - strokePx
        ),
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        style = Stroke(
            width = strokePx,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashOn.toPx(), dashOff.toPx()),
                0f
            )
        )
    )
}

/**
 * Signature detail: solid border in idle state; switches to dashed border when pressed or active.
 */
fun Modifier.statefulBorder(
    isDashed: Boolean,
    strokeWidth: Dp = 1.5.dp,
    color: Color,
    cornerRadius: Dp = 10.dp
): Modifier = if (isDashed) {
    this.dashedBorder(strokeWidth = strokeWidth, color = color, cornerRadius = cornerRadius)
} else {
    this.border(width = strokeWidth, color = color, shape = RoundedCornerShape(cornerRadius))
}

@Composable
fun PdPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = PlusDuelTheme.colors.line,
    backgroundColor: Color = PlusDuelTheme.colors.surface,
    cornerRadius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

enum class PdButtonVariant {
    PRIMARY, ACCENT, OUTLINE
}

@Composable
fun PdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PdButtonVariant = PdButtonVariant.PRIMARY,
    enabled: Boolean = true,
    isActive: Boolean = false,
    badgeText: String? = null
) {
    val colors = PlusDuelTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = tween(140),
        label = "btnScale"
    )

    val useDashed = (isPressed && enabled) || isActive

    val bg = when (variant) {
        PdButtonVariant.PRIMARY -> if (isPressed && enabled) colors.surface else if (enabled) colors.ink else colors.ink.copy(alpha = 0.3f)
        PdButtonVariant.ACCENT -> if (isPressed && enabled) colors.tint else if (enabled) colors.orange else colors.orange.copy(alpha = 0.35f)
        PdButtonVariant.OUTLINE -> if (isPressed && enabled || isActive) colors.tint else colors.surface
    }
    val fg = when (variant) {
        PdButtonVariant.PRIMARY -> if (isPressed && enabled) colors.ink else colors.onInk
        PdButtonVariant.ACCENT -> if (isPressed && enabled) colors.orange else Color.White
        PdButtonVariant.OUTLINE -> if (isActive) colors.orange else if (enabled) colors.ink else colors.muted.copy(alpha = 0.45f)
    }

    val borderColor = when (variant) {
        PdButtonVariant.PRIMARY -> if (enabled) colors.ink else colors.line
        PdButtonVariant.ACCENT -> if (enabled) colors.orange else colors.line
        PdButtonVariant.OUTLINE -> if (isPressed && enabled || isActive) colors.orange else colors.line
    }

    val radius = 10.dp
    val shape = RoundedCornerShape(radius)

    Box(
        modifier = modifier
            .scale(scale)
            .height(44.dp)
            .clip(shape)
            .background(bg, shape)
            .statefulBorder(
                isDashed = useDashed,
                strokeWidth = 1.5.dp,
                color = borderColor,
                cornerRadius = radius
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = fg,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (badgeText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = if (variant == PdButtonVariant.OUTLINE) colors.tint else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = if (variant == PdButtonVariant.OUTLINE) colors.orange else Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun TactileKeypadButton(
    label: String,
    fontSizeSp: Int,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentTint: Boolean = false,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val colors = PlusDuelTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = tween(120),
        label = "keyScale"
    )

    val useDashed = (isPressed && enabled) || isActive
    val bgColor = when {
        !enabled -> colors.surface.copy(alpha = 0.45f)
        isPressed || isActive -> colors.tint
        accentTint -> colors.tint
        else -> colors.surface
    }
    val borderColor = when {
        !enabled -> colors.line.copy(alpha = 0.5f)
        isPressed || isActive || accentTint -> colors.orange
        else -> colors.line
    }
    val textColor = when {
        !enabled -> colors.muted.copy(alpha = 0.4f)
        isPressed || isActive || accentTint -> colors.orange
        else -> colors.ink
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor, RoundedCornerShape(cornerRadius))
            .statefulBorder(
                isDashed = useDashed,
                strokeWidth = 1.5.dp,
                color = borderColor,
                cornerRadius = cornerRadius
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = fontSizeSp.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

private val PD_LOGO_DOTS: List<Pair<Int, Int>> = listOf(
    1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0,
    0 to 1, 1 to 1,
    4 to 2, 5 to 2,
    0 to 3, 1 to 3, 2 to 3, 3 to 3, 4 to 3
)

@Composable
fun PlusDuelDotMark(
    modifier: Modifier = Modifier,
    dotColor: Color = PlusDuelTheme.colors.orange
) {
    Canvas(modifier = modifier) {
        val cols = 6
        val rows = 4
        val step = minOf(size.width / cols, size.height / rows)
        val radius = step * 0.43f
        val startX = (size.width - (cols - 1) * step) / 2f
        val startY = (size.height - (rows - 1) * step) / 2f
        for ((c, r) in PD_LOGO_DOTS) {
            drawCircle(
                color = dotColor,
                radius = radius,
                center = Offset(
                    x = startX + c * step,
                    y = startY + r * step
                )
            )
        }
    }
}

@Composable
fun PlusDuelLogoHeader(
    subtitle: String? = null,
    compact: Boolean = false
) {
    val colors = PlusDuelTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlusDuelDotMark(
                    modifier = Modifier
                        .width(if (compact) 36.dp else 48.dp)
                        .height(if (compact) 24.dp else 32.dp),
                    dotColor = colors.orange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Plus",
                    color = colors.orange,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (compact) 24.sp else 32.sp,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Duel",
                    color = colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (compact) 24.sp else 32.sp,
                    letterSpacing = (-0.5).sp
                )
            }
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
