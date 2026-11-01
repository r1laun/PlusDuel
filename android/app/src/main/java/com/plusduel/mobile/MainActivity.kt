package com.plusduel.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plusduel.mobile.ads.AdMobBannerBar
import com.plusduel.mobile.ads.AdMobConfig
import com.plusduel.mobile.data.AppLanguage
import com.plusduel.mobile.data.COINS_PER_ROUND_SOLVED
import com.plusduel.mobile.data.PlayerProgressRepository
import com.plusduel.mobile.data.PlayerProgressState
import com.plusduel.mobile.engine.ActiveGameKind
import com.plusduel.mobile.engine.BotDifficulty
import com.plusduel.mobile.engine.EloCalculator
import com.plusduel.mobile.engine.MatchResultState
import com.plusduel.mobile.engine.RatingDelta
import com.plusduel.mobile.engine.RoundGenerator
import com.plusduel.mobile.engine.RoundOutcome
import com.plusduel.mobile.engine.RoundState
import com.plusduel.mobile.network.DuelNetworkClient
import com.plusduel.mobile.sound.SoundManager
import com.plusduel.mobile.ui.components.statefulBorder
import com.plusduel.mobile.ui.i18n.AppStrings
import com.plusduel.mobile.ui.screens.ArenaScreen
import com.plusduel.mobile.ui.screens.DuelGameScreen
import com.plusduel.mobile.ui.screens.RankingsScreen
import com.plusduel.mobile.ui.screens.SettingsScreen
import com.plusduel.mobile.ui.screens.ShopScreen
import com.plusduel.mobile.ui.screens.SoloScreen
import com.plusduel.mobile.ui.theme.PlusDuelTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private lateinit var repository: PlayerProgressRepository
    private lateinit var soundManager: SoundManager
    private lateinit var networkClient: DuelNetworkClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = PlayerProgressRepository(applicationContext)
        soundManager = SoundManager(applicationContext)
        AdMobConfig.initialize(applicationContext)

        networkClient = DuelNetworkClient(
            onOnlineRoundWonByMe = {
                // Coins are awarded at match end or round win
            },
            onOnlineMatchFinished = { matchRes ->
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    repository.recordDuelFinished(
                        iWon = matchRes.iWon,
                        isDraw = matchRes.isDraw,
                        newRating = matchRes.ratingDelta?.after,
                        coinsEarned = matchRes.coinsEarned
                    )
                }
            },
            onServerRatingSynced = { youEntry ->
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    repository.syncServerRating(
                        serverRating = youEntry.rating,
                        games = youEntry.games,
                        wins = youEntry.wins,
                        losses = youEntry.losses,
                        draws = youEntry.draws
                    )
                }
            }
        )

        setContent {
            val progress by repository.stateFlow.collectAsState(initial = PlayerProgressState())
            val onlineState by networkClient.state.collectAsState()

            LaunchedEffect(Unit) {
                val pid = repository.ensurePlayerId()
                networkClient.connect(pid)
            }

            LaunchedEffect(progress.soundEnabled, progress.musicEnabled, progress.hapticsEnabled) {
                soundManager.soundEnabled = progress.soundEnabled
                soundManager.musicEnabled = progress.musicEnabled
                soundManager.hapticsEnabled = progress.hapticsEnabled
            }

            PlusDuelTheme(
                themeMode = progress.themeMode,
                accentSkin = progress.selectedSkin,
                language = progress.language
            ) {
                PlusDuelRoot(
                    progress = progress,
                    onlineState = onlineState,
                    repository = repository,
                    soundManager = soundManager,
                    networkClient = networkClient
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::soundManager.isInitialized) {
            soundManager.appInForeground = true
        }
    }

    override fun onPause() {
        if (::soundManager.isInitialized) {
            soundManager.appInForeground = false
        }
        super.onPause()
    }
}

private enum class BottomTab {
    ARENA, SOLO, RANKINGS, SHOP, SETTINGS
}

