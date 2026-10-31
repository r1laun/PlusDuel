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
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==================== TOP STATUS BAR ====================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elo & Rank Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(colors.surface, RoundedCornerShape(999.dp))
                    .border(1.5.dp, colors.line, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "★ ${progress.rating} · ${AppStrings.rankTitle(lang, progress.rankTitle)}",
                    color = colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Coins Chip (opens Style/Shop)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.tint, RoundedCornerShape(999.dp))
                        .border(1.5.dp, colors.orange, RoundedCornerShape(999.dp))
                        .clickable {
                            onClickSound()
                            onOpenShop()
                        }
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "● ${progress.coins}",
                        color = colors.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Quick Audio Toggle
                val audioOn = progress.soundEnabled || progress.musicEnabled
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (audioOn) colors.tint else colors.surface, RoundedCornerShape(999.dp))
                        .border(1.5.dp, if (audioOn) colors.orange else colors.line, RoundedCornerShape(999.dp))
                        .clickable { onToggleAudio() }
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (audioOn) "♪ ON" else "♪ OFF",
                        color = if (audioOn) colors.orange else colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // ==================== LOGO & TAGLINE ====================
        PlusDuelLogoHeader(
            subtitle = AppStrings.tr(
                lang,
                "Математические дуэли 1 на 1 · Собери число из всех цифр быстрее соперника",
                "Real-time 1v1 math duels · Build the target from every digit faster",
                "Duelos matemáticos 1v1 en tiempo real · Usa cada dígito una vez",
                "实时 1v1 数学对决 · 用全部数字更快凑出目标数",
                "مبارزات رياضية ١ ضد ١ · كوّن الرقم المستهدف أسرع"
            )
        )

        if (onlineState.errorBanner.isNotBlank()) {
            val errText = if (onlineState.errorBanner == "OPPONENT_LEFT") {
                AppStrings.tr(lang, "Соперник покинул матч", "Opponent left the match", "El rival abandonó", "对手已离开对局", "غادر الخصم المباراة")
            } else {
                onlineState.errorBanner
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.danger.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                    .border(1.5.dp, colors.danger, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errText,
                    color = colors.danger,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ==================== QUEUED / PRIVATE ROOM WAITING STATE ====================
        if (onlineState.queued) {
            PdPanel(
                borderColor = colors.orange,
                backgroundColor = colors.surface,
                contentPadding = PaddingValues(20.dp)
            ) {
                if (onlineState.queuePosition == -1) {
                    Text(
                        text = AppStrings.tr(
                            lang,
                            "ПРИВАТНАЯ КОМНАТА ОТКРЫТА",
                            "PRIVATE ROOM OPEN",
                            "SALA PRIVADA ABIERTA",
                            "私人房间已开启",
                            "الغرفة الخاصة مفتوحة"
                        ),
                        color = colors.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (onlineState.privateRoomCode.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.tint)
                                .dashedBorder(1.5.dp, colors.orange, 16.dp)
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
                                    text = AppStrings.tr(lang, "КОД КОМНАТЫ", "ROOM CODE", "CÓDIGO", "房间代码", "رمز الغرفة"),
                                    color = colors.muted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = onlineState.privateRoomCode,
                                    color = colors.orange,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 34.sp,
                                    letterSpacing = 3.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (copiedToast) {
                                        AppStrings.tr(lang, "✓ Скопировано в буфер!", "✓ Copied to clipboard!", "✓ ¡Copiado!", "✓ 已复制！", "✓ تم النسخ!")
                                    } else {
                                        AppStrings.tr(lang, "Нажми, чтобы скопировать и отправить другу", "Tap to copy & share with a friend", "Toca para copiar", "点击复制并分享给好友", "اضغط للنسخ والمشاركة")
                                    },
                                    color = if (copiedToast) colors.orange else colors.muted,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (copiedToast) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = AppStrings.tr(
                                lang,
                                "Подключение к комнате… (${queueWaitSec}с)",
                                "Connecting to room… (${queueWaitSec}s)",
                                "Conectando a la sala… (${queueWaitSec}s)",
                                "正在连接房间… (${queueWaitSec}秒)",
                                "جارٍ الاتصال بالغرفة… (${queueWaitSec}ث)"
                            ),
                            color = colors.ink,
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
                            "Поиск онлайн-соперника… (${queueWaitSec}с)",
                            "Searching for online opponent… (${queueWaitSec}s)",
                            "Buscando rival en línea… (${queueWaitSec}s)",
                            "正在匹配在线对手… (${queueWaitSec}秒)",
                            "جارٍ البحث عن خصم… (${queueWaitSec}ث)"
                        ),
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (onlineState.connected) {
                            AppStrings.tr(
                                lang,
                                "Сервер подключён · ждём второго игрока или начните бой с ботом",
                                "Server connected · waiting for player or start a Bot Duel now",
                                "Servidor conectado · esperando jugador o juega contra el Bot",
                                "服务器已连接 · 等待玩家或立即开始人机对战",
                                "الخادم متصل · بانتظار لاعب أو ابدأ ضد الذكاء الاصطناعي"
                            )
                        } else {
                            AppStrings.tr(
                                lang,
                                "Пробуждение сервера… Можно сразу начать дуэль с ботом!",
                                "Waking server… Or start a Bot Duel immediately!",
                                "Conectando al servidor… ¡O empieza contra el Bot ya!",
                                "正在唤醒服务器… 也可以立即开始人机对战！",
                                "جارٍ الاتصال… أو ابدأ ضد البوت فوراً!"
                            )
                        },
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PdButton(
                        text = AppStrings.tr(
                            lang,
                            "Сыграть с ботом сейчас ▶",
                            "Play vs Smart Bot Now ▶",
                            "Jugar contra Bot ahora ▶",
                            "立即与智能机器人对战 ▶",
                            "العب ضد البوت الآن ▶"
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

                Spacer(modifier = Modifier.height(10.dp))
                PdButton(
                    text = AppStrings.tr(lang, "Отменить", "Cancel", "Cancelar", "取消", "إلغاء"),
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
            PdPanel(contentPadding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.tr(lang, "ТВОЙ НИКНЕЙМ", "YOUR NICKNAME", "TU APODO", "你的昵称", "اسم اللاعب"),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (onlineState.connected) colors.success else colors.orange)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (onlineState.connected) {
                                AppStrings.tr(lang, "Онлайн готов", "Server Online", "En línea", "在线就绪", "متصل")
                            } else {
                                AppStrings.tr(lang, "Автономный / Бот", "Offline / Bot Ready", "Modo Bot listo", "离线 / 人机就绪", "جاهز للعب")
                            },
                            color = colors.muted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bg, RoundedCornerShape(12.dp))
                        .border(1.5.dp, colors.line, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (progress.nickname.isEmpty()) {
                        Text(
                            text = AppStrings.tr(lang, "Анонимный игрок (введите ник)…", "Anonymous player (enter nickname)…", "Jugador anónimo…", "输入你的昵称…", "أدخل اسمك المستعار…"),
                            color = colors.muted.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
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
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                PdButton(
                    text = AppStrings.tr(
                        lang,
                        "Быстрый бой 1v1 (Онлайн) ▶",
                        "Quick Play 1v1 (Online) ▶",
                        "Partida rápida 1v1 (Online) ▶",
                        "快速在线 1v1 匹配 ▶",
                        "مباراة سريعة ١ ضد ١ (أونلاين) ▶"
                    ),
                    variant = PdButtonVariant.ACCENT,
                    badgeText = "+30 ●",
                    onClick = {
                        onClickSound()
                        onQuickPlayOnline()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ==================== 2. SMART BOT 1v1 DUEL CARD ====================
            PdPanel(contentPadding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.tr(
                            lang,
                            "ДУЭЛЬ С УМНЫМ БОТОМ (ДО 3 ПОБЕД)",
                            "1v1 DUEL VS SMART BOT (BEST OF 5)",
                            "DUELO 1v1 CONTRA BOT (AL MEJOR DE 5)",
                            "智能机器人 1v1 对决（五局三胜）",
                            "مبارزة ١ ضد ١ مع البوت الذكي"
                        ),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "+${selectedBot.winCoinBonus} ●",
                        color = colors.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active || pressed) colors.tint else colors.bg)
                                    .statefulBorder(
                                        isDashed = active || pressed,
                                        strokeWidth = if (active) 2.dp else 1.5.dp,
                                        color = if (active || pressed) colors.orange else colors.line,
                                        cornerRadius = 12.dp
                                    )
                                    .clickable(
                                        interactionSource = interaction,
                                        indication = null
                                    ) {
                                        onClickSound()
                                        onSelectBot(bot)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = AppStrings.botDifficultyLabel(lang, bot),
                                    color = if (active || pressed) colors.orange else colors.ink,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = AppStrings.tr(
                        lang,
                        "Мгновенный старт даже без интернета · Рейтинговый бой 1 на 1",
                        "Instant start offline or online · Rated best-of-5 match",
                        "Inicio instantáneo con o sin internet · Partida clasificatoria",
                        "无需等待即可开局（支持离线）· 计入 Elo 积分与金币奖励",
                        "بداية فورية حتى بدون إنترنت · مباراة مصنفة"
                    ),
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                PdButton(
                    text = AppStrings.tr(
                        lang,
                        "Начать бой с ботом ▶",
                        "Start Bot Duel ▶",
                        "Empezar duelo vs Bot ▶",
                        "开始人机对决 ▶",
                        "ابدأ المبارزة ضد البوت ▶"
                    ),
                    variant = PdButtonVariant.PRIMARY,
                    badgeText = "+${selectedBot.winCoinBonus} ●",
                    onClick = {
                        onClickSound()
                        onStartBotDuel(selectedBot)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ==================== 3. PRIVATE ROOM WITH A FRIEND ====================
            PdPanel(contentPadding = PaddingValues(18.dp)) {
                Text(
                    text = AppStrings.tr(
                        lang,
                        "ПРИВАТНАЯ ДУЭЛЬ С ДРУГОМ",
                        "PRIVATE DUEL WITH A FRIEND",
                        "DUELO PRIVADO CON UN AMIGO",
                        "好友私人房间对决",
                        "مبارزة خاصة مع صديق"
                    ),
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                PdButton(
                    text = AppStrings.tr(
                        lang,
                        "Создать приватную комнату",
                        "Create Private Room",
                        "Crear sala privada",
                        "创建私人房间",
                        "إنشاء غرفة خاصة"
                    ),
                    variant = PdButtonVariant.OUTLINE,
                    onClick = {
                        onClickSound()
                        onCreatePrivateRoom()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(colors.bg, RoundedCornerShape(12.dp))
                            .border(1.5.dp, colors.line, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (roomCodeInput.isEmpty()) {
                            Text(
                                text = AppStrings.tr(lang, "КОД КОМНАТЫ", "ROOM CODE", "CÓDIGO", "输入房间码", "رمز الغرفة"),
                                color = colors.muted.copy(alpha = 0.6f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
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
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 2.sp
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
