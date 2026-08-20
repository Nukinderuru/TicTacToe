package nukinderuru.route

import nukinderuru.datasource.repository.CurrentGameRepository
import nukinderuru.datasource.repository.UserRepository
import nukinderuru.domain.model.CurrentGame
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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRouteTest {
    private val json = Json

    @Test
    fun `POST signup should register user`() = testApplication {
        testApp()

        val response = client.post("/signup") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "login": "new-user",
                  "password": "new-password"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST signup should reject duplicate login`() = testApplication {
        testApp()

        val response = client.post("/signup") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "login": "$TEST_LOGIN",
                  "password": "$TEST_PASSWORD"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("already exists"))
    }

    @Test
    fun `POST login should return user id for valid credentials`() = testApplication {
        testApp()

        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "login": "$TEST_LOGIN",
                  "password": "$TEST_PASSWORD"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Bearer", body.getValue("type").jsonPrimitive.content)
        assertTrue(body.getValue("accessToken").jsonPrimitive.content.isNotBlank())
        assertTrue(body.getValue("refreshToken").jsonPrimitive.content.isNotBlank())
    }

    @Test
    fun `POST game should require authorization`() = testApplication {
        testApp()

        val response = client.post("/game") {
            contentType(ContentType.Application.Json)
            setBody("""{"opponentType":"computer"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
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

    private val testModule = module {
        single<CurrentGameRepository> { FakeCurrentGameRepository() }
        single<UserRepository> { FakeUserRepository() }
        single<UserService> { DefaultUserService(get()) }
        single { JwtProvider() }
        single<AuthService> { JwtAuthService(get(), get()) }
        single<GameService> { CurrentGameService(get()) }
    }

    private class FakeCurrentGameRepository : CurrentGameRepository {
        private val games = mutableMapOf<UUID, CurrentGame>()

        override fun saveCurrentGame(currentGame: CurrentGame) {
            games[currentGame.id] = currentGame
        }

        override fun fetchCurrentGame(gameId: UUID): CurrentGame? = games[gameId]

        override fun fetchCurrentGames(): List<CurrentGame> = games.values.toList()

        override fun fetchCompletedGamesByUserId(userId: UUID): List<CurrentGame> = games.values.filter { game ->
            game.state == nukinderuru.domain.model.GameState.Draw || game.state == nukinderuru.domain.model.GameState.PlayerWin(userId)
        }

        override fun fetchTopPlayers(limit: Int): List<PlayerWinRatio> = emptyList()
    }

    private class FakeUserRepository : UserRepository {
        private val users = mutableMapOf<UUID, User>()

        init {
            val user = User(
                id = UUID.randomUUID(),
                login = TEST_LOGIN,
                password = TEST_PASSWORD,
            )
            users[user.id] = user
        }

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
        const val TEST_LOGIN = "test-user"
        const val TEST_PASSWORD = "test-password"
    }
}
