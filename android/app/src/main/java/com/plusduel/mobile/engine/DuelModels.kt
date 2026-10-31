package com.plusduel.mobile.engine

enum class ActiveGameKind {
    ONLINE_DUEL,
    BOT_DUEL,
    SOLO_ENDLESS,
    SOLO_MATCH
}

data class RoundState(
    val index: Int,
    val digits: List<Int>,
    val target: Int,
    val timeLimitMs: Long,
    val receivedAtMs: Long,
    val difficultyCoefficient: Double,
    val sampleSolution: String? = null
)

data class RoundOutcome(
    val index: Int,
    val winnerId: String?,
    val winningExpression: String?,
    val sampleSolution: String?,
    val myWins: Int,
    val oppWins: Int,
    val myScore: Double,
    val oppScore: Double
)

data class RatingDelta(
    val before: Int,
    val after: Int,
    val title: String
) {
    val delta: Int get() = after - before
}

data class MatchResultState(
    val iWon: Boolean,
    val isDraw: Boolean,
    val reason: String, // "rounds" | "forfeit"
    val opponentName: String,
    val myWins: Int,
    val oppWins: Int,
    val myScore: Double,
    val oppScore: Double,
    val ratingDelta: RatingDelta? = null,
    val coinsEarned: Int = 0
)

data class LeaderboardItem(
    val playerId: String,
    val name: String,
    val rating: Int,
    val title: String,
    val games: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0
)
