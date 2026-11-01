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
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlusDuelLogoHeader(
            subtitle = AppStrings.tr(
                lang,
                "Настройки и правила",
                "Settings & Rules",
                "Ajustes y reglas",
                "设置与规则",
                "الإعدادات والقواعد"
            ),
            compact = true
        )

        // ==================== AUDIO & HAPTICS ====================
        PdPanel(contentPadding = PaddingValues(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.tr(lang, "Музыка в меню", "Menu Music", "Música", "背景音乐", "الموسيقى"),
                    color = colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
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

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.tr(lang, "Звуки (SFX)", "Sound Effects", "Efectos (SFX)", "按键音效", "المؤثرات الصوتية"),
                    color = colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = progress.soundEnabled,
                    onCheckedChange = { onSetSound(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.orange
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.tr(lang, "Вибрация", "Haptics", "Vibración", "震动反馈", "الاهتزاز"),
                    color = colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
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
        PdPanel(contentPadding = PaddingValues(16.dp)) {
            Text(
                text = AppStrings.tr(lang, "ЯЗЫК", "LANGUAGE", "IDIOMA", "语言", "اللغة"),
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

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
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = itemLang.label,
                            color = if (active || pressed) colors.orange else colors.ink,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = AppStrings.tr(lang, "ТЕМА", "THEME", "TEMA", "主题", "السمة"),
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf(
                    ThemeMode.SYSTEM to AppStrings.tr(lang, "Авто", "Auto", "Auto", "自动", "تلقائي"),
                    ThemeMode.LIGHT to AppStrings.tr(lang, "Бумага", "Paper", "Papel", "浅色", "فاتح"),
                    ThemeMode.DARK to AppStrings.tr(lang, "Карбон", "Carbon", "Carbón", "暗色", "داكن")
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
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (active || pressed) colors.orange else colors.ink,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // ==================== RULES OF PLUSDUEL ====================
        PdPanel(contentPadding = PaddingValues(16.dp)) {
            Text(
                text = AppStrings.tr(lang, "ПРАВИЛА", "RULES", "REGLAS", "规则", "القواعد"),
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = AppStrings.tr(
                    lang,
                    "• Используй каждую цифру ровно 1 раз, чтобы получить Целевое Число.\n" +
                        "• Знаки: + − × ÷ ( ) ^ √ ! · Цифры можно склеивать (2 и 3 → 23).\n" +
                        "• Кто первым отправит точный ответ — забирает раунд!",
                    "• Use every issued digit once to hit the Target Number.\n" +
                        "• Operators: + − × ÷ ( ) ^ √ ! · Concatenation (2 & 3 → 23) allowed.\n" +
                        "• First exact solution wins the round!",
                    "• Usa cada dígito una vez para lograr el Objetivo.\n" +
                        "• Operadores: + − × ÷ ( ) ^ √ ! · Unir dígitos permitido.\n" +
                        "• ¡El primero en acertar gana la ronda!",
                    "• 用完所有数字（各用一次）凑出目标数。\n" +
                        "• 运算符：+ − × ÷ ( ) ^ √ ! · 允许数字拼接。\n" +
                        "• 最先提交正确算式者赢得该回合！",
                    "• استخدم كل رقم مرة واحدة للوصول للهدف.\n" +
                        "• العمليات: + − × ÷ ( ) ^ √ ! · دمج الأرقام مسموح.\n" +
                        "• أول حل صحيح يفوز بالجولة!"
                ),
                color = colors.ink,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }

        PdButton(
            text = AppStrings.tr(lang, "Политика конфиденциальности", "Privacy Policy", "Privacidad", "隐私政策", "الخصوصية"),
            variant = PdButtonVariant.OUTLINE,
            onClick = {
                onClickSound()
                showPrivacyModal = true
            },
            modifier = Modifier.fillMaxWidth()
        )

        PdButton(
            text = AppStrings.tr(lang, "Сбросить статистику", "Reset Stats", "Reiniciar", "重置数据", "إعادة ضبط"),
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
                contentPadding = PaddingValues(20.dp)
            ) {
                Text(
                    text = AppStrings.tr(lang, "Политика конфиденциальности", "Privacy Policy — PlusDuel", "Privacidad", "隐私政策", "الخصوصية"),
                    color = colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = AppStrings.tr(
                        lang,
                        "• Настройки, соло-рекорды, монеты и открытые скины хранятся локально на устройстве.\n• Для показа нижнего баннера используется Google AdMob SDK (com.google.android.gms:play-services-ads), который может обрабатывать рекламный ID (AAID) и регион по IP согласно Политике Google.\n• В онлайн-дуэлях сервер обрабатывает никнейм, игровой ID для рейтинга Elo и отправленные формулы.\n• URL: https://plusduel.onrender.com/privacy.html",
                        "• Settings, solo stats, coins, and unlocked styles are stored locally on your device.\n• PlusDuel uses Google AdMob SDK (com.google.android.gms:play-services-ads) for bottom banner ads, which may process your Advertising ID (AAID), coarse IP location, and ad diagnostics under Google's Privacy Policy.\n• In online duels, the server processes your nickname, player ID for Elo rankings, and submitted math expressions.\n• Web: https://plusduel.onrender.com/privacy.html"
                    ),
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
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
                contentPadding = PaddingValues(20.dp)
            ) {
                Text(
                    text = AppStrings.tr(lang, "Сбросить статистику?", "Reset Local Stats?", "¿Reiniciar?", "重置统计？", "إعادة ضبط؟"),
                    color = colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = AppStrings.tr(
                        lang,
                        "Счётчик матчей и соло-рекорды будут сброшены. Монеты и скины сохранятся.",
                        "Local match counters and solo records will be reset. Coins and skins are kept."
                    ),
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                PdButton(
                    text = AppStrings.tr(lang, "Сбросить", "Reset", "Reiniciar", "确认", "نعم"),
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
