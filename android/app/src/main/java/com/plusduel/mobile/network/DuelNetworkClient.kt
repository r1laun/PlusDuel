package com.plusduel.mobile.network

import com.plusduel.mobile.data.COINS_PER_ONLINE_MATCH_WIN
import com.plusduel.mobile.data.COINS_PER_ROUND_SOLVED
import com.plusduel.mobile.engine.ExpressionValidator
import com.plusduel.mobile.engine.LeaderboardItem
import com.plusduel.mobile.engine.MatchResultState
import com.plusduel.mobile.engine.RatingDelta
import com.plusduel.mobile.engine.RoundOutcome
import com.plusduel.mobile.engine.RoundState
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

data class OnlineSessionState(
    val connected: Boolean = false,
    val queued: Boolean = false,
    val queuePosition: Int = 0,
    val privateRoomCode: String = "",
    val inMatch: Boolean = false,
    val mySocketId: String = "",
    val opponentId: String = "",
    val opponentName: String = "",
    val rated: Boolean = true,
    val currentRound: RoundState? = null,
    val roundOutcome: RoundOutcome? = null,
    val matchResult: MatchResultState? = null,
    val errorBanner: String = "",
    val rankingsTop: List<LeaderboardItem> = emptyList(),
    val rankingsYou: LeaderboardItem? = null
)

/**
 * Native Socket.io client for PlusDuel (`https://plusduel.onrender.com`).
 * Speaks the exact protocol defined in `shared/src/events.ts`.
 */
