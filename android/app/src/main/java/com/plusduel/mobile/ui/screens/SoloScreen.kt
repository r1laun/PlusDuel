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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plusduel.mobile.data.COINS_PER_ROUND_SOLVED
import com.plusduel.mobile.data.PlayerProgressState
import com.plusduel.mobile.engine.RoundGenerator
import com.plusduel.mobile.ui.components.PdButton
import com.plusduel.mobile.ui.components.PdButtonVariant
import com.plusduel.mobile.ui.components.PdPanel
import com.plusduel.mobile.ui.components.PlusDuelLogoHeader
import com.plusduel.mobile.ui.components.statefulBorder
import com.plusduel.mobile.ui.i18n.AppStrings
import com.plusduel.mobile.ui.theme.PlusDuelTheme

@Composable
fun SoloScreen(
    progress: PlayerProgressState,
    selectedLevel: Int,
    isMatchMode: Boolean,
    onSelectLevel: (Int) -> Unit,
    onSelectMode: (Boolean) -> Unit,
    onStartSolo: (isMatchMode: Boolean, level: Int) -> Unit,
    onClickSound: () -> Unit
) {
    val colors = PlusDuelTheme.colors
    val lang = progress.language
    val spec = RoundGenerator.specForLevel(selectedLevel)

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
                "Одиночный режим · +$COINS_PER_ROUND_SOLVED ● за раунд",
                "Solo Practice · +$COINS_PER_ROUND_SOLVED ● per round",
                "Modo Solo · +$COINS_PER_ROUND_SOLVED ● por ronda",
                "单人训练 · 每局 +$COINS_PER_ROUND_SOLVED ●",
                "التدريب الفردي · +$COINS_PER_ROUND_SOLVED ●"
            ),
            compact = true
        )

        // ==================== LIFETIME SOLO STATS CARD ====================
        PdPanel(contentPadding = PaddingValues(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = progress.soloSolved.toString(),
                        color = colors.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Text(
                        text = AppStrings.tr(lang, "Решено", "Solved", "Resueltos", "已解", "محلولة"),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = progress.soloBestStreak.toString(),
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Text(
                        text = AppStrings.tr(lang, "Серия", "Streak", "Racha", "连胜", "سلسلة"),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "● ${progress.coins}",
                        color = colors.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Text(
                        text = AppStrings.tr(lang, "Монеты", "Coins", "Monedas", "金币", "عملات"),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // ==================== MODE SELECTOR: ENDLESS VS MATCH ====================
        PdPanel(contentPadding = PaddingValues(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PdButton(
                    text = AppStrings.tr(lang, "Тренировка ∞", "Endless ∞", "Práctica ∞", "无限 ∞", "تدريب ∞"),
                    variant = if (!isMatchMode) PdButtonVariant.ACCENT else PdButtonVariant.OUTLINE,
                    isActive = !isMatchMode,
                    onClick = {
                        onClickSound()
                        onSelectMode(false)
                    },
                    modifier = Modifier.weight(1f)
                )

                PdButton(
                    text = AppStrings.tr(lang, "Матч (до 3)", "Match (Bo5)", "Partida", "五局赛", "مباراة"),
                    variant = if (isMatchMode) PdButtonVariant.ACCENT else PdButtonVariant.OUTLINE,
                    isActive = isMatchMode,
                    onClick = {
                        onClickSound()
                        onSelectMode(true)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ==================== DIFFICULTY LEVEL (1–6) ====================
        PdPanel(contentPadding = PaddingValues(16.dp)) {
            Text(
                text = AppStrings.tr(
                    lang,
                    "УРОВЕНЬ (1–6)",
                    "LEVEL (1–6)",
                    "NIVEL (1–6)",
                    "难度 (1–6)",
                    "المستوى (١–٦)"
                ),
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            for (row in RoundGenerator.LEVELS.chunked(3)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (item in row) {
                        val active = item.level == selectedLevel
                        val interaction = remember(item.level) { MutableInteractionSource() }
                        val pressed by interaction.collectIsPressedAsState()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active || pressed) colors.tint else colors.surface)
                                .statefulBorder(
                                    isDashed = active || pressed,
                                    strokeWidth = if (active) 2.dp else 1.5.dp,
                                    color = if (active || pressed) colors.orange else colors.ink,
                                    cornerRadius = 14.dp
                                )
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null
                                ) {
                                    onClickSound()
                                    onSelectLevel(item.level)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.level.toString(),
                                color = if (active || pressed) colors.orange else colors.ink,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg, RoundedCornerShape(12.dp))
                    .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = AppStrings.tr(
                        lang,
                        "${spec.minDigits}–${spec.maxDigits} цифр · цель ${spec.targetMin}–${spec.targetMax} · ${spec.timeLimitSec}с",
                        "${spec.minDigits}–${spec.maxDigits} digits · target ${spec.targetMin}–${spec.targetMax} · ${spec.timeLimitSec}s",
                        "${spec.minDigits}–${spec.maxDigits} dígitos · ${spec.targetMin}–${spec.targetMax} · ${spec.timeLimitSec}s",
                        "${spec.minDigits}–${spec.maxDigits} 数字 · 目标 ${spec.targetMin}–${spec.targetMax} · ${spec.timeLimitSec}秒",
                        "${spec.minDigits}–${spec.maxDigits} أرقام · ${spec.targetMin}–${spec.targetMax} · ${spec.timeLimitSec}ث"
                    ),
                    color = colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        PdButton(
            text = AppStrings.tr(
                lang,
                "Начать соло ▶",
                "Start Solo ▶",
                "Empezar Solo ▶",
                "开始单人 ▶",
                "ابدأ الفردي ▶"
            ),
            variant = PdButtonVariant.ACCENT,
            badgeText = "+$COINS_PER_ROUND_SOLVED ●",
            onClick = {
                onClickSound()
                onStartSolo(isMatchMode, selectedLevel)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
