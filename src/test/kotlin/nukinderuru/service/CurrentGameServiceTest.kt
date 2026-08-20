package nukinderuru.service

import nukinderuru.common.ValidationMessages
import nukinderuru.datasource.repository.CurrentGameRepository
import nukinderuru.domain.model.CurrentGame
import nukinderuru.domain.model.GameBoard
import nukinderuru.domain.model.GameOpponentType
import nukinderuru.domain.model.GamePlayer
import nukinderuru.domain.model.GameState
import nukinderuru.domain.model.GameSymbol
import nukinderuru.domain.model.PlayerWinRatio
import nukinderuru.domain.service.CurrentGameService
import java.time.Instant
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.util.UUID

class CurrentGameServiceTest {
    private val repository = FakeCurrentGameRepository()
    private val service = CurrentGameService(repository)

    @Test
    fun `createNewGame for user should save waiting game`() {
        val creatorId = UUID.randomUUID()

        val game = service.createNewGame(creatorId, GameOpponentType.User)

        val savedGame = repository.fetchCurrentGame(game.id)

        assertNotNull(savedGame)
        assertEquals(GameState.WaitingForPlayers, game.state)
        assertEquals(creatorId, game.players.single().userId)
        assertEquals(GameSymbol.X, game.players.single().symbol)
        assertEquals(null, game.computerSymbol)
        assertEquals(game, savedGame)
    }

    @Test
    fun `createNewGame for computer should save creator turn game`() {
        val creatorId = UUID.randomUUID()

        val game = service.createNewGame(creatorId, GameOpponentType.Computer)

        assertEquals(GameState.PlayerTurn(creatorId), game.state)
        assertEquals(GameSymbol.O, game.computerSymbol)
        assertEquals(List(3) { List(3) { 0 } }, game.board.cells)
    }

    @Test
    fun `joinGame should add second player and start with creator turn`() {
        val creatorId = UUID.randomUUID()
        val game = service.createNewGame(creatorId, GameOpponentType.User)
        val joinerId = UUID.randomUUID()

        val joinedGame = service.joinGame(game.id, joinerId)

        assertEquals(2, joinedGame.players.size)
        assertEquals(GameSymbol.O, joinedGame.players.first { it.userId == joinerId }.symbol)
        assertEquals(GameState.PlayerTurn(creatorId), joinedGame.state)
    }

    @Test
    fun `getAvailableGames should exclude current users own games`() {
        val creatorId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        service.createNewGame(creatorId, GameOpponentType.User)

        val availableGames = service.getAvailableGames(otherUserId)

        assertEquals(1, availableGames.size)
        assertTrue(availableGames.none { game -> game.players.any { player -> player.userId == otherUserId } })
        assertTrue(service.getAvailableGames(creatorId).isEmpty())
    }

    @Test
    fun `getAvailableGames should exclude computer and already started games`() {
        val creatorId = UUID.randomUUID()
        val searchingUserId = UUID.randomUUID()

        service.createNewGame(creatorId, GameOpponentType.Computer)
        val joinableGame = service.createNewGame(UUID.randomUUID(), GameOpponentType.User)
        val startedGame = service.createNewGame(UUID.randomUUID(), GameOpponentType.User)
        service.joinGame(startedGame.id, UUID.randomUUID())

        val availableGames = service.getAvailableGames(searchingUserId)

        assertEquals(listOf(joinableGame.id), availableGames.map { it.id })
    }

    @Test
    fun `makeMove against computer should save updated game`() {
        val creatorId = UUID.randomUUID()
        val game = service.createNewGame(creatorId, GameOpponentType.Computer)

        val updatedGame = service.makeMove(game.id, creatorId, rowIndex = 1, columnIndex = 1)

        val savedGame = repository.fetchCurrentGame(updatedGame.id)

        assertNotNull(savedGame)
        assertEquals(updatedGame, savedGame)
        assertEquals(1, updatedGame.board.cells.flatten().count { it == 1 })
        assertEquals(1, updatedGame.board.cells.flatten().count { it == 2 })
        assertEquals(GameState.PlayerTurn(creatorId), updatedGame.state)
    }

