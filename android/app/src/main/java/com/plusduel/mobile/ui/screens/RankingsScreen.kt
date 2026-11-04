package com.plusduel.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plusduel.mobile.data.PlayerProgressState
import com.plusduel.mobile.network.OnlineSessionState
import com.plusduel.mobile.ui.components.PdButton
import com.plusduel.mobile.ui.components.PdButtonVariant
import com.plusduel.mobile.ui.components.PdPanel
import com.plusduel.mobile.ui.components.PlusDuelLogoHeader
import com.plusduel.mobile.ui.i18n.AppStrings
import com.plusduel.mobile.ui.theme.PlusDuelTheme

@Composable
fun RankingsScreen(
    progress: PlayerProgressState,
    onlineState: OnlineSessionState,
    onRefreshRankings: () -> Unit,
    onClickSound: () -> Unit
) {
    val colors = PlusDuelTheme.colors
    val lang = progress.language

    val winRate = if (progress.duelGames > 0) {
        (progress.duelWins * 100) / progress.duelGames
    } else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlusDuelLogoHeader(
            subtitle = AppStrings.tr(
                lang,
                "Рейтинг Elo и таблица лидеров",
                "Elo rating and leaderboard",
                "Clasificación Elo y tabla",
                "Elo 积分与排行榜",
                "تصنيف Elo والمتصدرين"
            ),
            compact = true
        )

        // ==================== YOUR PROFILE & ELO CARD ====================
        PdPanel(
            borderColor = colors.line,
            backgroundColor = colors.surface,
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progress.nickname.ifBlank {
                            AppStrings.tr(lang, "Гость", "Guest", "Invitado", "访客", "ضيف")
                        },
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = AppStrings.rankTitle(lang, progress.rankTitle),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .background(colors.tint, RoundedCornerShape(10.dp))
                        .border(1.dp, colors.orange, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${progress.rating} Elo",
                        color = colors.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCell(
                    value = progress.duelGames.toString(),
                    label = AppStrings.tr(lang, "Матчи", "Games", "Juegos", "对局", "مباريات")
                )
                StatCell(
                    value = progress.duelWins.toString(),
                    label = AppStrings.tr(lang, "Победы", "Wins", "Victorias", "胜场", "فوز")
                )
                StatCell(
                    value = progress.duelLosses.toString(),
                    label = AppStrings.tr(lang, "Поражения", "Losses", "Derrotas", "负场", "خسارة")
                )
                StatCell(
                    value = "$winRate%",
                    label = AppStrings.tr(lang, "Побед %", "Win rate", "Victoria", "胜率", "نسبة")
                )
            }
        }

        // ==================== GLOBAL LEADERBOARD ====================
        PdPanel(contentPadding = PaddingValues(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.tr(
                        lang,
                        "Таблица лидеров",
                        "Leaderboard",
                        "Clasificación",
                        "排行榜",
                        "المتصدرون"
                    ),
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
                Text(
                    text = if (onlineState.connected) {
                        AppStrings.tr(lang, "Онлайн", "Online", "En línea", "在线", "متصل")
                    } else {
                        AppStrings.tr(lang, "Офлайн", "Offline", "Sin red", "离线", "غير متصل")
                    },
                    color = if (onlineState.connected) colors.success else colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (onlineState.rankingsTop.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    onlineState.rankingsTop.forEachIndexed { idx, entry ->
                        val isYou = entry.playerId == progress.playerId ||
                            (onlineState.rankingsYou != null && entry.playerId == onlineState.rankingsYou.playerId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(
                                    color = if (isYou) colors.tint else colors.bg,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isYou) colors.orange else colors.line,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${idx + 1}.",
                                    color = colors.muted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    text = entry.name,
                                    color = colors.ink,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "${entry.rating} · ${AppStrings.rankTitle(lang, entry.title)}",
                                color = if (isYou) colors.orange else colors.muted,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = AppStrings.tr(
                        lang,
                        "Пока нет записей",
                        "No ranked games yet",
                        "Sin partidas aún",
                        "暂无排位记录",
                        "لا توجد مباريات بعد"
                    ),
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            PdButton(
                text = AppStrings.tr(lang, "Обновить", "Refresh", "Actualizar", "刷新", "تحديث"),
                variant = PdButtonVariant.OUTLINE,
                onClick = {
                    onClickSound()
                    onRefreshRankings()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ==================== ELO TIERS GUIDE ====================
        PdPanel(contentPadding = PaddingValues(16.dp)) {
            Text(
                text = AppStrings.tr(
                    lang,
                    "Звания Elo",
                    "Rating tiers",
                    "Rangos Elo",
                    "Elo 段位",
                    "مستويات التصنيف"
                ),
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            val bands = listOf(
                1800 to "Grandmaster",
                1600 to "Master",
                1400 to "Diamond",
                1200 to "Platinum",
                1000 to "Gold",
                800 to "Club",
                0 to "Novice"
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for ((minElo, rawTitle) in bands) {
                    val isCurrent = progress.rankTitle == rawTitle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(if (isCurrent) colors.tint else colors.bg, RoundedCornerShape(8.dp))
                            .border(1.dp, if (isCurrent) colors.orange else colors.line, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.rankTitle(lang, rawTitle),
                            color = if (isCurrent) colors.orange else colors.ink,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Text(
                            text = if (minElo > 0) "$minElo+" else "< 800",
                            color = if (isCurrent) colors.orange else colors.muted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    val colors = PlusDuelTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = colors.ink,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            maxLines = 1
        )
        Text(
            text = label,
            color = colors.muted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}
