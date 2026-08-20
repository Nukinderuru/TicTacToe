package nukinderuru.datasource.repository

import nukinderuru.domain.model.CurrentGame
import nukinderuru.domain.model.PlayerWinRatio
import java.util.UUID

interface CurrentGameRepository {
    fun saveCurrentGame(currentGame: CurrentGame)

    fun fetchCurrentGame(gameId: UUID): CurrentGame?

    fun fetchCurrentGames(): List<CurrentGame>

    fun fetchCompletedGamesByUserId(userId: UUID): List<CurrentGame>

    fun fetchTopPlayers(limit: Int): List<PlayerWinRatio>
}
