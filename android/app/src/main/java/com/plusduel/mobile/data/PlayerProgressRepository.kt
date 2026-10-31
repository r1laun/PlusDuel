package com.plusduel.mobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.plusduel.mobile.engine.EloCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "plusduel_prefs")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppLanguage(val code: String, val label: String, val isRtl: Boolean = false) {
    RU("ru", "РУ", false),
    EN("en", "EN", false),
    ES("es", "ES", false),
    ZH("zh", "中文", false),
    AR("ar", "عربي", true);

    fun next(): AppLanguage {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    companion object {
        fun defaultForDevice(): AppLanguage {
            val lang = Locale.getDefault().language.lowercase(Locale.ROOT)
            return when {
                lang.startsWith("ru") || lang.startsWith("kk") || lang.startsWith("uk") || lang.startsWith("be") -> RU
                lang.startsWith("es") -> ES
                lang.startsWith("zh") -> ZH
                lang.startsWith("ar") -> AR
                else -> EN
            }
        }
    }
}

private const val INITIAL_COINS = 30
const val COINS_PER_ROUND_SOLVED = 5
const val COINS_PER_ONLINE_MATCH_WIN = 30

data class PlayerProgressState(
    val playerId: String = "",
    val nickname: String = "",
    val rating: Int = EloCalculator.START_RATING,
    val duelGames: Int = 0,
    val duelWins: Int = 0,
    val duelLosses: Int = 0,
    val duelDraws: Int = 0,
    val soloSolved: Int = 0,
    val soloBestStreak: Int = 0,
    val coins: Int = INITIAL_COINS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.defaultForDevice(),
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val selectedSkin: AccentSkin = AccentSkin.ORANGE,
    val selectedTileShape: TileShapeStyle = TileShapeStyle.ROUNDED,
    val unlockedItemIds: Set<String> = setOf(
        AccentSkin.ORANGE.id,
        TileShapeStyle.ROUNDED.id
    )
) {
    val rankTitle: String
        get() = EloCalculator.titleFor(rating)

    fun isItemUnlocked(itemId: String): Boolean {
        return itemId in unlockedItemIds ||
            itemId == AccentSkin.ORANGE.id ||
            itemId == TileShapeStyle.ROUNDED.id
    }
}

class PlayerProgressRepository(private val context: Context) {

    private object Keys {
        val PLAYER_ID = stringPreferencesKey("player_id")
        val NICKNAME = stringPreferencesKey("nickname")
        val RATING = intPreferencesKey("elo_rating")
        val DUEL_GAMES = intPreferencesKey("duel_games")
        val DUEL_WINS = intPreferencesKey("duel_wins")
        val DUEL_LOSSES = intPreferencesKey("duel_losses")
        val DUEL_DRAWS = intPreferencesKey("duel_draws")
        val SOLO_SOLVED = intPreferencesKey("solo_solved")
        val SOLO_BEST_STREAK = intPreferencesKey("solo_best_streak")
        val COINS = intPreferencesKey("player_coins")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SELECTED_SKIN = stringPreferencesKey("selected_skin")
        val SELECTED_SHAPE = stringPreferencesKey("selected_shape")
        val UNLOCKED_ITEMS_CSV = stringPreferencesKey("unlocked_items_csv")
    }

    val stateFlow: Flow<PlayerProgressState> = context.dataStore.data.map { prefs ->
        val theme = runCatching {
            ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        val lang = runCatching {
            val saved = prefs[Keys.LANGUAGE]
            if (saved != null) AppLanguage.valueOf(saved) else AppLanguage.defaultForDevice()
        }.getOrDefault(AppLanguage.defaultForDevice())
        val skin = runCatching {
            AccentSkin.valueOf(prefs[Keys.SELECTED_SKIN] ?: AccentSkin.ORANGE.name)
        }.getOrDefault(AccentSkin.ORANGE)
        val shape = runCatching {
            TileShapeStyle.valueOf(prefs[Keys.SELECTED_SHAPE] ?: TileShapeStyle.ROUNDED.name)
        }.getOrDefault(TileShapeStyle.ROUNDED)

        val unlockedSet = (prefs[Keys.UNLOCKED_ITEMS_CSV] ?: "")
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableSet()
            .apply {
                add(AccentSkin.ORANGE.id)
                add(TileShapeStyle.ROUNDED.id)
            }

        PlayerProgressState(
            playerId = prefs[Keys.PLAYER_ID] ?: "",
            nickname = prefs[Keys.NICKNAME] ?: "",
            rating = prefs[Keys.RATING] ?: EloCalculator.START_RATING,
            duelGames = prefs[Keys.DUEL_GAMES] ?: 0,
            duelWins = prefs[Keys.DUEL_WINS] ?: 0,
            duelLosses = prefs[Keys.DUEL_LOSSES] ?: 0,
            duelDraws = prefs[Keys.DUEL_DRAWS] ?: 0,
            soloSolved = prefs[Keys.SOLO_SOLVED] ?: 0,
            soloBestStreak = prefs[Keys.SOLO_BEST_STREAK] ?: 0,
            coins = (prefs[Keys.COINS] ?: INITIAL_COINS).coerceAtLeast(0),
            themeMode = theme,
            language = lang,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            selectedSkin = skin,
            selectedTileShape = shape,
            unlockedItemIds = unlockedSet
        )
    }

    suspend fun ensurePlayerId(): String {
        var result = ""
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.PLAYER_ID]
            if (current.isNullOrBlank()) {
                val created = "and-" + UUID.randomUUID().toString()
                prefs[Keys.PLAYER_ID] = created
                result = created
            } else {
                result = current
            }
        }
        return result
    }

    suspend fun setNickname(name: String) {
        val cleaned = name.trim().take(20)
        context.dataStore.edit { it[Keys.NICKNAME] = cleaned }
    }

    suspend fun syncServerRating(serverRating: Int, games: Int, wins: Int, losses: Int, draws: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RATING] = serverRating
            if (games > (prefs[Keys.DUEL_GAMES] ?: 0)) {
                prefs[Keys.DUEL_GAMES] = games
                prefs[Keys.DUEL_WINS] = wins
                prefs[Keys.DUEL_LOSSES] = losses
                prefs[Keys.DUEL_DRAWS] = draws
            }
        }
    }

    suspend fun recordDuelFinished(
        iWon: Boolean,
        isDraw: Boolean,
        newRating: Int?,
        coinsEarned: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DUEL_GAMES] = (prefs[Keys.DUEL_GAMES] ?: 0) + 1
            when {
                isDraw -> prefs[Keys.DUEL_DRAWS] = (prefs[Keys.DUEL_DRAWS] ?: 0) + 1
                iWon -> prefs[Keys.DUEL_WINS] = (prefs[Keys.DUEL_WINS] ?: 0) + 1
                else -> prefs[Keys.DUEL_LOSSES] = (prefs[Keys.DUEL_LOSSES] ?: 0) + 1
            }
            if (newRating != null) {
                prefs[Keys.RATING] = newRating.coerceAtLeast(100)
            }
            if (coinsEarned > 0) {
                val cur = prefs[Keys.COINS] ?: INITIAL_COINS
                prefs[Keys.COINS] = cur + coinsEarned
            }
        }
    }

    suspend fun recordSoloSolved(currentStreak: Int, coinReward: Int = COINS_PER_ROUND_SOLVED) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SOLO_SOLVED] = (prefs[Keys.SOLO_SOLVED] ?: 0) + 1
            val prevBest = prefs[Keys.SOLO_BEST_STREAK] ?: 0
            if (currentStreak > prevBest) {
                prefs[Keys.SOLO_BEST_STREAK] = currentStreak
            }
            if (coinReward > 0) {
                val cur = prefs[Keys.COINS] ?: INITIAL_COINS
                prefs[Keys.COINS] = cur + coinReward
            }
        }
    }

    suspend fun addCoins(amount: Int) {
        if (amount <= 0) return
        context.dataStore.edit { prefs ->
            val cur = prefs[Keys.COINS] ?: INITIAL_COINS
            prefs[Keys.COINS] = cur + amount
        }
    }

    suspend fun selectOrBuySkin(skin: AccentSkin) {
        context.dataStore.edit { prefs ->
            val unlocked = (prefs[Keys.UNLOCKED_ITEMS_CSV] ?: "")
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableSet()
            if (skin.id in unlocked || skin.costCoins == 0) {
                prefs[Keys.SELECTED_SKIN] = skin.name
            } else {
                val curCoins = prefs[Keys.COINS] ?: INITIAL_COINS
                if (curCoins >= skin.costCoins) {
                    prefs[Keys.COINS] = curCoins - skin.costCoins
                    unlocked.add(skin.id)
                    prefs[Keys.UNLOCKED_ITEMS_CSV] = unlocked.joinToString(",")
                    prefs[Keys.SELECTED_SKIN] = skin.name
                }
            }
        }
    }

    suspend fun selectOrBuyTileShape(shape: TileShapeStyle) {
        context.dataStore.edit { prefs ->
            val unlocked = (prefs[Keys.UNLOCKED_ITEMS_CSV] ?: "")
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableSet()
            if (shape.id in unlocked || shape.costCoins == 0) {
                prefs[Keys.SELECTED_SHAPE] = shape.name
            } else {
                val curCoins = prefs[Keys.COINS] ?: INITIAL_COINS
                if (curCoins >= shape.costCoins) {
                    prefs[Keys.COINS] = curCoins - shape.costCoins
                    unlocked.add(shape.id)
                    prefs[Keys.UNLOCKED_ITEMS_CSV] = unlocked.joinToString(",")
                    prefs[Keys.SELECTED_SHAPE] = shape.name
                }
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(lang: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = lang.name }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MUSIC_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun resetAllStats() {
        context.dataStore.edit { prefs ->
            prefs[Keys.RATING] = EloCalculator.START_RATING
            prefs[Keys.DUEL_GAMES] = 0
            prefs[Keys.DUEL_WINS] = 0
            prefs[Keys.DUEL_LOSSES] = 0
            prefs[Keys.DUEL_DRAWS] = 0
            prefs[Keys.SOLO_SOLVED] = 0
            prefs[Keys.SOLO_BEST_STREAK] = 0
        }
    }
}
