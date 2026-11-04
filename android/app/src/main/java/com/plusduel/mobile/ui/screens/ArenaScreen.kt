package com.plusduel.mobile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plusduel.mobile.data.PlayerProgressState
import com.plusduel.mobile.engine.BotDifficulty
import com.plusduel.mobile.network.OnlineSessionState
import com.plusduel.mobile.ui.components.PdButton
import com.plusduel.mobile.ui.components.PdButtonVariant
import com.plusduel.mobile.ui.components.PdPanel
import com.plusduel.mobile.ui.components.PlusDuelLogoHeader
import com.plusduel.mobile.ui.components.dashedBorder
import com.plusduel.mobile.ui.components.statefulBorder
import com.plusduel.mobile.ui.i18n.AppStrings
import com.plusduel.mobile.ui.theme.PlusDuelTheme
import kotlinx.coroutines.delay

@Composable
fun ArenaScreen(
    progress: PlayerProgressState,
    onlineState: OnlineSessionState,
    selectedBot: BotDifficulty,
    onSelectBot: (BotDifficulty) -> Unit,
    onNicknameChange: (String) -> Unit,
    onQuickPlayOnline: () -> Unit,
    onStartBotDuel: (BotDifficulty) -> Unit,
    onCreatePrivateRoom: () -> Unit,
    onJoinPrivateRoom: (String) -> Unit,
    onCancelQueue: () -> Unit,
    onOpenShop: () -> Unit,
    onToggleAudio: () -> Unit,
    onClickSound: () -> Unit
) {
    val colors = PlusDuelTheme.colors
    val lang = progress.language
    val context = LocalContext.current

    var roomCodeInput by remember { mutableStateOf("") }
    var copiedToast by remember { mutableStateOf(false) }
    var queueWaitSec by remember { mutableIntStateOf(0) }

    LaunchedEffect(onlineState.queued) {
        queueWaitSec = 0
        while (onlineState.queued) {
            delay(1000L)
            queueWaitSec++
        }
    }

    LaunchedEffect(copiedToast) {
        if (copiedToast) {
            delay(2000L)
            copiedToast = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ==================== TOP STATUS BAR ====================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(colors.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, colors.line, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${progress.rating} · ${AppStrings.rankTitle(lang, progress.rankTitle)}",
                    color = colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(8.dp))
                        .clickable {
                            onClickSound()
                            onOpenShop()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = AppStrings.tr(
                            lang,
                            "${progress.coins} монет",
                            "${progress.coins} coins",
                            "${progress.coins} monedas",
                            "${progress.coins} 金币",
                            "${progress.coins} عملة"
                        ),
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                val audioOn = progress.soundEnabled || progress.musicEnabled
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(8.dp))
                        .clickable { onToggleAudio() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (audioOn) {
                            AppStrings.tr(lang, "Звук", "Sound", "Sonido", "音效", "الصوت")
                        } else {
                            AppStrings.tr(lang, "Тихо", "Muted", "Silencio", "静音", "صامت")
                        },
                        color = if (audioOn) colors.ink else colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // ==================== LOGO ====================
        PlusDuelLogoHeader(
            subtitle = AppStrings.tr(
                lang,
                "Математические дуэли 1 на 1",
                "1v1 competitive math duels",
                "Duelos matemáticos 1v1",
                "1v1 实时数学对决",
                "مبارزات رياضية ١ ضد ١"
            ),
            compact = true
        )

        if (onlineState.errorBanner.isNotBlank()) {
            val errText = if (onlineState.errorBanner == "OPPONENT_LEFT") {
                AppStrings.tr(lang, "Соперник вышел", "Opponent left", "Rival salió", "对手已退出", "غادر الخصم")
            } else {
                onlineState.errorBanner
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.danger.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .border(1.dp, colors.danger, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errText,
                    color = colors.danger,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        // ==================== QUEUED / PRIVATE ROOM WAITING STATE ====================
        if (onlineState.queued) {
            PdPanel(
                borderColor = colors.orange,
                backgroundColor = colors.surface,
                contentPadding = PaddingValues(16.dp)
            ) {
                if (onlineState.queuePosition == -1) {
                    Text(
                        text = AppStrings.tr(
                            lang,
                            "Приватная комната открыта",
                            "Private room open",
                            "Sala privada abierta",
                            "私人房间已开启",
                            "الغرفة الخاصة مفتوحة"
                        ),
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (onlineState.privateRoomCode.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.tint)
                                .dashedBorder(1.5.dp, colors.orange, 10.dp)
                                .clickable {
                                    onClickSound()
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("PlusDuel Code", onlineState.privateRoomCode))
                                    copiedToast = true
                                }
                                .padding(vertical = 16.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = onlineState.privateRoomCode,
                                    color = colors.orange,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 32.sp,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (copiedToast) {
                                        AppStrings.tr(lang, "Код скопирован", "Copied to clipboard", "Código copiado", "已复制房间码", "تم نسخ الرمز")
                                    } else {
                                        AppStrings.tr(lang, "Нажмите, чтобы скопировать", "Tap to copy code", "Toca para copiar", "点击复制房间码", "اضغط للنسخ")
                                    },
                                    color = if (copiedToast) colors.orange else colors.muted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = AppStrings.tr(
                                lang,
                                "Создание комнаты (${queueWaitSec} с)",
                                "Opening room (${queueWaitSec}s)",
                                "Abriendo sala (${queueWaitSec}s)",
                                "正在创建房间 (${queueWaitSec}s)",
                                "جارٍ فتح الغرفة (${queueWaitSec}s)"
                            ),
                            color = colors.muted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(
                        text = AppStrings.tr(
                            lang,
                            "Поиск соперника (${queueWaitSec} с)",
                            "Searching for opponent (${queueWaitSec}s)",
                            "Buscando rival (${queueWaitSec}s)",
                            "正在匹配对手 (${queueWaitSec}s)",
                            "جارٍ البحث عن خصم (${queueWaitSec}s)"
                        ),
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PdButton(
                        text = AppStrings.tr(
                            lang,
                            "Играть с ботом",
                            "Play vs bot",
                            "Jugar contra bot",
                            "人机对决",
                            "العب ضد البوت"
                        ),
                        variant = PdButtonVariant.ACCENT,
                        onClick = {
                            onClickSound()
                            onCancelQueue()
                            onStartBotDuel(selectedBot)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                PdButton(
                    text = AppStrings.tr(lang, "Отмена", "Cancel", "Cancelar", "取消", "إلغاء"),
                    variant = PdButtonVariant.OUTLINE,
                    onClick = {
                        onClickSound()
                        onCancelQueue()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // ==================== 1. ONLINE 1v1 QUICK PLAY CARD ====================
            PdPanel(contentPadding = PaddingValues(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.tr(lang, "Никнейм", "Nickname", "Apodo", "昵称", "الاسم"),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (onlineState.connected) colors.success else colors.muted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (onlineState.connected) {
                                AppStrings.tr(lang, "Онлайн", "Online", "En línea", "在线", "متصل")
                            } else {
                                AppStrings.tr(lang, "Офлайн", "Offline", "Sin red", "离线", "غير متصل")
                            },
                            color = colors.muted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(colors.bg, RoundedCornerShape(10.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (progress.nickname.isEmpty()) {
                        Text(
                            text = AppStrings.tr(lang, "Гость", "Guest", "Invitado", "访客", "ضيف"),
                            color = colors.muted.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                    BasicTextField(
                        value = progress.nickname,
                        onValueChange = { if (it.length <= 20) onNicknameChange(it) },
                        singleLine = true,
                        cursorBrush = SolidColor(colors.orange),
                        textStyle = TextStyle(
                            color = colors.ink,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                PdButton(
                    text = AppStrings.tr(
                        lang,
                        "Быстрый бой 1v1",
                        "Quick play 1v1",
                        "Partida rápida 1v1",
                        "快速匹配 1v1",
                        "مباراة سريعة 1v1"
                    ),
                    variant = PdButtonVariant.ACCENT,
                    badgeText = "+30",
                    onClick = {
                        onClickSound()
                        onQuickPlayOnline()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ==================== 2. SMART BOT 1v1 DUEL CARD ====================
            PdPanel(contentPadding = PaddingValues(16.dp)) {
                Text(
                    text = AppStrings.tr(
                        lang,
                        "Бой с ботом · до 3 побед",
                        "Smart bot · Best of 5",
                        "Contra bot · Al mejor de 5",
                        "智能人机 · 五局三胜",
                        "ضد البوت · الأفضل من ٥"
                    ),
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                for (row in BotDifficulty.entries.chunked(2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (bot in row) {
                            val active = selectedBot == bot
                            val interaction = remember(bot.id) { MutableInteractionSource() }
                            val pressed by interaction.collectIsPressedAsState()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active || pressed) colors.tint else colors.bg)
                                    .statefulBorder(
                                        isDashed = active || pressed,
                                        strokeWidth = 1.5.dp,
                                        color = if (active || pressed) colors.orange else colors.line,
                                        cornerRadius = 10.dp
                                    )
                                    .clickable(
                                        interactionSource = interaction,
                                        indication = null
                                    ) {
                                        onClickSound()
                                        onSelectBot(bot)
                                    }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = AppStrings.botDifficultyLabel(lang, bot),
                                    color = if (active || pressed) colors.orange else colors.ink,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                PdButton(
                    text = AppStrings.tr(
                        lang,
                        "Играть с ботом",
                        "Play vs bot",
                        "Jugar contra bot",
                        "开始人机对决",
                        "العب ضد البوت"
                    ),
                    variant = PdButtonVariant.PRIMARY,
                    badgeText = "+${selectedBot.winCoinBonus}",
                    onClick = {
                        onClickSound()
                        onStartBotDuel(selectedBot)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ==================== 3. PRIVATE ROOM WITH A FRIEND ====================
            PdPanel(contentPadding = PaddingValues(16.dp)) {
                Text(
                    text = AppStrings.tr(
                        lang,
                        "Приватная комната",
                        "Private room",
                        "Sala privada",
                        "私人房间",
                        "غرفة خاصة"
                    ),
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                PdButton(
                    text = AppStrings.tr(
                        lang,
                        "Создать комнату",
                        "Create room",
                        "Crear sala",
                        "创建房间",
                        "إنشاء غرفة"
                    ),
                    variant = PdButtonVariant.OUTLINE,
                    onClick = {
                        onClickSound()
                        onCreatePrivateRoom()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(colors.bg, RoundedCornerShape(10.dp))
                            .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (roomCodeInput.isEmpty()) {
                            Text(
                                text = AppStrings.tr(lang, "Код комнаты", "Room code", "Código", "房间码", "رمز الغرفة"),
                                color = colors.muted.copy(alpha = 0.6f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                        BasicTextField(
                            value = roomCodeInput,
                            onValueChange = { raw ->
                                val filtered = raw.uppercase().filter { it.isLetterOrDigit() }.take(6)
                                roomCodeInput = filtered
                            },
                            singleLine = true,
                            cursorBrush = SolidColor(colors.orange),
                            textStyle = TextStyle(
                                color = colors.ink,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    PdButton(
                        text = AppStrings.tr(lang, "Войти", "Join", "Entrar", "加入", "دخول"),
                        variant = PdButtonVariant.OUTLINE,
                        enabled = roomCodeInput.trim().length >= 4,
                        onClick = {
                            onClickSound()
                            onJoinPrivateRoom(roomCodeInput.trim())
                        }
                    )
                }
            }
        }
    }
}
