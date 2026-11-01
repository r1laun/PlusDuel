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
            .padding(horizontal = 18.dp, vertical = 12.dp),
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
                    .background(colors.surface, RoundedCornerShape(999.dp))
                    .border(1.5.dp, colors.line, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "★ ${progress.rating} · ${AppStrings.rankTitle(lang, progress.rankTitle)}",
                    color = colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
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
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

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
                "1 на 1 математические дуэли",
                "Real-time 1v1 Competitive Math",
                "Duelos matemáticos 1v1",
                "1v1 实时数学竞技",
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
                    .background(colors.danger.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                    .border(1.5.dp, colors.danger, RoundedCornerShape(14.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errText,
                    color = colors.danger,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
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
                contentPadding = PaddingValues(18.dp)
            ) {
                if (onlineState.queuePosition == -1) {
                    Text(
                        text = AppStrings.tr(
                            lang,
                            "ПРИВАТНАЯ КОМНАТА",
                            "PRIVATE ROOM OPEN",
                            "SALA PRIVADA",
                            "私人房间",
                            "غرفة خاصة"
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
                                .padding(vertical = 14.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = onlineState.privateRoomCode,
                                    color = colors.orange,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp,
                                    letterSpacing = 3.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (copiedToast) {
                                        AppStrings.tr(lang, "✓ Скопировано", "✓ Copied!", "✓ Copiado", "✓ 已复制", "✓ تم النسخ")
                                    } else {
                                        AppStrings.tr(lang, "Нажми, чтобы скопировать", "Tap to copy code", "Toca para copiar", "点击复制房间码", "اضغط للنسخ")
                                    },
                                    color = if (copiedToast) colors.orange else colors.muted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = AppStrings.tr(
                                lang,
                                "Создание комнаты… (${queueWaitSec}с)",
                                "Opening room… (${queueWaitSec}s)",
                                "Abriendo sala… (${queueWaitSec}s)",
                                "正在创建… (${queueWaitSec}秒)",
                                "جارٍ الفتح… (${queueWaitSec}ث)"
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
                            "Поиск соперника… (${queueWaitSec}с)",
                            "Searching opponent… (${queueWaitSec}s)",
                            "Buscando rival… (${queueWaitSec}s)",
                            "正在匹配… (${queueWaitSec}秒)",
                            "جارٍ البحث… (${queueWaitSec}ث)"
                        ),
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PdButton(
                        text = AppStrings.tr(
                            lang,
                            "Бой с ботом ▶",
                            "Play vs Bot ▶",
                            "Jugar vs Bot ▶",
                            "人机对战 ▶",
                            "العب ضد البوت ▶"
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
                        text = AppStrings.tr(lang, "НИКНЕЙМ", "NICKNAME", "APODO", "昵称", "الاسم"),
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
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (onlineState.connected) "Online" else "Bot Ready",
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
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    if (progress.nickname.isEmpty()) {
                        Text(
                            text = AppStrings.tr(lang, "Аноним", "Anonymous", "Anónimo", "匿名玩家", "لاعب"),
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
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                PdButton(
                    text = AppStrings.tr(
                        lang,
                        "Онлайн 1v1 ▶",
                        "1v1 Online ▶",
                        "1v1 Online ▶",
                        "在线 1v1 ▶",
                        "أونلاين 1v1 ▶"
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
            PdPanel(contentPadding = PaddingValues(16.dp)) {
                Text(
                    text = AppStrings.tr(
                        lang,
                        "БОЙ С БОТОМ (ДО 3 ПОБЕД)",
                        "VS SMART BOT (BEST OF 5)",
                        "VS BOT (AL MEJOR DE 5)",
                        "智能人机对决 (五局三胜)",
                        "ضد البوت الذكي"
                    ),
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
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
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = AppStrings.botDifficultyLabel(lang, bot),
                                    color = if (active || pressed) colors.orange else colors.ink,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
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
                        "Бой с ботом ▶",
                        "Play vs Bot ▶",
                        "Jugar vs Bot ▶",
                        "开始人机 ▶",
                        "العب ضد البوت ▶"
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
            PdPanel(contentPadding = PaddingValues(16.dp)) {
                Text(
                    text = AppStrings.tr(
                        lang,
                        "ПРИВАТНАЯ КОМНАТА",
                        "PRIVATE ROOM",
                        "SALA PRIVADA",
                        "好友房间",
                        "غرفة خاصة"
                    ),
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                PdButton(
                    text = AppStrings.tr(
                        lang,
                        "Создать комнату",
                        "Create Room",
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
                            .background(colors.bg, RoundedCornerShape(12.dp))
                            .border(1.5.dp, colors.line, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        if (roomCodeInput.isEmpty()) {
                            Text(
                                text = AppStrings.tr(lang, "КОД", "ROOM CODE", "CÓDIGO", "房间码", "الرمز"),
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
