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
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PlusDuelLogoHeader(
            subtitle = AppStrings.tr(
                lang,
                "Твой рейтинг Elo, звания и мировой топ игроков",
                "Your Elo rating, rank progression & global leaderboard",
                "Tu clasificación Elo, rangos y tabla mundial",
                "你的 Elo 积分、段位晋升与全球排行榜",
                "تصنيف Elo الخاص بك ولوحة المتصدرين العالمية"
            ),
            compact = true
        )

        // ==================== YOUR PROFILE & ELO CARD ====================
        PdPanel(
            borderColor = colors.orange,
            backgroundColor = colors.surface,
            contentPadding = PaddingValues(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = progress.nickname.ifBlank {
                            AppStrings.tr(lang, "Игрок PlusDuel", "PlusDuel Player", "Jugador PlusDuel", "PlusDuel 玩家", "لاعب PlusDuel")
                        },
                        color = colors.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = AppStrings.rankTitle(lang, progress.rankTitle),
                        color = colors.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(colors.tint, RoundedCornerShape(14.dp))
                        .border(1.5.dp, colors.orange, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "★ ${progress.rating}",
                        color = colors.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCell(
                    value = progress.duelGames.toString(),
                    label = AppStrings.tr(lang, "Матчи", "Matches", "Partidas", "总对局", "مباريات")
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
                    label = AppStrings.tr(lang, "Винрейт", "Win Rate", "% Victoria", "胜率", "نسبة الفوز")
                )
            }
        }

        // ==================== GLOBAL LEADERBOARD ====================
        PdPanel(contentPadding = PaddingValues(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.tr(
                        lang,
                        "МИРОВОЙ ТОП ИГРОКОВ",
                        "GLOBAL LEADERBOARD",
                        "CLASIFICACIÓN MUNDIAL",
                        "全球排行榜",
                        "لوحة المتصدرين العالمية"
                    ),
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = if (onlineState.connected) "● ONLINE" else "○ OFFLINE",
                    color = if (onlineState.connected) colors.success else colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
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
                                .background(
                                    color = if (isYou) colors.tint else colors.bg,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = if (isYou) 1.5.dp else 1.dp,
                                    color = if (isYou) colors.orange else colors.line,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "#${idx + 1}",
                                    color = if (idx < 3) colors.orange else colors.muted,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = entry.name,
                                    color = colors.ink,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "${entry.rating} · ${AppStrings.rankTitle(lang, entry.title)}",
                                color = if (isYou) colors.orange else colors.muted,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = AppStrings.tr(
                        lang,
                        "Загрузка таблицы лидеров или пока нет рейтинговых матчей на сервере.",
                        "Connecting to leaderboard or no ranked games recorded yet.",
                        "Cargando clasificación o aún no hay partidas clasificatorias.",
                        "正在连接排行榜，或暂无排位赛记录。",
                        "جارٍ تحميل التصنيف أو لا توجد مباريات مصنفة بعد."
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
                text = AppStrings.tr(lang, "Обновить рейтинг ↻", "Refresh Leaderboard ↻", "Actualizar ↻", "刷新排行榜 ↻", "تحديث التصنيف ↻"),
                variant = PdButtonVariant.OUTLINE,
                onClick = {
                    onClickSound()
                    onRefreshRankings()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ==================== ELO TIERS GUIDE ====================
        PdPanel(contentPadding = PaddingValues(18.dp)) {
            Text(
                text = AppStrings.tr(
                    lang,
                    "СИСТЕМА ЗВАНИЙ ELO",
                    "ELO RANK TIERS",
                    "RANGOS ELO",
                    "ELO 段位体系",
                    "مستويات تصنيف ELO"
                ),
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            val bands = listOf(
                1800 to "Grandmaster",
                1600 to "Master",
                1400 to "Diamond",
                1200 to "Platinum",
                1000 to "Gold",
                800 to "Club",
                0 to "Novice"
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((minElo, rawTitle) in bands) {
                    val isCurrent = progress.rankTitle == rawTitle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isCurrent) colors.tint else colors.bg, RoundedCornerShape(10.dp))
                            .border(1.dp, if (isCurrent) colors.orange else colors.line, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.rankTitle(lang, rawTitle) + if (isCurrent) " ★" else "",
                            color = if (isCurrent) colors.orange else colors.ink,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (minElo > 0) "$minElo+ Elo" else "< 800 Elo",
                            color = if (isCurrent) colors.orange else colors.muted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
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
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = label,
            color = colors.muted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}
