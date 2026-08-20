package nukinderuru.route

import nukinderuru.datasource.repository.CurrentGameRepository
import nukinderuru.datasource.repository.UserRepository
import nukinderuru.domain.model.CurrentGame
import nukinderuru.domain.model.GameBoard
import nukinderuru.domain.model.GamePlayer
import nukinderuru.domain.model.GameState
import nukinderuru.domain.model.GameSymbol
import nukinderuru.domain.model.PlayerWinRatio
import nukinderuru.domain.model.User
import nukinderuru.domain.service.AuthService
import nukinderuru.domain.service.CurrentGameService
import nukinderuru.domain.service.DefaultUserService
import nukinderuru.domain.service.GameService
import nukinderuru.domain.service.JwtAuthService
import nukinderuru.domain.service.JwtProvider
import nukinderuru.domain.service.UserService
import nukinderuru.web.module.configureAuthentication
import nukinderuru.web.module.configureRouting
import nukinderuru.web.module.configureSerialization
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRouteTest {
    private val json = Json

    @Test
    fun `POST game should create a computer game`() = testApplication {
        testApp()

        val response = client.post("/game") {
            header(HttpHeaders.Authorization, bearerAuthorizationHeader())
            contentType(ContentType.Application.Json)
            setBody("""{"opponentType":"computer"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("turn", body.getValue("state").jsonObject.getValue("type").jsonPrimitive.content)
        assertEquals(1, body.getValue("players").jsonArray.size)
        assertEquals("O", body.getValue("computerSymbol").jsonPrimitive.content)
    }

    @Test
    fun `GET game should return waiting multiplayer games`() = testApplication {
        testApp()

        client.post("/game") {
            header(HttpHeaders.Authorization, bearerAuthorizationHeader())
            contentType(ContentType.Application.Json)
            setBody("""{"opponentType":"user"}""")
        }

        val response = client.get("/game") {
            header(HttpHeaders.Authorization, bearerAuthorizationHeader(SECOND_LOGIN, SECOND_PASSWORD))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, body.size)
    }

    @Test
    fun `POST game join should join waiting game`() = testApplication {
        testApp()

        val createdResponse = client.post("/game") {
            header(HttpHeaders.Authorization, bearerAuthorizationHeader())
            contentType(ContentType.Application.Json)
            setBody("""{"opponentType":"user"}""")
        }
        val gameId = json.parseToJsonElement(createdResponse.bodyAsText()).jsonObject.getValue("id").jsonPrimitive.content

        val response = client.post("/game/$gameId/join") {
            header(HttpHeaders.Authorization, bearerAuthorizationHeader(SECOND_LOGIN, SECOND_PASSWORD))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(2, body.getValue("players").jsonArray.size)
        assertEquals(TEST_USER_ID.toString(), body.getValue("state").jsonObject.getValue("playerId").jsonPrimitive.content)
    }

    @Test
    fun `POST game id should make a move`() = testApplication {
        testApp()

        val createdResponse = client.post("/game") {
            header(HttpHeaders.Authorization, bearerAuthorizationHeader())
            contentType(ContentType.Application.Json)
            setBody("""{"opponentType":"computer"}""")
        }
        val gameId = json.parseToJsonElement(createdResponse.bodyAsText()).jsonObject.getValue("id").jsonPrimitive.content

        val response = client.post("/game/$gameId") {
            header(HttpHeaders.Authorization, bearerAuthorizationHeader())
            contentType(ContentType.Application.Json)
            setBody("""{"rowIndex":1,"columnIndex":1}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val board = body.getValue("board").jsonObject.getValue("cells").jsonArray.toBoard()
        assertEquals(1, board.flatten().count { it == 1 })
        assertEquals(1, board.flatten().count { it == 2 })
    }

    @Test
    fun `GET game id should return game`() = testApplication {
        testApp()

        val createdResponse = client.post("/game") {
            header(HttpHeaders.Authorization, bearerAuthorizationHeader())
            contentType(ContentType.Application.Json)
            setBody("""{"opponentType":"computer"}""")
        }
        val gameId = json.parseToJsonElement(createdResponse.bodyAsText()).jsonObject.getValue("id").jsonPrimitive.content

        val response = client.get("/game/$gameId") {
            header(HttpHeaders.Authorization, bearerAuthorizationHeader())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(gameId, body.getValue("id").jsonPrimitive.content)
    }

    @Test
    fun `GET user id should return user info`() = testApplication {
        testApp()

        val response = client.get("/user/$TEST_USER_ID") {
            header(HttpHeaders.Authorization, bearerAuthorizationHeader())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(TEST_LOGIN, body.getValue("login").jsonPrimitive.content)
    }

    @Test
    fun `GET game history should return completed games for authorized user`() = testApplication {
        testApp()

        val response = client.get("/game/history") {
            header(HttpHeaders.Authorization, bearerAuthorizationHeader())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, body.size)
        assertEquals("draw", body[0].jsonObject.getValue("state").jsonObject.getValue("type").jsonPrimitive.content)
        assertEquals(TEST_USER_ID.toString(), body[1].jsonObject.getValue("state").jsonObject.getValue("playerId").jsonPrimitive.content)
        assertEquals("2026-04-28T12:00:00Z", body[0].jsonObject.getValue("createdAt").jsonPrimitive.content)
    }

    @Test
    fun `GET leaderboard should return top players`() = testApplication {
        testApp()

        val response = client.get("/game/leaderboard?limit=2") {
            header(HttpHeaders.Authorization, bearerAuthorizationHeader())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, body.size)
        assertEquals(TEST_LOGIN, body[0].jsonObject.getValue("login").jsonPrimitive.content)
        assertEquals(2.0, body[0].jsonObject.getValue("winRatio").jsonPrimitive.content.toDouble())
        assertEquals(SECOND_LOGIN, body[1].jsonObject.getValue("login").jsonPrimitive.content)
    }

    private fun ApplicationTestBuilder.testApp() {
        application {
            configureSerialization()
            install(Koin) {
                modules(testModule)
            }
            configureAuthentication()
            configureRouting()
        }
    }

    private fun JsonArray.toBoard(): List<List<Int>> =
        map { row -> row.jsonArray.map { it.jsonPrimitive.int } }

    private fun bearerAuthorizationHeader(login: String = TEST_LOGIN, password: String = TEST_PASSWORD): String {
        val accessToken = JwtProvider().generateAccessToken(
            User(
                id = when (login) {
                    TEST_LOGIN -> TEST_USER_ID
                    SECOND_LOGIN -> SECOND_USER_ID
                    else -> error("Unsupported test login: $login")
                },
                login = login,
                password = password
            )
        )
        return "Bearer $accessToken"
    }

    private val testModule = module {
        single<CurrentGameRepository> { FakeCurrentGameRepository() }
        single<UserRepository> { FakeUserRepository() }
        single<UserService> { DefaultUserService(get()) }
        single { JwtProvider() }
        single<AuthService> { JwtAuthService(get(), get()) }
        single<GameService> { CurrentGameService(get()) }
    }

    private class FakeCurrentGameRepository : CurrentGameRepository {
        private val games = mutableMapOf(
            COMPLETED_DRAW_GAME.id to COMPLETED_DRAW_GAME,
            COMPLETED_WIN_GAME.id to COMPLETED_WIN_GAME,
            COMPLETED_LOSS_GAME.id to COMPLETED_LOSS_GAME,
        )

        override fun saveCurrentGame(currentGame: CurrentGame) {
            games[currentGame.id] = currentGame
        }

        override fun fetchCurrentGame(gameId: UUID): CurrentGame? = games[gameId]

        override fun fetchCurrentGames(): List<CurrentGame> = games.values.toList()

        override fun fetchCompletedGamesByUserId(userId: UUID): List<CurrentGame> = games.values
            .filter { game -> game.state == GameState.Draw || game.state == GameState.PlayerWin(userId) }
            .sortedByDescending { it.createdAt }

        override fun fetchTopPlayers(limit: Int): List<PlayerWinRatio> = listOf(
            PlayerWinRatio(TEST_USER_ID, 2.0),
            PlayerWinRatio(SECOND_USER_ID, 0.5)
        ).take(limit)
    }

    private class FakeUserRepository : UserRepository {
        private val users = mutableMapOf(
            TEST_USER_ID to User(TEST_USER_ID, TEST_LOGIN, TEST_PASSWORD),
            SECOND_USER_ID to User(SECOND_USER_ID, SECOND_LOGIN, SECOND_PASSWORD)
        )

        override fun saveUser(user: User): Boolean {
            if (users.values.any { it.login == user.login }) {
                return false
            }

            users[user.id] = user
            return true
        }

        override fun fetchUserByLogin(login: String): User? = users.values.firstOrNull { it.login == login }

        override fun fetchUserById(userId: UUID): User? = users[userId]
    }

    private companion object {
        val TEST_USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000011")
        val SECOND_USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000022")
        val COMPLETED_DRAW_GAME: CurrentGame = CurrentGame(
            id = UUID.fromString("00000000-0000-0000-0000-000000000101"),
            createdAt = Instant.parse("2026-04-28T12:00:00Z"),
            board = GameBoard(List(3) { List(3) { 0 } }),
            state = GameState.Draw,
            players = listOf(
                GamePlayer(TEST_USER_ID, GameSymbol.X),
                GamePlayer(SECOND_USER_ID, GameSymbol.O)
            )
        )
        val COMPLETED_WIN_GAME: CurrentGame = CurrentGame(
            id = UUID.fromString("00000000-0000-0000-0000-000000000102"),
            createdAt = Instant.parse("2026-04-28T11:00:00Z"),
            board = GameBoard(List(3) { List(3) { 0 } }),
            state = GameState.PlayerWin(TEST_USER_ID),
            players = listOf(
                GamePlayer(TEST_USER_ID, GameSymbol.X),
                GamePlayer(SECOND_USER_ID, GameSymbol.O)
            )
        )
        val COMPLETED_LOSS_GAME: CurrentGame = CurrentGame(
            id = UUID.fromString("00000000-0000-0000-0000-000000000103"),
            createdAt = Instant.parse("2026-04-28T10:00:00Z"),
            board = GameBoard(List(3) { List(3) { 0 } }),
            state = GameState.PlayerWin(SECOND_USER_ID),
            players = listOf(
                GamePlayer(TEST_USER_ID, GameSymbol.X),
                GamePlayer(SECOND_USER_ID, GameSymbol.O)
            )
        )
        const val TEST_LOGIN = "test-user"
        const val TEST_PASSWORD = "test-password"
        const val SECOND_LOGIN = "other-user"
        const val SECOND_PASSWORD = "other-password"
    }
}