class DuelNetworkClient(
    private val serverUrl: String = "https://plusduel.onrender.com",
    private val onOnlineRoundWonByMe: () -> Unit = {},
    private val onOnlineMatchFinished: (MatchResultState) -> Unit = {},
    private val onServerRatingSynced: (LeaderboardItem) -> Unit = {}
) {
    private val _state = MutableStateFlow(OnlineSessionState())
    val state: StateFlow<OnlineSessionState> = _state.asStateFlow()

    private var socket: Socket? = null
    private var lastPlayerId: String = ""

    fun connect(playerId: String = lastPlayerId) {
        if (playerId.isNotBlank()) {
            lastPlayerId = playerId
        }
        val existing = socket
        if (existing != null) {
            if (!existing.connected()) existing.connect()
            return
        }

        runCatching {
            val opts = IO.Options.builder()
                .setTransports(arrayOf("websocket", "polling"))
                .setReconnection(true)
                .setReconnectionAttempts(Int.MAX_VALUE)
                .setReconnectionDelay(1500)
                .setTimeout(12000)
                .build()

            val s = IO.socket(URI.create(serverUrl), opts)
            registerListeners(s)
            socket = s
            s.connect()
        }
    }

    private fun registerListeners(s: Socket) {
        s.on(Socket.EVENT_CONNECT) {
            _state.value = _state.value.copy(connected = true, errorBanner = "")
            refreshRankings()
        }

        s.on(Socket.EVENT_DISCONNECT) {
            _state.value = _state.value.copy(connected = false)
        }

        s.on(Socket.EVENT_CONNECT_ERROR) {
            _state.value = _state.value.copy(connected = false)
        }

        s.on("game:queued") { args ->
            val obj = args.firstOrNull() as? JSONObject
            val pos = obj?.optInt("position", 1) ?: 1
            _state.value = _state.value.copy(queued = true, queuePosition = pos)
        }

        s.on("match:start") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val youAre = obj.optString("youAre", "")
            val oppObj = obj.optJSONObject("opponent")
            val oppId = oppObj?.optString("id", "") ?: ""
            val oppName = oppObj?.optString("name", "Opponent") ?: "Opponent"
            val rated = if (obj.has("rated")) obj.optBoolean("rated", true) else true

            _state.value = _state.value.copy(
                queued = false,
                privateRoomCode = "",
                inMatch = true,
                mySocketId = youAre,
                opponentId = oppId,
                opponentName = oppName,
                rated = rated,
                currentRound = null,
                roundOutcome = null,
                matchResult = null,
                errorBanner = ""
            )
        }

        s.on("round:start") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val index = obj.optInt("index", 1)
            val digitsArr = obj.optJSONArray("digits") ?: JSONArray()
            val digits = buildList {
                for (i in 0 until digitsArr.length()) {
                    add(digitsArr.optInt(i))
                }
            }
            val target = obj.optInt("target", 0)
            val timeLimitMs = obj.optLong("timeLimitMs", 75000L)
            val diff = obj.optDouble("difficultyCoefficient", 1.0)

            _state.value = _state.value.copy(
                currentRound = RoundState(
                    index = index,
                    digits = digits,
                    target = target,
                    timeLimitMs = timeLimitMs,
                    receivedAtMs = System.currentTimeMillis(),
                    difficultyCoefficient = diff
                ),
                roundOutcome = null,
                errorBanner = ""
            )
        }

        s.on("round:end") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val cur = _state.value
            val myId = cur.mySocketId
            val oppId = cur.opponentId
            val index = obj.optInt("index", cur.currentRound?.index ?: 1)
            val winnerId = if (obj.isNull("winnerId")) null else obj.optString("winnerId").takeIf { it.isNotBlank() }
            val winningExpr = if (obj.isNull("winningExpression")) null else obj.optString("winningExpression").takeIf { it.isNotBlank() }
            val sampleSol = if (obj.isNull("sampleSolution")) null else obj.optString("sampleSolution").takeIf { it.isNotBlank() }

            val winsObj = obj.optJSONObject("winsByPlayer")
            val scoresObj = obj.optJSONObject("scoresByPlayer")

            val myWins = winsObj?.optInt(myId, 0) ?: 0
            val oppWins = winsObj?.optInt(oppId, 0) ?: 0
            val myScore = scoresObj?.optDouble(myId, 0.0) ?: 0.0
            val oppScore = scoresObj?.optDouble(oppId, 0.0) ?: 0.0

            if (winnerId != null && winnerId == myId) {
                onOnlineRoundWonByMe()
            }

            _state.value = cur.copy(
                roundOutcome = RoundOutcome(
                    index = index,
                    winnerId = winnerId,
                    winningExpression = winningExpr,
                    sampleSolution = sampleSol,
                    myWins = myWins,
                    oppWins = oppWins,
                    myScore = myScore,
                    oppScore = oppScore
                )
            )
        }

        s.on("match:end") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val cur = _state.value
            val myId = cur.mySocketId
            val oppId = cur.opponentId
            val winnerId = if (obj.isNull("winnerId")) null else obj.optString("winnerId").takeIf { it.isNotBlank() }
            val reason = obj.optString("reason", "rounds")

            val winsObj = obj.optJSONObject("winsByPlayer")
            val scoresObj = obj.optJSONObject("scoresByPlayer")
            val ratingsObj = obj.optJSONObject("ratingsByPlayer")?.optJSONObject(myId)

            val myWins = winsObj?.optInt(myId, 0) ?: 0
            val oppWins = winsObj?.optInt(oppId, 0) ?: 0
            val myScore = scoresObj?.optDouble(myId, 0.0) ?: 0.0
            val oppScore = scoresObj?.optDouble(oppId, 0.0) ?: 0.0

            val ratingDelta = if (ratingsObj != null) {
                RatingDelta(
                    before = ratingsObj.optInt("before", 1000),
                    after = ratingsObj.optInt("after", 1000),
                    title = ratingsObj.optString("title", "Gold")
                )
            } else null

            val isDraw = winnerId == null && reason != "forfeit"
            val iWon = !isDraw && winnerId == myId
            val coinsEarned = (myWins * COINS_PER_ROUND_SOLVED) + if (iWon) COINS_PER_ONLINE_MATCH_WIN else 0

            val matchRes = MatchResultState(
                iWon = iWon,
                isDraw = isDraw,
                reason = reason,
                opponentName = cur.opponentName.ifBlank { "Opponent" },
                myWins = myWins,
                oppWins = oppWins,
                myScore = myScore,
                oppScore = oppScore,
                ratingDelta = ratingDelta,
                coinsEarned = coinsEarned
            )

            _state.value = cur.copy(matchResult = matchRes)
            onOnlineMatchFinished(matchRes)
            refreshRankings()
        }

        s.on("opponent:left") {
            _state.value = _state.value.copy(errorBanner = "OPPONENT_LEFT")
        }

        s.on("game:error") { args ->
            val obj = args.firstOrNull() as? JSONObject
            val msg = obj?.optString("message", "") ?: ""
            if (msg.isNotBlank()) {
                _state.value = _state.value.copy(errorBanner = msg)
            }
        }
    }

    fun refreshRankings(playerId: String = lastPlayerId) {
        if (playerId.isNotBlank()) lastPlayerId = playerId
        val s = socket ?: return
        if (!s.connected()) return
        s.emit("rankings:get", Ack { ackArgs ->
            val obj = ackArgs.firstOrNull() as? JSONObject ?: return@Ack
            val topArr = obj.optJSONArray("top") ?: JSONArray()
            val topList = buildList {
                for (i in 0 until topArr.length()) {
                    val item = topArr.optJSONObject(i) ?: continue
                    add(parseLeaderboardItem(item))
                }
            }
            val youObj = if (obj.isNull("you")) null else obj.optJSONObject("you")
            val youItem = youObj?.let { parseLeaderboardItem(it) }
            if (youItem != null) {
                onServerRatingSynced(youItem)
            }
            _state.value = _state.value.copy(
                rankingsTop = topList,
                rankingsYou = youItem
            )
        })
    }

    private fun parseLeaderboardItem(obj: JSONObject): LeaderboardItem {
        return LeaderboardItem(
            playerId = obj.optString("playerId", ""),
            name = obj.optString("name", "Player"),
            rating = obj.optInt("rating", 1000),
            title = obj.optString("title", "Gold"),
            games = obj.optInt("games", 0),
            wins = obj.optInt("wins", 0),
            losses = obj.optInt("losses", 0),
            draws = obj.optInt("draws", 0)
        )
    }

    fun joinQuickPlay(name: String, playerId: String) {
        lastPlayerId = playerId
        connect(playerId)
        val payload = JSONObject().apply {
            put("name", name.trim())
            put("playerId", playerId)
        }
        _state.value = _state.value.copy(
            queued = true,
            queuePosition = 1,
            privateRoomCode = "",
            errorBanner = ""
        )
        socket?.emit("game:queue_join", payload)
    }

    fun createPrivateRoom(name: String, playerId: String) {
        lastPlayerId = playerId
        connect(playerId)
        val payload = JSONObject().apply {
            put("name", name.trim())
        }
        _state.value = _state.value.copy(
            queued = true,
            queuePosition = -1,
            privateRoomCode = "",
            errorBanner = ""
        )
        socket?.emit("game:create_private", arrayOf(payload), Ack { ackArgs ->
            val res = ackArgs.firstOrNull() as? JSONObject
            val code = res?.optString("code", "") ?: ""
            if (code.isNotBlank()) {
                _state.value = _state.value.copy(
                    queued = true,
                    queuePosition = -1,
                    privateRoomCode = code
                )
            }
        })
    }

    fun joinPrivateRoom(name: String, code: String, playerId: String, onJoinFailed: (String) -> Unit) {
        lastPlayerId = playerId
        connect(playerId)
        val cleanCode = code.trim().uppercase()
        val payload = JSONObject().apply {
            put("name", name.trim())
            put("code", cleanCode)
        }
        _state.value = _state.value.copy(
            queued = true,
            queuePosition = -1,
            privateRoomCode = "",
            errorBanner = ""
        )
        socket?.emit("game:join_private", arrayOf(payload), Ack { ackArgs ->
            val res = ackArgs.firstOrNull() as? JSONObject
            val joined = res?.optBoolean("joined", false) ?: false
            if (!joined) {
                _state.value = _state.value.copy(
                    queued = false,
                    privateRoomCode = ""
                )
                onJoinFailed(cleanCode)
            }
        })
    }

    fun cancelQueue() {
        socket?.emit("game:queue_leave")
        _state.value = _state.value.copy(
            queued = false,
            queuePosition = 0,
            privateRoomCode = "",
            errorBanner = ""
        )
    }

    fun submitExpression(expression: String) {
        val normalized = ExpressionValidator.normalizeForServer(expression)
        val payload = JSONObject().apply {
            put("expression", normalized)
        }
        socket?.emit("round:submit", arrayOf(payload), Ack { })
    }

    fun leaveMatchOrResetToLobby() {
        socket?.emit("game:queue_leave")
        socket?.disconnect()
        _state.value = _state.value.copy(
            queued = false,
            queuePosition = 0,
            privateRoomCode = "",
            inMatch = false,
            currentRound = null,
            roundOutcome = null,
            matchResult = null,
            errorBanner = ""
        )
        socket?.connect()
    }
}
