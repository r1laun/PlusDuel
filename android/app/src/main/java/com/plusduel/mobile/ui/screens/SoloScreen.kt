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
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PlusDuelLogoHeader(
            subtitle = AppStrings.tr(
                lang,
                "Одиночная тренировка · Зарабатывай +$COINS_PER_ROUND_SOLVED ● за каждую решённую задачу",
                "Solo Practice · Earn +$COINS_PER_ROUND_SOLVED ● coins for every solved round",
                "Práctica en solitario · Gana +$COINS_PER_ROUND_SOLVED ● por cada ronda resuelta",
                "单人训练模式 · 每解出一题奖励 +$COINS_PER_ROUND_SOLVED ● 金币",
                "التدريب الفردي · اربح +$COINS_PER_ROUND_SOLVED ● لكل جولة تحلها"
            ),
            compact = true
        )

        // ==================== LIFETIME SOLO STATS CARD ====================
        PdPanel(contentPadding = PaddingValues(18.dp)) {
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
                        fontSize = 26.sp
                    )
                    Text(
                        text = AppStrings.tr(lang, "Решено соло", "Solved Solo", "Resueltos", "单人已解", "تم حلها"),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = progress.soloBestStreak.toString(),
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )
                    Text(
                        text = AppStrings.tr(lang, "Лучшая серия", "Best Streak", "Mejor racha", "最高连胜", "أفضل سلسلة"),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "● ${progress.coins}",
                        color = colors.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )
                    Text(
                        text = AppStrings.tr(lang, "Монеты", "Coins", "Monedas", "金币", "العملات"),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // ==================== MODE SELECTOR: ENDLESS VS MATCH ====================
        PdPanel(contentPadding = PaddingValues(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PdButton(
                    text = AppStrings.tr(lang, "Тренировка ∞", "Training ∞", "Práctica ∞", "无限训练 ∞", "تدريب ∞"),
                    variant = if (!isMatchMode) PdButtonVariant.ACCENT else PdButtonVariant.OUTLINE,
                    isActive = !isMatchMode,
                    onClick = {
                        onClickSound()
                        onSelectMode(false)
                    },
                    modifier = Modifier.weight(1f)
                )

                PdButton(
                    text = AppStrings.tr(lang, "Матч (до 3)", "Match (Bo5)", "Partida (Bo5)", "五局挑战", "مباراة (٥)"),
                    variant = if (isMatchMode) PdButtonVariant.ACCENT else PdButtonVariant.OUTLINE,
                    isActive = isMatchMode,
                    onClick = {
                        onClickSound()
                        onSelectMode(true)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (!isMatchMode) {
                    AppStrings.tr(
                        lang,
                        "Бесконечный поток задач выбранного уровня без ограничений по числу раундов.",
                        "Endless stream of solvable rounds at your chosen difficulty level.",
                        "Flujo infinito de problemas del nivel elegido.",
                        "在所选难度下无限生成可解算式。",
                        "سلسلة لا نهائية من المسائل بالمستوى المختار."
                    )
                } else {
                    AppStrings.tr(
                        lang,
                        "Матч из 5 раундов против таймера: сложность растёт с каждым раундом!",
                        "Best-of-5 match against the clock: difficulty climbs every round!",
                        "Partida al mejor de 5 contra el reloj: ¡la dificultad sube!",
                        "五局三胜限时挑战：难度随回合递增！",
                        "مباراة من ٥ جولات ضد الوقت: تزداد الصعوبة كل جولة!"
                    )
                },
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ==================== DIFFICULTY LEVEL (1–6) ====================
        PdPanel(contentPadding = PaddingValues(18.dp)) {
            Text(
                text = AppStrings.tr(
                    lang,
                    "УРОВЕНЬ СЛОЖНОСТИ (1–6)",
                    "DIFFICULTY LEVEL (1–6)",
                    "NIVEL DE DIFICULTAD (1–6)",
                    "难度等级 (1–6)",
                    "مستوى الصعوبة (١–٦)"
                ),
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            for (row in RoundGenerator.LEVELS.chunked(3)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (item in row) {
                        val active = item.level == selectedLevel
                        val interaction = remember(item.level) { MutableInteractionSource() }
                        val pressed by interaction.collectIsPressedAsState()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
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
                                fontSize = 22.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg, RoundedCornerShape(12.dp))
                    .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = AppStrings.tr(
                        lang,
                        "${spec.minDigits}–${spec.maxDigits} цифр · цель ${spec.targetMin}–${spec.targetMax} · таймер ${spec.timeLimitSec}с",
                        "${spec.minDigits}–${spec.maxDigits} digits · target ${spec.targetMin}–${spec.targetMax} · ${spec.timeLimitSec}s timer",
                        "${spec.minDigits}–${spec.maxDigits} dígitos · objetivo ${spec.targetMin}–${spec.targetMax} · ${spec.timeLimitSec}s",
                        "${spec.minDigits}–${spec.maxDigits} 个数字 · 目标 ${spec.targetMin}–${spec.targetMax} · 限时 ${spec.timeLimitSec}秒",
                        "${spec.minDigits}–${spec.maxDigits} أرقام · الهدف ${spec.targetMin}–${spec.targetMax} · ${spec.timeLimitSec}ث"
                    ),
                    color = colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        PdButton(
            text = AppStrings.tr(
                lang,
                "Начать соло-игру ▶",
                "Start Solo Game ▶",
                "Empezar partida Solo ▶",
                "开始单人挑战 ▶",
                "ابدأ اللعب الفردي ▶"
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
