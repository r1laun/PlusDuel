package com.plusduel.mobile.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plusduel.mobile.data.AppLanguage
import com.plusduel.mobile.data.COINS_PER_ROUND_SOLVED
import com.plusduel.mobile.data.TileShapeStyle
import com.plusduel.mobile.engine.ActiveGameKind
import com.plusduel.mobile.engine.ExpressionValidator
import com.plusduel.mobile.engine.MatchResultState
import com.plusduel.mobile.engine.RoundOutcome
import com.plusduel.mobile.engine.RoundState
import com.plusduel.mobile.engine.ValidationErrorCode
import com.plusduel.mobile.sound.SoundManager
import com.plusduel.mobile.ui.components.PdButton
import com.plusduel.mobile.ui.components.PdButtonVariant
import com.plusduel.mobile.ui.components.PdPanel
import com.plusduel.mobile.ui.components.PlusDuelLogoHeader
import com.plusduel.mobile.ui.components.TactileKeypadButton
import com.plusduel.mobile.ui.components.statefulBorder
import com.plusduel.mobile.ui.i18n.AppStrings
import com.plusduel.mobile.ui.theme.PlusDuelTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private data class ExprEntry(
    val text: String,
    val digitSlotIndex: Int? = null
)

private val OPERATOR_SYMBOLS = listOf("+", "−", "×", "÷", "(", ")", "^", "√", "!")
private const val DIGIT_SLOTS = 5

