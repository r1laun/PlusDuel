package com.plusduel.mobile.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.plusduel.mobile.data.AppLanguage
import com.plusduel.mobile.data.PlayerProgressState
import com.plusduel.mobile.data.ThemeMode
import com.plusduel.mobile.ui.components.PdButton
import com.plusduel.mobile.ui.components.PdButtonVariant
import com.plusduel.mobile.ui.components.PdPanel
import com.plusduel.mobile.ui.components.PlusDuelLogoHeader
import com.plusduel.mobile.ui.components.statefulBorder
import com.plusduel.mobile.ui.i18n.AppStrings
import com.plusduel.mobile.ui.theme.PlusDuelTheme

@Composable
fun SettingsScreen(
    progress: PlayerProgressState,
    onSetTheme: (ThemeMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetSound: (Boolean) -> Unit,
    onSetMusic: (Boolean) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onResetStats: () -> Unit,
    onClickSound: () -> Unit
) {
    val colors = PlusDuelTheme.colors
    val lang = progress.language
    var showPrivacyModal by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

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
                "Настройки звука, языка, темы и правила дуэлей",
                "Audio, language, theme settings & duel rules",
                "Ajustes de sonido, idioma, tema y reglas",
                "音效、语言、主题设置与对决规则",
                "إعدادات الصوت واللغة والمظهر وقواعد اللعبة"
            ),
            compact = true
        )

        // ==================== AUDIO & HAPTICS ====================
        PdPanel(contentPadding = PaddingValues(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.tr(lang, "Музыка в меню", "Menu Ambient Music", "Música del menú", "主菜单背景音乐", "موسيقى القائمة"),
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = AppStrings.tr(lang, "Мягкая фоновая мелодия в лобби", "Smooth ambient melody in lobby", "Melodía suave en el lobby", "在大厅播放柔和背景旋律", "موسيقى هادئة في القائمة"),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = progress.musicEnabled,
                    onCheckedChange = {
                        onClickSound()
                        onSetMusic(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.orange
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.tr(lang, "Звуки нажатий (SFX)", "Sound Effects (SFX)", "Efectos de sonido", "按键音效 (SFX)", "المؤثرات الصوتية"),
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = AppStrings.tr(lang, "Акустические звуки плиток и побед", "Warm acoustic tile & victory sounds", "Sonidos acústicos suaves", "柔和原声按键与胜利音效", "مؤثرات صوتية دافئة"),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = progress.soundEnabled,
                    onCheckedChange = { onSetSound(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.orange
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.tr(lang, "Тактильная вибрация", "Haptic Vibration", "Vibración táctil", "触感震动反馈", "اهتزاز اللمس"),
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = AppStrings.tr(lang, "Отдача при нажатии клавиш и ошибках", "Tactile feedback on tile taps", "Respuesta al tocar fichas", "点击方块时的震动反馈", "استجابة لمسية عند النقر"),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = progress.hapticsEnabled,
                    onCheckedChange = {
                        onSetHaptics(it)
                        onClickSound()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.orange
                    )
                )
            }
        }

        // ==================== LANGUAGE & THEME ====================
        PdPanel(contentPadding = PaddingValues(18.dp)) {
            Text(
                text = AppStrings.tr(lang, "ЯЗЫК ИНТЕРФЕЙСА", "LANGUAGE", "IDIOMA", "语言", "اللغة"),
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (itemLang in AppLanguage.entries) {
                    val active = progress.language == itemLang
                    val interaction = remember(itemLang.code) { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active || pressed) colors.tint else colors.surface)
                            .statefulBorder(
                                isDashed = active || pressed,
                                strokeWidth = if (active) 2.dp else 1.5.dp,
                                color = if (active || pressed) colors.orange else colors.ink,
                                cornerRadius = 12.dp
                            )
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) {
                                onClickSound()
                                onSetLanguage(itemLang)
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = itemLang.label,
                            color = if (active || pressed) colors.orange else colors.ink,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = AppStrings.tr(lang, "ТЕМА ЭКРАНА", "THEME MODE", "TEMA VISUAL", "深浅色主题", "السمة"),
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf(
                    ThemeMode.SYSTEM to AppStrings.tr(lang, "Авто", "Auto", "Auto", "跟随系统", "تلقائي"),
                    ThemeMode.LIGHT to AppStrings.tr(lang, "Бумага", "Paper", "Papel", "暖纸浅色", "فاتح"),
                    ThemeMode.DARK to AppStrings.tr(lang, "Карбон", "Carbon", "Carbón", "暗黑碳色", "داكن")
                )
                for ((mode, label) in modes) {
                    val active = progress.themeMode == mode
                    val interaction = remember(mode.name) { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active || pressed) colors.tint else colors.surface)
                            .statefulBorder(
                                isDashed = active || pressed,
                                strokeWidth = if (active) 2.dp else 1.5.dp,
                                color = if (active || pressed) colors.orange else colors.ink,
                                cornerRadius = 12.dp
                            )
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) {
                                onClickSound()
                                onSetTheme(mode)
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (active || pressed) colors.orange else colors.ink,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // ==================== RULES OF PLUSDUEL ====================
        PdPanel(contentPadding = PaddingValues(18.dp)) {
            Text(
                text = AppStrings.tr(lang, "ПРАВИЛА ИГРЫ PLUSDUEL", "HOW TO PLAY PLUSDUEL", "REGLAS DEL JUEGO", "PLUSDUEL 游戏规则", "قواعد لعبة PLUSDUEL"),
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = AppStrings.tr(
                    lang,
                    "1. Собери выражение, равное Целевому Числу, используя каждую выданную цифру ровно один раз.\n" +
                        "2. Доступные знаки: + − × ÷ ( ) ^ √ ! · Цифры можно склеивать (2 и 3 → 23), ведущий ноль (03) запрещён.\n" +
                        "3. Вся математика точная: √ и ! должны давать целые числа.\n" +
                        "4. Побеждает тот, кто первым отправит верное решение (до 3 побед в матче).",
                    "1. Build a math expression equal to the Target Number using every issued digit exactly once.\n" +
                        "2. Allowed operators: + − × ÷ ( ) ^ √ ! · Digit concatenation (2 and 3 → 23) is allowed; leading zeros (03) are rejected.\n" +
                        "3. Exact rational arithmetic: √ and ! must produce exact integers.\n" +
                        "4. First valid submission wins the round instantly (Best-of-5 match).",
                    "1. Crea una expresión igual al Objetivo usando cada dígito exactamente una vez.\n" +
                        "2. Operadores: + − × ÷ ( ) ^ √ ! · Puedes unir dígitos (23), sin ceros iniciales (03).\n" +
                        "3. Matemática exacta: √ y ! deben dar enteros exactos.\n" +
                        "4. El primero en enviar una solución válida gana la ronda.",
                    "1. 使用全部给定数字（每个数字恰好用一次）构建等于目标数的算式。\n" +
                        "2. 可用运算符：+ − × ÷ ( ) ^ √ ! · 允许数字拼接（2与3 → 23），禁止前导零（03）。\n" +
                        "3. 严格精确计算：√ 与 ! 必须得到整数。\n" +
                        "4. 最先提交正确算式的一方立即赢得该回合（五局三胜）。",
                    "١. كوّن معادلة تساوي الرقم المستهدف باستخدام كل رقم معطى مرة واحدة بالضبط.\n" +
                        "٢. العمليات المتاحة: + − × ÷ ( ) ^ √ ! · يُسمح بدمج الأرقام (23) ويُمنع الصفر البادئ (03).\n" +
                        "٣. حساب دقيق: الجذر والعاملي يجب أن ينتجا أعداداً صحيحة.\n" +
                        "٤. أول من يرسل حلاً صحيحاً يفوز بالجولة فوراً."
                ),
                color = colors.ink,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        PdButton(
            text = AppStrings.tr(lang, "Политика конфиденциальности", "Privacy Policy", "Política de privacidad", "隐私政策", "سياسة الخصوصية"),
            variant = PdButtonVariant.OUTLINE,
            onClick = {
                onClickSound()
                showPrivacyModal = true
            },
            modifier = Modifier.fillMaxWidth()
        )

        PdButton(
            text = AppStrings.tr(lang, "Сбросить локальную статистику", "Reset Local Stats", "Reiniciar estadísticas", "重置本地统计", "إعادة ضبط الإحصائيات"),
            variant = PdButtonVariant.OUTLINE,
            onClick = {
                onClickSound()
                showResetConfirm = true
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showPrivacyModal) {
        Dialog(onDismissRequest = { showPrivacyModal = false }) {
            PdPanel(
                borderColor = colors.orange,
                backgroundColor = colors.surface,
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(22.dp)
            ) {
                Text(
                    text = AppStrings.tr(lang, "Политика конфиденциальности", "Privacy Policy — PlusDuel", "Política de privacidad", "隐私政策 — PlusDuel", "سياسة الخصوصية — PlusDuel"),
                    color = colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = AppStrings.tr(
                        lang,
                        "• Настройки, прогресс соло-режима, монеты и открытые скины хранятся локально на вашем устройстве.\n• Для показа нижнего рекламного баннера в меню используется Google AdMob SDK (com.google.android.gms:play-services-ads), который может обрабатывать рекламный ID устройства (AAID), регион по IP и диагностику показа рекламы согласно Политике конфиденциальности Google.\n• В онлайн-дуэлях сервер обрабатывает ваш никнейм, игровой ID устройства для рейтинга Elo и отправленные математические формулы.\n• Полная версия: https://plusduel.onrender.com/privacy.html\n• Контакт: nuraly2907@icloud.com",
                        "• Your settings, solo stats, coins, and unlocked styles are stored locally on your device.\n• PlusDuel uses Google AdMob SDK (com.google.android.gms:play-services-ads) to display banner ads in menus, which may process your device Advertising ID (AAID), coarse IP location, and ad diagnostics under Google's Privacy Policy.\n• When playing online duels, the server processes only your chosen nickname, device player ID for Elo rankings, and submitted math expressions.\n• Web policy: https://plusduel.onrender.com/privacy.html\n• Contact: nuraly2907@icloud.com"
                    ),
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                PdButton(
                    text = AppStrings.tr(lang, "Закрыть", "Close", "Cerrar", "关闭", "إغلاق"),
                    variant = PdButtonVariant.PRIMARY,
                    onClick = {
                        onClickSound()
                        showPrivacyModal = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showResetConfirm) {
        Dialog(onDismissRequest = { showResetConfirm = false }) {
            PdPanel(
                borderColor = colors.danger,
                backgroundColor = colors.surface,
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(22.dp)
            ) {
                Text(
                    text = AppStrings.tr(lang, "Сбросить статистику?", "Reset Local Stats?", "¿Reiniciar estadísticas?", "确定重置统计数据？", "إعادة ضبط الإحصائيات؟"),
                    color = colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = AppStrings.tr(
                        lang,
                        "Локальный счётчик матчей и соло-рекордов будет сброшен. Открытые скины и монеты сохранятся.",
                        "Your local match counters and solo records will be reset. Unlocked skins and coins will be kept."
                    ),
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                PdButton(
                    text = AppStrings.tr(lang, "Да, сбросить", "Yes, Reset", "Sí, reiniciar", "确认重置", "نعم"),
                    variant = PdButtonVariant.ACCENT,
                    onClick = {
                        onClickSound()
                        showResetConfirm = false
                        onResetStats()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                PdButton(
                    text = AppStrings.tr(lang, "Отмена", "Cancel", "Cancelar", "取消", "إلغاء"),
                    variant = PdButtonVariant.OUTLINE,
                    onClick = {
                        onClickSound()
                        showResetConfirm = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
