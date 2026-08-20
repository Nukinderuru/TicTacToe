package nukinderuru.datasource.repository

import nukinderuru.datasource.mapper.toDatasourceModel
import nukinderuru.datasource.mapper.toDomainModel
import nukinderuru.datasource.model.DatasourceCurrentGame
import nukinderuru.datasource.table.CurrentGamesTable
import nukinderuru.domain.model.CurrentGame
import nukinderuru.domain.model.GameState
import nukinderuru.domain.model.PlayerWinRatio
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SortOrder
import java.util.UUID

class ExposedCurrentGameRepository : CurrentGameRepository {
    override fun saveCurrentGame(currentGame: CurrentGame) {
        val datasourceGame = currentGame.toDatasourceModel()
        val serializedGame = Json.encodeToString(DatasourceCurrentGame.serializer(), datasourceGame)
        val winnerId = (currentGame.state as? GameState.PlayerWin)?.playerId
        val stateType = currentGame.state.databaseStateType()
        val firstPlayerId = currentGame.players.firstOrNull()?.userId
        val secondPlayerId = currentGame.players.getOrNull(1)?.userId

        transaction {
            val existingGame = CurrentGamesTable
                .selectAll()
                .where { CurrentGamesTable.id eq currentGame.id }
                .singleOrNull()

            if (existingGame == null) {
                CurrentGamesTable.insert {
                    it[id] = currentGame.id
                    it[createdAt] = currentGame.createdAt
                    it[CurrentGamesTable.stateType] = stateType
                    it[CurrentGamesTable.winnerId] = winnerId
                    it[CurrentGamesTable.firstPlayerId] = firstPlayerId
                    it[CurrentGamesTable.secondPlayerId] = secondPlayerId
                    it[board] = serializedGame
                }
            } else {
                CurrentGamesTable.update({ CurrentGamesTable.id eq currentGame.id }) {
                    it[createdAt] = currentGame.createdAt
                    it[CurrentGamesTable.stateType] = stateType
                    it[CurrentGamesTable.winnerId] = winnerId
                    it[CurrentGamesTable.firstPlayerId] = firstPlayerId
                    it[CurrentGamesTable.secondPlayerId] = secondPlayerId
                    it[board] = serializedGame
                }
            }
        }
    }

    override fun fetchCurrentGame(gameId: UUID): CurrentGame? = transaction {
        CurrentGamesTable
            .selectAll()
            .where { CurrentGamesTable.id eq gameId }
            .singleOrNull()
            ?.toDomainCurrentGame()
    }

    override fun fetchCurrentGames(): List<CurrentGame> = transaction {
        CurrentGamesTable
            .selectAll()
            .map { it.toDomainCurrentGame() }
    }

    override fun fetchCompletedGamesByUserId(userId: UUID): List<CurrentGame> = transaction {
        CurrentGamesTable
            .selectAll()
            .where {
                (CurrentGamesTable.stateType eq STATE_DRAW) or
                    ((CurrentGamesTable.stateType eq STATE_WIN) and (CurrentGamesTable.winnerId eq userId))
            }
            .orderBy(CurrentGamesTable.createdAt, SortOrder.DESC)
            .map { it.toDomainCurrentGame() }
    }

    override fun fetchTopPlayers(limit: Int): List<PlayerWinRatio> {
        require(limit > 0)

        return transaction {
            exec(
                """
                SELECT leaderboard.user_id, leaderboard.win_ratio
                FROM (
                    SELECT
                        participant_id AS user_id,
                        CASE
                            WHEN (SUM(losses) + SUM(draws)) = 0 THEN SUM(wins) * 1.0
                            ELSE SUM(wins) * 1.0 / (SUM(losses) + SUM(draws))
                        END AS win_ratio,
                        SUM(wins) AS wins
                    FROM (
                        SELECT
                            first_player_id AS participant_id,
                            CASE WHEN state_type = '$STATE_WIN' AND winner_id = first_player_id THEN 1 ELSE 0 END AS wins,
                            CASE WHEN state_type = '$STATE_WIN' AND winner_id IS NOT NULL AND winner_id <> first_player_id THEN 1 ELSE 0 END AS losses,
                            CASE WHEN state_type = '$STATE_DRAW' THEN 1 ELSE 0 END AS draws
                        FROM current_games
                        WHERE first_player_id IS NOT NULL AND state_type IN ('$STATE_WIN', '$STATE_DRAW')

                        UNION ALL

                        SELECT
                            second_player_id AS participant_id,
                            CASE WHEN state_type = '$STATE_WIN' AND winner_id = second_player_id THEN 1 ELSE 0 END AS wins,
                            CASE WHEN state_type = '$STATE_WIN' AND winner_id IS NOT NULL AND winner_id <> second_player_id THEN 1 ELSE 0 END AS losses,
                            CASE WHEN state_type = '$STATE_DRAW' THEN 1 ELSE 0 END AS draws
                        FROM current_games
                        WHERE second_player_id IS NOT NULL AND state_type IN ('$STATE_WIN', '$STATE_DRAW')
                    ) player_results
                    GROUP BY participant_id
                ) leaderboard
                ORDER BY leaderboard.win_ratio DESC, leaderboard.wins DESC, leaderboard.user_id ASC
                LIMIT $limit
                """.trimIndent(),
            ) { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            PlayerWinRatio(
                                userId = UUID.fromString(resultSet.getString("user_id")),
                                winRatio = resultSet.getDouble("win_ratio"),
                            ),
                        )
                    }
                }
            } ?: emptyList()
        }
    }

    private fun ResultRow.toDomainCurrentGame(): CurrentGame = Json
        .decodeFromString(DatasourceCurrentGame.serializer(), this[CurrentGamesTable.board])
        .toDomainModel()

    private fun GameState.databaseStateType(): String = when (this) {
        GameState.WaitingForPlayers -> STATE_WAITING
        is GameState.PlayerTurn -> STATE_TURN
        GameState.Draw -> STATE_DRAW
        is GameState.PlayerWin -> STATE_WIN
    }

    private companion object {
        const val STATE_WAITING = "waiting"
        const val STATE_TURN = "turn"
        const val STATE_DRAW = "draw"
        const val STATE_WIN = "win"
    }
}
