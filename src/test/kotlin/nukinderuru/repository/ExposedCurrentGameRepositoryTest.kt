package nukinderuru.repository

import nukinderuru.datasource.repository.ExposedCurrentGameRepository
import nukinderuru.datasource.table.CurrentGamesTable
import nukinderuru.datasource.table.UsersTable
import nukinderuru.domain.model.CurrentGame
import nukinderuru.domain.model.GameBoard
import nukinderuru.domain.model.GamePlayer
import nukinderuru.domain.model.GameState
import nukinderuru.domain.model.GameSymbol
import nukinderuru.domain.model.PlayerWinRatio
import java.time.Instant
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExposedCurrentGameRepositoryTest {
    private val repository = ExposedCurrentGameRepository()

    @BeforeTest
    fun setUp() {
        Database.connect(
            url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )

        transaction {
            SchemaUtils.drop(CurrentGamesTable, UsersTable)
            SchemaUtils.create(CurrentGamesTable, UsersTable)
        }
    }

    @Test
    fun `saveCurrentGame should persist game`() {
        val currentGame = CurrentGame(
            id = UUID.randomUUID(),
            createdAt = Instant.parse("2026-04-28T10:00:00Z"),
            board = GameBoard(
                listOf(
                    listOf(1, 2, 0),
                    listOf(0, 1, 0),
                    listOf(2, 0, 0)
                )
            ),
            state = GameState.PlayerTurn(UUID.fromString("00000000-0000-0000-0000-000000000011")),
            players = listOf(
                GamePlayer(UUID.fromString("00000000-0000-0000-0000-000000000011"), GameSymbol.X),
            ),
            computerSymbol = GameSymbol.O
        )

        repository.saveCurrentGame(currentGame)

        val savedGame = repository.fetchCurrentGame(currentGame.id)

        assertEquals(currentGame, savedGame)
        transaction {
            assertEquals(1, CurrentGamesTable.selectAll().count())
        }
    }

    @Test
    fun `saveCurrentGame should update existing game`() {
        val gameId = UUID.randomUUID()
        repository.saveCurrentGame(
            CurrentGame(
                id = gameId,
                createdAt = Instant.parse("2026-04-28T10:00:00Z"),
                board = GameBoard(
                    listOf(
                        listOf(0, 0, 0),
                        listOf(0, 0, 0),
                        listOf(0, 0, 0)
                    )
                ),
                state = GameState.WaitingForPlayers,
                players = listOf(
                    GamePlayer(UUID.fromString("00000000-0000-0000-0000-000000000011"), GameSymbol.X),
                ),
            )
        )

        val updatedGame = CurrentGame(
            id = gameId,
            createdAt = Instant.parse("2026-04-28T10:00:00Z"),
            board = GameBoard(
                listOf(
                    listOf(1, 0, 0),
                    listOf(0, 2, 0),
                    listOf(0, 0, 1)
                )
            ),
            state = GameState.PlayerTurn(UUID.fromString("00000000-0000-0000-0000-000000000022")),
            players = listOf(
                GamePlayer(UUID.fromString("00000000-0000-0000-0000-000000000011"), GameSymbol.X),
                GamePlayer(UUID.fromString("00000000-0000-0000-0000-000000000022"), GameSymbol.O),
            )
        )

        repository.saveCurrentGame(updatedGame)

        val savedGame = repository.fetchCurrentGame(gameId)

        assertEquals(updatedGame, savedGame)
        transaction {
            assertEquals(1, CurrentGamesTable.selectAll().count())
        }
    }

    @Test
    fun `fetchCurrentGame should return null for missing game`() {
        val savedGame = repository.fetchCurrentGame(UUID.randomUUID())

        assertNull(savedGame)
    }

    @Test
    fun `fetchCurrentGames should return all saved games`() {
        val firstGame = CurrentGame(
            id = UUID.randomUUID(),
            createdAt = Instant.parse("2026-04-28T10:00:00Z"),
            board = GameBoard(List(3) { List(3) { 0 } }),
            state = GameState.WaitingForPlayers,
            players = listOf(GamePlayer(UUID.randomUUID(), GameSymbol.X))
        )
        val secondGame = CurrentGame(
            id = UUID.randomUUID(),
            createdAt = Instant.parse("2026-04-28T11:00:00Z"),
            board = GameBoard(List(3) { List(3) { 0 } }),
            state = GameState.Draw,
            players = listOf(
                GamePlayer(UUID.randomUUID(), GameSymbol.X),
                GamePlayer(UUID.randomUUID(), GameSymbol.O),
            )
        )

        repository.saveCurrentGame(firstGame)
        repository.saveCurrentGame(secondGame)

        assertEquals(setOf(firstGame, secondGame), repository.fetchCurrentGames().toSet())
    }

    @Test
    fun `fetchCompletedGamesByUserId should return wins and draws ordered by creation date`() {
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000011")
        val winGame = CurrentGame(
            id = UUID.randomUUID(),
            createdAt = Instant.parse("2026-04-28T10:00:00Z"),
            board = GameBoard(List(3) { List(3) { 0 } }),
            state = GameState.PlayerWin(userId),
            players = listOf(GamePlayer(userId, GameSymbol.X), GamePlayer(UUID.randomUUID(), GameSymbol.O))
        )
        val drawGame = CurrentGame(
            id = UUID.randomUUID(),
            createdAt = Instant.parse("2026-04-28T11:00:00Z"),
            board = GameBoard(List(3) { List(3) { 0 } }),
            state = GameState.Draw,
            players = listOf(GamePlayer(userId, GameSymbol.X), GamePlayer(UUID.randomUUID(), GameSymbol.O))
        )
        val lostGame = CurrentGame(
            id = UUID.randomUUID(),
            createdAt = Instant.parse("2026-04-28T12:00:00Z"),
            board = GameBoard(List(3) { List(3) { 0 } }),
            state = GameState.PlayerWin(UUID.randomUUID()),
            players = listOf(GamePlayer(userId, GameSymbol.X), GamePlayer(UUID.randomUUID(), GameSymbol.O))
        )

        repository.saveCurrentGame(winGame)
        repository.saveCurrentGame(drawGame)
        repository.saveCurrentGame(lostGame)

        assertEquals(listOf(drawGame, winGame), repository.fetchCompletedGamesByUserId(userId))
    }

    @Test
    fun `fetchTopPlayers should return users ordered by win ratio`() {
        val firstUserId = UUID.fromString("00000000-0000-0000-0000-000000000011")
        val secondUserId = UUID.fromString("00000000-0000-0000-0000-000000000022")
        val thirdUserId = UUID.fromString("00000000-0000-0000-0000-000000000033")

        repository.saveCurrentGame(
            CurrentGame(
                id = UUID.randomUUID(),
                createdAt = Instant.parse("2026-04-28T10:00:00Z"),
                board = GameBoard(List(3) { List(3) { 0 } }),
                state = GameState.PlayerWin(firstUserId),
                players = listOf(GamePlayer(firstUserId, GameSymbol.X), GamePlayer(secondUserId, GameSymbol.O))
            )
        )
        repository.saveCurrentGame(
            CurrentGame(
                id = UUID.randomUUID(),
                createdAt = Instant.parse("2026-04-28T11:00:00Z"),
                board = GameBoard(List(3) { List(3) { 0 } }),
                state = GameState.PlayerWin(firstUserId),
                players = listOf(GamePlayer(firstUserId, GameSymbol.X), GamePlayer(thirdUserId, GameSymbol.O))
            )
        )
        repository.saveCurrentGame(
            CurrentGame(
                id = UUID.randomUUID(),
                createdAt = Instant.parse("2026-04-28T12:00:00Z"),
                board = GameBoard(List(3) { List(3) { 0 } }),
                state = GameState.Draw,
                players = listOf(GamePlayer(secondUserId, GameSymbol.X), GamePlayer(thirdUserId, GameSymbol.O))
            )
        )

        assertEquals(
            listOf(
                PlayerWinRatio(firstUserId, 2.0),
                PlayerWinRatio(secondUserId, 0.0)
            ),
            repository.fetchTopPlayers(2)
        )
    }
}