private const val LOCAL_ME = "local-you"
private const val LOCAL_OPP = "local-opp"
private const val ROUNDS_TO_WIN = 3
private const val MAX_MATCH_ROUNDS = 5
private const val BETWEEN_ROUNDS_MS = 3200L

@Composable
private fun PlusDuelRoot(
    progress: PlayerProgressState,
    onlineState: com.plusduel.mobile.network.OnlineSessionState,
    repository: PlayerProgressRepository,
    soundManager: SoundManager,
    networkClient: DuelNetworkClient
) {
    val colors = PlusDuelTheme.colors
    val lang = progress.language
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(BottomTab.ARENA) }
    var selectedBot by remember { mutableStateOf(BotDifficulty.GOLD) }
    var selectedSoloLevel by remember { mutableIntStateOf(2) }
    var soloIsMatchMode by remember { mutableStateOf(false) }

    // Local session state (Bot Duel or Solo)
    var localGameKind by remember { mutableStateOf<ActiveGameKind?>(null) }
    var localOpponentName by remember { mutableStateOf("") }
    var localRound by remember { mutableStateOf<RoundState?>(null) }
    var localRoundOutcome by remember { mutableStateOf<RoundOutcome?>(null) }
    var localMatchResult by remember { mutableStateOf<MatchResultState?>(null) }
    var localMyWins by remember { mutableIntStateOf(0) }
    var localOppWins by remember { mutableIntStateOf(0) }
    var localMyScore by remember { mutableDoubleStateOf(0.0) }
    var localOppScore by remember { mutableDoubleStateOf(0.0) }
    var soloStreak by remember { mutableIntStateOf(0) }
    var roundTimerJob by remember { mutableStateOf<Job?>(null) }
    var botSolveJob by remember { mutableStateOf<Job?>(null) }
    var nextRoundJob by remember { mutableStateOf<Job?>(null) }

    val inOnlineDuel = onlineState.inMatch
    val inLocalGame = localGameKind != null
    val inActiveGameplay = inOnlineDuel || inLocalGame

    LaunchedEffect(inActiveGameplay) {
        soundManager.inMenuScreen = !inActiveGameplay
    }

    fun cancelLocalTimers() {
        roundTimerJob?.cancel()
        botSolveJob?.cancel()
        nextRoundJob?.cancel()
        roundTimerJob = null
        botSolveJob = null
        nextRoundJob = null
    }

    fun exitLocalGame() {
        cancelLocalTimers()
        localGameKind = null
        localRound = null
        localRoundOutcome = null
        localMatchResult = null
    }

    fun finishLocalMatch(kind: ActiveGameKind) {
        val isDraw = localMyWins == localOppWins
        val iWon = localMyWins > localOppWins

        val ratingDelta: RatingDelta? = if (kind == ActiveGameKind.BOT_DUEL) {
            val before = progress.rating
            val k = EloCalculator.kFor(progress.duelGames)
            val scoreA = when {
                isDraw -> 0.5
                iWon -> 1.0
                else -> 0.0
            }
            val after = EloCalculator.eloUpdate(before, selectedBot.rating, scoreA, k)
            RatingDelta(
                before = before,
                after = after,
                title = EloCalculator.titleFor(after)
            )
        } else null

        val matchWinBonus = when (kind) {
            ActiveGameKind.BOT_DUEL -> if (iWon) selectedBot.winCoinBonus else 0
            ActiveGameKind.SOLO_MATCH -> if (iWon) 20 else 0
            else -> 0
        }
        val totalCoins = (localMyWins * COINS_PER_ROUND_SOLVED) + matchWinBonus

        val result = MatchResultState(
            iWon = iWon,
            isDraw = isDraw,
            reason = "rounds",
            opponentName = localOpponentName,
            myWins = localMyWins,
            oppWins = localOppWins,
            myScore = localMyScore,
            oppScore = localOppScore,
            ratingDelta = ratingDelta,
            coinsEarned = totalCoins
        )
        localMatchResult = result

        scope.launch {
            if (kind == ActiveGameKind.BOT_DUEL) {
                repository.recordDuelFinished(
                    iWon = iWon,
                    isDraw = isDraw,
                    newRating = ratingDelta?.after,
                    coinsEarned = totalCoins
                )
            } else if (matchWinBonus > 0) {
                repository.addCoins(matchWinBonus)
            }
        }
    }

    fun spawnLocalRound(roundIndex: Int) {
        val kind = localGameKind ?: return
        cancelLocalTimers()
        localRoundOutcome = null

        val level = when (kind) {
            ActiveGameKind.SOLO_ENDLESS -> selectedSoloLevel
            else -> RoundGenerator.levelForMatchRound(roundIndex)
        }

        scope.launch {
            val gen = withContext(Dispatchers.Default) {
                RoundGenerator.generate(level)
            }
            if (localGameKind != kind) return@launch

            val rState = RoundState(
                index = roundIndex,
                digits = gen.digits,
                target = gen.target,
                timeLimitMs = gen.timeLimitMs,
                receivedAtMs = System.currentTimeMillis(),
                difficultyCoefficient = gen.difficultyCoefficient,
                sampleSolution = gen.solution
            )
            localRound = rState

            // If playing against a Smart Bot, schedule bot's solve attempt
            if (kind == ActiveGameKind.BOT_DUEL) {
                val botDelayMs = selectedBot.rollBotSolveDelayMs(roundIndex, gen.timeLimitMs)
                if (botDelayMs != null) {
                    botSolveJob = scope.launch {
                        delay(botDelayMs)
                        if (localGameKind == kind && localRound == rState && localRoundOutcome == null) {
                            roundTimerJob?.cancel()
                            val remMs = (gen.timeLimitMs - botDelayMs).coerceAtLeast(0L)
                            val spec = RoundGenerator.specForLevel(level)
                            val pts = RoundGenerator.roundScore(gen.digits.size, spec.targetMax, remMs, gen.timeLimitMs)
                            localOppWins += 1
                            localOppScore = ((localOppScore + pts) * 10.0).roundToInt() / 10.0
                            val outcome = RoundOutcome(
                                index = roundIndex,
                                winnerId = LOCAL_OPP,
                                winningExpression = gen.solution,
                                sampleSolution = gen.solution,
                                myWins = localMyWins,
                                oppWins = localOppWins,
                                myScore = localMyScore,
                                oppScore = localOppScore
                            )
                            localRoundOutcome = outcome

                            val matchOver = localMyWins >= ROUNDS_TO_WIN ||
                                localOppWins >= ROUNDS_TO_WIN ||
                                roundIndex >= MAX_MATCH_ROUNDS
                            if (matchOver) {
                                nextRoundJob = scope.launch {
                                    delay(1600L)
                                    finishLocalMatch(kind)
                                }
                            } else {
                                nextRoundJob = scope.launch {
                                    delay(BETWEEN_ROUNDS_MS)
                                    spawnLocalRound(roundIndex + 1)
                                }
                            }
                        }
                    }
                }
            }

            // Round timeout timer
            roundTimerJob = scope.launch {
                delay(gen.timeLimitMs)
                if (localGameKind == kind && localRound == rState && localRoundOutcome == null) {
                    botSolveJob?.cancel()
                    if (kind == ActiveGameKind.SOLO_ENDLESS || kind == ActiveGameKind.SOLO_MATCH) {
                        localOppWins += 1
                        soloStreak = 0
                    }
                    val outcome = RoundOutcome(
                        index = roundIndex,
                        winnerId = null,
                        winningExpression = null,
                        sampleSolution = gen.solution,
                        myWins = localMyWins,
                        oppWins = localOppWins,
                        myScore = localMyScore,
                        oppScore = localOppScore
                    )
                    localRoundOutcome = outcome

                    if (kind == ActiveGameKind.SOLO_ENDLESS) {
                        nextRoundJob = scope.launch {
                            delay(BETWEEN_ROUNDS_MS)
                            spawnLocalRound(roundIndex + 1)
                        }
                    } else {
                        val matchOver = localMyWins >= ROUNDS_TO_WIN ||
                            localOppWins >= ROUNDS_TO_WIN ||
                            roundIndex >= MAX_MATCH_ROUNDS
                        if (matchOver) {
                            nextRoundJob = scope.launch {
                                delay(1600L)
                                finishLocalMatch(kind)
                            }
                        } else {
                            nextRoundJob = scope.launch {
                                delay(BETWEEN_ROUNDS_MS)
                                spawnLocalRound(roundIndex + 1)
                            }
                        }
                    }
                }
            }
        }
    }

    fun startBotDuel(bot: BotDifficulty) {
        selectedBot = bot
        cancelLocalTimers()
        localGameKind = ActiveGameKind.BOT_DUEL
        localOpponentName = "${bot.pickBotName()} (${bot.rating})"
        localMyWins = 0
        localOppWins = 0
        localMyScore = 0.0
        localOppScore = 0.0
        localRound = null
        localRoundOutcome = null
        localMatchResult = null
        spawnLocalRound(1)
    }

    fun startSoloSession(isMatch: Boolean, level: Int) {
        selectedSoloLevel = level
        soloIsMatchMode = isMatch
        cancelLocalTimers()
        localGameKind = if (isMatch) ActiveGameKind.SOLO_MATCH else ActiveGameKind.SOLO_ENDLESS
        localOpponentName = if (isMatch) {
            AppStrings.tr(lang, "Таймер (Матч)", "Clock (Match)", "Reloj", "计时挑战", "المؤقت")
        } else {
            AppStrings.tr(lang, "Соло · Уровень $level", "Solo · Level $level", "Solo · Nivel $level", "单人 · 等级 $level", "فردي · مستوى $level")
        }
        localMyWins = 0
        localOppWins = 0
        localMyScore = 0.0
        localOppScore = 0.0
        soloStreak = 0
        localRound = null
        localRoundOutcome = null
        localMatchResult = null
        spawnLocalRound(1)
    }

    fun handleLocalPlayerValidSubmit(expression: String) {
        val kind = localGameKind ?: return
        val rState = localRound ?: return
        if (localRoundOutcome != null) return

        cancelLocalTimers()
        val elapsedMs = (System.currentTimeMillis() - rState.receivedAtMs).coerceAtLeast(0L)
        val remMs = (rState.timeLimitMs - elapsedMs).coerceAtLeast(0L)
        val level = when (kind) {
            ActiveGameKind.SOLO_ENDLESS -> selectedSoloLevel
            else -> RoundGenerator.levelForMatchRound(rState.index)
        }
        val spec = RoundGenerator.specForLevel(level)
        val pts = RoundGenerator.roundScore(rState.digits.size, spec.targetMax, remMs, rState.timeLimitMs)

        localMyWins += 1
        localMyScore = ((localMyScore + pts) * 10.0).roundToInt() / 10.0

        if (kind == ActiveGameKind.SOLO_ENDLESS || kind == ActiveGameKind.SOLO_MATCH) {
            val newStreak = soloStreak + 1
            soloStreak = newStreak
            scope.launch {
                repository.recordSoloSolved(newStreak, COINS_PER_ROUND_SOLVED)
            }
        }

        val outcome = RoundOutcome(
            index = rState.index,
            winnerId = LOCAL_ME,
            winningExpression = expression,
            sampleSolution = rState.sampleSolution,
            myWins = localMyWins,
            oppWins = localOppWins,
            myScore = localMyScore,
            oppScore = localOppScore
        )
        localRoundOutcome = outcome

        if (kind == ActiveGameKind.SOLO_ENDLESS) {
            nextRoundJob = scope.launch {
                delay(BETWEEN_ROUNDS_MS)
                spawnLocalRound(rState.index + 1)
            }
        } else {
            val matchOver = localMyWins >= ROUNDS_TO_WIN ||
                localOppWins >= ROUNDS_TO_WIN ||
                rState.index >= MAX_MATCH_ROUNDS
            if (matchOver) {
                nextRoundJob = scope.launch {
                    delay(1600L)
                    finishLocalMatch(kind)
                }
            } else {
                nextRoundJob = scope.launch {
                    delay(BETWEEN_ROUNDS_MS)
                    spawnLocalRound(rState.index + 1)
                }
            }
        }
    }

    BackHandler(enabled = inActiveGameplay || onlineState.queued || currentTab != BottomTab.ARENA) {
        soundManager.playClick()
        when {
            inOnlineDuel -> networkClient.leaveMatchOrResetToLobby()
            inLocalGame -> exitLocalGame()
            onlineState.queued -> networkClient.cancelQueue()
            else -> currentTab = BottomTab.ARENA
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                inOnlineDuel -> {
                    DuelGameScreen(
                        gameKind = ActiveGameKind.ONLINE_DUEL,
                        myId = onlineState.mySocketId,
                        opponentName = onlineState.opponentName,
                        rated = onlineState.rated,
                        round = onlineState.currentRound,
                        roundOutcome = onlineState.roundOutcome,
                        matchResult = onlineState.matchResult,
                        myWins = onlineState.roundOutcome?.myWins ?: 0,
                        oppWins = onlineState.roundOutcome?.oppWins ?: 0,
                        soloStreak = 0,
                        errorBanner = onlineState.errorBanner,
                        language = lang,
                        tileShape = progress.selectedTileShape,
                        soundManager = soundManager,
                        onSubmitExpression = { expr -> networkClient.submitExpression(expr) },
                        onPlayAgain = {
                            networkClient.leaveMatchOrResetToLobby()
                            networkClient.joinQuickPlay(progress.nickname, progress.playerId)
                        },
                        onLeave = { networkClient.leaveMatchOrResetToLobby() }
                    )
                }

                inLocalGame -> {
                    val kind = localGameKind!!
                    DuelGameScreen(
                        gameKind = kind,
                        myId = LOCAL_ME,
                        opponentName = localOpponentName,
                        rated = kind == ActiveGameKind.BOT_DUEL,
                        round = localRound,
                        roundOutcome = localRoundOutcome,
                        matchResult = localMatchResult,
                        myWins = localMyWins,
                        oppWins = localOppWins,
                        soloStreak = soloStreak,
                        errorBanner = "",
                        language = lang,
                        tileShape = progress.selectedTileShape,
                        soundManager = soundManager,
                        onSubmitExpression = { expr -> handleLocalPlayerValidSubmit(expr) },
                        onPlayAgain = {
                            if (kind == ActiveGameKind.BOT_DUEL) {
                                startBotDuel(selectedBot)
                            } else {
                                startSoloSession(soloIsMatchMode, selectedSoloLevel)
                            }
                        },
                        onLeave = { exitLocalGame() }
                    )
                }

                else -> {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(260)) + scaleIn(initialScale = 0.97f, animationSpec = tween(260)))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.98f, animationSpec = tween(200))
                                )
                        },
                        label = "tabTransition"
                    ) { tab ->
                        when (tab) {
                            BottomTab.ARENA -> ArenaScreen(
                                progress = progress,
                                onlineState = onlineState,
                                selectedBot = selectedBot,
                                onSelectBot = { selectedBot = it },
                                onNicknameChange = { newName ->
                                    scope.launch { repository.setNickname(newName) }
                                },
                                onQuickPlayOnline = {
                                    networkClient.joinQuickPlay(progress.nickname, progress.playerId)
                                },
                                onStartBotDuel = { bot -> startBotDuel(bot) },
                                onCreatePrivateRoom = {
                                    networkClient.createPrivateRoom(progress.nickname, progress.playerId)
                                },
                                onJoinPrivateRoom = { code ->
                                    networkClient.joinPrivateRoom(
                                        name = progress.nickname,
                                        code = code,
                                        playerId = progress.playerId,
                                        onJoinFailed = { }
                                    )
                                },
                                onCancelQueue = { networkClient.cancelQueue() },
                                onOpenShop = { currentTab = BottomTab.SHOP },
                                onToggleAudio = {
                                    val nextAudio = !(progress.soundEnabled || progress.musicEnabled)
                                    if (nextAudio) soundManager.playClick()
                                    scope.launch {
                                        repository.setSoundEnabled(nextAudio)
                                        repository.setMusicEnabled(nextAudio)
                                    }
                                },
                                onClickSound = { soundManager.playClick() }
                            )

                            BottomTab.SOLO -> SoloScreen(
                                progress = progress,
                                selectedLevel = selectedSoloLevel,
                                isMatchMode = soloIsMatchMode,
                                onSelectLevel = { selectedSoloLevel = it },
                                onSelectMode = { soloIsMatchMode = it },
                                onStartSolo = { isMatch, lvl -> startSoloSession(isMatch, lvl) },
                                onClickSound = { soundManager.playClick() }
                            )

                            BottomTab.RANKINGS -> RankingsScreen(
                                progress = progress,
                                onlineState = onlineState,
                                onRefreshRankings = { networkClient.refreshRankings(progress.playerId) },
                                onClickSound = { soundManager.playClick() }
                            )

                            BottomTab.SHOP -> ShopScreen(
                                progress = progress,
                                soundManager = soundManager,
                                onSelectOrBuySkin = { skin ->
                                    scope.launch { repository.selectOrBuySkin(skin) }
                                },
                                onSelectOrBuyShape = { shape ->
                                    scope.launch { repository.selectOrBuyTileShape(shape) }
                                }
                            )

                            BottomTab.SETTINGS -> SettingsScreen(
                                progress = progress,
                                onSetTheme = { scope.launch { repository.setThemeMode(it) } },
                                onSetLanguage = { scope.launch { repository.setLanguage(it) } },
                                onSetSound = { scope.launch { repository.setSoundEnabled(it) } },
                                onSetMusic = { scope.launch { repository.setMusicEnabled(it) } },
                                onSetHaptics = { scope.launch { repository.setHapticsEnabled(it) } },
                                onResetStats = { scope.launch { repository.resetAllStats() } },
                                onClickSound = { soundManager.playClick() }
                            )
                        }
                    }
                }
            }
        }

        if (!inActiveGameplay) {
            BottomNavigationBar(
                currentTab = currentTab,
                lang = lang,
                onSelectTab = { tab ->
                    soundManager.playClick()
                    if (onlineState.queued && tab != BottomTab.ARENA) {
                        networkClient.cancelQueue()
                    }
                    currentTab = tab
                    if (tab == BottomTab.RANKINGS) {
                        networkClient.refreshRankings(progress.playerId)
                    }
                }
            )
            AdMobBannerBar()
        }
    }
}