@Composable
fun DuelGameScreen(
    gameKind: ActiveGameKind,
    myId: String,
    opponentName: String,
    rated: Boolean,
    round: RoundState?,
    roundOutcome: RoundOutcome?,
    matchResult: MatchResultState?,
    myWins: Int,
    oppWins: Int,
    soloStreak: Int,
    errorBanner: String,
    language: AppLanguage,
    tileShape: TileShapeStyle,
    soundManager: SoundManager,
    onSubmitExpression: (String) -> Unit,
    onPlayAgain: (() -> Unit)?,
    onLeave: () -> Unit
) {
    val colors = PlusDuelTheme.colors
    val coroutineScope = rememberCoroutineScope()
    val tileRadius = tileShape.cornerDp.dp

    // ==================== MATCH ENDED SCREEN ====================
    if (matchResult != null) {
        LaunchedEffect(matchResult) {
            when {
                matchResult.isDraw -> soundManager.playMatchDraw()
                matchResult.iWon -> soundManager.playMatchWin()
                else -> soundManager.playMatchLose()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlusDuelLogoHeader(compact = true)
            Spacer(modifier = Modifier.height(14.dp))

            val headline = when {
                matchResult.isDraw -> AppStrings.tr(language, "НИЧЬЯ", "DRAW", "EMPATE", "平局", "تعادل")
                matchResult.iWon -> AppStrings.tr(language, "ПОБЕДА!", "VICTORY!", "¡VICTORIA!", "胜利！", "انتصار!")
                else -> AppStrings.tr(language, "ПОРАЖЕНИЕ", "DEFEAT", "DERROTA", "惜败", "هزيمة")
            }

            Text(
                text = headline,
                color = if (matchResult.iWon) colors.orange else colors.ink,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            val subMsg = when {
                matchResult.reason == "forfeit" -> AppStrings.tr(
                    language,
                    "Соперник покинул матч — победа присуждена вам!",
                    "Opponent forfeited the match!",
                    "¡El rival abandonó la partida!",
                    "对手退出对局，你直接获胜！",
                    "انسحب الخصم من المباراة!"
                )
                matchResult.isDraw -> AppStrings.tr(
                    language,
                    "Равная борьба до последней секунды!",
                    "Evenly matched to the final second!",
                    "¡Igualados hasta el último segundo!",
                    "势均力敌的对决！",
                    "مباراة متكافئة حتى اللحظة الأخيرة!"
                )
                matchResult.iWon -> AppStrings.tr(
                    language,
                    "Ты оказался быстрее и точнее в расчётах!",
                    "You solved faster and outcalculated ${matchResult.opponentName}!",
                    "¡Calculaste más rápido que ${matchResult.opponentName}!",
                    "你的计算比 ${matchResult.opponentName} 更快更准！",
                    "كنت أسرع في الحساب من ${matchResult.opponentName}!"
                )
                else -> AppStrings.tr(
                    language,
                    "${matchResult.opponentName} выиграл этот матч.",
                    "${matchResult.opponentName} took the match.",
                    "${matchResult.opponentName} ganó la partida.",
                    "${matchResult.opponentName} 赢得了本场对局。",
                    "فاز ${matchResult.opponentName} بالمباراة."
                )
            }

            Text(
                text = subMsg,
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            PdPanel(
                borderColor = if (matchResult.iWon) colors.orange else colors.line,
                backgroundColor = colors.surface,
                contentPadding = PaddingValues(18.dp)
            ) {
                if (matchResult.ratingDelta != null) {
                    val rd = matchResult.ratingDelta
                    val sign = if (rd.delta >= 0) "+${rd.delta}" else rd.delta.toString()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.tint, RoundedCornerShape(12.dp))
                            .border(1.dp, colors.orange, RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Elo ${rd.after} ($sign) · ${AppStrings.rankTitle(language, rd.title)}",
                            color = colors.orange,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (matchResult.coinsEarned > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.bg, RoundedCornerShape(12.dp))
                            .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = AppStrings.tr(
                                language,
                                "Награда за матч: +${matchResult.coinsEarned} ● монет",
                                "Match Reward: +${matchResult.coinsEarned} ● coins",
                                "Recompensa: +${matchResult.coinsEarned} ● monedas",
                                "对局奖励：+${matchResult.coinsEarned} ● 金币",
                                "مكافأة المباراة: +${matchResult.coinsEarned} ● عملة"
                            ),
                            color = colors.ink,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // My score row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.tint, RoundedCornerShape(12.dp))
                        .border(1.5.dp, colors.orange, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.tr(language, "Ты", "You", "Tú", "你", "أنت"),
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${matchResult.myWins}W · ${String.format(Locale.US, "%.1f", matchResult.myScore)}",
                        color = colors.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Opponent score row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bg, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = matchResult.opponentName,
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${matchResult.oppWins}W · ${String.format(Locale.US, "%.1f", matchResult.oppScore)}",
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (onPlayAgain != null) {
                PdButton(
                    text = AppStrings.tr(language, "Сыграть ещё раз ▶", "Play Again ▶", "Jugar de nuevo ▶", "再来一局 ▶", "العب مرة أخرى ▶"),
                    variant = PdButtonVariant.ACCENT,
                    onClick = {
                        soundManager.playClick()
                        onPlayAgain()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            PdButton(
                text = AppStrings.tr(language, "Вернуться в лобби", "Back to Lobby", "Volver al lobby", "返回大厅", "العودة للقائمة"),
                variant = if (onPlayAgain != null) PdButtonVariant.OUTLINE else PdButtonVariant.PRIMARY,
                onClick = {
                    soundManager.playClick()
                    onLeave()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        return
    }

    // ==================== WAITING FOR FIRST ROUND ====================
    if (round == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlusDuelLogoHeader(compact = true)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = AppStrings.tr(language, "Генерация раунда…", "Preparing round…", "Preparando ronda…", "正在生成题目…", "جارٍ تجهيز الجولة…"),
                color = colors.ink,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            PdButton(
                text = AppStrings.tr(language, "← Выход", "← Leave", "← Salir", "← 退出", "← خروج"),
                variant = PdButtonVariant.OUTLINE,
                onClick = {
                    soundManager.playClick()
                    onLeave()
                }
            )
        }
        return
    }

    // ==================== ACTIVE ROUND OR BETWEEN-ROUND SUMMARY ====================
    val entries = remember(round.index, round.target, round.digits) {
        mutableStateListOf<ExprEntry>()
    }
    var feedbackMessage by remember(round.index, round.target, round.digits) { mutableStateOf("") }
    var feedbackIsError by remember(round.index, round.target, round.digits) { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    val active = roundOutcome == null

    // Remaining time ticker
    var remainingMs by remember(round.index, round.receivedAtMs, round.timeLimitMs) {
        mutableLongStateOf(round.timeLimitMs)
    }
    LaunchedEffect(round.index, round.receivedAtMs, round.timeLimitMs, active) {
        while (active) {
            val elapsed = (System.currentTimeMillis() - round.receivedAtMs).coerceAtLeast(0L)
            remainingMs = (round.timeLimitMs - elapsed).coerceAtLeast(0L)
            delay(80L)
        }
    }

    LaunchedEffect(roundOutcome) {
        if (roundOutcome != null) {
            if (roundOutcome.winnerId == myId) {
                soundManager.playRoundWin()
            } else {
                soundManager.playRoundLose()
            }
        }
    }

    val expressionString = entries.joinToString("") { it.text }
    val usedSlotIndices = entries.mapNotNull { it.digitSlotIndex }.toSet()

    val livePreview = remember(expressionString, round.digits, round.target) {
        ExpressionValidator.previewExpression(expressionString, round.digits, round.target)
    }

    fun triggerShake() {
        coroutineScope.launch {
            for (delta in listOf(-12f, 12f, -8f, 8f, -4f, 0f)) {
                shakeOffset.animateTo(delta, tween(40))
            }
        }
    }

    fun handleSubmit() {
        if (!active || expressionString.isBlank()) return
        val result = ExpressionValidator.validateExpression(
            expression = expressionString,
            issuedDigits = round.digits,
            target = round.target
        )
        if (!result.valid) {
            soundManager.playMistake()
            triggerShake()
            feedbackIsError = true
            feedbackMessage = AppStrings.validationError(
                lang = language,
                code = result.errorCode,
                detail = result.errorDetail,
                target = round.target
            )
            return
        }
        soundManager.playSubmit()
        onSubmitExpression(expressionString)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ==================== TOP SECTION (FIXED STRUCTURE) ====================
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. TOP BAR: Exit | Timer Bar | Score | Round
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val backInteraction = remember { MutableInteractionSource() }
                val backPressed by backInteraction.collectIsPressedAsState()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (backPressed) colors.tint else colors.surface)
                        .statefulBorder(
                            isDashed = backPressed,
                            strokeWidth = 1.5.dp,
                            color = colors.ink,
                            cornerRadius = 12.dp
                        )
                        .clickable(
                            interactionSource = backInteraction,
                            indication = null
                        ) {
                            soundManager.playClick()
                            onLeave()
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = AppStrings.tr(language, "← ВЫХ", "← EXT", "← SAL", "← 退出", "← خروج"),
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Timer Bar
                val progressFraction = if (round.timeLimitMs > 0 && active) {
                    (remainingMs.toFloat() / round.timeLimitMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val secText = if (active) {
                    String.format(Locale.US, "%.1fs", remainingMs / 1000.0)
                } else {
                    "0.0s"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.surface)
                        .border(1.5.dp, colors.line, RoundedCornerShape(999.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(
                                if (progressFraction < 0.22f) colors.danger.copy(alpha = 0.28f)
                                else colors.orange.copy(alpha = 0.22f)
                            )
                    )
                    Text(
                        text = secText,
                        color = if (progressFraction < 0.22f && active) colors.danger else colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Score Badge
                val displayMyWins = roundOutcome?.myWins ?: myWins
                val displayOppWins = roundOutcome?.oppWins ?: oppWins
                Box(
                    modifier = Modifier
                        .background(colors.tint, RoundedCornerShape(12.dp))
                        .border(1.5.dp, colors.orange, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (gameKind == ActiveGameKind.SOLO_ENDLESS) {
                            "🔥 $soloStreak"
                        } else {
                            "$displayMyWins : $displayOppWins"
                        },
                        color = colors.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Round Index Badge
                Box(
                    modifier = Modifier
                        .background(colors.surface, RoundedCornerShape(12.dp))
                        .border(1.5.dp, colors.line, RoundedCornerShape(12.dp))
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "R${round.index}",
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // 2. TARGET CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = colors.surface,
                border = BorderStroke(1.5.dp, colors.ink)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.targetHeader(language),
                            color = colors.muted,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = opponentName,
                            color = colors.orange,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = round.target.toString(),
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 38.sp
                    )

                    Text(
                        text = AppStrings.useEveryDigitOnce(language, usedSlotIndices.size, round.digits.size),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. FIXED-HEIGHT STATUS BANNER (58.dp SO KEYPAD NEVER JUMPS)
            val showSolvedHighlight = livePreview.isSolved
            val hasServerBanner = errorBanner.isNotBlank()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(
                        color = when {
                            hasServerBanner -> colors.danger.copy(alpha = 0.1f)
                            feedbackIsError && feedbackMessage.isNotEmpty() -> colors.danger.copy(alpha = 0.1f)
                            showSolvedHighlight -> colors.tint
                            else -> colors.surface
                        },
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = when {
                            hasServerBanner -> colors.danger
                            feedbackIsError && feedbackMessage.isNotEmpty() -> colors.danger
                            showSolvedHighlight -> colors.orange
                            else -> colors.line
                        },
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    hasServerBanner -> {
                        val txt = if (errorBanner == "OPPONENT_LEFT") {
                            AppStrings.tr(language, "Соперник покинул матч", "Opponent left the match", "El rival abandonó", "对手已离开对局", "غادر الخصم المباراة")
                        } else errorBanner
                        Text(
                            text = txt,
                            color = colors.danger,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    showSolvedHighlight -> {
                        Text(
                            text = AppStrings.readyToSubmitBanner(language),
                            color = colors.orange,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    feedbackMessage.isNotEmpty() -> {
                        Text(
                            text = feedbackMessage,
                            color = if (feedbackIsError) colors.danger else colors.ink,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    livePreview.syntaxErrorCode == ValidationErrorCode.LEADING_ZERO -> {
                        Text(
                            text = AppStrings.validationError(
                                language,
                                ValidationErrorCode.LEADING_ZERO,
                                livePreview.syntaxErrorDetail,
                                round.target
                            ),
                            color = colors.danger,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    !rated && gameKind == ActiveGameKind.ONLINE_DUEL -> {
                        Text(
                            text = AppStrings.tr(
                                language,
                                "Товарищеский матч · Операторы: + − × ÷ ( ) ^ √ !",
                                "Unrated match · Operators: + − × ÷ ( ) ^ √ !",
                                "Partida amistosa · Operadores: + − × ÷ ( ) ^ √ !",
                                "非排位赛 · 运算符: + − × ÷ ( ) ^ √ !",
                                "مباراة ودية · العمليات: + − × ÷ ( ) ^ √ !"
                            ),
                            color = colors.muted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        Text(
                            text = AppStrings.operatorsHint(language),
                            color = colors.muted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 4. EXPRESSION DISPLAY BOX (FIXED 56.dp HEIGHT, ALWAYS LTR)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            color = if (showSolvedHighlight) colors.tint else colors.surface,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = if (showSolvedHighlight) 2.dp else 1.5.dp,
                            color = if (showSolvedHighlight) colors.orange else colors.ink,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (entries.isEmpty()) {
                            Text(
                                text = AppStrings.tapTilesPlaceholder(language),
                                color = colors.muted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Text(
                                text = ExpressionValidator.normalizePretty(expressionString),
                                color = if (showSolvedHighlight) colors.orange else colors.ink,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val liveVal = livePreview.liveResult
                        if (liveVal != null) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (showSolvedHighlight) colors.orange else colors.bg,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (showSolvedHighlight) colors.orange else colors.line,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "= ${liveVal.toDisplayString()}",
                                    color = if (showSolvedHighlight) Color.White else colors.muted,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==================== BOTTOM SECTION: KEYPAD OR ROUND END PANEL ====================
        if (active) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. SMART DIGIT ROW (5 SLOTS, 54.dp HEIGHT)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (slotIndex in 0 until DIGIT_SLOTS) {
                            val digit = round.digits.getOrNull(slotIndex)
                            val isUsed = slotIndex in usedSlotIndices

                            if (digit == null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .background(colors.surface, RoundedCornerShape(tileRadius))
                                        .border(1.5.dp, colors.line, RoundedCornerShape(tileRadius)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "·",
                                        color = colors.muted.copy(alpha = 0.4f),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                            } else {
                                val digitInteraction = remember(slotIndex) { MutableInteractionSource() }
                                val isPressed by digitInteraction.collectIsPressedAsState()
                                val scale by animateFloatAsState(
                                    targetValue = if (isPressed) 0.94f else 1f,
                                    label = "digitScale"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .scale(scale)
                                        .clip(RoundedCornerShape(tileRadius))
                                        .background(
                                            color = if (isUsed || isPressed) colors.tint else colors.ink,
                                            shape = RoundedCornerShape(tileRadius)
                                        )
                                        .statefulBorder(
                                            isDashed = isPressed || isUsed,
                                            strokeWidth = 1.5.dp,
                                            color = if (isUsed || isPressed) colors.orange else colors.ink,
                                            cornerRadius = tileRadius
                                        )
                                        .clickable(
                                            interactionSource = digitInteraction,
                                            indication = null
                                        ) {
                                            if (!isUsed) {
                                                soundManager.playKey()
                                                feedbackMessage = ""
                                                entries.add(ExprEntry(digit.toString(), slotIndex))
                                            } else {
                                                val entryIdx = entries.indexOfLast { it.digitSlotIndex == slotIndex }
                                                if (entryIdx != -1) {
                                                    soundManager.playDelete()
                                                    feedbackMessage = ""
                                                    entries.removeAt(entryIdx)
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = digit.toString(),
                                        color = if (isUsed || isPressed) colors.orange else colors.onInk,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    )
                                    if (isUsed) {
                                        Text(
                                            text = "✓",
                                            color = colors.orange,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 3.dp, end = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. OPERATOR KEYPAD (3x3 SYMMETRIC GRID)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (row in OPERATOR_SYMBOLS.chunked(3)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (op in row) {
                                    TactileKeypadButton(
                                        label = op,
                                        fontSizeSp = 20,
                                        cornerRadius = tileRadius,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        onClick = {
                                            soundManager.playKey()
                                            feedbackMessage = ""
                                            entries.add(ExprEntry(op, null))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. ACTIONS ROW (BACKSPACE / CLEAR / SUBMIT)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TactileKeypadButton(
                            label = "⌫",
                            fontSizeSp = 19,
                            cornerRadius = tileRadius,
                            enabled = entries.isNotEmpty(),
                            modifier = Modifier
                                .weight(0.75f)
                                .height(50.dp),
                            onClick = {
                                soundManager.playDelete()
                                feedbackMessage = ""
                                if (entries.isNotEmpty()) {
                                    entries.removeAt(entries.lastIndex)
                                }
                            }
                        )

                        TactileKeypadButton(
                            label = AppStrings.clearBtn(language),
                            fontSizeSp = 13,
                            cornerRadius = tileRadius,
                            enabled = entries.isNotEmpty(),
                            modifier = Modifier
                                .weight(0.95f)
                                .height(50.dp),
                            onClick = {
                                soundManager.playDelete()
                                feedbackMessage = ""
                                entries.clear()
                            }
                        )

                        PdButton(
                            text = AppStrings.submitBtn(language),
                            variant = if (livePreview.isSolved) PdButtonVariant.ACCENT else PdButtonVariant.PRIMARY,
                            enabled = entries.isNotEmpty(),
                            isActive = livePreview.isSolved,
                            onClick = { handleSubmit() },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(50.dp)
                        )
                    }
                }
            }
        } else {
            // ==================== ROUND END SUMMARY PANEL ====================
            val iWonRound = roundOutcome.winnerId == myId
            val timedOut = roundOutcome.winnerId == null || roundOutcome.winningExpression == null

            PdPanel(
                borderColor = colors.orange,
                backgroundColor = colors.surface,
                contentPadding = PaddingValues(20.dp)
            ) {
                val titleText = when {
                    iWonRound -> AppStrings.tr(
                        language,
                        "✓ Раунд за тобой! +$COINS_PER_ROUND_SOLVED ●",
                        "✓ You won the round! +$COINS_PER_ROUND_SOLVED ●",
                        "✓ ¡Ganaste la ronda! +$COINS_PER_ROUND_SOLVED ●",
                        "✓ 你赢下了本回合！+$COINS_PER_ROUND_SOLVED ●",
                        "✓ فزت بالجولة! +$COINS_PER_ROUND_SOLVED ●"
                    )
                    timedOut -> AppStrings.tr(
                        language,
                        "Время раунда истекло",
                        "Round timed out",
                        "Tiempo agotado",
                        "本回合超时",
                        "انتهى وقت الجولة"
                    )
                    else -> AppStrings.tr(
                        language,
                        "$opponentName забрал раунд",
                        "$opponentName won the round",
                        "$opponentName ganó la ronda",
                        "$opponentName 赢得了本回合",
                        "فاز $opponentName بالجولة"
                    )
                }

                Text(
                    text = titleText,
                    color = if (iWonRound) colors.orange else colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                val formulaToShow = roundOutcome.winningExpression ?: roundOutcome.sampleSolution
                if (!formulaToShow.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.tint, RoundedCornerShape(14.dp))
                                .border(1.5.dp, colors.orange, RoundedCornerShape(14.dp))
                                .padding(vertical = 12.dp, horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${ExpressionValidator.normalizePretty(formulaToShow)} = ${round.target}",
                                color = colors.orange,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = AppStrings.tr(
                        language,
                        "Следующий раунд начинается…",
                        "Next round starting…",
                        "Siguiente ronda…",
                        "下一回合即将开始…",
                        "الجولة التالية تبدأ الآن…"
                    ),
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
