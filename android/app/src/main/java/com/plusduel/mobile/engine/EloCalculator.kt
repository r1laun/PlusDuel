package com.plusduel.mobile.engine

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

enum class BotDifficulty(
    val id: String,
    val rating: Int,
    val titleKey: String,
    val botNames: List<String>,
    val minSolveSec: Int,
    val maxSolveSec: Int,
    val solveChance: Double,
    val winCoinBonus: Int
) {
    NOVICE(
        id = "novice",
        rating = 900,
        titleKey = "Club",
        botNames = listOf("PixelBot", "CalcJunior", "Abacus01", "Modulo_9"),
        minSolveSec = 28,
        maxSolveSec = 55,
        solveChance = 0.72,
        winCoinBonus = 20
    ),
    GOLD(
        id = "gold",
        rating = 1120,
        titleKey = "Gold",
        botNames = listOf("VectorAI", "EulerPrime", "GaussNode", "RatioX"),
        minSolveSec = 18,
        maxSolveSec = 38,
        solveChance = 0.85,
        winCoinBonus = 25
    ),
    MASTER(
        id = "master",
        rating = 1450,
        titleKey = "Diamond",
        botNames = listOf("TuringCore", "Ramanujan_7", "NoetherAI", "RiemannZ"),
        minSolveSec = 11,
        maxSolveSec = 25,
        solveChance = 0.93,
        winCoinBonus = 35
    ),
    GRANDMASTER(
        id = "grandmaster",
        rating = 1820,
        titleKey = "Grandmaster",
        botNames = listOf("OmegaSolver", "DeepGauss", "AxiomZero", "PrimeApex"),
        minSolveSec = 7,
        maxSolveSec = 16,
        solveChance = 0.98,
        winCoinBonus = 50
    );

    fun pickBotName(rng: Random = Random.Default): String {
        return botNames[rng.nextInt(botNames.size)]
    }

    /**
     * Computes when the bot will solve the round (in milliseconds from round start),
     * or null if the bot fails to find a solution before timeout on this round.
     */
    fun rollBotSolveDelayMs(
        roundIndex: Int,
        timeLimitMs: Long,
        rng: Random = Random.Default
    ): Long? {
        if (rng.nextDouble() > solveChance) return null
        val roundPenaltySec = (roundIndex - 1).coerceAtLeast(0) * 2
        val lowMs = (minSolveSec + roundPenaltySec) * 1000L
        val highMs = (maxSolveSec + roundPenaltySec * 2) * 1000L
        val raw = rng.nextLong(lowMs, (highMs + 1000L).coerceAtLeast(lowMs + 1000L))
        return if (raw < timeLimitMs - 800L) raw else null
    }
}

/**
 * Chess-style Elo rating calculator matching `shared/src/rating.ts`.
 */
object EloCalculator {
    const val START_RATING = 1000
    const val PROVISIONAL_GAMES = 10
    const val K_PROVISIONAL = 40
    const val K_REGULAR = 32

    fun kFor(gamesPlayed: Int): Int = if (gamesPlayed < PROVISIONAL_GAMES) K_PROVISIONAL else K_REGULAR

    fun expectedScore(ra: Int, rb: Int): Double {
        return 1.0 / (1.0 + 10.0.pow((rb - ra) / 400.0))
    }

    fun eloUpdate(ra: Int, rb: Int, scoreA: Double, k: Int): Int {
        return (ra + k * (scoreA - expectedScore(ra, rb))).roundToInt()
    }

    fun titleFor(rating: Int): String = when {
        rating >= 1800 -> "Grandmaster"
        rating >= 1600 -> "Master"
        rating >= 1400 -> "Diamond"
        rating >= 1200 -> "Platinum"
        rating >= 1000 -> "Gold"
        rating >= 800 -> "Club"
        else -> "Novice"
    }
}