    @Test
    fun `getCurrentGame should return saved game`() {
        val game = service.createNewGame(UUID.randomUUID(), GameOpponentType.User)

        assertEquals(game, service.getCurrentGame(game.id))
    }

    @Test
    fun `getCurrentGame should return null for unknown game`() {
        assertNull(service.getCurrentGame(UUID.randomUUID()))
    }

    @Test
    fun `getCompletedGames should delegate to repository`() {
        val winnerId = UUID.randomUUID()
        val drawGame = CurrentGame(
            id = UUID.randomUUID(),
            createdAt = Instant.parse("2026-04-28T11:00:00Z"),
            board = emptyBoardGameBoard(),
            state = GameState.Draw,
            players = listOf(GamePlayer(winnerId, GameSymbol.X), GamePlayer(UUID.randomUUID(), GameSymbol.O))
        )
        repository.saveCurrentGame(drawGame)

        assertEquals(listOf(drawGame), service.getCompletedGames(winnerId))
    }

    @Test
    fun `getTopPlayers should delegate to repository`() {
        val topPlayer = PlayerWinRatio(UUID.randomUUID(), 2.5)
        repository.topPlayers = listOf(topPlayer)

        assertEquals(listOf(topPlayer), service.getTopPlayers(5))
    }

    @Test
    fun `getTopPlayers should reject non positive limit`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            service.getTopPlayers(0)
        }

        assertEquals(ValidationMessages.LEADERBOARD_LIMIT_MUST_BE_POSITIVE, exception.message)
    }

    @Test
    fun `joinGame should throw when game does not exist`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            service.joinGame(UUID.randomUUID(), UUID.randomUUID())
        }

        assertEquals(ValidationMessages.GAME_NOT_FOUND, exception.message)
    }

    @Test
    fun `joinGame should reject computer game`() {
        val game = service.createNewGame(UUID.randomUUID(), GameOpponentType.Computer)

        val exception = assertFailsWith<IllegalArgumentException> {
            service.joinGame(game.id, UUID.randomUUID())
        }

        assertEquals(ValidationMessages.COMPUTER_GAME_CANNOT_BE_JOINED, exception.message)
    }

    @Test
    fun `joinGame should reject game that is not waiting for players`() {
        val creatorId = UUID.randomUUID()
        val game = service.createNewGame(creatorId, GameOpponentType.User)
        service.joinGame(game.id, UUID.randomUUID())

        val exception = assertFailsWith<IllegalArgumentException> {
            service.joinGame(game.id, UUID.randomUUID())
        }

        assertEquals(ValidationMessages.GAME_IS_NOT_WAITING_FOR_PLAYERS, exception.message)
    }

    @Test
    fun `joinGame should reject current participant`() {
        val creatorId = UUID.randomUUID()
        val game = service.createNewGame(creatorId, GameOpponentType.User)

        val exception = assertFailsWith<IllegalArgumentException> {
            service.joinGame(game.id, creatorId)
        }

        assertEquals(ValidationMessages.USER_ALREADY_IN_GAME, exception.message)
    }

    @Test
    fun `joinGame should reject malformed waiting game with two players`() {
        val gameId = UUID.randomUUID()
        repository.saveCurrentGame(
            CurrentGame(
                id = gameId,
                board = emptyBoardGameBoard(),
                state = GameState.WaitingForPlayers,
                players = listOf(
                    GamePlayer(UUID.randomUUID(), GameSymbol.X),
                    GamePlayer(UUID.randomUUID(), GameSymbol.O)
                ),
                computerSymbol = null
            )
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            service.joinGame(gameId, UUID.randomUUID())
        }

        assertEquals(ValidationMessages.GAME_ALREADY_HAS_TWO_PLAYERS, exception.message)
    }

    @Test
    fun `makeMove should throw when game does not exist`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            service.makeMove(UUID.randomUUID(), UUID.randomUUID(), 0, 0)
        }

        assertEquals(ValidationMessages.GAME_NOT_FOUND, exception.message)
    }

    @Test
    fun `makeMove should reject game that is not ready for move`() {
        val userId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        repository.saveCurrentGame(
            CurrentGame(
                id = gameId,
                board = emptyBoardGameBoard(),
                state = GameState.WaitingForPlayers,
                players = listOf(GamePlayer(userId, GameSymbol.X))
            )
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            service.makeMove(gameId, userId, 0, 0)
        }

        assertEquals(ValidationMessages.GAME_IS_NOT_READY_FOR_MOVE, exception.message)
    }

    @Test
    fun `makeMove should reject move on another users turn`() {
        val firstUserId = UUID.randomUUID()
        val secondUserId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        repository.saveCurrentGame(
            CurrentGame(
                id = gameId,
                board = emptyBoardGameBoard(),
                state = GameState.PlayerTurn(firstUserId),
                players = listOf(
                    GamePlayer(firstUserId, GameSymbol.X),
                    GamePlayer(secondUserId, GameSymbol.O)
                )
            )
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            service.makeMove(gameId, secondUserId, 0, 0)
        }

        assertEquals(ValidationMessages.NOT_USERS_TURN, exception.message)
    }

    @Test
    fun `makeMove should reject non participant`() {
        val userId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        repository.saveCurrentGame(
            CurrentGame(
                id = gameId,
                board = emptyBoardGameBoard(),
                state = GameState.PlayerTurn(userId),
                players = listOf(GamePlayer(UUID.randomUUID(), GameSymbol.X))
            )
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            service.makeMove(gameId, userId, 0, 0)
        }

        assertEquals(ValidationMessages.USER_IS_NOT_GAME_PARTICIPANT, exception.message)
    }

    @Test
    fun `makeMove should reject invalid board structure`() {
        val userId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        repository.saveCurrentGame(
            CurrentGame(
                id = gameId,
                board = GameBoard(listOf(listOf(0, 0), listOf(0, 0))),
                state = GameState.PlayerTurn(userId),
                players = listOf(GamePlayer(userId, GameSymbol.X))
            )
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            service.makeMove(gameId, userId, 0, 0)
        }

        assertEquals(ValidationMessages.INVALID_BOARD_STRUCTURE, exception.message)
    }

    @Test
    fun `makeMove should reject out of bounds move`() {
        val creatorId = UUID.randomUUID()
        val game = service.createNewGame(creatorId, GameOpponentType.Computer)

        val exception = assertFailsWith<IllegalArgumentException> {
            service.makeMove(game.id, creatorId, -1, 0)
        }

        assertEquals(ValidationMessages.MOVE_IS_OUT_OF_BOUNDS, exception.message)
    }

    @Test
    fun `makeMove should reject move past board size`() {
        val creatorId = UUID.randomUUID()
        val game = service.createNewGame(creatorId, GameOpponentType.Computer)

        val exception = assertFailsWith<IllegalArgumentException> {
            service.makeMove(game.id, creatorId, 0, 3)
        }

        assertEquals(ValidationMessages.MOVE_IS_OUT_OF_BOUNDS, exception.message)
    }

    @Test
    fun `makeMove should reject occupied cell`() {
        val creatorId = UUID.randomUUID()
        val game = service.createNewGame(creatorId, GameOpponentType.Computer)
        service.makeMove(game.id, creatorId, 1, 1)

        val exception = assertFailsWith<IllegalArgumentException> {
            service.makeMove(game.id, creatorId, 1, 1)
        }

        assertEquals(ValidationMessages.CELL_IS_ALREADY_OCCUPIED, exception.message)
    }

    @Test
    fun `makeMove in multiplayer should switch turn to other player`() {
        val firstUserId = UUID.randomUUID()
        val game = service.createNewGame(firstUserId, GameOpponentType.User)
        val secondUserId = UUID.randomUUID()
        service.joinGame(game.id, secondUserId)

        val updatedGame = service.makeMove(game.id, firstUserId, 0, 0)

        assertEquals(GameState.PlayerTurn(secondUserId), updatedGame.state)
        assertEquals(1, updatedGame.board.cells[0][0])
    }

    @Test
    fun `makeMove should finish game when player wins`() {
        val winnerId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        repository.saveCurrentGame(
            CurrentGame(
                id = gameId,
                board = GameBoard(
                    listOf(
                        listOf(1, 1, 0),
                        listOf(0, 2, 0),
                        listOf(0, 0, 2)
                    )
                ),
                state = GameState.PlayerTurn(winnerId),
                players = listOf(
                    GamePlayer(winnerId, GameSymbol.X),
                    GamePlayer(UUID.randomUUID(), GameSymbol.O)
                )
            )
        )

        val updatedGame = service.makeMove(gameId, winnerId, 0, 2)

        assertEquals(GameState.PlayerWin(winnerId), updatedGame.state)
        assertEquals(updatedGame, repository.fetchCurrentGame(gameId))
    }

    @Test
    fun `makeMove should throw when winner symbol is not mapped`() {
        val userId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        repository.saveCurrentGame(
            CurrentGame(
                id = gameId,
                board = GameBoard(
                    listOf(
                        listOf(1, 1, 1),
                        listOf(0, 2, 0),
                        listOf(0, 0, 0)
                    )
                ),
                state = GameState.PlayerTurn(userId),
                players = listOf(GamePlayer(userId, GameSymbol.O))
            )
        )

        val exception = assertFailsWith<IllegalStateException> {
            service.makeMove(gameId, userId, 1, 0)
        }

        assertEquals(ValidationMessages.WINNER_SYMBOL_NOT_MAPPED, exception.message)
    }

    @Test
    fun `makeMove should mark draw when player fills final empty cell without winner`() {
        val userId = UUID.randomUUID()
        val opponentId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        repository.saveCurrentGame(
            CurrentGame(
                id = gameId,
                board = GameBoard(
                    listOf(
                        listOf(1, 2, 1),
                        listOf(1, 2, 2),
                        listOf(2, 1, 0)
                    )
                ),
                state = GameState.PlayerTurn(userId),
                players = listOf(
                    GamePlayer(userId, GameSymbol.X),
                    GamePlayer(opponentId, GameSymbol.O)
                )
            )
        )

        val updatedGame = service.makeMove(gameId, userId, 2, 2)

        assertEquals(GameState.Draw, updatedGame.state)
        assertEquals(updatedGame, repository.fetchCurrentGame(gameId))
    }

    @Test
    fun `makeMove should mark computer as winner when computer completes line`() {
        val userId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        repository.saveCurrentGame(
            CurrentGame(
                id = gameId,
                board = GameBoard(
                    listOf(
                        listOf(2, 2, 0),
                        listOf(1, 0, 0),
                        listOf(0, 0, 1)
                    )
                ),
                state = GameState.PlayerTurn(userId),
                players = listOf(GamePlayer(userId, GameSymbol.X)),
                computerSymbol = GameSymbol.O
            )
        )

        val updatedGame = service.makeMove(gameId, userId, 1, 1)

        assertEquals(
            GameState.PlayerWin(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            updatedGame.state,
        )
        assertEquals(updatedGame, repository.fetchCurrentGame(gameId))
    }

    private fun emptyBoardGameBoard(): GameBoard = GameBoard(List(3) { List(3) { 0 } })

    private class FakeCurrentGameRepository : CurrentGameRepository {
        private val games = mutableMapOf<UUID, CurrentGame>()
        var topPlayers: List<PlayerWinRatio> = emptyList()

        override fun saveCurrentGame(currentGame: CurrentGame) {
            games[currentGame.id] = currentGame
        }

        override fun fetchCurrentGame(gameId: UUID): CurrentGame? = games[gameId]

        override fun fetchCurrentGames(): List<CurrentGame> = games.values.toList()

        override fun fetchCompletedGamesByUserId(userId: UUID): List<CurrentGame> = games.values.filter { game ->
            game.state == GameState.Draw || game.state == GameState.PlayerWin(userId)
        }

        override fun fetchTopPlayers(limit: Int): List<PlayerWinRatio> = topPlayers.take(limit)
    }
}
