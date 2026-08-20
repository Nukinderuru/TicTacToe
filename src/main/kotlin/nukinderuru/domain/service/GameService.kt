package nukinderuru.domain.service

import nukinderuru.domain.model.CurrentGame
import nukinderuru.domain.model.GameOpponentType
import nukinderuru.domain.model.PlayerWinRatio
import java.util.UUID

interface GameService {
    fun createNewGame(creatorId: UUID, opponentType: GameOpponentType): CurrentGame

    fun getAvailableGames(userId: UUID): List<CurrentGame>

    fun joinGame(gameId: UUID, userId: UUID): CurrentGame

    fun makeMove(gameId: UUID, userId: UUID, rowIndex: Int, columnIndex: Int): CurrentGame

    fun getCurrentGame(gameId: UUID): CurrentGame?

    fun getCompletedGames(userId: UUID): List<CurrentGame>

    fun getTopPlayers(limit: Int): List<PlayerWinRatio>
}
