package nukinderuru.service

import nukinderuru.domain.model.User
import nukinderuru.domain.service.JwtAuthService
import nukinderuru.domain.service.JwtProvider
import nukinderuru.domain.service.UserService
import nukinderuru.web.model.JwtRequest
import nukinderuru.web.model.RefreshJwtRequest
import nukinderuru.web.model.SignUpRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.util.UUID

class JwtAuthServiceTest {
    private val jwtProvider = JwtProvider()

    @Test
    fun `register should delegate to user service`() {
        val userService = FakeUserService().apply { registerResult = false }
        val authService = JwtAuthService(userService, jwtProvider)

        val request = SignUpRequest(login = "new-user", password = "new-password")

        assertFalse(authService.register(request))
        assertEquals(request, userService.registeredRequests.single())
    }

    @Test
    fun `login should return null when user does not exist`() {
        val authService = JwtAuthService(FakeUserService(), jwtProvider)

        val response = authService.login(JwtRequest(login = "missing", password = "password"))

        assertNull(response)
    }

    @Test
    fun `login should return null when password is incorrect`() {
        val user = testUser()
        val authService = JwtAuthService(FakeUserService(user), jwtProvider)

        val response = authService.login(JwtRequest(login = user.login, password = "wrong-password"))

        assertNull(response)
    }

    @Test
    fun `login should return bearer access and refresh tokens for valid credentials`() {
        val user = testUser()
        val authService = JwtAuthService(FakeUserService(user), jwtProvider)

        val response = authService.login(JwtRequest(login = user.login, password = user.password))

        assertNotNull(response)
        assertEquals("Bearer", response.type)
        assertTrue(jwtProvider.validateAccessToken(response.accessToken))
        assertTrue(jwtProvider.validateRefreshToken(response.refreshToken))
        assertEquals(user.id, jwtProvider.getUserId(response.accessToken))
        assertEquals(user.id, jwtProvider.getUserId(response.refreshToken))
    }

    @Test
    fun `refreshAccessToken should return null for invalid refresh token`() {
        val authService = JwtAuthService(FakeUserService(), jwtProvider)

        val response = authService.refreshAccessToken(RefreshJwtRequest("bad-token"))

        assertNull(response)
    }

    @Test
    fun `refreshAccessToken should return null when token user does not exist`() {
        val authService = JwtAuthService(FakeUserService(), jwtProvider)
        val refreshToken = jwtProvider.generateRefreshToken(testUser())

        val response = authService.refreshAccessToken(RefreshJwtRequest(refreshToken))

        assertNull(response)
    }

    @Test
    fun `refreshAccessToken should issue new access token and keep refresh token`() {
        val user = testUser()
        val authService = JwtAuthService(FakeUserService(user), jwtProvider)
        val refreshToken = jwtProvider.generateRefreshToken(user)

        val response = authService.refreshAccessToken(RefreshJwtRequest(refreshToken))

        assertNotNull(response)
        assertEquals("Bearer", response.type)
        assertTrue(jwtProvider.validateAccessToken(response.accessToken))
        assertEquals(refreshToken, response.refreshToken)
        assertEquals(user.id, jwtProvider.getUserId(response.accessToken))
    }

    @Test
    fun `refreshRefreshToken should return null for invalid refresh token`() {
        val authService = JwtAuthService(FakeUserService(), jwtProvider)

        val response = authService.refreshRefreshToken(RefreshJwtRequest("bad-token"))

        assertNull(response)
    }

    @Test
    fun `refreshRefreshToken should return null when token user does not exist`() {
        val authService = JwtAuthService(FakeUserService(), jwtProvider)
        val refreshToken = jwtProvider.generateRefreshToken(testUser())

        val response = authService.refreshRefreshToken(RefreshJwtRequest(refreshToken))

        assertNull(response)
    }

    @Test
    fun `refreshRefreshToken should issue new access and refresh tokens`() {
        val user = testUser()
        val authService = JwtAuthService(FakeUserService(user), jwtProvider)
        val refreshToken = jwtProvider.generateRefreshToken(user)

        val response = authService.refreshRefreshToken(RefreshJwtRequest(refreshToken))

        assertNotNull(response)
        assertEquals("Bearer", response.type)
        assertTrue(jwtProvider.validateAccessToken(response.accessToken))
        assertTrue(jwtProvider.validateRefreshToken(response.refreshToken))
        assertEquals(user.id, jwtProvider.getUserId(response.accessToken))
        assertEquals(user.id, jwtProvider.getUserId(response.refreshToken))
    }

    private fun testUser(): User = User(
        id = UUID.fromString("00000000-0000-0000-0000-000000000321"),
        login = "test-user",
        password = "test-password"
    )

    private class FakeUserService(vararg existingUsers: User) : UserService {
        private val usersById = existingUsers.associateBy { it.id }.toMutableMap()
        private val usersByLogin = existingUsers.associateBy { it.login }.toMutableMap()

        var registerResult: Boolean = true
        val registeredRequests = mutableListOf<SignUpRequest>()

        override fun register(signUpRequest: SignUpRequest): Boolean {
            registeredRequests += signUpRequest
            return registerResult
        }

        override fun getUserByLogin(login: String): User? = usersByLogin[login]

        override fun getUserById(userId: UUID): User? = usersById[userId]
    }
}
