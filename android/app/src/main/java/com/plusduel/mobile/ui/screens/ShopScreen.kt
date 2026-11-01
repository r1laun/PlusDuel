package com.plusduel.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plusduel.mobile.data.AccentSkin
import com.plusduel.mobile.data.AppLanguage
import com.plusduel.mobile.data.PlayerProgressState
import com.plusduel.mobile.data.TileShapeStyle
import com.plusduel.mobile.sound.SoundManager
import com.plusduel.mobile.ui.components.PdPanel
import com.plusduel.mobile.ui.components.PlusDuelLogoHeader
import com.plusduel.mobile.ui.components.statefulBorder
import com.plusduel.mobile.ui.i18n.AppStrings
import com.plusduel.mobile.ui.theme.PlusDuelTheme

@Composable
fun ShopScreen(
    progress: PlayerProgressState,
    soundManager: SoundManager,
    onSelectOrBuySkin: (AccentSkin) -> Unit,
    onSelectOrBuyShape: (TileShapeStyle) -> Unit
) {
    val colors = PlusDuelTheme.colors
    val lang = progress.language

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlusDuelLogoHeader(
            subtitle = AppStrings.tr(
                lang,
                "Цветовые палитры и формы плиток",
                "Accent Palettes & Tile Shapes",
                "Colores y formas de fichas",
                "主题配色与方块造型",
                "الألوان وأشكال المربعات"
            ),
            compact = true
        )

        // ==================== COIN BALANCE CARD ====================
        PdPanel(
            borderColor = colors.orange,
            backgroundColor = colors.surface,
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.tr(lang, "БАЛАНС", "BALANCE", "SALDO", "余额", "الرصيد"),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "● ${progress.coins}",
                        color = colors.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        maxLines = 1
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = AppStrings.tr(lang, "+20..50 ● за дуэль", "+20..50 ● per duel", "+20..50 ● / duelo", "对决 +20..50 ●", "+20..50 ●"),
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AppStrings.tr(lang, "+5 ● за раунд", "+5 ● per round", "+5 ● / ronda", "每回合 +5 ●", "+5 ● / جولة"),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // ==================== 1. ACCENT COLOR SKINS ====================
        PdPanel(contentPadding = PaddingValues(16.dp)) {
            Text(
                text = AppStrings.tr(
                    lang,
                    "1. ЦВЕТОВОЙ АКЦЕНТ",
                    "1. ACCENT PALETTE",
                    "1. COLOR DE ACENTO",
                    "1. 主题配色",
                    "١. اللون المميز"
                ),
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (skin in AccentSkin.entries) {
                    val unlocked = progress.isItemUnlocked(skin.id)
                    val selected = progress.selectedSkin == skin
                    val swatchColor = if (colors.isDark) skin.darkAccent else skin.lightAccent
                    val interaction = remember(skin.id) { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected || pressed) colors.tint else colors.bg)
                            .statefulBorder(
                                isDashed = selected || pressed,
                                strokeWidth = if (selected) 2.dp else 1.5.dp,
                                color = if (selected || pressed) colors.orange else colors.line,
                                cornerRadius = 14.dp
                            )
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) {
                                if (unlocked || progress.coins >= skin.costCoins) {
                                    soundManager.playCoinEarned()
                                    onSelectOrBuySkin(skin)
                                } else {
                                    soundManager.playMistake()
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(1.5.dp, colors.ink, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = skin.title(lang),
                                color = colors.ink,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        ShopActionChip(
                            selected = selected,
                            unlocked = unlocked,
                            costCoins = skin.costCoins,
                            canAfford = progress.coins >= skin.costCoins,
                            lang = lang
                        )
                    }
                }
            }
        }

        // ==================== 2. DIGIT TILE SHAPES ====================
        PdPanel(contentPadding = PaddingValues(16.dp)) {
            Text(
                text = AppStrings.tr(
                    lang,
                    "2. ФОРМА ПЛИТОК",
                    "2. TILE SHAPE",
                    "2. FORMA DE FICHAS",
                    "2. 方块造型",
                    "٢. شكل المربعات"
                ),
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (shape in TileShapeStyle.entries) {
                    val unlocked = progress.isItemUnlocked(shape.id)
                    val selected = progress.selectedTileShape == shape
                    val interaction = remember(shape.id) { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected || pressed) colors.tint else colors.bg)
                            .statefulBorder(
                                isDashed = selected || pressed,
                                strokeWidth = if (selected) 2.dp else 1.5.dp,
                                color = if (selected || pressed) colors.orange else colors.line,
                                cornerRadius = 14.dp
                            )
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) {
                                if (unlocked || progress.coins >= shape.costCoins) {
                                    soundManager.playCoinEarned()
                                    onSelectOrBuyShape(shape)
                                } else {
                                    soundManager.playMistake()
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(shape.cornerDp.dp))
                                    .background(colors.ink),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "7",
                                    color = colors.onInk,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = shape.title(lang),
                                color = colors.ink,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        ShopActionChip(
                            selected = selected,
                            unlocked = unlocked,
                            costCoins = shape.costCoins,
                            canAfford = progress.coins >= shape.costCoins,
                            lang = lang
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopActionChip(
    selected: Boolean,
    unlocked: Boolean,
    costCoins: Int,
    canAfford: Boolean,
    lang: AppLanguage
) {
    val colors = PlusDuelTheme.colors
    val bg = when {
        selected -> colors.orange
        unlocked -> colors.ink
        canAfford -> colors.orange
        else -> colors.surface
    }
    val fg = when {
        selected -> Color.White
        unlocked -> colors.onInk
        canAfford -> Color.White
        else -> colors.muted
    }
    val label = when {
        selected -> AppStrings.tr(lang, "Активно ✓", "Active ✓", "Activo ✓", "使用中 ✓", "مفعّل ✓")
        unlocked -> AppStrings.tr(lang, "Выбрать", "Equip", "Usar", "装备", "اختيار")
        else -> "● $costCoins"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, if (unlocked || canAfford) Color.Transparent else colors.line, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}