private data class NavTabItem(
    val tab: BottomTab,
    val icon: ImageVector,
    val label: String
)

@Composable
private fun BottomNavigationBar(
    currentTab: BottomTab,
    lang: AppLanguage,
    onSelectTab: (BottomTab) -> Unit
) {
    val colors = PlusDuelTheme.colors
    val items = listOf(
        NavTabItem(BottomTab.ARENA, Icons.Outlined.Bolt, AppStrings.navArena(lang)),
        NavTabItem(BottomTab.SOLO, Icons.Outlined.AllInclusive, AppStrings.navSolo(lang)),
        NavTabItem(BottomTab.RANKINGS, Icons.Outlined.Leaderboard, AppStrings.navRankings(lang)),
        NavTabItem(BottomTab.SHOP, Icons.Outlined.Palette, AppStrings.navShop(lang)),
        NavTabItem(BottomTab.SETTINGS, Icons.Outlined.Tune, AppStrings.navSettings(lang))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(1.5.dp, colors.line)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (item in items) {
            val active = currentTab == item.tab
            val interaction = remember(item.tab.name) { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val fgColor = if (active || pressed) colors.orange else colors.muted

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active || pressed) colors.tint else colors.surface)
                    .statefulBorder(
                        isDashed = active || pressed,
                        strokeWidth = 1.5.dp,
                        color = if (active || pressed) colors.orange else colors.surface,
                        cornerRadius = 14.dp
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) { onSelectTab(item.tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = fgColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.label,
                        color = fgColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
